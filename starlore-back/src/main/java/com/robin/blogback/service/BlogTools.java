package com.robin.blogback.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.robin.blogback.config.UserContext;
import com.robin.blogback.config.SseContextHolder;
import com.robin.blogback.entity.Article;
import com.robin.blogback.entity.Category;
import com.robin.blogback.mapper.ArticleMapper;
import com.robin.blogback.mapper.CategoryMapper;
import com.robin.blogback.mcp.McpClientManager;
import com.robin.blogback.mcp.McpToolDescriptor;
import org.commonmark.Extension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class BlogTools {

    private static final Logger log = LoggerFactory.getLogger(BlogTools.class);

    @Autowired
    private ArticleMapper articleMapper;

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired(required = false)
    private ArticleEmbeddingService articleEmbeddingService;

    @Autowired(required = false)
    private McpClientManager mcpClientManager;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final Parser MD_PARSER;
    private static final HtmlRenderer MD_RENDERER;

    static {
        List<Extension> extensions = List.of(TablesExtension.create());
        MD_PARSER = Parser.builder().extensions(extensions).build();
        MD_RENDERER = HtmlRenderer.builder().extensions(extensions).build();
    }

    private String markdownToHtml(String content) {
        if (content == null || content.isEmpty()) return content;
        if (content.contains("<p>") || content.contains("<h1>") || content.contains("<h2>")) {
            return content;
        }
        Node document = MD_PARSER.parse(content);
        return MD_RENDERER.render(document);
    }

    private String extractPlain(String content) {
        return content.replaceAll("#+\\s*", "")
                .replaceAll("\\*\\*?", "")
                .replaceAll("`{1,3}", "")
                .replaceAll("\\[([^]]*)\\]\\([^)]*\\)", "$1")
                .replaceAll("!\\[.*?\\]\\(.*?\\)", "")
                .replaceAll("\\n+", " ")
                .trim();
    }

    @Tool(description = "搜索博客文章。优先使用语义搜索理解用户意图，找到相关文章；如果语义搜索无结果则回退到关键词搜索。可按分类和标签筛选。")
    public String searchArticles(

    private static final Parser MD_PARSER;
    private static final HtmlRenderer MD_RENDERER;

    static {
        List<Extension> extensions = List.of(TablesExtension.create());
        MD_PARSER = Parser.builder().extensions(extensions).build();
        MD_RENDERER = HtmlRenderer.builder().extensions(extensions).build();
    }

    private String markdownToHtml(String content) {
        if (content == null || content.isEmpty()) return content;
        if (content.contains("<p>") || content.contains("<h1>") || content.contains("<h2>")) {
            return content;
        }
        Node document = MD_PARSER.parse(content);
        return MD_RENDERER.render(document);
    }

    private String extractPlain(String content) {
        return content.replaceAll("#+\\s*", "")
                .replaceAll("\\*\\*?", "")
                .replaceAll("`{1,3}", "")
                .replaceAll("\\[([^]]*)\\]\\([^)]*\\)", "$1")
                .replaceAll("!\\[.*?\\]\\(.*?\\)", "")
                .replaceAll("\\n+", " ")
                .trim();
    }

    @Tool(description = "搜索博客文章。优先使用语义搜索理解用户意图，找到相关文章；如果语义搜索无结果则回退到关键词搜索。可按分类和标签筛选。")
    public String searchArticles(
            @ToolParam(description = "搜索关键词或语义描述") String keyword,
            @ToolParam(description = "分类名称", required = false) String category,
            @ToolParam(description = "标签名称", required = false) String tag) {
        log.info("[Agent Tool] searchArticles - keyword: {}, category: {}, tag: {}", keyword, category, tag);
        SseContextHolder.sendToolStart("searchArticles");
        try {
            Integer userId = UserContext.getUserId();
            List<Article> articles = new ArrayList<>();
            String retrievalMode = "vector";

            // 1. 优先语义搜索
            if (articleEmbeddingService != null) {
                articles = articleEmbeddingService.searchSimilar(keyword, userId, 10);
                if (!articles.isEmpty()) {
                    log.info("[RAG] 语义搜索初步命中 {} 篇文章", articles.size());
                    // 立即应用分类/标签过滤
                    if (category != null && !category.isEmpty()) {
                        articles = articles.stream()
                                .filter(a -> category.equals(a.getCategory()))
                                .collect(Collectors.toList());
                    }
                    if (tag != null && !tag.isEmpty()) {
                        articles = articles.stream()
                                .filter(a -> a.getTags() != null && a.getTags().contains(tag))
                                .collect(Collectors.toList());
                    }
                }
            }

            // 2. 语义搜索无结果（或过滤后为空），回退到关键词搜索
            if (articles.isEmpty()) {
                retrievalMode = "keyword";
                log.info("[RAG] 语义搜索无匹配结果（或过滤后为空），回退到关键词搜索");
                LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<Article>()
                        .eq(Article::getUserId, userId)
                        .eq(Article::getStatus, "published")
                        .and(w -> w.like(Article::getTitle, keyword).or().like(Article::getDescription, keyword))
                        .orderByDesc(Article::getCreatedAt)
                        .last("LIMIT 20");

                if (category != null && !category.isEmpty()) {
                    wrapper.eq(Article::getCategory, category);
                }
                if (tag != null && !tag.isEmpty()) {
                    wrapper.apply("JSON_CONTAINS(tags, JSON_ARRAY({0}))", tag);
                }
                articles = articleMapper.selectList(wrapper);
            }

            List<Map<String, Object>> results = articles.stream().map(a -> {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("id", a.getId());
                map.put("title", a.getTitle());
                map.put("description", a.getDescription());
                map.put("category", a.getCategory());
                map.put("tags", a.getTags());
                map.put("viewCount", a.getViewCount());
                map.put("createdAt", a.getCreatedAt() != null ? a.getCreatedAt().toString() : null);
                // 附带内容摘要，让 AI 有足够的信息回答
                String content = a.getContent();
                if (content != null && !content.isEmpty()) {
                    String plain = extractPlain(content);
                    map.put("contentPreview", plain.length() > 800 ? plain.substring(0, 800) + "..." : plain);
                }
                return map;
            }).collect(Collectors.toList());

            if (results.isEmpty()) {
                return "{\"results\":[],\"message\":\"未找到匹配的文章\"}";
            }
            List<Map<String, Object>> sources = articles.stream()
                    .map(article -> Map.<String, Object>of(
                            "articleId", article.getId(),
                            "title", article.getTitle()))
                    .toList();
            SseContextHolder.sendEvent("rag_context", Map.of(
                    "retrieval_mode", retrievalMode,
                    "articles", sources));
            return objectMapper.writeValueAsString(results);
        } catch (JsonProcessingException e) {
            return "{\"error\":\"序列化失败: " + e.getMessage() + "\"}";
        }
    }

    @Tool(description = "获取文章详情。根据文章ID获取完整文章，不会增加阅读量。")
    public String getArticleDetail(@ToolParam(description = "文章ID") Integer articleId) {
        log.info("[Agent Tool] getArticleDetail - articleId: {}", articleId);
        SseContextHolder.sendToolStart("getArticleDetail");
        try {
            Integer userId = UserContext.getUserId();
            Article article = articleMapper.selectOne(
                    new LambdaQueryWrapper<Article>()
                            .eq(Article::getId, articleId)
                            .eq(Article::getUserId, userId));
            if (article == null) {
                return "{\"error\":\"文章不存在\"}";
            }
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", article.getId());
            map.put("title", article.getTitle());
            map.put("description", article.getDescription());
            map.put("category", article.getCategory());
            map.put("tags", article.getTags());
            map.put("viewCount", article.getViewCount());
            map.put("createdAt", article.getCreatedAt() != null ? article.getCreatedAt().toString() : null);
            map.put("updatedAt", article.getUpdatedAt() != null ? article.getUpdatedAt().toString() : null);
            String content = article.getContent();
            if (content != null && content.length() > 3000) {
                content = content.substring(0, 3000) + "...(内容已截断)";
            }
            map.put("content", content);
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            return "{\"error\":\"序列化失败: " + e.getMessage() + "\"}";
        }
    }

    @Tool(description = "获取所有博客分类及其文章数量。")
    public String getCategories() {
        log.info("[Agent Tool] getCategories - 查询所有分类");
        SseContextHolder.sendToolStart("getCategories");
        try {
            Integer userId = UserContext.getUserId();
            log.info("[Agent Tool] getCategories - userId: {}", userId);
            List<Category> categories = categoryMapper.selectList(
                    new LambdaQueryWrapper<Category>()
                            .eq(Category::getUserId, userId)
                            .orderByDesc(Category::getArticleCount));
            log.info("[Agent Tool] getCategories - 查询到 {} 条分类", categories.size());
            List<Map<String, Object>> results = categories.stream().map(c -> {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("id", c.getId());
                map.put("name", c.getName());
                map.put("description", c.getDescription());
                map.put("articleCount", c.getArticleCount());
                return map;
            }).collect(Collectors.toList());
            return objectMapper.writeValueAsString(results);
        } catch (JsonProcessingException e) {
            return "{\"error\":\"序列化失败: " + e.getMessage() + "\"}";
        }
    }

    @Tool(description = "获取博客统计数据：文章总数、分类总数、总浏览量等。")
    public String getBlogStats() {
        log.info("[Agent Tool] getBlogStats - 查询博客统计");
        SseContextHolder.sendToolStart("getBlogStats");
        try {
            Integer userId = UserContext.getUserId();
            log.info("[Agent Tool] getBlogStats - userId: {}", userId);
            List<Article> articles = articleMapper.selectList(
                    new LambdaQueryWrapper<Article>()
                            .eq(Article::getUserId, userId)
                            .eq(Article::getStatus, "published"));
            List<Category> categories = categoryMapper.selectList(
                    new LambdaQueryWrapper<Category>()
                            .eq(Category::getUserId, userId)
                            .orderByDesc(Category::getArticleCount));

            int totalArticles = articles.size();
            int totalCategories = categories.size();
            int totalViews = articles.stream().mapToInt(a -> a.getViewCount() != null ? a.getViewCount() : 0).sum();

            List<Map<String, Object>> topCategories = categories.stream().limit(5).map(c -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("name", c.getName());
                m.put("count", c.getArticleCount());
                return m;
            }).collect(Collectors.toList());

            List<Map<String, Object>> recentArticles = articles.stream()
                    .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                    .limit(5)
                    .map(a -> {
                        Map<String, Object> m = new LinkedHashMap<>();
                        m.put("id", a.getId());
                        m.put("title", a.getTitle());
                        m.put("createdAt", a.getCreatedAt() != null ? a.getCreatedAt().toString() : null);
                        return m;
                    }).collect(Collectors.toList());

            Map<String, Object> stats = new LinkedHashMap<>();
            stats.put("totalArticles", totalArticles);
            stats.put("totalCategories", totalCategories);
            stats.put("totalViews", totalViews);
            stats.put("topCategories", topCategories);
            stats.put("recentArticles", recentArticles);
            String result = objectMapper.writeValueAsString(stats);
            log.info("[Agent Tool] getBlogStats - 返回数据: {}", result);
            return result;
        } catch (Exception e) {
            log.error("[Agent Tool] getBlogStats - 异常: {}", e.getMessage(), e);
            return "{\"error\":\"获取统计失败: " + e.getMessage() + "\"}";
        }
    }

    @Tool(description = "获取最新的N篇博客文章摘要。")
    public String getRecentArticles(@ToolParam(description = "返回数量，默认5", required = false) Integer limit) {
        log.info("[Agent Tool] getRecentArticles - limit: {}", limit);
        SseContextHolder.sendToolStart("getRecentArticles");
        try {
            Integer userId = UserContext.getUserId();
            if (limit == null || limit <= 0) limit = 5;
            if (limit > 20) limit = 20;

            List<Article> articles = articleMapper.selectList(
                    new LambdaQueryWrapper<Article>()
                            .eq(Article::getUserId, userId)
                            .eq(Article::getStatus, "published")
                            .orderByDesc(Article::getCreatedAt)
                            .last("LIMIT " + limit));

            List<Map<String, Object>> results = articles.stream().map(a -> {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("id", a.getId());
                map.put("title", a.getTitle());
                map.put("description", a.getDescription());
                map.put("category", a.getCategory());
                map.put("createdAt", a.getCreatedAt() != null ? a.getCreatedAt().toString() : null);
                return map;
            }).collect(Collectors.toList());

            return objectMapper.writeValueAsString(results);
        } catch (JsonProcessingException e) {
            return "{\"error\":\"序列化失败: " + e.getMessage() + "\"}";
        }
    }

    @Tool(description = "创建新文章。可指定标题、内容、分类、标签、描述和状态（draft/published），不指定状态则默认为草稿。")
    public String writeArticle(
            @ToolParam(description = "文章标题") String title,
            @ToolParam(description = "文章内容（支持Markdown）") String content,
            @ToolParam(description = "文章分类") String category,
            @ToolParam(description = "文章标签，用逗号分隔，如：Vue,TypeScript", required = false) String tags,
            @ToolParam(description = "文章描述/摘要，不传则自动截取内容前150字", required = false) String description,
            @ToolParam(description = "文章状态：draft(草稿) 或 published(已发布)，默认draft", required = false) String status) {
        log.info("[Agent Tool] writeArticle - title: {}, category: {}, tags: {}, status: {}", title, category, tags, status);
        SseContextHolder.sendToolStart("writeArticle");
        try {
            Integer userId = UserContext.getUserId();
            Article article = new Article();
            article.setUserId(userId);
            article.setTitle(title);
            article.setContent(markdownToHtml(content));
            article.setCategory(category);
            if (tags != null && !tags.isEmpty()) {
                article.setTags(Arrays.stream(tags.split(","))
                        .map(String::trim)
                        .filter(t -> !t.isEmpty())
                        .collect(Collectors.toList()));
            }
            if (description != null && !description.isEmpty()) {
                article.setDescription(description);
            } else {
                String plain = extractPlain(content);
                article.setDescription(plain.length() > 150 ? plain.substring(0, 150) + "..." : plain);
            }
            article.setStatus(status != null && status.equals("published") ? "published" : "draft");
            article.setViewCount(0);
            article.setCreatedAt(LocalDateTime.now());
            article.setUpdatedAt(LocalDateTime.now());

            articleMapper.insert(article);

            // 自动索引用于语义搜索
            if (articleEmbeddingService != null && "published".equals(article.getStatus())) {
                try { articleEmbeddingService.indexArticle(article); } catch (Exception e) { log.warn("[RAG] 索引文章失败: {}", e.getMessage()); }
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("id", article.getId());
            result.put("title", article.getTitle());
            result.put("category", article.getCategory());
            result.put("status", article.getStatus());
            result.put("message", "文章创建成功");
            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            return "{\"error\":\"创建文章失败: " + e.getMessage() + "\"}";
        }
    }

    @Tool(description = "更新已有文章。可更新标题、内容、分类、标签、描述、状态，只传需要修改的字段。")
    public String updateArticle(
            @ToolParam(description = "文章ID") Integer articleId,
            @ToolParam(description = "新的文章标题", required = false) String title,
            @ToolParam(description = "新的文章内容（支持Markdown）", required = false) String content,
            @ToolParam(description = "新的文章分类", required = false) String category,
            @ToolParam(description = "新的文章标签，用逗号分隔", required = false) String tags,
            @ToolParam(description = "新的文章描述", required = false) String description,
            @ToolParam(description = "新的文章状态：draft(草稿) 或 published(已发布)", required = false) String status) {
        log.info("[Agent Tool] updateArticle - articleId: {}, title: {}, category: {}, status: {}", articleId, title, category, status);
        SseContextHolder.sendToolStart("updateArticle");
        try {
            Integer userId = UserContext.getUserId();
            Article existing = articleMapper.selectOne(
                    new LambdaQueryWrapper<Article>()
                            .eq(Article::getId, articleId)
                            .eq(Article::getUserId, userId));
            if (existing == null) {
                return "{\"error\":\"文章不存在\"}";
            }
            if (title != null) existing.setTitle(title);
            if (content != null) existing.setContent(markdownToHtml(content));
            if (category != null) existing.setCategory(category);
            if (tags != null && !tags.isEmpty()) {
                existing.setTags(Arrays.stream(tags.split(","))
                        .map(String::trim)
                        .filter(t -> !t.isEmpty())
                        .collect(Collectors.toList()));
            }
            if (description != null) existing.setDescription(description);
            if (status != null && (status.equals("draft") || status.equals("published"))) {
                existing.setStatus(status);
            }
            existing.setUpdatedAt(LocalDateTime.now());

            articleMapper.updateById(existing);

            // 重新索引用于语义搜索
            if (articleEmbeddingService != null) {
                try { articleEmbeddingService.indexArticle(existing); } catch (Exception e) { log.warn("[RAG] 重新索引文章失败: {}", e.getMessage()); }
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("id", existing.getId());
            result.put("title", existing.getTitle());
            result.put("status", existing.getStatus());
            result.put("message", "文章更新成功");
            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            return "{\"error\":\"更新文章失败: " + e.getMessage() + "\"}";
        }
    }

    @Tool(description = "删除指定ID的文章。")
    public String deleteArticle(@ToolParam(description = "要删除的文章ID") Integer articleId) {
        log.info("[Agent Tool] deleteArticle - articleId: {}", articleId);
        SseContextHolder.sendToolStart("deleteArticle");
        try {
            Integer userId = UserContext.getUserId();
            Article existing = articleMapper.selectOne(
                    new LambdaQueryWrapper<Article>()
                            .eq(Article::getId, articleId)
                            .eq(Article::getUserId, userId));
            if (existing == null) {
                return "{\"error\":\"文章不存在\"}";
            }
            articleMapper.deleteById(articleId);

            // 删除向量嵌入
            if (articleEmbeddingService != null) {
                try { articleEmbeddingService.removeArticle(articleId); } catch (Exception e) { log.warn("[RAG] 删除文章向量失败: {}", e.getMessage()); }
            }

            return "{\"success\":true,\"message\":\"文章已删除\",\"id\":" + articleId + "}";
        } catch (Exception e) {
            return "{\"error\":\"删除文章失败: " + e.getMessage() + "\"}";
        }
    }

    @Tool(description = "获取所有文章中使用的标签列表（去重）。")
    public String getAllTags() {
        log.info("[Agent Tool] getAllTags - 查询所有标签");
        SseContextHolder.sendToolStart("getAllTags");
        try {
            Integer userId = UserContext.getUserId();
            List<Article> articles = articleMapper.selectList(
                    new LambdaQueryWrapper<Article>()
                            .eq(Article::getUserId, userId)
                            .eq(Article::getStatus, "published"));
            Set<String> allTags = new LinkedHashSet<>();
            for (Article a : articles) {
                if (a.getTags() != null) {
                    allTags.addAll(a.getTags());
                }
            }
            List<Map<String, Object>> results = allTags.stream().map(tag -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("name", tag);
                long count = articles.stream().filter(a -> a.getTags() != null && a.getTags().contains(tag)).count();
                m.put("articleCount", count);
                return m;
            }).collect(Collectors.toList());
            return objectMapper.writeValueAsString(results);
        } catch (JsonProcessingException e) {
            return "{\"error\":\"序列化失败: " + e.getMessage() + "\"}";
        }
    }

    @Tool(description = "获取指定分类下的所有已发布文章列表。")
    public String getArticlesByCategory(@ToolParam(description = "分类名称") String category) {
        log.info("[Agent Tool] getArticlesByCategory - category: {}", category);
        SseContextHolder.sendToolStart("getArticlesByCategory");
        try {
            Integer userId = UserContext.getUserId();
            List<Article> articles = articleMapper.selectList(
                    new LambdaQueryWrapper<Article>()
                            .eq(Article::getUserId, userId)
                            .eq(Article::getStatus, "published")
                            .eq(Article::getCategory, category)
                            .orderByDesc(Article::getCreatedAt)
                            .last("LIMIT 50"));
            List<Map<String, Object>> results = articles.stream().map(a -> {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("id", a.getId());
                map.put("title", a.getTitle());
                map.put("description", a.getDescription());
                map.put("tags", a.getTags());
                map.put("viewCount", a.getViewCount());
                map.put("createdAt", a.getCreatedAt() != null ? a.getCreatedAt().toString() : null);
                return map;
            }).collect(Collectors.toList());
            if (results.isEmpty()) {
                return "{\"results\":[],\"message\":\"该分类下暂无已发布的文章\"}";
            }
            return objectMapper.writeValueAsString(results);
        } catch (JsonProcessingException e) {
            return "{\"error\":\"序列化失败: " + e.getMessage() + "\"}";
        }
    }

    @Tool(description = "创建新的博客分类。")
    public String createCategory(
            @ToolParam(description = "分类名称") String name,
            @ToolParam(description = "分类描述", required = false) String description,
            @ToolParam(description = "分类颜色（十六进制，如 #FF6B6B）", required = false) String color) {
        log.info("[Agent Tool] createCategory - name: {}, description: {}, color: {}", name, description, color);
        SseContextHolder.sendToolStart("createCategory");
        try {
            Integer userId = UserContext.getUserId();
            Category existing = categoryMapper.selectOne(
                    new LambdaQueryWrapper<Category>()
                            .eq(Category::getUserId, userId)
                            .eq(Category::getName, name));
            if (existing != null) {
                return "{\"error\":\"分类「" + name + "」已存在\"}";
            }
            Category category = new Category();
            category.setUserId(userId);
            category.setName(name);
            category.setDescription(description != null ? description : "");
            category.setColor(color != null ? color : "#6D63FF");
            category.setArticleCount(0);
            category.setCreatedAt(LocalDateTime.now());
            category.setUpdatedAt(LocalDateTime.now());

            categoryMapper.insert(category);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("id", category.getId());
            result.put("name", category.getName());
            result.put("message", "分类创建成功");
            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            return "{\"error\":\"创建分类失败: " + e.getMessage() + "\"}";
        }
    }

    @Tool(description = "动态发现当前已配置的外部 MCP Server 及其工具。调用外部工具前先使用本工具，获取 server、toolName 和参数 JSON Schema。")
    public String listMcpTools() {
        log.info("[Agent Tool] listMcpTools");
        SseContextHolder.sendToolStart("listMcpTools");
        if (mcpClientManager == null || mcpClientManager.configuredServers().isEmpty()) {
            return "{\"servers\":[],\"tools\":[],\"message\":\"当前未配置 MCP Server\"}";
        }
        try {
            List<McpToolDescriptor> tools = mcpClientManager.listTools();
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("servers", mcpClientManager.configuredServers());
            result.put("tools", tools);
            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            return errorJson("MCP 工具发现失败", e);
        }
    }

    @Tool(description = "调用已发现的外部 MCP 工具。必须先调用 listMcpTools，并严格按照返回的 inputSchema 构造 argumentsJson。")
    public String callMcpTool(
            @ToolParam(description = "listMcpTools 返回的 MCP Server 名称") String serverName,
            @ToolParam(description = "listMcpTools 返回的工具名称") String toolName,
            @ToolParam(description = "符合工具 inputSchema 的 JSON 对象字符串，例如 {\"query\":\"Spring AI\"}") String argumentsJson) {
        log.info("[Agent Tool] callMcpTool - server: {}, tool: {}", serverName, toolName);
        SseContextHolder.sendToolStart("callMcpTool");
        if (mcpClientManager == null) {
            return "{\"error\":\"MCP 客户端不可用\"}";
        }
        try {
            JsonNode arguments = argumentsJson == null || argumentsJson.isBlank()
                    ? objectMapper.createObjectNode()
                    : objectMapper.readTree(argumentsJson);
            if (!arguments.isObject()) {
                return "{\"error\":\"argumentsJson 必须是 JSON 对象\"}";
            }
            JsonNode response = mcpClientManager.callTool(serverName, toolName, arguments);
            return objectMapper.writeValueAsString(response);
        } catch (Exception e) {
            return errorJson("MCP 工具调用失败", e);
        }
    }

    private String errorJson(String message, Exception exception) {
        try {
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("error", message);
            error.put("detail", exception.getMessage());
            return objectMapper.writeValueAsString(error);
        } catch (JsonProcessingException ignored) {
            return "{\"error\":\"" + message + "\"}";
        }
    }
}
