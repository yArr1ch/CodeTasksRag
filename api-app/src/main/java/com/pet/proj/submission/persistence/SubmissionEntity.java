package com.pet.proj.submission.persistence;

import com.pet.proj.submission.domain.SubmissionStatus;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "submissions")
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SubmissionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID taskId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(nullable = false)
    private String executionMode;

    @Column(nullable = false, columnDefinition = "text")
    private String sourceCode;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubmissionStatus status;

    @Setter
    @Column(nullable = false)
    private int passedTests;

    @Setter
    @Column(nullable = false)
    private int totalTests;

    @Setter
    @Column(columnDefinition = "text")
    private String error;
}
