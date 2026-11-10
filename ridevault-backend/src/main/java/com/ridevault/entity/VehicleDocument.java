package com.ridevault.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/**
 * Entity 8/13 — Legal/insurance compliance records for a vehicle.
 *
 * Separated from vehicles table to satisfy BCNF: insurance attributes
 * (policy_no, expiry_date) are functionally dependent on the insurance
 * policy itself, not on the vehicle identity.
 *
 * BCNF: doc_id → all attributes; vehicle_id is a candidate key (1:1).
 */
@Entity
@Table(name = "vehicle_documents")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "doc_id")
    private Long docId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false, unique = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Vehicle vehicle;

    @Column(name = "insurance_policy_no", nullable = false, length = 50)
    private String insurancePolicyNo;

    @Column(name = "insurance_expiry_date", nullable = false)
    private LocalDate insuranceExpiryDate;

    @Column(name = "rc_number", length = 50)
    private String rcNumber;
}
