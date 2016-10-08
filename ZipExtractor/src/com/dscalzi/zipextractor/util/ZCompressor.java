package com.dscalzi.zipextractor.util;

import java.io.File;
import java.io.IOException;
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
	
	public int asyncCompress(CommandSender sender, File srcLoc, File destLoc){
		if(!srcLoc.exists()){
			mm.invalidPath(sender, "source");
			return -1;
		}
		String fileExtension = plugin.getFileExtension(destLoc);
		String properPath = destLoc.getAbsolutePath();
		if(fileExtension.equals(""))
			properPath = destLoc.toString() + ".zip";
		else if(!fileExtension.equals(".zip"))
			properPath = destLoc.getAbsolutePath().substring(0, destLoc.getAbsolutePath().lastIndexOf(".")) + ".zip";
		
		File dF = new File(properPath);
		
		Thread th = new Thread(() -> {
			compressToZip(sender, srcLoc, dF);
			synchronized(this){
				notify();
			}
		});
		ZServicer.getInstance().submit(th);
		
		mm.startingProcess(sender, "compression", srcLoc.getName());
		return ZServicer.getInstance().getSize();
	}
	
	private void compressToZip(CommandSender sender, File sourceFile, File destFolder){
		Logger logger = mm.getLogger();
		boolean log = cm.getLoggingProperty();
		try {
			ZipOutputStream zs = new ZipOutputStream(Files.newOutputStream(destFolder.toPath()));
	        Path pp = sourceFile.toPath();
	        Files.walk(pp)
	          .filter(path -> !Files.isDirectory(path))
	          .forEach(path -> {
	              String sp = path.toAbsolutePath().toString().replace(pp.toAbsolutePath().toString(), "").substring(1);
	              ZipEntry zipEntry = new ZipEntry(pp.getFileName() + File.separator + sp);
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
	        zs.close();
	        mm.compressionComplete(sender, destFolder.getAbsolutePath());
	    } catch (AccessDeniedException e) {
	    	mm.fileAccessDenied(sender, ZTask.COMPRESS, e.getMessage());
	    } catch (IOException e) {
			e.printStackTrace();
		}
	}
}
