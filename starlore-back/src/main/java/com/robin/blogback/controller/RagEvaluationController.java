package com.robin.blogback.controller;

import com.robin.blogback.dto.RagEvaluationRequest;
import com.robin.blogback.dto.RagEvaluationResponse;
import com.robin.blogback.service.RagEvaluationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/rag")
public class RagEvaluationController {

    private final RagEvaluationService ragEvaluationService;

    public RagEvaluationController(RagEvaluationService ragEvaluationService) {
        this.ragEvaluationService = ragEvaluationService;
    }

    @PostMapping("/evaluate")
    public RagEvaluationResponse evaluate(
            HttpServletRequest httpRequest,
            @Valid @RequestBody RagEvaluationRequest request) {
        Integer userId = (Integer) httpRequest.getAttribute("userId");
        return ragEvaluationService.evaluate(
                userId,
                request.question(),
                request.answer(),
                request.groundTruth(),
                request.topK(),
                request.articleIds()
        );
    }
}
