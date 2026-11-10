package com.ridevault.repository;

import com.ridevault.entity.Ride;
import com.ridevault.entity.Wallet;
import com.ridevault.entity.WalletTransaction;
import com.ridevault.entity.enums.MovementType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {

    List<WalletTransaction> findByWalletOrderByCreatedAtDesc(Wallet wallet);

    Page<WalletTransaction> findByWallet(Wallet wallet, Pageable pageable);

    Page<WalletTransaction> findByWalletAndMovementType(Wallet wallet, MovementType movementType, Pageable pageable);

    List<WalletTransaction> findByRide(Ride ride);
}
