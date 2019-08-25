package me.hugmanrique.taskgroup;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

/**
 * An {@link Executor} that runs submitted tasks considered
 * safe to run beside each other while the specified
 * invoker thread blocks until their completion.
 *
 * <p>This executor is designed for homogeneous tasks that
 * access unshared thread-confined state. Synchronization
 * is required to access shared state between tasks;
 * failure to do so can result in data corruption.
 * Attempting to acquire a lock held by the invoker
 * thread from a task will always deadlock.
 *
 * <p>Tasks can only be submitted from the invoker thread.
 * Submitting tasks from any other thread will result in
 * a {@link IllegalStateException}. Additionally, the
 * invoker is advised to submit tasks using open calls
 * (i.e. with no locks held) to avoid deadlock.
 *
 * <p>Memory consistency effects: Actions in the invoker
 * thread prior to the submission of {@link Runnable} or
 * {@link Callable} tasks to a {@code TaskGroupExecutor}
 * <i>happen-before</i> any actions taken by any task,
 * which in turn <i>happen-before</i> their completion.
 *
 * @since 1.0.0
 * @author Hugo Manrique
 */
public interface TaskGroupExecutor {

    /**
     * Initiates an orderly shutdown of the underlying thread pool
     * in which no new tasks will be accepted. Invocation has no
     * effect if already shut down.
     *
     * @throws IllegalStateException if this method is called
     *         from a thread other than the invoker thread
     */
    void shutdown();

    /**
     * Returns {@code true} if this executor has been shut down.
     *
     * @return {@code true} if this executor has been shut down
     */
    boolean isShutdown();

    /**
     * Returns the invoker thread, the only thread that can
     * submit tasks to this executor.
     *
     * @return the invoker thread of this executor
     */
    Thread getInvoker();

    /**
     * Returns {@code true} if this executor is currently
     * working on submitted tasks, essentially blocking
     * the invoker thread.
     *
     * @return {@code true} if this executor is currently working
     */
    boolean isSuspended();

    /**
     * Returns {@code true} if the current thread can access the
     * local state of a task, either because it is the invoker
     * thread and no tasks are currently running, or it is
     * a worker thread actively executing a submitted task.
     *
     * @return {@code true} if the current thread can access
     *         the local state of a task
     * @see TaskGroupExecutor for memory visibility
     *      guarantees made by this executor
     */
    boolean isThreadSafe();

    /**
     * Executes the given tasks, returning a list holding their
     * results when all complete successfully (i.e. without
     * throwing an unchecked exception or {@link Error}).
     * Upon exceptional return, tasks that have not completed
     * are cancelled.
     *
     * <p>Modifying the given collection while the tasks are
     * running will yield undefined results.
     *
     * @param tasks the collection of tasks
     * @param <T> the type of the values returned by the tasks
     * @return a list of the computed results, in the same
     *         sequential order as produced by the iterator
     *         of the given tasks collection
     * @throws InterruptedException if interrupted while waiting, in
     *         which case unfinished tasks are cancelled
     * @throws NullPointerException if tasks, or any of its elements
     *         are {@code null}
     * @throws IllegalStateException if this method is called
     *         from a thread other than the invoker thread
     * @throws ExecutionException if a task aborted by throwing
     *         an exception
     * @throws RejectedExecutionException if any task cannot be
     *         scheduled for execution
     */
    <T> List<T> computeAll(Collection<? extends Callable<T>> tasks)
            throws InterruptedException, ExecutionException;

    /**
     * Executes the given tasks, blocking until all complete successfully
     * (i.e. without throwing an unchecked exception or {@link Error}).
     * Upon exceptional return, tasks that have not completed are
     * cancelled.
     *
     * <p>Modifying the given collection while the tasks are
     * running will yield undefined results.
     *
     * @param tasks the collection of tasks
     * @throws InterruptedException if interrupted while waiting, in
     *         which case unfinished tasks are cancelled
     * @throws NullPointerException if tasks, or any of its elements
     *         are {@code null}
     * @throws IllegalStateException if this method is called
     *         from a thread other than the invoker thread
     * @throws ExecutionException if a task aborted by throwing
     *         an exception
     * @throws RejectedExecutionException if any task cannot be
     *         scheduled for execution
     */
    void invokeAll(Collection<? extends Runnable> tasks)
            throws InterruptedException, ExecutionException;
}
