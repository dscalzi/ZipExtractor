/*
 * ZipExtractor
 * Copyright (C) 2016-2018 Daniel D. Scalzi
 * See License.txt for license information.
 */
package com.dscalzi.zipextractor.providers;

import java.io.File;
import java.util.List;

import org.bukkit.command.CommandSender;

/**
 * The TypeProvider Interface.
 * 
 * Implementations of TypeProvider will usually be executed asynchronously.
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
public interface TypeProvider {

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
	public default List<String> scanForExtractionConflicts(CommandSender sender, File src, File dest){
		throw new UnsupportedOperationException();
	}
	
	/**
	 * Extracts the source file into the destination directory.
	 * 
	 * @param sender The sender of the command, used for error relays.
	 * @param src The source file to be extracted.
	 * @param dest The destination root for extracted files.
	 */
	public default void extract(CommandSender sender, File src, File dest) {
		throw new UnsupportedOperationException();
	}
	
	/**
	 * Compresses the source file/directory to the destination file.
	 * 
	 * @param sender The sender of the command, used for error relays.
	 * @param src The source file/directory to be compressed.
	 * @param dest The file to be compressed to.
	 */
	public default void compress(CommandSender sender, File src, File dest) {
		throw new UnsupportedOperationException();
	}
	
	/**
	 * Returns if the given source file can be extracted by this provider.
	 * Implementations usually check the extension of the file.
	 * 
	 * @param src The source file to be checked.
	 * @return If this provider can extract the given type of file.
	 */
	public abstract boolean validForExtraction(File src);
	
	/**
	 * Returns if the given source file can be compressed by this provider.
	 * Implementations usually check the extension of the file.
	 * 
	 * @param src The source file to be checked.
	 * @return If this provider can compress the given type of file.
	 */
	public abstract boolean srcValidForCompression(File src);
	
	/**
	 * Returns if the given destination file matches the format
	 * of this provider. Implementations usually check the extension 
	 * of the file.
	 * 
	 * @param dest The destination file to be checked.
	 * @return If this provider can compress the given type of file.
	 */
	public abstract boolean destValidForCompression(File dest);
	
	/**
	 * Returns a List of the file extensions this provider supports for extraction.
	 * 
	 * @return A List of the file extensions this provider supports for extraction.
	 */
	public abstract List<String> supportedExtractionTypes();
	
	/**
	 * Returns a List of file extensions this provider can compress files to.
	 * 
	 * @return A List of file extensions this provider can compress files to.
	 */
	public abstract List<String> canCompressTo();
	
	/**
	 * Returns a List of file extensions this provider can compress.
	 * 
	 * @return A List of file extensions this provider can compress.
	 * If any file can be compressed, returns null.
	 */
	public default List<String> canCompressFrom(){
		return null;
	}
	
}
