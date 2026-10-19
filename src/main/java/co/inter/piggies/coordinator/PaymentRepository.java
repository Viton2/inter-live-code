package co.inter.piggies.coordinator;

import io.micronaut.data.annotation.Repository;
import io.micronaut.data.repository.CrudRepository;

import java.util.UUID;

@Repository
public interface PaymentRepository extends CrudRepository<Payment, UUID> {
}
