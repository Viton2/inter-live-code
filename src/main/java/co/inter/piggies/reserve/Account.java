package co.inter.piggies.reserve;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Conta Piggies do cliente: saldo total e quanto dele esta reservado. */
@Entity
@Table(name = "reserve_account")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Account {

    @Id
    private String customerId;

    private long balance;

    private long reserved;

    public Account(String customerId) {
        this.customerId = customerId;
    }

    public long getAvailable() {
        return balance - reserved;
    }

    public void deposit(long amount) {
        requirePositive(amount);
        balance += amount;
    }

    /** @return false se nao ha saldo disponivel suficiente */
    public boolean reserve(long amount) {
        requirePositive(amount);
        if (getAvailable() < amount) {
            return false;
        }
        reserved += amount;
        return true;
    }

    /** Efetiva o debito de um valor previamente reservado. */
    public void capture(long amount) {
        requireReserved(amount);
        reserved -= amount;
        balance -= amount;
    }

    public void release(long amount) {
        requireReserved(amount);
        reserved -= amount;
    }

    private void requireReserved(long amount) {
        requirePositive(amount);
        if (reserved < amount) {
            throw new IllegalStateException("Valor reservado insuficiente na conta " + customerId);
        }
    }

    private static void requirePositive(long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Valor deve ser maior que zero");
        }
    }
}
