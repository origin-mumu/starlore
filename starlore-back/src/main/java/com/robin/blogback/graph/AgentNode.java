package com.robin.blogback.graph;

/**
 * Graph 节点接口。每个节点接收状态、执行逻辑、返回更新后的状态。
 */
@FunctionalInterface
public interface AgentNode {

    /**
     * 执行节点逻辑。
     *
     * @param state 当前共享状态
     * @return 更新后的状态
     * @throws Exception 节点执行异常
     */
    AgentState execute(AgentState state) throws Exception;

    /**
     * 节点名称，用于 tracing 和日志。
     */
    default String name() {
        return getClass().getSimpleName();
    }
}
