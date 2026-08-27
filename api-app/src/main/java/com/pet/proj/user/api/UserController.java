package com.pet.proj.user.api;

import com.pet.proj.user.application.UserAccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UserController {
    private final UserAccountService userAccounts;

    @GetMapping("/me")
    public UserProfileResponse profile() {
        return userAccounts.profile();
    }

    @GetMapping("/me/points")
    public PointSummaryResponse points() {
        return userAccounts.points();
    }

    @GetMapping("/leaderboard")
    public List<LeaderboardEntryResponse> leaderboard() {
        return userAccounts.leaderboard();
    }
}
