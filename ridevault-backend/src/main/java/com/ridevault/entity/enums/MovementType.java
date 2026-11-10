package com.ridevault.entity.enums;

/**
 * Wallet transaction movement direction for double-entry ledger.
 *
 * Resume: "wallet settlements using Spring Core (DI) and @Transactional
 *          (ACID rollbacks)"
 */
public enum MovementType {
    CREDIT,
    DEBIT
}
