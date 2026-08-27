package com.pet.proj.user.api;

import java.util.List;

public record PointSummaryResponse(int points, int totalEarned, List<PointTransactionResponse> transactions) {
}
