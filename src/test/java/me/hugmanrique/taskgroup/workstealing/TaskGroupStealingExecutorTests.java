package me.hugmanrique.taskgroup.workstealing;

import me.hugmanrique.taskgroup.TaskGroupExecutor;
import me.hugmanrique.taskgroup.TaskGroupExecutorOptions;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RejectedExecutionException;

import static org.junit.jupiter.api.Assertions.*;

final class TaskGroupStealingExecutorTests {

    @Test
    void testState() {
        TaskGroupExecutor executor = TaskGroupStealingExecutor.newInstance(Thread.currentThread());

        // Invoker thread operations are considered thread-safe
        assertTrue(executor.isThreadSafe());
        assertFalse(executor.isShutdown());
        assertEquals(Thread.currentThread(), executor.getInvoker());

        executor.shutdown();
    }

    @Test
    void testSingleComputation() throws ExecutionException, InterruptedException {
        Thread invoker = Thread.currentThread();
        TaskGroupExecutor executor = TaskGroupStealingExecutor.newInstance(invoker);

        List<Callable<Car>> tasks = new ArrayList<>();

        tasks.add(() -> {
            // We're running on a executor thread
            assertNotEquals(invoker, Thread.currentThread());
            assertTrue(executor.isThreadSafe());
            assertFalse(executor.isShutdown());

            return new Car("red");
        });

        List<Car> results = executor.computeAll(tasks);

        assertNotNull(results);
        assertEquals(1, results.size());

        Car redCar = results.get(0);
        assertEquals("red", redCar.color);

        executor.shutdown();
    }

    @Test
    void testComputeAll() throws ExecutionException, InterruptedException {
        Thread invoker = Thread.currentThread();
        TaskGroupExecutor executor = TaskGroupStealingExecutor.newInstance(invoker);

        List<Callable<Car>> tasks = new ArrayList<>();

        class CarFactory implements Callable<Car> {
            private final String carColor;

            private CarFactory(String carColor) {
                this.carColor = carColor;
            }

            @Override
            public Car call() {
                // We're running on a executor thread
                assertNotEquals(invoker, Thread.currentThread());
                assertTrue(executor.isThreadSafe());
                assertFalse(executor.isShutdown());

                return new Car(carColor);
            }
        }

        tasks.add(new CarFactory("red"));
        tasks.add(new CarFactory("green"));
        tasks.add(new CarFactory("blue"));

        List<Car> results = executor.computeAll(tasks);

        assertNotNull(results);
        assertEquals(3, results.size());

        // Results must be in submission order
        assertEquals("red", results.get(0).color);
        assertEquals("green", results.get(1).color);
        assertEquals("blue", results.get(2).color);

        executor.shutdown();
    }

    @Test
    void testInvokeAll() throws ExecutionException, InterruptedException {
        Thread invoker = Thread.currentThread();
        TaskGroupExecutor executor = TaskGroupStealingExecutor.newInstance(invoker);

        List<Runnable> tasks = new ArrayList<>();
        final int taskCount = 3;
        CountDownLatch latch = new CountDownLatch(taskCount);

        class CountDownRunnable implements Runnable {
            @Override
            public void run() {
                // We're running on a executor thread
                assertNotEquals(invoker, Thread.currentThread());
                assertTrue(executor.isThreadSafe());
                assertFalse(executor.isShutdown());

                latch.countDown();
            }
        }

        for (int i = 0; i < taskCount; i++) {
            tasks.add(new CountDownRunnable());
        }

        executor.invokeAll(tasks);
        // Invoker thread blocks until all tasks complete so
        // the latch must have reached zero at this point.
        assertEquals(0, latch.getCount(), "All tasks must have run");

        executor.shutdown();
    }

    @Test
    void testExternalSubmissionFails() throws InterruptedException {
        TaskGroupExecutor executor = TaskGroupStealingExecutor.newInstance(Thread.currentThread());

        Thread external = new Thread(() -> {
            List<Runnable> tasks = new ArrayList<>();

            // These tasks should never run
            tasks.add(Assertions::fail);
            tasks.add(Assertions::fail);

            assertThrows(IllegalStateException.class, () -> {
                // Cannot invoke task from non-invoker thread
                executor.invokeAll(tasks);
            });
        });

        external.start();
        external.join();

        executor.shutdown();
    }

    @Test
    void testOptions() throws ExecutionException, InterruptedException {
        TaskGroupExecutorOptions options = new TaskGroupExecutorOptions(
                2, "foo", 3);

        Thread invoker = Thread.currentThread();
        TaskGroupExecutor executor = TaskGroupStealingExecutor.newInstance(invoker, options);

        List<Runnable> tasks = new ArrayList<>();
        
        tasks.add(() -> {
            Thread workerThread = Thread.currentThread();
            
            assertNotEquals(workerThread, invoker);
            assertTrue(workerThread.getName().startsWith("foo"));
            assertEquals(3, workerThread.getPriority());
        });

        executor.invokeAll(tasks);
        executor.shutdown();
    }

    @Test
    void testShutdown() throws InterruptedException {
        TaskGroupExecutor executor = TaskGroupStealingExecutor.newInstance(Thread.currentThread());

        executor.shutdown();
        Thread.sleep(1000L); // TODO Is 1 second enough?

        assertTrue(executor.isShutdown());

        // A task submission attempt must fail
        List<Runnable> tasks = new ArrayList<>();
        tasks.add(Assertions::fail);

        assertThrows(RejectedExecutionException.class, () -> {
            executor.invokeAll(tasks);
        }, "Submission must throw when shut down");
    }

    @Test
    void testExternalShutdownFails() throws InterruptedException {
        TaskGroupExecutor executor = TaskGroupStealingExecutor.newInstance(Thread.currentThread());

        Thread external = new Thread(() -> {
            // Cannot shutdown from external thread
            assertThrows(IllegalStateException.class, executor::shutdown);
        });

        external.start();
        external.join();

        executor.shutdown();
    }

    private static final class Car {

        private final String color;

        private Car(String color) {
            this.color = color;
        }
    }
}
