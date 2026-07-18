package com.robin.blogback.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class UserContextTest {

    @AfterEach
    void cleanup() {
        UserContext.clear();
    }

    @Test
    void doesNotFallBackToAnotherThreadsUser() {
        assertNull(UserContext.getUserId());
    }

    @Test
    void keepsCurrentThreadUser() {
        UserContext.setUserId(7);
        assertEquals(7, UserContext.getUserId());
    }
}
