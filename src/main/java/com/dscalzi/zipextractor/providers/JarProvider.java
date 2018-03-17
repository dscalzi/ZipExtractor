/*
 * ZipExtractor
 * Copyright (C) 2016-2018 Daniel D. Scalzi
 * See LICENSE for license information.
 */
package com.dscalzi.zipextractor.providers;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AccessDeniedException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import org.bukkit.command.CommandSender;

import com.dscalzi.zipextractor.managers.ConfigManager;
import com.dscalzi.zipextractor.managers.MessageManager;
import com.dscalzi.zipextractor.util.TaskInterruptedException;
import com.dscalzi.zipextractor.util.ZTask;

public class JarProvider implements TypeProvider {

	//Shared pattern by JarProviders
	public static final Pattern PATH_END = Pattern.compile("\\.jar$");
	public static final List<String> SUPPORTED = new ArrayList<String>(Arrays.asList("jar"));
	
	@Override
	public List<String> scanForExtractionConflicts(CommandSender sender, File src, File dest) {
		List<String> existing = new ArrayList<String>();
		final MessageManager mm = MessageManager.getInstance();
		
		try(JarFile jar = new JarFile(src)){
			mm.scanningForConflics(sender);
			Enumeration<JarEntry> enumEntries = jar.entries();
			while (enumEntries.hasMoreElements()) {
				if(Thread.interrupted())
					throw new TaskInterruptedException();
				
				JarEntry file = enumEntries.nextElement();
				File newFile = new File(dest + File.separator + file.getName());
				if(newFile.exists()) {
					existing.add(file.getName());
				}
			}
		} catch (TaskInterruptedException e) {
	    	mm.taskInterruption(sender, ZTask.EXTRACT);
	    } catch (IOException e) {
			e.printStackTrace();
		}
		
		return existing;
	}

	@Override
	public void extract(CommandSender sender, File src, File dest) {
		final ConfigManager cm = ConfigManager.getInstance();
		final MessageManager mm = MessageManager.getInstance();
		final Logger logger = mm.getLogger();
		final boolean log = cm.getLoggingProperty();
		mm.startingProcess(sender, ZTask.EXTRACT, src.getName());
		try(JarFile jar = new JarFile(src)){
			Enumeration<JarEntry> enumEntries = jar.entries();
			while (enumEntries.hasMoreElements()) {
				if(Thread.interrupted())
					throw new TaskInterruptedException();
			    JarEntry file = enumEntries.nextElement();
			    File f = new File(dest + File.separator + file.getName());
			    if(log)
			    	logger.info("Extracting : "+ f.getAbsolutePath());
			    if (file.isDirectory()) {
			        f.mkdir();
			        continue;
			    }
			    try(InputStream is = jar.getInputStream(file);
			    	FileOutputStream fos = new FileOutputStream(f);){
			    	while (is.available() > 0) fos.write(is.read());
			    }
			}
			mm.extractionComplete(sender, dest.getAbsolutePath());
		} catch (AccessDeniedException e) {
	    	mm.fileAccessDenied(sender, ZTask.EXTRACT, e.getMessage());
	    } catch (TaskInterruptedException e) {
	    	mm.taskInterruption(sender, ZTask.EXTRACT);
	    } catch (IOException e){
			e.printStackTrace();
		}
	}

	@Override
	public boolean validForExtraction(File src) {
		return PATH_END.matcher(src.getAbsolutePath()).find();
	}
	
	@Override
	public boolean srcValidForCompression(File src) {
		return false; //Compression to Jars is not supported.
	}
	
	@Override
	public boolean destValidForCompression(File dest) {
		return false; //Compression to Jars is not supported.
	}

	@Override
	public List<String> supportedExtractionTypes() {
		return SUPPORTED;
	}
	
	@Override
	public List<String> canCompressTo() {
		return new ArrayList<String>();
	}

}
