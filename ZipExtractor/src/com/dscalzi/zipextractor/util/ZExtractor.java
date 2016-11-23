package com.dscalzi.zipextractor.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AccessDeniedException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.bukkit.command.CommandSender;

import com.dscalzi.zipextractor.ZipExtractor;
import com.dscalzi.zipextractor.managers.ConfigManager;
import com.dscalzi.zipextractor.managers.MessageManager;
import com.github.junrar.Archive;
import com.github.junrar.exception.RarException;
import com.github.junrar.impl.FileVolumeManager;
import com.github.junrar.rarfile.FileHeader;

public class ZExtractor {

	private final MessageManager mm;
	private final ConfigManager cm;
	
	private ZipExtractor plugin;
	
	public ZExtractor(ZipExtractor plugin){
		this.mm = MessageManager.getInstance();
		this.cm = ConfigManager.getInstance();
		this.plugin = plugin;
	}
	
	public void asyncExtract(CommandSender sender, File srcLoc, File destLoc){
		String fileExtension = plugin.getFileExtension(srcLoc);
		if(fileExtension.length() == 0 || !srcLoc.exists()) {
			mm.invalidPath(sender, "source");
			return;
		}
		
		try{
			Paths.get(srcLoc.getAbsolutePath());
		} catch(InvalidPathException e){
			mm.invalidPath(sender, "destination");
			return;
		}
		
		if(!destLoc.exists())
    		destLoc.mkdir();
		
		if(destLoc.exists()){
			if(!destLoc.isDirectory()){
				mm.destNotDirectory(sender);
				return;
			}
		}
		
		Thread th = null;
		
		boolean valid = true;
		switch(fileExtension){
		case ".jar":
			th = new Thread(() -> {
					extractJar(sender, srcLoc, destLoc);
					synchronized(this){
						notify();
					}
				});
			break;
		case ".rar":
			th = new Thread(() -> {
					extractRar(sender, srcLoc, destLoc);
					synchronized(this){
						notify();
					}
				});
			break;
		case ".zip":
			th = new Thread(() -> {
					extractZip(sender, srcLoc, destLoc);
					synchronized(this){
						notify();
					}
				});
			break;
		default:
			mm.invalidExtension(sender, fileExtension);
			valid = false;
			break;
		}
		if(valid) {
			if(ZServicer.getInstance().submit(th))
				mm.addToQueue(sender, ZServicer.getInstance().getSize());
			else
				mm.queueFull(sender);
		}
	}
	
	private void extractZip(CommandSender sender, File sourceFile, File destFolder){
		Logger logger = MessageManager.getInstance().getLogger();
		boolean log = cm.getLoggingProperty();
		byte[] buffer = new byte[1024];
		mm.startingProcess(sender, ZTask.EXTRACT, sourceFile.getName());
		try {
			ZipInputStream zis = new ZipInputStream(new FileInputStream(sourceFile));
	    	ZipEntry ze = zis.getNextEntry();
	    	
	    	while(ze!=null){
	    		String fileName = ze.getName();
	    		File newFile = new File(destFolder + File.separator + fileName);	    		
	    		if(log)
	    			logger.info("Extracting : "+ newFile.getAbsoluteFile());
	    		if(newFile.getParentFile() != null)
	    			new File(newFile.getParent()).mkdirs();         
	            FileOutputStream fos = new FileOutputStream(newFile);	            
	            int len;
	            while ((len = zis.read(buffer)) > 0) {
	            	fos.write(buffer, 0, len);
	            }      
	            fos.close();
	            ze = zis.getNextEntry();
	    	}
	        zis.closeEntry();
	    	zis.close();  	
	    	mm.extractionComplete(sender, destFolder.getAbsolutePath());
	    } catch (AccessDeniedException e) {
	    	mm.fileAccessDenied(sender, ZTask.EXTRACT, e.getMessage());
	    } catch(IOException ex){
	    	ex.printStackTrace();
	    }
	}
	
	private void extractJar(CommandSender sender, File sourceFile, File destFolder){
		Logger logger = mm.getLogger();
		boolean log = cm.getLoggingProperty();
		mm.startingProcess(sender, ZTask.EXTRACT, sourceFile.getName());
		try{			
			JarFile jar = new JarFile(sourceFile);
			Enumeration<JarEntry> enumEntries = jar.entries();
			while (enumEntries.hasMoreElements()) {
			    JarEntry file = enumEntries.nextElement();
			    File f = new File(destFolder + File.separator + file.getName());
			    if(log)
			    	logger.info("Extracting : "+ f.getAbsolutePath());
			    if (file.isDirectory()) {
			        f.mkdir();
			        continue;
			    }
			    InputStream is = jar.getInputStream(file);
			    FileOutputStream fos = new FileOutputStream(f);
			    while (is.available() > 0) fos.write(is.read());
			    fos.close();
			    is.close();
			}
			jar.close();
			mm.extractionComplete(sender, destFolder.getAbsolutePath());
		} catch (AccessDeniedException e) {
	    	mm.fileAccessDenied(sender, ZTask.EXTRACT, e.getMessage());
	    } catch (IOException e){
			e.printStackTrace();
		}
	}
	
	private void extractRar(CommandSender sender, File sourceFile, File destFolder){
		Logger logger = mm.getLogger();
		boolean log = cm.getLoggingProperty();
		Archive a = null;
		try {
			a = new Archive(new FileVolumeManager(sourceFile));
		} catch (RarException | IOException e) {
			e.printStackTrace();
		}
		if (a != null) {
			FileHeader fh = a.nextFileHeader();
			mm.startingProcess(sender, ZTask.EXTRACT, sourceFile.getName());
			while (fh != null) {
				try {
					InputStream is = a.getInputStream(fh);
					Path p = Paths.get(destFolder + File.separator + fh.getFileNameString()); 
					File parent = p.toFile().getParentFile();
					if(!parent.exists() && !parent.mkdirs()){
					    throw new IllegalStateException("Couldn't create dir: " + parent);
					}
					try{
						if(log)
					    	logger.info("Extracting : "+ p.toString());
						Files.copy(is, p, StandardCopyOption.REPLACE_EXISTING);
					} catch (DirectoryNotEmptyException e){
						fh = a.nextFileHeader();
						continue;
					}
					is.close();
				} catch (AccessDeniedException e) {
			    	mm.fileAccessDenied(sender, ZTask.EXTRACT, e.getMessage());
			    } catch (RarException | IOException e) {
					e.printStackTrace();
				}
				fh = a.nextFileHeader();
			}
		}
		mm.extractionComplete(sender, destFolder.getAbsolutePath());
	}
}
