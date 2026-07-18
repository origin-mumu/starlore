package com.robin.blogback.graph;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentGraphIsolationTest {

    @Test
    void requestListenersAreIsolatedByExecutionThread() throws Exception {
        AgentGraph graph = AgentGraph.builder()
                .node("only", state -> state)
                .edge("only", AgentGraph.END)
                .compile("only");

        AtomicInteger firstEvents = new AtomicInteger();
        AtomicInteger secondEvents = new AtomicInteger();
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        Thread first = new Thread(() -> execute(graph, firstEvents, ready, start));
        Thread second = new Thread(() -> execute(graph, secondEvents, ready, start));
        first.start();
        second.start();
        assertTrue(ready.await(2, TimeUnit.SECONDS));
        start.countDown();
        first.join();
        second.join();

        assertEquals(4, firstEvents.get());
        assertEquals(4, secondEvents.get());
    }

    private static void execute(AgentGraph graph, AtomicInteger events,
                                CountDownLatch ready, CountDownLatch start) {
        graph.addEvent((type, state, nodeId) -> events.incrementAndGet());
        ready.countDown();
        try {
            start.await();
            graph.execute(new AgentState());
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            graph.clearEvents();
        }
    }
}
