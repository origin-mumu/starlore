package com.robin.blogback.graph;

/**
 * 条件边接口。根据当前状态决定下一个节点。
 */
@FunctionalInterface
public interface ConditionalEdge {

    /**
     * 根据状态决定路由目标。
     *
     * @param state 当前共享状态
     * @return 目标节点名称（必须与 Graph 中注册的节点名匹配）
     */
    String route(AgentState state);
}
