package co.inter.piggies.coordinator;

import org.junit.jupiter.api.Test;

import static co.inter.piggies.coordinator.Payment.Transition.CONFIRMED;
import static co.inter.piggies.coordinator.Payment.Transition.NONE;
import static co.inter.piggies.coordinator.Payment.Transition.REJECTED;
import static org.assertj.core.api.Assertions.assertThat;

class PaymentTest {

    private Payment newPayment() {
        return Payment.initiate("cliente-a", new QrCode("merchant-x", 100));
    }

    @Test
    void startsPending() {
        Payment p = newPayment();

        assertThat(p.getStatus()).isEqualTo(Payment.Status.PENDING);
        assertThat(p.getAmount()).isEqualTo(100);
    }

    @Test
    void confirmsOnlyWhenReserveAndMerchantSucceed_reserveFirst() {
        Payment p = newPayment();

        assertThat(p.onReserveResult(true, null)).isEqualTo(NONE);
        assertThat(p.getStatus()).isEqualTo(Payment.Status.PENDING);
        assertThat(p.onMerchantResult(true, null)).isEqualTo(CONFIRMED);
        assertThat(p.getStatus()).isEqualTo(Payment.Status.CONFIRMED);
    }

    @Test
    void confirmsOnlyWhenReserveAndMerchantSucceed_merchantFirst() {
        Payment p = newPayment();

        assertThat(p.onMerchantResult(true, null)).isEqualTo(NONE);
        assertThat(p.onReserveResult(true, null)).isEqualTo(CONFIRMED);
    }

    @Test
    void rejectsAsSoonAsReserveFails() {
        Payment p = newPayment();

        assertThat(p.onReserveResult(false, "INSUFFICIENT_BALANCE")).isEqualTo(REJECTED);
        assertThat(p.getReason()).isEqualTo("INSUFFICIENT_BALANCE");
        // resultado posterior do merchant nao muda o desfecho
        assertThat(p.onMerchantResult(true, null)).isEqualTo(NONE);
        assertThat(p.getStatus()).isEqualTo(Payment.Status.REJECTED);
    }

    @Test
    void reservationArrivingAfterRejectionIsOrphan() {
        Payment p = newPayment();

        assertThat(p.onMerchantResult(false, "MERCHANT_INACTIVE")).isEqualTo(REJECTED);
        assertThat(p.onReserveResult(true, null)).isEqualTo(NONE);
        assertThat(p.hasOrphanReservation()).isTrue();
        assertThat(p.getReason()).isEqualTo("MERCHANT_INACTIVE");
    }

    @Test
    void ignoresDuplicateResults() {
        Payment p = newPayment();
        p.onReserveResult(true, null);

        assertThat(p.onReserveResult(false, "X")).isEqualTo(NONE);
        assertThat(p.getReserveStatus()).isEqualTo(Payment.ReserveStatus.RESERVED);
    }
}
