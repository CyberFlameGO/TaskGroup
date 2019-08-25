package me.hugmanrique.taskgroup.workstealing;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;

final class TaskGroupStealingWorkerThread extends ForkJoinWorkerThread {

    TaskGroupStealingWorkerThread(ForkJoinPool pool) {
        super(pool);
    }
}
