package com.pet.proj.user.api;

public record LeaderboardEntryResponse(int rank, String displayName, int points, int totalEarned) {
}
