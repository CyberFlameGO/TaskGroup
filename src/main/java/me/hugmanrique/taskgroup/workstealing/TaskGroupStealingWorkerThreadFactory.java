package me.hugmanrique.taskgroup.workstealing;

import me.hugmanrique.taskgroup.TaskGroupExecutorOptions;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Objects.requireNonNull;

final class TaskGroupStealingWorkerThreadFactory implements ForkJoinPool.ForkJoinWorkerThreadFactory {

    private final TaskGroupExecutorOptions options;
    private final AtomicInteger threadCount = new AtomicInteger(1);

    TaskGroupStealingWorkerThreadFactory(TaskGroupExecutorOptions options) {
        this.options = requireNonNull(options, "options");
    }

    private String getWorkerThreadName() {
        return options.getWorkerNameTemplate() + " #" + threadCount.getAndIncrement();
    }

    @Override
    public ForkJoinWorkerThread newThread(ForkJoinPool pool) {
        TaskGroupStealingWorkerThread thread = new TaskGroupStealingWorkerThread(pool);
        thread.setName(getWorkerThreadName());

        // Set thread priority if set
        Integer priority = options.getThreadPriority();

        if (priority != null) {
            thread.setPriority(priority);
        }

        return thread;
    }
}
