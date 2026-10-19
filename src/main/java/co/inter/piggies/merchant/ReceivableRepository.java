package co.inter.piggies.merchant;

import io.micronaut.data.annotation.Repository;
import io.micronaut.data.repository.CrudRepository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReceivableRepository extends CrudRepository<Receivable, UUID> {

    List<Receivable> findByMerchantId(String merchantId);
}
