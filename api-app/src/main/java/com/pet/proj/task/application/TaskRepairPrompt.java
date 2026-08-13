package com.pet.proj.task.application;

import java.util.List;

record TaskRepairPrompt(String title, String description, List<String> constraints, ReferenceSolutionException failure,
                        int attempt) implements TaskPrompt {
}
