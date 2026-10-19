package co.inter.piggies.events;

/** Nomes dos topicos Kafka. Schemas em contracts/events. */
public final class Topics {

    public static final String PAYMENT_INITIATED = "piggies.payment.initiated";
    public static final String PAYMENT_CONFIRMED = "piggies.payment.confirmed";
    public static final String PAYMENT_REJECTED = "piggies.payment.rejected";
    public static final String RESERVE_RESULT = "piggies.reserve.result";
    public static final String MERCHANT_VALIDATION_RESULT = "piggies.merchant.validation.result";

    private Topics() {
    }
}
