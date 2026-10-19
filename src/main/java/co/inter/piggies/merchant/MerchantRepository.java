package co.inter.piggies.merchant;

import io.micronaut.data.annotation.Repository;
import io.micronaut.data.repository.CrudRepository;

@Repository
public interface MerchantRepository extends CrudRepository<Merchant, String> {
}
