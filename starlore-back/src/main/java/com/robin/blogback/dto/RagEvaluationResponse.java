package com.robin.blogback.dto;

import java.util.List;

public record RagEvaluationResponse(
        boolean success,
        String evaluator,
        Scores scores,
        List<ContextSource> contexts
) {
    public record Scores(
            double faithfulness,
            double answerRelevance,
            double contextPrecision,
            double contextRecall,
            double overall
    ) {
    }

    public record ContextSource(Integer articleId, String title) {
    }
}
