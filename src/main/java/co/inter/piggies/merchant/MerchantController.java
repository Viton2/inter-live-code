package co.inter.piggies.merchant;

import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** contracts/openapi/piggies-merchant-service.yaml */
@Controller("/merchants")
@ExecuteOn(TaskExecutors.BLOCKING)
@RequiredArgsConstructor
public class MerchantController {

    private final MerchantService service;

    @Post
    MerchantDto upsert(@Body @Valid MerchantDto request) {
        Merchant saved = service.upsert(new Merchant(request.merchantId(), request.name(), request.active()));
        return new MerchantDto(saved.getMerchantId(), saved.getName(), saved.isActive());
    }

    @Get("/{merchantId}/receivables")
    List<ReceivableDto> receivables(String merchantId) {
        return service.receivables(merchantId).stream()
                .map(r -> new ReceivableDto(r.getPaymentId(), r.getMerchantId(), r.getAmount(), r.getCreatedAt()))
                .toList();
    }

    @Serdeable
    public record MerchantDto(@NotBlank String merchantId, @NotBlank String name, boolean active) {
    }

    @Serdeable
    public record ReceivableDto(UUID paymentId, String merchantId, long amount, Instant createdAt) {
    }
}
