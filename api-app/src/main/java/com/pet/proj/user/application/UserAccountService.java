package com.pet.proj.user.application;

import com.pet.proj.user.api.LeaderboardEntryResponse;
import com.pet.proj.user.api.PointSummaryResponse;
import com.pet.proj.user.api.PointTransactionResponse;
import com.pet.proj.user.api.UserProfileResponse;
import com.pet.proj.user.persistence.UserEntity;
import com.pet.proj.user.persistence.UserRepository;
import com.pet.proj.points.persistence.PointTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class UserAccountService {
    private final UserRepository userRepository;
    private final PointTransactionRepository pointRepository;

    public UserEntity current() {
        var jwt = currentJwt();
        var subject = jwt.getSubject();
        var displayName = displayName(jwt);
        var existing = userRepository.findByKeycloakSubject(subject);
        if (existing.isEmpty()) {
            return userRepository.save(UserEntity.builder()
                    .keycloakSubject(subject)
                    .displayName(displayName)
                    .build());
        }
        var user = existing.get();
        if (!user.getDisplayName().equals(displayName)) {
            user.setDisplayName(displayName);
            return userRepository.save(user);
        }
        return user;
    }

    @Transactional
    public UserProfileResponse profile() {
        var user = current();
        return new UserProfileResponse(user.getId(), user.getDisplayName(), isAdmin(),
                user.getPoints(), user.getTotalEarned());
    }

    @Transactional
    public PointSummaryResponse points() {
        var user = current();
        var history = pointRepository.findTop50ByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(transaction -> new PointTransactionResponse(
                        transaction.getType(), transaction.getAmount(), transaction.getTaskId(),
                        transaction.getSubmissionId(), transaction.getCreatedAt()))
                .toList();
        return new PointSummaryResponse(user.getPoints(), user.getTotalEarned(), history);
    }

    public List<LeaderboardEntryResponse> leaderboard() {
        var leaderboard = userRepository.findTop20ByOrderByPointsDescTotalEarnedDescDisplayNameAsc();
        return IntStream.range(0, leaderboard.size())
                .mapToObj(index -> {
                    var user = leaderboard.get(index);
                    return new LeaderboardEntryResponse(index + 1, user.getDisplayName(),
                            user.getPoints(), user.getTotalEarned());
                })
                .toList();
    }

    public UUID currentId() {
        return current().getId();
    }

    public boolean isAdmin() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }

    private Jwt currentJwt() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof AnonymousAuthenticationToken || !(authentication instanceof JwtAuthenticationToken token)) {
            throw new IllegalStateException("authenticated user is required");
        }
        return token.getToken();
    }

    private String displayName(Jwt jwt) {
        var preferredUsername = jwt.getClaimAsString("preferred_username");
        if (preferredUsername != null && !preferredUsername.isBlank()) {
            return preferredUsername;
        }
        var name = jwt.getClaimAsString("name");
        return name == null || name.isBlank() ? jwt.getSubject() : name;
    }
}
