/*
 * ZipExtractor
 * Copyright (C) 2016-2018 Daniel D. Scalzi
 * See LICENSE for license information.
 */
package com.dscalzi.zipextractor.util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.command.CommandSender;

import com.dscalzi.zipextractor.ZipExtractor;
import com.dscalzi.zipextractor.managers.MessageManager;
import com.dscalzi.zipextractor.providers.TypeProvider;

public class ZCompressor {
	
	private static List<String> SUPPORTED;
	
	public static void asyncCompress(CommandSender sender, File src, File dest, final boolean override){
		final MessageManager mm = MessageManager.getInstance();
		//If the source does not exist, abort.
		if(!src.exists()){
			mm.sourceNotFound(sender, src.getAbsolutePath());
			return;
		}
		
		Runnable task = null;
		for(final TypeProvider p : ZipExtractor.getProviders()) {
			if(p.destValidForCompression(dest)) {
				if(p.srcValidForCompression(src)) {
					if(dest.exists() && !override) {
						mm.destExists(sender);
						return;
					}
					task = () -> {
						p.compress(sender, src, dest);
					};
				} else {
					mm.invalidSourceForDest(sender, p.canCompressFrom(), p.canCompressTo());
					return;
				}
			}
		}
		
		if(task != null) {
			int result = ZServicer.getInstance().submit(task);
			if(result == 0)
				mm.addToQueue(sender, ZServicer.getInstance().getSize());
			else if(result == 1)
				mm.queueFull(sender);
			else if(result == 2)
				mm.executorTerminated(sender, ZTask.COMPRESS);
		} else {
			mm.invalidCompressionExtension(sender);
		}
	}
	
	public static List<String> supportedExtensions(){
		if(SUPPORTED == null) {
			SUPPORTED = new ArrayList<String>();
			for(final TypeProvider p : ZipExtractor.getProviders()) {
				SUPPORTED.addAll(p.canCompressTo());
			}
		}
		return SUPPORTED;
	}
	
}
