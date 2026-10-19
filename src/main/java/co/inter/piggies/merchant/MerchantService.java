package co.inter.piggies.merchant;

import co.inter.piggies.events.MerchantValidationResult;
import co.inter.piggies.events.PaymentConfirmed;
import co.inter.piggies.events.PaymentInitiated;
import io.micronaut.transaction.annotation.Transactional;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Singleton
@RequiredArgsConstructor
public class MerchantService {

    static final String MERCHANT_NOT_FOUND = "MERCHANT_NOT_FOUND";
    static final String MERCHANT_INACTIVE = "MERCHANT_INACTIVE";

    private final MerchantRepository merchants;
    private final ReceivableRepository receivables;
    private final MerchantEventPublisher publisher;

    @Transactional
    public Merchant upsert(Merchant merchant) {
        return merchants.existsById(merchant.getMerchantId()) ? merchants.update(merchant) : merchants.save(merchant);
    }

    @Transactional
    public List<Receivable> receivables(String merchantId) {
        return receivables.findByMerchantId(merchantId);
    }

    @Transactional
    public void validate(PaymentInitiated event) {
        String failure = failureReason(merchants.findById(event.merchantId()));
        publisher.validationResult(event.paymentId(), new MerchantValidationResult(UUID.randomUUID(), Instant.now(),
                event.paymentId(),
                failure == null ? MerchantValidationResult.Status.VALID : MerchantValidationResult.Status.INVALID,
                failure));
    }

    static String failureReason(Optional<Merchant> merchant) {
        if (merchant.isEmpty()) {
            return MERCHANT_NOT_FOUND;
        }
        return merchant.get().isActive() ? null : MERCHANT_INACTIVE;
    }

    /** Pagamento confirmado: registra o credito. Idempotente por paymentId. */
    @Transactional
    public void credit(PaymentConfirmed event) {
        if (receivables.existsById(event.paymentId())) {
            return;
        }
        receivables.save(new Receivable(event.paymentId(), event.merchantId(), event.amount(), Instant.now()));
        log.info("Creditados {} Piggies a {} (pagamento {})", event.amount(), event.merchantId(), event.paymentId());
    }
}
