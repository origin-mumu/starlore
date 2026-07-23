package com.robin.blogback.dto;

import jakarta.validation.constraints.NotBlank;

public record RagEvaluationRequest(
        @NotBlank String question,
        @NotBlank String answer,
        String groundTruth,
        Integer topK
) {
}
