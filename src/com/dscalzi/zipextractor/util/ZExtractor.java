package com.dscalzi.zipextractor.util;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import org.bukkit.command.CommandSender;

import com.dscalzi.zipextractor.managers.MessageManager;
import com.dscalzi.zipextractor.providers.BaseProvider;
import com.dscalzi.zipextractor.providers.JarProvider;
import com.dscalzi.zipextractor.providers.RarProvider;
import com.dscalzi.zipextractor.providers.ZipProvider;

public class ZExtractor {

	private static final BaseProvider[] PROVIDERS = {
			new ZipProvider(),
			new RarProvider(),
			new JarProvider()
	};
	private static Collection<String> SUPPORTED;
	
	public static void asyncExtract(CommandSender sender, File src, File dest) {
		final MessageManager mm = MessageManager.getInstance();
		
		//If the source file does not exist, abort.
		if(!src.exists()) {
			mm.sourceNotFound(sender, src.getAbsolutePath());
			return;
		}
				
		//If the destination directory does not exist, create it.
		if(!dest.exists()) {
		    dest.mkdir();
		}
				
		//If the destination exists and it's not a directory, abort.
		if(dest.exists()){
			if(!dest.isDirectory()){
				mm.destNotDirectory(sender, dest.getAbsolutePath());
				return;
			}
		}
				
		Runnable task = null;
		for(final BaseProvider p : PROVIDERS) {
			if(p.sourceMatches(src)) {
				task = () -> {
					p.extract(sender, src, dest);
				};
				break;
			}
		}
		if(task != null) {
			int result = ZServicer.getInstance().submit(task);
			if(result == 0)
				mm.addToQueue(sender, ZServicer.getInstance().getSize());
			else if(result == 1)
				mm.queueFull(sender);
			else if(result == 2)
				mm.executorTerminated(sender, ZTask.EXTRACT);
		} else {
			mm.invalidSourceExtension(sender);
		}
	}
	
	public static Collection<String> supportedExtensions(){
		if(SUPPORTED == null) {
			SUPPORTED = new ArrayList<String>();
			for(final BaseProvider p : PROVIDERS) {
				SUPPORTED.addAll(p.supportedExtensions());
			}
		}
		return SUPPORTED;
	}
	
}
