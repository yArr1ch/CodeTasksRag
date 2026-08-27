package com.pet.proj.user.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "app_users")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "keycloak_subject", nullable = false, unique = true, length = 128)
    private String keycloakSubject;

    @Column(nullable = false, length = 160)
    @Setter
    private String displayName;

    @Column(nullable = false)
    @Builder.Default
    private int points = 0;

    @Column(nullable = false)
    @Builder.Default
    private int totalEarned = 0;

    @Version
    @Column(nullable = false)
    private int version;

    @Column(nullable = false, updatable = false)
    @CreatedDate
    private Instant createdAt;

    @Column(nullable = false)
    @LastModifiedDate
    private Instant updatedAt;

    public void credit(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("credit amount must be positive");
        }
        points += amount;
        totalEarned += amount;
    }

    public void debit(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("debit amount must be positive");
        }
        if (points < amount) {
            throw new InsufficientPointsException();
        }
        points -= amount;
    }
}
