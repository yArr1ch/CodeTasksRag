package com.pet.proj.user.persistence;

import jakarta.persistence.LockModeType;
import org.jspecify.annotations.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {
    Optional<UserEntity> findByKeycloakSubject(String keycloakSubject);

    @Override
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<UserEntity> findById(@NonNull UUID id);

    List<UserEntity> findTop20ByOrderByPointsDescTotalEarnedDescDisplayNameAsc();
}
