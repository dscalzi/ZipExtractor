/*
 * This file is part of ZipExtractor.
 * Copyright (C) 2016-2020 Daniel D. Scalzi <https://github.com/dscalzi/ZipExtractor>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.dscalzi.zipextractor.core;

import java.util.Collection;
import java.util.LinkedList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.dscalzi.zipextractor.core.managers.MessageManager;

public class ZServicer {

    private static boolean initialized;
    private static ZServicer instance;

    private int maxQueueSize;
    
    private ThreadPoolExecutor executor;
    private ArrayBlockingQueue<Runnable> queue;

    private Collection<Future<?>> futures = new LinkedList<>();

    private ZServicer(int maxQueueSize, int maxPoolSize) {
        this.maxQueueSize = maxQueueSize;
        this.queue = new ArrayBlockingQueue<>(maxQueueSize);
        this.executor = new ThreadPoolExecutor(1, maxPoolSize, 10, TimeUnit.SECONDS, queue);
    }

    public static void initalize(int limit, int maxPoolSize) {
        if (!initialized) {
            instance = new ZServicer(limit, maxPoolSize);
            initialized = true;
        }
    }

    public static ZServicer getInstance() {
        return ZServicer.instance;
    }

    /**
     * Error code key: 0 = success 1 = queue full 2 = executor is shutdown
     * 
     * @param task
     *            A runnable task to be queued and executed.
     * @return An error code based on the key above
     */
    public int submit(Runnable task) {
        try {
            futures.add(executor.submit(task));
        } catch (RejectedExecutionException e) {
            return executor.isShutdown() ? 2 : 1;
        }
        return 0;
    }

    public int getMaxQueueSize() {
        return maxQueueSize;
    }
    
    public int getSize() {
        return queue.size();
    }

    public int getActive() {
        return executor.getActiveCount();
    }

    public int getQueued() {
        return executor.getQueue().size();
    }

    public boolean isQueueFull() {
        return executor.getQueue().remainingCapacity() == 0;
    }

    public void setMaximumPoolSize(int size) {
        if (executor.getMaximumPoolSize() == size)
            return;
        executor.setMaximumPoolSize(size);
    }

    public boolean isTerminated() {
        return executor.isShutdown();
    }

    public boolean isTerminating() {
        return executor.isTerminating();
    }

    public void terminate(boolean force, boolean wait) {
        if (isTerminated() || isTerminating())
            return;
        MessageManager mm = MessageManager.inst();
        try {
            if (force) {
                mm.info(
                        "Forcing executor service to shutdown. This could be messy if there are outstanding tasks.");
                executor.shutdownNow();
            } else {
                executor.shutdown();
                if ((executor.getActiveCount() > 0 || !executor.getQueue().isEmpty()) && wait) {
                    String info = "Waiting for any outstanding tasks to finish"
                            + ((executor.getActiveCount() > 0) ? " | Active : " + executor.getActiveCount() : "")
                            + ((!executor.getQueue().isEmpty()) ? " | Queued : " + executor.getQueue().size() : "")
                            + ".";
                    mm.info(info);
                    mm.sendGlobal(info, "zipextractor.harmless.notify");
                    for (Future<?> future : futures) {
                        if (future.isDone())
                            continue;
                        future.get();
                    }
                    mm.sendGlobal("All tasks have been completed.",
                            "zipextractor.harmless.notify");
                    mm.info("All tasks have been completed.");
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            mm.severe("Executor servive termination has been interrupted.", e);
        }
    }
}
