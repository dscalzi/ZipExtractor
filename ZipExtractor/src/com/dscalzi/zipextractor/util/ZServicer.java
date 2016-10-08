package com.dscalzi.zipextractor.util;

import java.util.concurrent.ConcurrentLinkedQueue;

public class ZServicer {

	private static boolean initialized;
	private static ZServicer instance;
	
	private ConcurrentLinkedQueue<Thread> queue;
	private volatile boolean inExec;
	
	private ZServicer(){
		this.queue = new ConcurrentLinkedQueue<Thread>();
		this.inExec = false;
	}
	
	public static void initalize(){
		if(!initialized){
			instance = new ZServicer();
			initialized = true;
		}
	}
	
	public static ZServicer getInstance(){
		return ZServicer.instance;
	}
	
	public void submit(Thread th){
		queue.add(th);
		if(!inExec)
			runTasks();
	}
	
	private void runTasks(){
		Thread worker = new Thread(() -> {
			inExec = true;
			Thread th;
			while((th = queue.poll()) != null){
				th.start();
				synchronized(th){
					System.out.println("Waiting for thread " + th.toString());
					try {
						th.wait();
					} catch (InterruptedException e) {
						System.out.println("Error while processing queue, all items in the current queue will be purged.");
						e.printStackTrace();
						queue.clear();
						inExec = false;
						break;
					}
				}
			}
			inExec = false;
		});
		worker.start();
	}
	
	public int getSize(){
		return queue.size();
	}
}
