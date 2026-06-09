package com.robin.blogback.graph;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Consumer;

/**
 * Multi-Agent 状态机引擎。
 * <p>
 * 支持：
 * - 串行节点执行
 * - 条件边路由（类似 LangGraph 的 conditional_edges）
 * - 执行回调（用于 SSE 流式推送和 tracing）
 * <p>
 * 使用 Builder 模式构建：
 * <pre>
 * AgentGraph graph = AgentGraph.builder()
 *     .node("planner", plannerNode)
 *     .node("executor", executorNode)
 *     .node("reviewer", reviewerNode)
 *     .edge("START", "planner")
 *     .edge("planner", "executor")
 *     .conditionalEdge("reviewer", reviewRouter)
 *     .edge("executor_pass", "END")
 *     .compile("planner");
 * </pre>
 */
public class AgentGraph {

    private static final Logger log = LoggerFactory.getLogger(AgentGraph.class);

    public static final String START = "START";
    public static final String END = "END";

    private final Map<String, AgentNode> nodes = new LinkedHashMap<>();
    private final Map<String, String> staticEdges = new LinkedHashMap<>();
    private final Map<String, ConditionalEdge> conditionalEdges = new LinkedHashMap<>();
    private final List<GraphEvent> eventListeners = new ArrayList<>();
    private String entryNode;

    private AgentGraph() {}

    /**
     * 运行时添加事件监听器（用于 SSE 流式推送等场景）。
     */
    public AgentGraph addEvent(GraphEvent listener) {
        this.eventListeners.add(listener);
        return this;
    }

    // ========== Builder ==========

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final AgentGraph graph = new AgentGraph();

        public Builder node(String name, AgentNode node) {
            graph.nodes.put(name, node);
            return this;
        }

        public Builder edge(String from, String to) {
            graph.staticEdges.put(from, to);
            return this;
        }

        public Builder conditionalEdge(String from, ConditionalEdge edge) {
            graph.conditionalEdges.put(from, edge);
            return this;
        }

        public Builder onEvent(GraphEvent listener) {
            graph.eventListeners.add(listener);
            return this;
        }

        public AgentGraph compile(String entryNode) {
            graph.entryNode = entryNode;
            // 验证图结构
            if (!graph.nodes.containsKey(entryNode)) {
                throw new IllegalArgumentException("Entry node '" + entryNode + "' not found");
            }
            for (Map.Entry<String, String> e : graph.staticEdges.entrySet()) {
                String to = e.getValue();
                if (!END.equals(to) && !graph.nodes.containsKey(to)) {
                    throw new IllegalArgumentException("Target node '" + to + "' in edge from '" + e.getKey() + "' not found");
                }
            }
            return graph;
        }
    }

    // ========== 执行 ==========

    /**
     * 执行整个 Graph 流程。
     *
     * @param initialState 初始状态
     * @return 最终状态
     */
    public AgentState execute(AgentState initialState) throws Exception {
        AgentState state = initialState;
        String currentNode = entryNode;
        int steps = 0;
        int maxSteps = 20; // 安全限制

        emitEvent("graph_start", state, null);

        while (currentNode != null && !END.equals(currentNode) && steps < maxSteps) {
            steps++;
            AgentNode node = nodes.get(currentNode);
            if (node == null) {
                throw new IllegalStateException("Node '" + currentNode + "' not found in graph");
            }

            log.info("[AgentGraph] Step {} - Executing node: {}", steps, currentNode);
            emitEvent("node_start", state, currentNode);

            long startTime = System.currentTimeMillis();
            try {
                state = node.execute(state);
                long elapsed = System.currentTimeMillis() - startTime;
                state.recordNodeTiming(currentNode, elapsed);
                log.info("[AgentGraph] Node '{}' completed in {}ms", currentNode, elapsed);
                emitEvent("node_end", state, currentNode);
            } catch (Exception e) {
                long elapsed = System.currentTimeMillis() - startTime;
                state.recordNodeTiming(currentNode, elapsed);
                log.error("[AgentGraph] Node '{}' failed after {}ms: {}", currentNode, elapsed, e.getMessage());
                emitEvent("node_error", state, currentNode);
                throw e;
            }

            // 路由到下一个节点
            currentNode = resolveNext(currentNode, state);
        }

        if (steps >= maxSteps) {
            log.warn("[AgentGraph] Reached max steps ({}), forcing end", maxSteps);
        }

        emitEvent("graph_end", state, null);
        return state;
    }

    private String resolveNext(String current, AgentState state) {
        // 优先检查条件边
        ConditionalEdge condEdge = conditionalEdges.get(current);
        if (condEdge != null) {
            String next = condEdge.route(state);
            log.info("[AgentGraph] Conditional edge from '{}' -> '{}'", current, next);
            return next;
        }
        // 回退到静态边
        String next = staticEdges.get(current);
        if (next == null) {
            log.warn("[AgentGraph] No edge defined from '{}', going to END", current);
            return END;
        }
        return next;
    }

    // ========== 事件 ==========

    private void emitEvent(String type, AgentState state, String nodeId) {
        for (GraphEvent listener : eventListeners) {
            try {
                listener.onEvent(type, state, nodeId);
            } catch (Exception e) {
                log.warn("[AgentGraph] Event listener error: {}", e.getMessage());
            }
        }
    }

    @FunctionalInterface
    public interface GraphEvent {
        void onEvent(String type, AgentState state, String nodeId);
    }
}
