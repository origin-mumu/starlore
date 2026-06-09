package com.robin.blogback.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AiCharacterCardsResponse {
    private boolean success;
    private List<CharacterCard> cards;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CharacterCard {
        private String key;
        private String name;
        private String description;
        private String systemPrompt;
    }
}
