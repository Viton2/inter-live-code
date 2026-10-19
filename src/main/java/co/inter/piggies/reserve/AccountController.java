package co.inter.piggies.reserve;

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
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;

/** contracts/openapi/piggies-reserve-service.yaml */
@Controller("/accounts")
@ExecuteOn(TaskExecutors.BLOCKING)
@RequiredArgsConstructor
public class AccountController {

    private final ReserveService service;

    @Post("/{customerId}/deposits")
    AccountResponse deposit(String customerId, @Body @Valid DepositRequest request) {
        return AccountResponse.of(service.deposit(customerId, request.amount()));
    }

    @Get("/{customerId}")
    AccountResponse get(String customerId) {
        return service.find(customerId)
                .map(AccountResponse::of)
                .orElseThrow(() -> new HttpStatusException(HttpStatus.NOT_FOUND, "Conta nao encontrada"));
    }

    @Serdeable
    public record DepositRequest(@Positive long amount) {
    }

    @Serdeable
    public record AccountResponse(String customerId, long balance, long reserved, long available) {
        static AccountResponse of(Account a) {
            return new AccountResponse(a.getCustomerId(), a.getBalance(), a.getReserved(), a.getAvailable());
        }
    }
}
