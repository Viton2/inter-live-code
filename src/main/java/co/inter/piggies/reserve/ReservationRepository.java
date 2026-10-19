package co.inter.piggies.reserve;

import io.micronaut.data.annotation.Repository;
import io.micronaut.data.repository.CrudRepository;

import java.util.UUID;

@Repository
public interface ReservationRepository extends CrudRepository<Reservation, UUID> {
}
