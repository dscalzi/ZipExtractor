/*
 * ZipExtractor
 * Copyright (C) 2016-2018 Daniel D. Scalzi
 * See LICENSE for license information.
 */
package com.dscalzi.zipextractor.providers;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Pack200;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import org.bukkit.command.CommandSender;

import com.dscalzi.zipextractor.managers.ConfigManager;
import com.dscalzi.zipextractor.managers.MessageManager;
import com.dscalzi.zipextractor.util.ZTask;

public class PackProvider implements TypeProvider {

	public static final Pattern PATH_END_EXTRACT = Pattern.compile("\\.pack$");
	public static final Pattern PATH_END_COMPRESS = Pattern.compile("\\.jar$");
	public static final List<String> SUPPORTED_EXTRACT = new ArrayList<String>(Arrays.asList("pack"));
	public static final List<String> SUPPORTED_COMPRESS = new ArrayList<String>(Arrays.asList("jar"));
	
	@Override
	public List<String> scanForExtractionConflicts(CommandSender sender, File src, File dest) {
		final MessageManager mm = MessageManager.getInstance();
		mm.scanningForConflics(sender);
		File realDest = new File(dest.getAbsolutePath(), PATH_END_EXTRACT.matcher(src.getName()).replaceAll(""));
		List<String> ret = new ArrayList<String>();
		if(realDest.exists()) {
			ret.add(realDest.getAbsolutePath());
		}
		return ret;
	}
	
	@Override
	public void extract(CommandSender sender, File src, File dest) {
		final ConfigManager cm = ConfigManager.getInstance();
		final MessageManager mm = MessageManager.getInstance();
		final Logger logger = mm.getLogger();
		final boolean log = cm.getLoggingProperty();
		mm.startingProcess(sender, ZTask.EXTRACT, src.getName());
		File realDest = new File(dest.getAbsolutePath(), PATH_END_EXTRACT.matcher(src.getName()).replaceAll(""));
		try(JarOutputStream jarStream = new JarOutputStream(new FileOutputStream(realDest))){
			if(log)	logger.info("Extracting : " + src.getAbsoluteFile());
    		Pack200.newUnpacker().unpack(src, jarStream);
    		mm.extractionComplete(sender, realDest.getAbsolutePath());
    	} catch(IOException e) {
    		e.printStackTrace();
    	}
	}
	
	@Override
	public void compress(CommandSender sender, File src, File dest) {
		final ConfigManager cm = ConfigManager.getInstance();
		final MessageManager mm = MessageManager.getInstance();
		final Logger logger = mm.getLogger();
		final boolean log = cm.getLoggingProperty();
		mm.startingProcess(sender, ZTask.COMPRESS, src.getName());
		try(JarFile in = new JarFile(src);
			OutputStream out = Files.newOutputStream(dest.toPath())){
			if(log) logger.info("Compressing : " + src.getAbsolutePath());
			Pack200.newPacker().pack(in, out);
			mm.compressionComplete(sender, dest.getAbsolutePath());
		} catch(IOException e) {
    		e.printStackTrace();
    	}
	}
	
	@Override
	public boolean validForExtraction(File src) {
		return PATH_END_EXTRACT.matcher(src.getAbsolutePath()).find();
	}

	@Override
	public boolean srcValidForCompression(File src) {
		return PATH_END_COMPRESS.matcher(src.getAbsolutePath()).find();
	}
	
	@Override
	public boolean destValidForCompression(File dest) {
		return validForExtraction(dest);
	}

	@Override
	public List<String> supportedExtractionTypes() {
		return SUPPORTED_EXTRACT;
	}

	@Override
	public List<String> canCompressTo() {
		return SUPPORTED_EXTRACT;
	}
	
	@Override
	public List<String> canCompressFrom() {
		return SUPPORTED_COMPRESS;
	}

}
