package com.example.shared.kernel.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class BaseEntityTest {

    static class DemoRow extends BaseEntity {
    }

    @Test
    void persistSetsBothTimestamps() {
        DemoRow row = new DemoRow();
        assertNull(row.getCreatedAt());

        Instant before = Instant.now();
        row.onPersist();

        assertNotNull(row.getCreatedAt());
        assertNotNull(row.getUpdatedAt());
        assertTrue(!row.getCreatedAt().isBefore(before));
    }

    @Test
    void updateMovesUpdatedAtButNotCreatedAt() throws InterruptedException {
        DemoRow row = new DemoRow();
        row.onPersist();
        Instant createdAt = row.getCreatedAt();
        Instant updatedAt = row.getUpdatedAt();

        Thread.sleep(5);
        row.onUpdate();

        assertEquals(createdAt, row.getCreatedAt());
        assertTrue(row.getUpdatedAt().isAfter(updatedAt));
    }
}
