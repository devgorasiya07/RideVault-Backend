package com.ridevault.repository;

import com.ridevault.entity.Driver;
import com.ridevault.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, Long> {

    Optional<Wallet> findByDriver(Driver driver);

    @Query("SELECT w FROM Wallet w JOIN FETCH w.driver d WHERE d.driverId = :driverId")
    Optional<Wallet> findByDriverId(@Param("driverId") Long driverId);

    // Pessimistic write lock for critical settlement operations to prevent balance drift
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM Wallet w WHERE w.walletId = :walletId")
    Optional<Wallet> findByIdWithLock(@Param("walletId") Long walletId);

    // Single-query pessimistic lock by driver ID — eliminates double fetch
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM Wallet w JOIN FETCH w.driver d WHERE d.driverId = :driverId")
    Optional<Wallet> findByDriverIdWithLock(@Param("driverId") Long driverId);
}
