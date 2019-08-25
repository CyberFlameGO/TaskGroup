# :briefcase: TaskGroup

[![jitpack][jitpack]][jitpack-url]
[![tests][tests]][tests-url]
[![license][license]][license-url]

**TaskGroup** provides specialized Java `Executor` implementations designed
to run submitted tasks considered safe to run beside each other while the
invoker thread blocks until their completion.

This project is ideal for single-threaded applications that wish to exploit
parallelism while maintaining thread-safety. Once the _invoker thread_ submits
a collection of tasks (either `Callable`s or `Runnable`s), a `TaskGroupExecutor`
will schedule them in a thread pool, blocking until all complete successfully
(i.e. without throwing an exception). Upon exceptional return, tasks that
have not completed are cancelled.

The project's idea came from Paper's [Regionized Entity Ticking](https://github.com/PaperMC/Paper/issues/1001) 
issue, and Aikar's [ParaTask](https://github.com/aikar/paratask) WIP project.
Operations like ticking (i.e. updating) a game world are perfect candidates for
parallelization since work can be split up into tasks that update a specific
region of the world.

More generally, a `TaskGroupExecutor` is designed to run homogeneous tasks
that access unshared thread-confined state. Thread-safety is ensured by
exploiting the memory visibility guarantees made by [`Future.get`](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/Future.html) 
and [`ExecutorService`](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/ExecutorService.html).
In short, actions in the _invoker thread_ prior to the submission of a
collection of tasks to a `TaskGroupExecutor` [_happen-before_](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/package-summary.html#MemoryVisibility)
any actions taken by any task, which in turn _happen-before_ their results
are retrieved through `Future.get`.

However, as with all things, it's not all rainbows and butterflies.
Thread safety is only guaranteed if tasks only access their own state.
Accessing shared state between tasks without proper synchronization can
result in data corruption. Additionally, attempting to acquire a lock
held by the _invoker thread_ from a task will always deadlock
(since the _invoker thread_ is awaiting for the task completion).
As so, task submission should preferably be performed using open calls
(i.e. with no locks held) to avoid deadlock.

Thankfully, `TaskGroupExecutor` provides utilities such as [`#isThreadSafe()`](https://jitpack.io/com/github/hugmanrique/TaskGroup/master-SNAPSHOT/javadoc/me/hugmanrique/taskgroup/TaskGroupExecutor.html#isThreadSafe()) 
to aid implementors assert their programs work correctly.

Finally note that, as with any `ExecutorService`, applications should shut
down a `TaskGroupExecutor` via `#shutdown()` (which performs a shutdown of
the underlying thread pool in which no new tasks will be accepted).

## Installation

You can install TaskGroup using [Maven](https://maven.apache.org/) by adding 
the JitPack repository to your `pom.xml` file:

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>
```

Next, add the `TaskGroup` dependency:

```xml
<dependency>
    <groupId>com.github.hugmanrique</groupId>
    <artifactId>TaskGroup</artifactId>
    <version>master-SNAPSHOT</version>
</dependency>
```

You will need to have Java 8 or later (older versions _might_ work).

## Usage

As an example, we're going to create a parallel world ticker based on regions.
First, we need to create the `TaskGroupExecutor`. Currently, TaskGroup only
provides one implementation based on [`ForkJoinPool`](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/ForkJoinPool.html),
the [`TaskGroupStealingExecutor`](https://jitpack.io/com/github/hugmanrique/TaskGroup/master-SNAPSHOT/javadoc/me/hugmanrique/taskgroup/workstealing/TaskGroupStealingExecutor.html):

```java
public class World {
    
    private final TaskGroupExecutor tickExecutor;
    
    public World() {
        // ...
        Thread invokerThread = Thread.currentThread();
        tickExecutor = TaskGroupStealingExecutor.newInstance(invokerThread);
    }
}
```

You can also use [`TaskGroupStealingExecutor#newInstance(Thread, TaskGroupExecutorOptions)`](https://jitpack.io/com/github/hugmanrique/TaskGroup/master-SNAPSHOT/javadoc/me/hugmanrique/taskgroup/workstealing/TaskGroupStealingExecutor.html#newInstance(java.lang.Thread,me.hugmanrique.taskgroup.TaskGroupExecutorOptions)) 
to provide options used by the underlying thread pool (e.g. parallelism level).

Each world region should have a `getTickTask` that returns a `TickResult` upon completion:

```java
public class TickResult {
    final int entitiesTicked;
    
    public TickResult(int entitiesTicked) {
        this.entitiesTicked = entitiesTicked;
    }
}
```

```java
public class Region {
    
    public Callable<TickResult> getTickTask() {
        return () -> {
            // Remember you can only access unshared local
            // state from this Callable, as it'll be run
            // on a worker thread.
            
            // TODO Perform actual entity ticking
            
            return new TickResult(0);
        };
    }
}
```

Each `World` will then have a `tick()` method that gathers all the regions,
and then asks each of them to create its own tick task:

```java
public class World {
    // ...
    
    public List<Region> getRegions() {
        // TODO Store regions in a local field or compute on demand.
        return new ArrayList<>();
    }
    
    public void tick() {
        List<Callable<TickResult>> tasks = getRegions().stream()
                .map(Region::getTickTask)
                .collect(Collectors.toList());
        
        List<TickResult> results = tickExecutor.computeAll(tasks);
        
        // TODO Log tick results
    }
}
```

The `computeAll` method will submit all tick tasks to a thread pool,
returning a list holding their results when they all complete successfully.
If any tick task throws an unchecked exception or `Error`, tasks
that have not completed are cancelled and a `ExecutionException` wrapping
the unchecked exception is thrown.

## Shared state

Operations that require the coordination of multiple tasks must always
use synchronization to avoid data corruption. Another alternative is to
queue operations in a concurrent collection (like a [`ConcurrentLinkedQueue`](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/ConcurrentLinkedQueue.html))
that can be processed by the _invoker thread_ once all tasks complete.

One such case would be an entity wanting to move from one region to another.
Since entities are stored on the `Region` object, `World` could have a
teleport queue where tasks could add `TeleportRequest`. These could be processed
on the `tick()` method after `computeAll` returns.

## Resources

You can read the [javadoc](https://jitpack.io/com/github/hugmanrique/TaskGroup/master-SNAPSHOT/javadoc/) for more in-depth documentation.

For additional help, you can create an issue and I will try to respond as fast as I can.

## License

[MIT](LICENSE) &copy; [Hugo Manrique](https://hugmanrique.me)


[jitpack]: https://jitpack.io/v/hugmanrique/TaskGroup.svg
[jitpack-url]: https://jitpack.io/#hugmanrique/TaskGroup
[tests]: https://img.shields.io/travis/hugmanrique/TaskGroup/master.svg
[tests-url]: https://travis-ci.org/hugmanrique/TaskGroup
[license]: https://img.shields.io/github/license/hugmanrique/TaskGroup.svg
[license-url]: LICENSE
