package com.robin.blogback.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.robin.blogback.entity.Article;
import com.robin.blogback.entity.ArticleChunk;
import com.robin.blogback.mapper.ArticleChunkMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class KnowledgeChunkService {

    private static final int TARGET_TOKENS = 600;
    private static final int MAX_TOKENS = 800;
    private static final int OVERLAP_TOKENS = 80;
    private static final Pattern HEADING = Pattern.compile("^#{1,6}\\s+.+");
    private static final Pattern SENTENCE_BOUNDARY =
            Pattern.compile("(?<=[。！？；.!?;])|(?=\\n)|(?<=\\n)");

    private final ArticleChunkMapper articleChunkMapper;

    public List<ArticleChunk> getChunks(Integer articleId) {
        return articleChunkMapper.selectList(
                new LambdaQueryWrapper<ArticleChunk>()
                        .eq(ArticleChunk::getArticleId, articleId)
                        .orderByAsc(ArticleChunk::getChunkIndex));
    }

    public List<ArticleChunk> ensureChunks(Article article) {
        if (article == null) return Collections.emptyList();
        List<ArticleChunk> chunks = getChunks(article.getId());
        return chunks.isEmpty() ? rebuildChunks(article) : chunks;
    }

    @Transactional
    public List<ArticleChunk> rebuildChunks(Article article) {
        if (article == null || article.getId() == null) return Collections.emptyList();
        articleChunkMapper.delete(
                new LambdaQueryWrapper<ArticleChunk>().eq(ArticleChunk::getArticleId, article.getId()));

        String content = normalize(article.getContent());
        if (content.isBlank()) return Collections.emptyList();

        List<String> pieces = splitMarkdown(content);
        List<ArticleChunk> chunks = new ArrayList<>();
        for (int index = 0; index < pieces.size(); index++) {
            String piece = pieces.get(index).trim();
            if (piece.isEmpty()) continue;
            ArticleChunk chunk = new ArticleChunk();
            chunk.setArticleId(article.getId().longValue());
            chunk.setChunkIndex(index);
            chunk.setContent(piece);
            chunk.setTokenCount(estimateTokens(piece));
            chunk.setIsEnabled(1);
            articleChunkMapper.insert(chunk);
            chunks.add(chunk);
        }
        return chunks;
    }

    public int estimateTokens(String text) {
        if (text == null || text.isBlank()) return 0;
        int cjk = 0;
        int ascii = 0;
        for (int offset = 0; offset < text.length();) {
            int codePoint = text.codePointAt(offset);
            offset += Character.charCount(codePoint);
            if (isCjk(codePoint)) cjk++;
            else ascii++;
        }
        return cjk + Math.max(1, (int) Math.ceil(ascii / 4.0));
    }

    @Transactional
    public void deleteChunks(Integer articleId) {
        if (articleId == null) return;
        articleChunkMapper.delete(
                new LambdaQueryWrapper<ArticleChunk>().eq(ArticleChunk::getArticleId, articleId));
    }

    private List<String> splitMarkdown(String text) {
        List<String> units = structuralUnits(text);
        List<String> chunks = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (String unit : units) {
            for (String part : splitOversized(unit)) {
                if (!current.isEmpty()
                        && estimateTokens(current + "\n\n" + part) > MAX_TOKENS) {
                    String completed = current.toString().trim();
                    chunks.add(completed);
                    current = new StringBuilder(overlapTail(completed));
                }
                if (!current.isEmpty()) current.append("\n\n");
                current.append(part.trim());
                if (estimateTokens(current.toString()) >= TARGET_TOKENS) {
                    String completed = current.toString().trim();
                    chunks.add(completed);
                    current = new StringBuilder(overlapTail(completed));
                }
            }
        }
        String remainder = current.toString().trim();
        boolean overlapOnly = !chunks.isEmpty()
                && estimateTokens(remainder) <= OVERLAP_TOKENS
                && chunks.get(chunks.size() - 1).endsWith(remainder);
        if (!remainder.isEmpty() && !overlapOnly
                && (chunks.isEmpty() || !remainder.equals(chunks.get(chunks.size() - 1)))) {
            chunks.add(remainder);
        }
        return chunks;
    }

    private List<String> structuralUnits(String text) {
        List<String> units = new ArrayList<>();
        StringBuilder block = new StringBuilder();
        boolean inCode = false;
        for (String line : text.split("\\n", -1)) {
            String trimmed = line.trim();
            if (trimmed.startsWith("```")) {
                if (!inCode) flush(block, units);
                inCode = !inCode;
                block.append(line).append('\n');
                if (!inCode) flush(block, units);
            } else if (!inCode && (trimmed.isEmpty() || HEADING.matcher(trimmed).matches())) {
                flush(block, units);
                if (!trimmed.isEmpty()) units.add(trimmed);
            } else {
                block.append(line).append('\n');
            }
        }
        flush(block, units);
        return units;
    }

    private List<String> splitOversized(String unit) {
        if (estimateTokens(unit) <= MAX_TOKENS) return List.of(unit);
        List<String> result = new ArrayList<>();
        StringBuilder part = new StringBuilder();
        for (String sentence : SENTENCE_BOUNDARY.split(unit)) {
            if (sentence.isBlank()) continue;
            if (!part.isEmpty() && estimateTokens(part + sentence) > MAX_TOKENS) {
                result.add(part.toString().trim());
                part = new StringBuilder(overlapTail(part.toString()));
            }
            part.append(sentence);
        }
        if (!part.isEmpty()) result.add(part.toString().trim());

        List<String> bounded = new ArrayList<>();
        for (String value : result) {
            if (estimateTokens(value) <= MAX_TOKENS) bounded.add(value);
            else bounded.addAll(hardSplit(value));
        }
        return bounded;
    }

    private List<String> hardSplit(String text) {
        List<String> result = new ArrayList<>();
        StringBuilder part = new StringBuilder();
        for (int offset = 0; offset < text.length();) {
            int codePoint = text.codePointAt(offset);
            offset += Character.charCount(codePoint);
            part.appendCodePoint(codePoint);
            if (estimateTokens(part.toString()) >= MAX_TOKENS) {
                String completed = part.toString().trim();
                result.add(completed);
                part = new StringBuilder(overlapTail(completed));
            }
        }
        String remainder = part.toString().trim();
        if (!remainder.isEmpty()) result.add(remainder);
        return result;
    }

    private String overlapTail(String text) {
        if (text.isBlank()) return "";
        int start = text.length();
        while (start > 0 && estimateTokens(text.substring(start)) < OVERLAP_TOKENS) {
            start = text.offsetByCodePoints(start, -1);
        }
        String tail = text.substring(start).trim();
        int boundary = Math.max(
                Math.max(tail.indexOf('。'), tail.indexOf('！')),
                Math.max(tail.indexOf('？'), tail.indexOf('\n')));
        return boundary >= 0 && boundary + 1 < tail.length()
                ? tail.substring(boundary + 1).trim()
                : tail;
    }

    private void flush(StringBuilder block, List<String> units) {
        String value = block.toString().trim();
        if (!value.isEmpty()) units.add(value);
        block.setLength(0);
    }

    private String normalize(String text) {
        return text == null ? "" : text.replace("\r\n", "\n").replace('\r', '\n').trim();
    }

    private boolean isCjk(int codePoint) {
        Character.UnicodeScript script = Character.UnicodeScript.of(codePoint);
        return script == Character.UnicodeScript.HAN
                || script == Character.UnicodeScript.HIRAGANA
                || script == Character.UnicodeScript.KATAKANA
                || script == Character.UnicodeScript.HANGUL;
    }
}
