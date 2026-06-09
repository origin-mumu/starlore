package com.robin.blogback.observability;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.robin.blogback.entity.AiBadCase;
import com.robin.blogback.mapper.AiBadCaseMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Bad Case 收集器。
 * <p>
 * 收集执行失败或 Reviewer REVISE 的案例，持久化到数据库。
 * 提供相似 Bad Case 查询，用于 Few-Shot 动态反馈机制。
 */
@Component
public class BadCaseCollector {

    private static final Logger log = LoggerFactory.getLogger(BadCaseCollector.class);

    @Autowired
    private AiBadCaseMapper badCaseMapper;

    /**
     * 收集一个 Bad Case。
     */
    public void collect(Integer userId, String question, String actualAnswer,
                         String errorMessage, String agentPath, int tokens, long latencyMs) {
        try {
            AiBadCase badCase = new AiBadCase();
            badCase.setUserId(userId);
            badCase.setQuestion(question);
            badCase.setActualAnswer(truncate(actualAnswer, 4000));
            badCase.setErrorMessage(truncate(errorMessage, 1000));
            badCase.setAgentPath(agentPath);
            badCase.setTokens(tokens);
            badCase.setLatencyMs(latencyMs);
            badCase.setCreatedAt(LocalDateTime.now());

            badCaseMapper.insert(badCase);
            log.info("[BadCase] Collected: userId={}, question={}",
                    userId, question.substring(0, Math.min(50, question.length())));
        } catch (Exception e) {
            log.warn("[BadCase] Failed to collect: {}", e.getMessage());
        }
    }

    /**
     * 获取相似的 Bad Case（基于关键词匹配）。
     * 用于 Few-Shot 动态反馈，让 Executor 避免重复犯错。
     *
     * @param query 当前子任务描述
     * @param limit 最大返回数
     * @return 相似 Bad Case 的描述列表
     */
    public List<String> getSimilarBadCases(String query, int limit) {
        try {
            // 提取查询中的关键词
            String[] keywords = extractKeywords(query);
            if (keywords.length == 0) return Collections.emptyList();

            // 构建关键词 LIKE 查询
            LambdaQueryWrapper<AiBadCase> wrapper = new LambdaQueryWrapper<>();
            wrapper.and(w -> {
                for (int i = 0; i < keywords.length; i++) {
                    if (i > 0) w.or();
                    w.like(AiBadCase::getQuestion, keywords[i]);
                }
            });
            wrapper.orderByDesc(AiBadCase::getCreatedAt);
            wrapper.last("LIMIT " + limit);

            List<AiBadCase> cases = badCaseMapper.selectList(wrapper);
            return cases.stream()
                    .map(bc -> "问题: " + truncate(bc.getQuestion(), 100)
                            + " | 错误: " + truncate(bc.getErrorMessage(), 200))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("[BadCase] Failed to query similar cases: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 获取 Bad Case 列表（分页）。
     */
    public List<AiBadCase> getBadCases(int page, int size) {
        LambdaQueryWrapper<AiBadCase> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(AiBadCase::getCreatedAt);
        wrapper.last("LIMIT " + size + " OFFSET " + (page - 1) * size);
        return badCaseMapper.selectList(wrapper);
    }

    /**
     * 获取 Bad Case 总数。
     */
    public long getBadCaseCount() {
        return badCaseMapper.selectCount(null);
    }

    /**
     * 从文本中提取关键词（简单的分词：按空格和标点分割，取长度>1的词）。
     */
    private String[] extractKeywords(String text) {
        if (text == null || text.isEmpty()) return new String[0];
        return Arrays.stream(text.split("[\\s,，。！？.!?;；:：\\-]+"))
                .filter(w -> w.length() > 1)
                .limit(5)
                .toArray(String[]::new);
    }

    private String truncate(String s, int maxLen) {
        if (s == null) return "";
        return s.length() > maxLen ? s.substring(0, maxLen) + "..." : s;
    }
}
