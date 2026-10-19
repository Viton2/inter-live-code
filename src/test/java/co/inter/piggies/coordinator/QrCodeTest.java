package co.inter.piggies.coordinator;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QrCodeTest {

    @Test
    void parsesMerchantAndAmount() {
        QrCode qr = QrCode.parse("piggies://pay?merchant=merchant-x&amount=100");

        assertThat(qr.merchantId()).isEqualTo("merchant-x");
        assertThat(qr.amount()).isEqualTo(100);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
            "https://pay?merchant=x&amount=1",      // esquema errado
            "piggies://pay",                         // sem parametros
            "piggies://pay?amount=10",               // sem merchant
            "piggies://pay?merchant=x&amount=1.5",   // valor nao inteiro
            "piggies://pay?merchant=x&amount=0",     // valor zero
            "piggies://pay?merchant=x&amount=-3",    // valor negativo
            "piggies://pay?merchant=x&amount=abc"
    })
    void rejectsInvalidQrCodes(String raw) {
        assertThatThrownBy(() -> QrCode.parse(raw)).isInstanceOf(QrCode.InvalidQrCodeException.class);
    }
}
