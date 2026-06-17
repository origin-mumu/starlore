package com.robin.blogback.dto;

import lombok.Data;

@Data
public class AppendPairRequest {
    private String userContent;
    private String assistantContent;
    private String agentTrace;
}
