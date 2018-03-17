/*
 * ZipExtractor
 * Copyright (C) 2016-2018 Daniel D. Scalzi
 * See LICENSE for license information.
 */
package com.dscalzi.zipextractor.util;

public class TaskInterruptedException extends RuntimeException{

	private static final long serialVersionUID = 3942881135656327340L;

	public TaskInterruptedException(){
		
	}
	
	public TaskInterruptedException(String paramString){
		super(paramString);
	}
	  
	public TaskInterruptedException(String paramString, Throwable paramThrowable){
		super(paramString, paramThrowable);
	}
	  
	public TaskInterruptedException(Throwable paramThrowable){
		super(paramThrowable);
	}
	
}
