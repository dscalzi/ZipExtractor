/*
 * ZipExtractor
 * Copyright (C) 2017 Daniel D. Scalzi
 * See License.txt for license information.
 */
package com.dscalzi.zipextractor.util;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
	private static final Map<String, WarnData> WARNED = new HashMap<String, WarnData>();
	private static List<String> SUPPORTED;
	
	public static void asyncExtract(CommandSender sender, File src, File dest) {
		asyncExtract(sender, src, dest, false);
	}
	
	public static void asyncExtract(CommandSender sender, File src, File dest, boolean override) {
		final MessageManager mm = MessageManager.getInstance();
		
		//If the user was warned, clear it.
		if(WARNED.containsKey(sender.getName())) {
			WARNED.remove(sender.getName());
		}
		
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
		final boolean finalOverride = override;
		for(final BaseProvider p : PROVIDERS) {
			if(p.sourceMatches(src)) {
				task = () -> {
					List<String> atRisk = new ArrayList<String>();
					if(!finalOverride) {
						atRisk = p.scan(sender, src, dest);
					}
					if(atRisk.size() == 0 || finalOverride) {
						p.extract(sender, src, dest);
					} else {
						WARNED.put(sender.getName(), new WarnData(sender, src, dest, new PageList<String>(4, atRisk)));
						mm.warnOfConflicts(sender, atRisk.size());
					}
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
	
	public static List<String> supportedExtensions(){
		if(SUPPORTED == null) {
			SUPPORTED = new ArrayList<String>();
			for(final BaseProvider p : PROVIDERS) {
				SUPPORTED.addAll(p.supportedExtensions());
			}
		}
		return SUPPORTED;
	}
	
	public static boolean wasWarned(CommandSender sender, File src, File dest) {
		if(WARNED.containsKey(sender.getName())) {
			final WarnData data = WARNED.get(sender.getName());
			return data.getSrc().equals(src) && data.getDest().equals(dest);
		} 
		return false;
	}
	
	public static Optional<WarnData> getWarnData(CommandSender sender) {
		WarnData data = WARNED.get(sender.getName());
		return data != null ? Optional.of(data) : Optional.empty();
	}
	
}
