package co.inter.piggies.merchant;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class MerchantValidationTest {

    @Test
    void activeMerchantIsValid() {
        assertThat(MerchantService.failureReason(Optional.of(new Merchant("x", "Loja X", true)))).isNull();
    }

    @Test
    void inactiveMerchantIsInvalid() {
        assertThat(MerchantService.failureReason(Optional.of(new Merchant("x", "Loja X", false))))
                .isEqualTo(MerchantService.MERCHANT_INACTIVE);
    }

    @Test
    void unknownMerchantIsInvalid() {
        assertThat(MerchantService.failureReason(Optional.empty())).isEqualTo(MerchantService.MERCHANT_NOT_FOUND);
    }
}
