/*
 * ZipExtractor
 * Copyright (C) 2016-2018 Daniel D. Scalzi
 * See LICENSE for license information.
 */
package com.dscalzi.zipextractor.util;

public enum ZTask {

	COMPRESS("compression"),
	EXTRACT("extraction"),
	SCAN("scan");
	
	private final String processName;
	
	ZTask(String processName){
		this.processName = processName;
	}
	
	public String getProcessName(){
		return this.processName;
	}
	
}
