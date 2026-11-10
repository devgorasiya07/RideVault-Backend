package com.ridevault.repository;

import com.ridevault.entity.User;
import com.ridevault.entity.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByPhoneNo(String phoneNo);

    boolean existsByEmail(String email);

    boolean existsByPhoneNo(String phoneNo);

    long countByRole(Role role);
}
