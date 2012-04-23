// Copyright © 2011-2012, Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.jumi.actors;

import javax.annotation.concurrent.ThreadSafe;
import java.util.*;
import java.util.concurrent.*;

@ThreadSafe
public class MultiThreadedActors extends Actors {

    private final Set<Thread> actorThreads = Collections.synchronizedSet(new HashSet<Thread>());
    private final ExecutorService unattendedWorkers;

    public MultiThreadedActors(Eventizer<?>... factories) {
        this(Executors.newCachedThreadPool(), factories);
    }

    public MultiThreadedActors(ExecutorService threadPool, Eventizer<?>... factories) {
        super(factories);
        unattendedWorkers = threadPool;
    }

    @Override
    protected <T> void startEventPoller(String name, Actor<T> actor) {
        Thread t = new Thread(new EventPoller<T>(actor), name);
        t.start();
        actorThreads.add(t);
    }

    @Override
    protected void doStartUnattendedWorker(Runnable worker) {
        unattendedWorkers.execute(worker);
    }

    public void shutdown(long timeout) throws InterruptedException {
        for (Thread t : actorThreads) {
            t.interrupt();
        }
        unattendedWorkers.shutdown();
        for (Thread t : actorThreads) {
            t.join(timeout);
        }
        unattendedWorkers.awaitTermination(timeout, TimeUnit.MILLISECONDS);
    }


    @ThreadSafe
    private static class EventPoller<T> implements Runnable {
        private final Actor<T> actor;

        public EventPoller(Actor<T> actor) {
            this.actor = actor;
        }

        @Override
        public void run() {
            try {
                while (!Thread.interrupted()) {
                    actor.processNextMessage();
                }
            } catch (InterruptedException e) {
                // actor was told to exit
            }
        }
    }
}
