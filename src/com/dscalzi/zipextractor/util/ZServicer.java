/*
 * ZipExtractor
 * Copyright (C) 2017 Daniel D. Scalzi
 * See License.txt for license information.
 */
package com.dscalzi.zipextractor.util;

import java.util.Collection;
import java.util.LinkedList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import com.dscalzi.zipextractor.managers.MessageManager;

public class ZServicer {

	private static boolean initialized;
	private static ZServicer instance;
	
	private ThreadPoolExecutor executor;
	private ArrayBlockingQueue<Runnable> queue;
	
	private Collection<Future<?>> futures = new LinkedList<Future<?>>();
	
	private ZServicer(int maxQueueSize, int maxPoolSize){
		this.queue = new ArrayBlockingQueue<Runnable>(maxQueueSize);
		this.executor = new ThreadPoolExecutor(1, maxPoolSize, 10, TimeUnit.SECONDS,queue);
	}
	
	public static void initalize(int limit, int maxPoolSize){
		if(!initialized){
			instance = new ZServicer(limit, maxPoolSize);
			initialized = true;
		}
	}
	
	public static ZServicer getInstance(){
		return ZServicer.instance;
	}
	
	/**
	 * Error code key:
	 * 0 = success
	 * 1 = queue full
	 * 2 = executor is shutdown
	 * 
	 * @param task A runnable task to be queued and executed.
	 * @return An error code based on the key above
	 */
	public int submit(Runnable task){
		try{
			futures.add(executor.submit(task));
		} catch (RejectedExecutionException e){
			return executor.isShutdown() ? 2 : 1;
		}
		return 0;
	}
	
	public int getSize(){
		return queue.size();
	}
	
	public void setMaximumPoolSize(int size){
		if(executor.getMaximumPoolSize() == size) return;
		executor.setMaximumPoolSize(size);
	}
	
	public boolean isTerminated(){
		return executor.isShutdown();
	}
	
	public boolean isTerminating(){
		return executor.isTerminating();
	}
	
	public void terminate(boolean force, boolean wait){
		if(isTerminated() || isTerminating()) return;
		try {
			if(force){
				MessageManager.getInstance().getLogger().info("Forcing executor service to shutdown. This could be messy if there are outstanding tasks.");
				executor.shutdownNow();
			} else {
				executor.shutdown();
				if((executor.getActiveCount() > 0 || executor.getQueue().size() > 0) && wait){
					String info = "Waiting for any outstanding tasks to finish" + ((executor.getActiveCount() > 0) ? " | Active : " + executor.getActiveCount() : "") + ((executor.getQueue().size() > 0) ? " | Queued : " + executor.getQueue().size() : "") + ".";
					MessageManager.getInstance().getLogger().info(info);
					MessageManager.getInstance().sendGlobal(info, "zipextractor.harmless.notify");
					for (Future<?> future : futures) {
						if(future.isDone()) continue;
					    future.get();
					}
					MessageManager.getInstance().sendGlobal("All tasks have been completed.", "zipextractor.harmless.notify");
					MessageManager.getInstance().getLogger().info("All tasks have been completed.");
				}
			}
		} catch (InterruptedException | ExecutionException e) {
			MessageManager.getInstance().getLogger().log(Level.SEVERE, "Executor servive termination has been interrupted.", e);
		}
	}
}
