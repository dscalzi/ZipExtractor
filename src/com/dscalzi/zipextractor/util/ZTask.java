package com.dscalzi.zipextractor.util;

public enum ZTask {

	COMPRESS("compression"),
	EXTRACT("extraction");
	
	private final String processName;
	
	ZTask(String processName){
		this.processName = processName;
	}
	
	public String getProcessName(){
		return this.processName;
	}
	
}
