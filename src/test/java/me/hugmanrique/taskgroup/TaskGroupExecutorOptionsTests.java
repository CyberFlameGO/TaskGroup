package me.hugmanrique.taskgroup;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class TaskGroupExecutorOptionsTests {

    @Test
    void testGetters() {
        TaskGroupExecutorOptions options = new TaskGroupExecutorOptions(2, "abc", 3);

        assertEquals(2, options.getParallelism());
        assertEquals("abc", options.getWorkerNameTemplate());
        assertEquals(3, options.getThreadPriority());
    }
}
