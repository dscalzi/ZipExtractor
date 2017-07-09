package com.dscalzi.zipextractor.providers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.bukkit.command.CommandSender;

import com.dscalzi.zipextractor.managers.ConfigManager;
import com.dscalzi.zipextractor.managers.MessageManager;
import com.dscalzi.zipextractor.util.TaskInterruptedException;
import com.dscalzi.zipextractor.util.ZTask;

public class ZipProvider implements BaseProvider{

	//Shared pattern by ZipProviders
	public static final Pattern PATH_END = Pattern.compile("\\.zip$");
	public static final Collection<String> SUPPORTED = new ArrayList<String>(Arrays.asList("zip"));
	
	@Override
	public Collection<String> scan(CommandSender sender, File src, File dest) {
		// TODO Will be completed for 1.0.0 release - support needs to be integrated elsewhere in
		// the plugin first.
		return null;
	}

	@Override
	public void extract(CommandSender sender, File src, File dest) {
		final ConfigManager cm = ConfigManager.getInstance();
		final MessageManager mm = MessageManager.getInstance();
		final Logger logger = MessageManager.getInstance().getLogger();
		boolean log = cm.getLoggingProperty();
		byte[] buffer = new byte[1024];
		mm.startingProcess(sender, ZTask.EXTRACT, src.getName());
		try(FileInputStream fis = new FileInputStream(src);
			ZipInputStream zis = new ZipInputStream(fis);){
			ZipEntry ze = zis.getNextEntry();
	    	
	    	while(ze!=null){
	    		if (Thread.interrupted())
	        		  throw new TaskInterruptedException();
	    		
	    		String fileName = ze.getName();
	    		File newFile = new File(dest + File.separator + fileName);
	    		if(log)
	    			logger.info("Extracting : "+ newFile.getAbsoluteFile());
	    		File parent = newFile.getParentFile();
	    		if(!parent.exists() && !parent.mkdirs()){
				    throw new IllegalStateException("Couldn't create dir: " + parent);
				}
	    		if(ze.isDirectory()){
	    			newFile.mkdir();
	    			ze = zis.getNextEntry();
	    			continue;
	    		}
	            try(FileOutputStream fos = new FileOutputStream(newFile)){	            
		            int len;
		            while ((len = zis.read(buffer)) > 0) {
		            	fos.write(buffer, 0, len);
		            }      
	            }
	            ze = zis.getNextEntry();
	    	}
	        zis.closeEntry();
	    	mm.extractionComplete(sender, dest.getAbsolutePath());
	    } catch (AccessDeniedException e) {
	    	mm.fileAccessDenied(sender, ZTask.EXTRACT, e.getMessage());
	    } catch (TaskInterruptedException e) {
	    	mm.taskInterruption(sender, ZTask.EXTRACT);
	    } catch(IOException ex){
	    	ex.printStackTrace();
	    }
	}

	@Override
	public boolean sourceMatches(File src) {
		return PATH_END.matcher(src.getAbsolutePath()).find();
	}

	@Override
	public Collection<String> supportedExtensions() {
		return SUPPORTED;
	}

}
