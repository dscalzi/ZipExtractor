/*
 * ZipExtractor
 * Copyright (C) 2017 Daniel D. Scalzi
 * See License.txt for license information.
 */
package com.dscalzi.zipextractor.util;

import java.io.File;
import java.io.OutputStream;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.bukkit.command.CommandSender;

import com.dscalzi.zipextractor.ZipExtractor;
import com.dscalzi.zipextractor.managers.ConfigManager;
import com.dscalzi.zipextractor.managers.MessageManager;

public class ZCompressor {

	private final MessageManager mm;
	private final ConfigManager cm;
	
	private ZipExtractor plugin;
	
	public ZCompressor(ZipExtractor plugin){
		this.mm = MessageManager.getInstance();
		this.cm = ConfigManager.getInstance();
		this.plugin = plugin;
	}
	
	public void asyncCompress(CommandSender sender, File srcLoc, File destLoc){
		if(!srcLoc.exists()){
			mm.invalidPath(sender, "source");
			return;
		}
		String fileExtension = plugin.getFileExtension(destLoc);
		String properPath = destLoc.getAbsolutePath();
		if(fileExtension.equals(""))
			properPath = destLoc.toString() + ".zip";
		else if(!fileExtension.equals(".zip"))
			properPath = destLoc.getAbsolutePath().substring(0, destLoc.getAbsolutePath().lastIndexOf(".")) + ".zip";
		
		File dF = new File(properPath);
		
		Runnable task = () -> {
			compressToZip(sender, srcLoc, dF);
			synchronized(this){
				notify();
			}
		};
		int result = ZServicer.getInstance().submit(task);
		if(result == 0)
			mm.addToQueue(sender, ZServicer.getInstance().getSize());
		else if(result == 1)
			mm.queueFull(sender);
		else if(result == 2)
			mm.executorTerminated(sender, ZTask.COMPRESS);
	}
	
	private void compressToZip(CommandSender sender, File sourceFile, File destFolder){
		Logger logger = mm.getLogger();
		boolean log = cm.getLoggingProperty();
		mm.startingProcess(sender, ZTask.COMPRESS, sourceFile.getName());
		try(OutputStream os = Files.newOutputStream(destFolder.toPath());
			ZipOutputStream zs = new ZipOutputStream(os);){
	        Path pp = sourceFile.toPath();
	        Files.walk(pp)
	          .filter(path -> !Files.isDirectory(path))
	          .forEach(path -> {
	        	  if (Thread.interrupted())
	        		  throw new TaskInterruptedException();
	        	  //Prevent recursive compressions
	        	  if(path.equals(destFolder.toPath()))
	        		  return;
	        	  String sp = path.toAbsolutePath().toString().replace(pp.toAbsolutePath().toString(), "");
	        	  if(sp.length() > 0)
	        		  sp = sp.substring(1);
	              ZipEntry zipEntry = new ZipEntry(pp.getFileName() + ((sp.length() > 0) ? (File.separator + sp) : ""));
	              try {
	            	  if(log)
	            		  logger.info("Compressing : "+ zipEntry.toString());
	                  zs.putNextEntry(zipEntry);
	                  zs.write(Files.readAllBytes(path));
	                  zs.closeEntry();
	              } catch (Exception e) {
	            	  e.printStackTrace();
	              }
	          });
	        mm.compressionComplete(sender, destFolder.getAbsolutePath());
	    } catch (AccessDeniedException e) {
	    	mm.fileAccessDenied(sender, ZTask.COMPRESS, e.getMessage());
	    } catch (TaskInterruptedException e) {
	    	mm.taskInterruption(sender, ZTask.COMPRESS);
	    } catch (Throwable e) {
			e.printStackTrace();
		}
	}
}
