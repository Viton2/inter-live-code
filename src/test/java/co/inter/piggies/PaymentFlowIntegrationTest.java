package co.inter.piggies;

import co.inter.piggies.coordinator.Payment;
import co.inter.piggies.coordinator.PaymentController.InitiatePaymentRequest;
import co.inter.piggies.coordinator.PaymentController.PaymentResponse;
import co.inter.piggies.merchant.MerchantController.MerchantDto;
import co.inter.piggies.merchant.MerchantController.ReceivableDto;
import co.inter.piggies.reserve.AccountController.AccountResponse;
import co.inter.piggies.reserve.AccountController.DepositRequest;
import co.inter.piggies.support.AbstractIntegrationTest;
import io.micronaut.core.type.Argument;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

@MicronautTest
class PaymentFlowIntegrationTest extends AbstractIntegrationTest {

    @Inject
    @Client("/")
    HttpClient http;

    @Test
    void confirmsPayment_debitsCustomerAndCreditsMerchant() {
        String customer = unique("cliente-a");
        String merchant = unique("merchant-x");
        deposit(customer, 500);
        registerMerchant(merchant, true);

        HttpResponse<PaymentResponse> accepted = initiate(customer, merchant, 100);

        assertThat(accepted.getStatus().getCode()).isEqualTo(202);
        assertThat(accepted.body().status()).isEqualTo(Payment.Status.PENDING);
        UUID paymentId = accepted.body().paymentId();
        assertThat((Object) accepted.getHeaders().get("Location")).isEqualTo("/payments/" + paymentId);

        assertThat(awaitFinalStatus(paymentId).status()).isEqualTo(Payment.Status.CONFIRMED);
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            AccountResponse account = account(customer);
            assertThat(account.balance()).isEqualTo(400);
            assertThat(account.reserved()).isZero();
            assertThat(receivables(merchant)).extracting(ReceivableDto::amount).containsExactly(100L);
        });
    }

    @Test
    void rejectsPayment_whenBalanceIsInsufficient() {
        String customer = unique("cliente-b");
        String merchant = unique("merchant-y");
        deposit(customer, 50);
        registerMerchant(merchant, true);

        UUID paymentId = initiate(customer, merchant, 100).body().paymentId();

        PaymentResponse payment = awaitFinalStatus(paymentId);
        assertThat(payment.status()).isEqualTo(Payment.Status.REJECTED);
        assertThat(payment.reason()).isEqualTo("INSUFFICIENT_BALANCE");
        assertThat(account(customer).balance()).isEqualTo(50);
        assertThat(receivables(merchant)).isEmpty();
    }

    @Test
    void rejectsPayment_andReleasesReservation_whenMerchantIsInactive() {
        String customer = unique("cliente-c");
        String merchant = unique("merchant-z");
        deposit(customer, 500);
        registerMerchant(merchant, false);

        UUID paymentId = initiate(customer, merchant, 100).body().paymentId();

        PaymentResponse payment = awaitFinalStatus(paymentId);
        assertThat(payment.status()).isEqualTo(Payment.Status.REJECTED);
        assertThat(payment.reason()).isEqualTo("MERCHANT_INACTIVE");
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            AccountResponse account = account(customer);
            assertThat(account.balance()).isEqualTo(500);
            assertThat(account.reserved()).isZero();
        });
        assertThat(receivables(merchant)).isEmpty();
    }

    @Test
    void returnsBadRequest_forInvalidQrCode() {
        var request = HttpRequest.POST("/payments", new InitiatePaymentRequest("cliente", "piggies://pay?merchant=x&amount=0"));

        assertThatThrownBy(() -> http.toBlocking().exchange(request, PaymentResponse.class))
                .isInstanceOfSatisfying(HttpClientResponseException.class,
                        e -> assertThat(e.getStatus().getCode()).isEqualTo(400));
    }

    @Test
    void returnsNotFound_forUnknownPayment() {
        assertThatThrownBy(() -> http.toBlocking().retrieve("/payments/" + UUID.randomUUID(), PaymentResponse.class))
                .isInstanceOfSatisfying(HttpClientResponseException.class,
                        e -> assertThat(e.getStatus().getCode()).isEqualTo(404));
    }

    private HttpResponse<PaymentResponse> initiate(String customer, String merchant, long amount) {
        String qr = "piggies://pay?merchant=" + merchant + "&amount=" + amount;
        return http.toBlocking().exchange(HttpRequest.POST("/payments", new InitiatePaymentRequest(customer, qr)),
                PaymentResponse.class);
    }

    private PaymentResponse awaitFinalStatus(UUID paymentId) {
        return await().atMost(Duration.ofSeconds(30)).pollInterval(Duration.ofMillis(200))
                .until(() -> http.toBlocking().retrieve("/payments/" + paymentId, PaymentResponse.class),
                        p -> p.status() != Payment.Status.PENDING);
    }

    private void deposit(String customer, long amount) {
        http.toBlocking().exchange(HttpRequest.POST("/accounts/" + customer + "/deposits", new DepositRequest(amount)));
    }

    private AccountResponse account(String customer) {
        return http.toBlocking().retrieve("/accounts/" + customer, AccountResponse.class);
    }

    private void registerMerchant(String merchant, boolean active) {
        http.toBlocking().exchange(HttpRequest.POST("/merchants", new MerchantDto(merchant, "Loja " + merchant, active)));
    }

    private List<ReceivableDto> receivables(String merchant) {
        return http.toBlocking().retrieve(HttpRequest.GET("/merchants/" + merchant + "/receivables"),
                Argument.listOf(ReceivableDto.class));
    }

    private static String unique(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
    }
}
