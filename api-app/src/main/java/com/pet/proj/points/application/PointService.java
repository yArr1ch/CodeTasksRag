package com.pet.proj.points.application;

import com.pet.proj.points.persistence.PointTransactionRepository;
import com.pet.proj.points.persistence.PointTransactionType;
import com.pet.proj.points.persistence.UserTaskProgressEntity;
import com.pet.proj.points.persistence.UserTaskProgressRepository;
import com.pet.proj.user.api.ReferenceUnlockResponse;
import com.pet.proj.user.application.UserAccountService;
import com.pet.proj.user.persistence.UserEntity;
import com.pet.proj.user.persistence.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PointService {
    private final UserRepository users;
    private final PointTransactionRepository pointRepository;
    private final PointTransactionMapper transactionMapper;
    private final UserTaskProgressRepository progressRepository;
    private final UserAccountService userService;

    @Value("${app.points.pass-reward:100}")
    private int passReward;

    @Value("${app.points.hint-cost:10}")
    private int hintCost;

    @Value("${app.points.reference-answer-cost:50}")
    private int referenceAnswerCost;

    @Transactional
    public void chargeHint(UUID userId, UUID taskId) {
        var user = lockedUser(userId);
        user.debit(hintCost);
        var progress = progress(userId, taskId);
        progress.markHintUsed();
        pointRepository.save(transactionMapper.toEntity(
                userId, PointTransactionType.HINT_CHARGE, -hintCost, taskId, null,
                "hint:" + UUID.randomUUID()));
        users.save(user);
        progressRepository.save(progress);
    }

    @Transactional
    public ReferenceUnlockResponse unlockReference(UUID taskId) {
        var userId = userService.currentId();
        var user = lockedUser(userId);
        var progress = progress(userId, taskId);

        if (!progress.isReferenceUnlocked()) {
            user.debit(referenceAnswerCost);
            progress.unlockReference();
            pointRepository.save(transactionMapper.toEntity(
                    userId, PointTransactionType.REFERENCE_UNLOCK, -referenceAnswerCost,
                    taskId, null, "reference:" + userId + ":" + taskId));
            users.save(user);
            progressRepository.save(progress);
        }
        return new ReferenceUnlockResponse(true, user.getPoints());
    }

    @Transactional
    public void recordPassedSubmission(UUID userId, UUID taskId, UUID submissionId) {
        if (userId == null) {
            return;
        }
        var user = lockedUser(userId);
        var progress = progress(userId, taskId);
        progress.markPassed();
        if (!progress.isHintUsed() && !progress.isReferenceUnlocked()
                && !pointRepository.existsByIdempotencyKey("pass:" + submissionId)) {
            user.credit(passReward);
            pointRepository.save(transactionMapper.toEntity(
                    userId, PointTransactionType.PASS_REWARD, passReward, taskId, submissionId,
                    "pass:" + submissionId));
            users.save(user);
        }
        progressRepository.save(progress);
    }

    public boolean canViewReference(UUID taskId) {
        return userService.isAdmin() || progressRepository.findByUserIdAndTaskId(userService.currentId(), taskId)
                .map(UserTaskProgressEntity::isReferenceUnlocked)
                .orElse(false);
    }

    public boolean canViewCommunity(UUID taskId) {
        return progressRepository.findByUserIdAndTaskId(userService.currentId(), taskId)
                .map(UserTaskProgressEntity::isPassed)
                .orElse(false);
    }

    public int currentBalance() {
        return userService.current().getPoints();
    }

    private UserEntity lockedUser(UUID userId) {
        return users.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("user not found"));
    }

    private UserTaskProgressEntity progress(UUID userId, UUID taskId) {
        return progressRepository.findByUserIdAndTaskId(userId, taskId)
                .orElseGet(() -> UserTaskProgressEntity.builder()
                        .userId(userId)
                        .taskId(taskId)
                        .build());
    }
}
