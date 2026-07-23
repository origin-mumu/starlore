package com.robin.blogback.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record RagEvaluationRequest(
        @NotBlank String question,
        @NotBlank String answer,
        String groundTruth,
        Integer topK,
        List<Integer> articleIds
) {
}
