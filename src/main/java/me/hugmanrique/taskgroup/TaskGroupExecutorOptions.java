package me.hugmanrique.taskgroup;

import javax.annotation.Nullable;

import static java.util.Objects.requireNonNull;

/**
 * Represents an immutable holder of options
 * for a {@link TaskGroupExecutor}.
 *
 * @since 1.0.0
 * @author Hugo Manrique
 */
@SuppressWarnings("WeakerAccess")
public class TaskGroupExecutorOptions {

    private static final String DEFAULT_WORKER_THREAD_NAME = "TaskGroupWorker";

    private final int parallelism;
    private final String workerNameTemplate;
    private final Integer threadPriority;

    /**
     * Creates a {@code TaskGroupExecutorOptions} object using
     * defaults for all parameters.
     */
    public TaskGroupExecutorOptions() {
        this(Runtime.getRuntime().availableProcessors());
    }

    /**
     * Creates a {@code TaskGroupExecutorOptions} object with
     * the given parallelism level, using defaults for all
     * other parameters.
     *
     * @param parallelism the targeted parallelism level
     */
    public TaskGroupExecutorOptions(int parallelism) {
        this(parallelism, DEFAULT_WORKER_THREAD_NAME);
    }

    /**
     * Creates a {@code TaskGroupExecutorOptions} object with the
     * given parallelism level and worker thread name template,
     * using defaults for all other parameters.
     *
     * @param parallelism the targeted parallelism level
     * @param workerNameTemplate the worker thread name template
     */
    public TaskGroupExecutorOptions(int parallelism, String workerNameTemplate) {
        this(parallelism, workerNameTemplate, null);
    }

    /**
     * Creates a {@code TaskGroupExecutorOptions} object with the
     * given parallelism level, worker thread name template, and
     * worker thread priority level.
     *
     * @param parallelism the targeted parallelism level
     * @param workerNameTemplate the worker thread name template
     * @param threadPriority the priority of worker threads, {@code null}
     *                       indicates the default value
     */
    public TaskGroupExecutorOptions(int parallelism, String workerNameTemplate, @Nullable Integer threadPriority) {
        if (parallelism < 1) {
            throw new IllegalArgumentException("Parallelism cannot be less than 1");
        }

        this.parallelism = parallelism;
        this.workerNameTemplate = requireNonNull(workerNameTemplate, "workerNameTemplate");
        this.threadPriority = threadPriority;
    }

    /**
     * Returns the targeted parallelism level of the executor.
     *
     * @return the targeted parallelism level
     */
    public int getParallelism() {
        return parallelism;
    }

    /**
     * Returns the worker thread name template. The default
     * thread worker factory appends a counter at the end
     * of this string.
     *
     * @return the worker thread name template
     */
    public String getWorkerNameTemplate() {
        return workerNameTemplate;
    }

    /**
     * Returns the priority of worker threads. A {@code null}
     * value indicates the default thread priority.
     *
     * @return the priority of worker threads
     */
    @Nullable
    public Integer getThreadPriority() {
        return threadPriority;
    }
}
