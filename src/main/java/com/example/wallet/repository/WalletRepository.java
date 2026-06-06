package com.example.wallet.repository;

import com.example.wallet.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.util.UUID;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, UUID> {

    @Modifying
    @Query("UPDATE Wallet w SET w.balance = w.balance + :amount, w.version = w.version + 1 " +
           "WHERE w.id = :id AND (w.balance + :amount) >= 0")
    int updateBalanceAtomic(@Param("id") UUID id, @Param("amount") BigDecimal amount);
}