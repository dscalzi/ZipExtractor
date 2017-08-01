package com.dscalzi.zipextractor.providers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import org.bukkit.command.CommandSender;
import org.tukaani.xz.LZMA2Options;
import org.tukaani.xz.XZInputStream;
import org.tukaani.xz.XZOutputStream;

import com.dscalzi.zipextractor.managers.ConfigManager;
import com.dscalzi.zipextractor.managers.MessageManager;
import com.dscalzi.zipextractor.util.TaskInterruptedException;
import com.dscalzi.zipextractor.util.ZTask;

public class XZProvider implements TypeProvider {

	//Shared pattern by ZipProviders
	public static final Pattern PATH_END = Pattern.compile("\\.xz$");
	public static final List<String> SUPPORTED = new ArrayList<String>(Arrays.asList("xz"));
	
	@Override
	public List<String> scanForExtractionConflicts(CommandSender sender, File src, File dest) {
		final MessageManager mm = MessageManager.getInstance();
		mm.scanningForConflics(sender);
		File realDest = new File(dest.getAbsolutePath(), PATH_END.matcher(src.getName()).replaceAll(""));
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
		final Logger logger = MessageManager.getInstance().getLogger();
		final boolean log = cm.getLoggingProperty();
		mm.startingProcess(sender, ZTask.EXTRACT, src.getName());
		File realDest = new File(dest.getAbsolutePath(), PATH_END.matcher(src.getName()).replaceAll(""));
		try(FileInputStream fis = new FileInputStream(src);
			XZInputStream xzis = new XZInputStream(fis);
			FileOutputStream fos = new FileOutputStream(realDest)){
			if(log)	logger.info("Extracting : " + src.getAbsoluteFile());
			byte[] buf = new byte[65536];
			int read = xzis.read(buf);
    		while (read >= 1) {
    			if(Thread.interrupted())
    				throw new TaskInterruptedException();
    			fos.write(buf, 0, read);
    			read = xzis.read(buf);
    		}
    		mm.extractionComplete(sender, realDest.getAbsolutePath());
		} catch (TaskInterruptedException e) {
	    	mm.taskInterruption(sender, ZTask.EXTRACT);
	    } catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void compress(CommandSender sender, File src, File dest) {
		final ConfigManager cm = ConfigManager.getInstance();
		final MessageManager mm = MessageManager.getInstance();
		final Logger logger = MessageManager.getInstance().getLogger();
		final boolean log = cm.getLoggingProperty();
		mm.startingProcess(sender, ZTask.COMPRESS, src.getName());
		try(FileOutputStream fos = new FileOutputStream(dest);
			XZOutputStream xzos = new XZOutputStream(fos, new LZMA2Options());){
			if(log)
	        	logger.info("Compressing : " + src.getAbsolutePath());
			xzos.write(Files.readAllBytes(src.toPath()));
			mm.compressionComplete(sender, dest.getAbsolutePath());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public boolean validForExtraction(File src) {
		return PATH_END.matcher(src.getAbsolutePath()).find();
	}

	@Override
	public boolean srcValidForCompression(File src) {
		return true; //Any source file can be compressed to .xz.
	}

	@Override
	public boolean destValidForCompression(File dest) {
		return validForExtraction(dest);
	}

	@Override
	public List<String> supportedExtractionTypes() {
		return SUPPORTED;
	}

	@Override
	public List<String> canCompressTo() {
		return SUPPORTED;
	}

}
