package com.pet.proj.user.api;

import java.util.UUID;

public record UserProfileResponse(UUID id, String displayName, boolean admin, int points, int totalEarned) {
}
