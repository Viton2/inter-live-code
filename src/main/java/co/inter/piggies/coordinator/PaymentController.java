package co.inter.piggies.coordinator;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.exceptions.HttpStatusException;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;

import java.net.URI;
import java.util.UUID;

/**
 * Borda assincrona (contracts/openapi/piggies-payment-coordinator.yaml): o POST so registra a
 * intencao e responde 202; o resultado e acompanhado via GET.
 */
@Controller("/payments")
@ExecuteOn(TaskExecutors.BLOCKING)
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentCoordinatorService service;

    @Post
    HttpResponse<PaymentResponse> initiate(@Body @Valid InitiatePaymentRequest request) {
        QrCode qrCode;
        try {
            qrCode = QrCode.parse(request.qrCode());
        } catch (QrCode.InvalidQrCodeException e) {
            throw new HttpStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
        Payment payment = service.initiate(request.customerId(), qrCode);
        return HttpResponse.accepted(URI.create("/payments/" + payment.getId()))
                .body(PaymentResponse.of(payment));
    }

    @Get("/{paymentId}")
    PaymentResponse get(UUID paymentId) {
        return service.find(paymentId)
                .map(PaymentResponse::of)
                .orElseThrow(() -> new HttpStatusException(HttpStatus.NOT_FOUND, "Pagamento nao encontrado"));
    }

    @Serdeable
    public record InitiatePaymentRequest(@NotBlank String customerId, @NotBlank String qrCode) {
    }

    @Serdeable
    public record PaymentResponse(UUID paymentId, String customerId, String merchantId, long amount,
                                  Payment.Status status, Payment.ReserveStatus reserveStatus,
                                  Payment.MerchantStatus merchantStatus, String reason) {
        static PaymentResponse of(Payment p) {
            return new PaymentResponse(p.getId(), p.getCustomerId(), p.getMerchantId(), p.getAmount(),
                    p.getStatus(), p.getReserveStatus(), p.getMerchantStatus(), p.getReason());
        }
    }
}
