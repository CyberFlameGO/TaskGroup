package me.hugmanrique.taskgroup;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import static java.util.Objects.requireNonNull;

/**
 * Provides default task submission and shutdown implementations
 * for a {@link TaskGroupExecutor}.
 *
 * @since 1.0.0
 * @author Hugo Manrique
 */
public abstract class AbstractTaskGroupExecutor implements TaskGroupExecutor {

    private static <T> void cancelAll(List<Future<T>> tasks) {
        for (Future<T> task : tasks) {
            task.cancel(true);
        }
    }

    protected final Thread invoker;

    protected final ExecutorService service;
    protected volatile boolean isSuspended = false;

    protected AbstractTaskGroupExecutor(Thread invoker, ExecutorService service) {
        requireNonNull(service, "service");

        if (service.isShutdown()) {
            throw new IllegalArgumentException("Cannot instantiate executor with shut down service");
        }

        this.invoker = requireNonNull(invoker, "invoker");
        this.service = service;
    }

    private void ensureInvoker() {
        Thread thread = Thread.currentThread();

        if (thread != invoker) {
            throw new IllegalStateException("Cannot run operation on non-invoker thread");
        }
    }

    @Override
    public void shutdown() {
        ensureInvoker();
        service.shutdown();
    }

    @Override
    public boolean isShutdown() {
        return service.isShutdown();
    }

    @Override
    public Thread getInvoker() {
        return invoker;
    }

    @Override
    public boolean isSuspended() {
        // Thread-safe, volatile read
        return isSuspended;
    }

    @Override
    public <T> List<T> computeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        requireNonNull(tasks, "tasks");

        ensureInvoker();
        List<Future<T>> futures = new ArrayList<>(tasks.size());

        try {
            isSuspended = true;

            // Submit all tasks
            for (Callable<T> task : tasks) {
                Future<T> future = service.submit(
                        requireNonNull(task, "task"));

                futures.add(future);
            }

            List<T> results = new ArrayList<>();

            // Await for all results in sequential order to
            // avoid excessive context switches that would
            // occur if we used a CompletionService.
            // This guarantees an average of n/2 context
            // switches (the underlying ExecutorService can
            // cause arbitrarily more), worst case of n (when
            // all tasks complete sequentially), and best
            // case of 1 (when the first future is the last
            // to complete).

            for (Future<T> future : futures) {
                T result = future.get();

                results.add(result);
            }

            // All tasks completed successfully
            return results;
        } catch (Throwable throwable) {
            cancelAll(futures);

            // Either a task couldn't be submitted, a task
            // threw an unchecked exception (or Error), or
            // the current thread was interrupted.
            throw throwable;
        } finally {
            isSuspended = false;
        }
    }

    @Override
    public void invokeAll(Collection<? extends Runnable> tasks) throws InterruptedException, ExecutionException {
        requireNonNull(tasks, "tasks");

        ensureInvoker();
        List<Future<Void>> futures = new ArrayList<>(tasks.size());

        try {
            isSuspended = true;

            // Submit all tasks
            for (Runnable task : tasks) {
                @SuppressWarnings("unchecked")
                Future<Void> future = (Future<Void>) service.submit(
                        requireNonNull(task, "task"));

                futures.add(future);
            }

            // (see await-related comment on `computeAll`)
            for (Future<?> future : futures) {
                future.get();
            }

            // All tasks completed successfully
        } catch (Throwable throwable) {
            cancelAll(futures);

            // Either a task couldn't be submitted, a task
            // threw an unchecked exception (or Error), or
            // the current thread was interrupted
            throw throwable;
        } finally {
            isSuspended = false;
        }
    }
}
