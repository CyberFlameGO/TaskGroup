package me.hugmanrique.taskgroup.workstealing;

import me.hugmanrique.taskgroup.AbstractTaskGroupExecutor;
import me.hugmanrique.taskgroup.TaskGroupExecutorOptions;

import java.util.concurrent.ForkJoinPool;

import static java.util.Objects.requireNonNull;

/**
 * A {@code TaskGroupExecutor} that submits tasks to a {@link ForkJoinPool}.
 */
@SuppressWarnings("WeakerAccess")
public class TaskGroupStealingExecutor extends AbstractTaskGroupExecutor {

    /**
     * Creates a {@code TaskGroupStealingExecutor} with the specified
     * invoker thread and default options.
     *
     * @param invokerThread the only thread allowed to invoke tasks
     *                      on this executor
     * @return a new {@code TaskGroupStealingExecutor} instance
     */
    public static TaskGroupStealingExecutor newInstance(Thread invokerThread) {
        return newInstance(invokerThread, new TaskGroupExecutorOptions());
    }

    /**
     * Creates a {@code TaskGroupStealingExecutor} with the specified
     * invoker thread and options.
     *
     * @param invokerThread the only thread allowed to invoke tasks
     *                      on this executor
     * @param options the options of the underlying thread pool
     * @return a new {@code TaskGroupStealingExecutor} instance
     */
    public static TaskGroupStealingExecutor newInstance(Thread invokerThread, TaskGroupExecutorOptions options) {
        requireNonNull(invokerThread, "invokerThread");
        requireNonNull(options, "options");

        ForkJoinPool pool = new ForkJoinPool(
                options.getParallelism(),
                new TaskGroupStealingWorkerThreadFactory(options),
                null,
                false);

        return new TaskGroupStealingExecutor(invokerThread, pool);
    }

    private TaskGroupStealingExecutor(Thread invoker, ForkJoinPool pool) {
        super(invoker, pool);
    }

    @Override
    public boolean isThreadSafe() {
        Thread thread = Thread.currentThread();

        return thread == invoker ||
                (thread instanceof TaskGroupStealingWorkerThread &&
                        service.equals(((TaskGroupStealingWorkerThread) thread).getPool()) &&
                        isSuspended);
    }

    // The default `computeAll` and `invokeAll` are ideal for a
    // work stealing executor since the worker threads that
    // complete initial tasks will run the last non-started
    // task, essentially reducing the possibility of locking
    // on the last future (`.get()`) collection elements.
}
