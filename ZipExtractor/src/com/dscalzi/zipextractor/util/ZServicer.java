package com.dscalzi.zipextractor.util;

import java.util.concurrent.ArrayBlockingQueue;
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
			executor.submit(task);
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
		try {
			if(force){
				MessageManager.getInstance().getLogger().info("Forcing executor service to shutdown. This could be messy if there are outstanding tasks.");
				executor.shutdownNow();
			}
			executor.shutdown();
			if(wait){
				MessageManager.getInstance().getLogger().info("Waiting a maximum of 15 seconds for any outstanding task to finish.");
				executor.awaitTermination(15, TimeUnit.SECONDS);
			}
		} catch (InterruptedException e) {
			MessageManager.getInstance().getLogger().log(Level.SEVERE, "Executor servive termination has been interrupted.", e);
		}
	}
}
