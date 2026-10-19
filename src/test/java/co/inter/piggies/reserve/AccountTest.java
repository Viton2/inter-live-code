package co.inter.piggies.reserve;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccountTest {

    private Account accountWith(long balance) {
        Account a = new Account("cliente-a");
        a.deposit(balance);
        return a;
    }

    @Test
    void reserveReducesAvailableButNotBalance() {
        Account a = accountWith(500);

        assertThat(a.reserve(100)).isTrue();
        assertThat(a.getBalance()).isEqualTo(500);
        assertThat(a.getReserved()).isEqualTo(100);
        assertThat(a.getAvailable()).isEqualTo(400);
    }

    @Test
    void cannotReserveMoreThanAvailable() {
        Account a = accountWith(150);
        a.reserve(100);

        assertThat(a.reserve(51)).isFalse();
        assertThat(a.getReserved()).isEqualTo(100);
    }

    @Test
    void captureDebitsReservedAmount() {
        Account a = accountWith(500);
        a.reserve(100);

        a.capture(100);

        assertThat(a.getBalance()).isEqualTo(400);
        assertThat(a.getReserved()).isZero();
    }

    @Test
    void releaseReturnsAmountToAvailable() {
        Account a = accountWith(500);
        a.reserve(100);

        a.release(100);

        assertThat(a.getBalance()).isEqualTo(500);
        assertThat(a.getAvailable()).isEqualTo(500);
    }

    @Test
    void cannotCaptureWithoutReservation() {
        Account a = accountWith(500);

        assertThatThrownBy(() -> a.capture(100)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rejectsNonPositiveAmounts() {
        Account a = accountWith(500);

        assertThatThrownBy(() -> a.reserve(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> a.deposit(-1)).isInstanceOf(IllegalArgumentException.class);
    }
}
