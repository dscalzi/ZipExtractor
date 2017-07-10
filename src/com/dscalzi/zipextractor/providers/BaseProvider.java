/*
 * ZipExtractor
 * Copyright (C) 2017 Daniel D. Scalzi
 * See License.txt for license information.
 */
package com.dscalzi.zipextractor.providers;

import java.io.File;
import java.util.List;

import org.bukkit.command.CommandSender;

/**
 * The BaseProvider Interface.
 * 
 * Implementations of BaseProvider will usually be executed asynchronously.
 * At times, the thread running the provider may be interrupted. For that
 * reason, scan, extract, and other methods which may take a long time to complete
 * MUST often check to see if the thread was interrupted. If the thread was
 * interrupted, you can break the operation by throwing a {@link 
com.dscalzi.zipextractor.util.TaskInterruptedException.TaskInterruptedException TaskInterruptedException}
 * and handling it.
 * 
 * @author Daniel D. Scalzi
 *
 */
public interface BaseProvider {

	/**
	 * Scans the source file and calculates if any files in the destination directory
	 * would be overridden by an extraction. Returns a List containing the paths
	 * of the files which would be overridden.
	 * 
	 * @param sender The sender of the command, used to error relays.
	 * @param src The source file to be scanned.
	 * @param dest The destination file to be scanned.
	 * @return A List containing the paths of the files which would be overridden.
	 */
	public abstract List<String> scan(CommandSender sender, File src, File dest);
	
	/**
	 * Extracts the source file into the destination directory.
	 * 
	 * @param sender The sender of the command, used for error relays.
	 * @param src The source file to be extracted.
	 * @param dest The destination root for extracted files.
	 */
	public abstract void extract(CommandSender sender, File src, File dest);
	
	/**
	 * Returns if the given source file can be processed by this provider.
	 * Implementations usually check the extension of the file.
	 * 
	 * @param src The source file to be checked.
	 * @return If this provider can process the given type of file.
	 */
	public abstract boolean sourceMatches(File src);
	
	/**
	 * Returns a List of the file extensions this provider supports.
	 * 
	 * @return A List of the file extensions this provider supports.
	 */
	public abstract List<String> supportedExtensions();
	
}
