/*
 * This file is part of ZipExtractor.
 * Copyright (C) 2016-2018 Daniel D. Scalzi <https://github.com/dscalzi/ZipExtractor>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.dscalzi.zipextractor.core.provider;

import java.io.File;
import java.util.List;

import com.dscalzi.zipextractor.core.util.ICommandSender;

/**
 * The TypeProvider Interface.
 * 
 * Implementations of TypeProvider will usually be executed asynchronously. At
 * times, the thread running the provider may be interrupted. For that reason,
 * scan, extract, and other methods which may take a long time to complete MUST
 * often check to see if the thread was interrupted. If the thread was
 * interrupted, you can break the operation by throwing a
 * {@link com.dscalzi.zipextractor.bukkit.util.TaskInterruptedException.TaskInterruptedException
 * TaskInterruptedException} and handling it.
 * 
 * @author Daniel D. Scalzi
 *
 */
public interface TypeProvider {

    public static final TypeProvider[] PROVIDERS = {
            new ZipProvider(),
            new RarProvider(),
            new JarProvider(),
            new PackProvider(),
            new XZProvider(),
            new GZProvider()
    };
    
    /**
     * Get all implemented providers.
     * 
     * @return An array of all supported providers.
     */
    public static TypeProvider[] getProviders() {
        return PROVIDERS;
    }
    
    /**
     * Scans the source file and calculates if any files in the destination
     * directory would be overridden by an extraction. Returns a List containing the
     * paths of the files which would be overridden.
     * 
     * @param sender
     *            The sender of the command, used to error relays.
     * @param src
     *            The source file to be scanned.
     * @param dest
     *            The destination file to be scanned.
     * @return A List containing the paths of the files which would be overridden.
     */
    public default List<String> scanForExtractionConflicts(ICommandSender sender, File src, File dest) {
        throw new UnsupportedOperationException();
    }

    /**
     * Extracts the source file into the destination directory.
     * 
     * @param sender
     *            The sender of the command, used for error relays.
     * @param src
     *            The source file to be extracted.
     * @param dest
     *            The destination root for extracted files.
     * @param log
     *            Whether or not to log the progress.
     */
    public default void extract(ICommandSender sender, File src, File dest, boolean log) {
        throw new UnsupportedOperationException();
    }

    /**
     * Compresses the source file/directory to the destination file.
     * 
     * @param sender
     *            The sender of the command, used for error relays.
     * @param src
     *            The source file/directory to be compressed.
     * @param dest
     *            The file to be compressed to.
     * @param log
     *            Whether or not to log the progress.
     * @param pipe
     *            Whether this output will be piped.
     */
    public default void compress(ICommandSender sender, File src, File dest, boolean log, boolean pipe) {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns if the given source file can be extracted by this provider.
     * Implementations usually check the extension of the file.
     * 
     * @param src
     *            The source file to be checked.
     * @return If this provider can extract the given type of file.
     */
    public abstract boolean validForExtraction(File src);

    /**
     * Returns if the given source file can be compressed by this provider.
     * Implementations usually check the extension of the file.
     * 
     * @param src
     *            The source file to be checked.
     * @return If this provider can compress the given type of file.
     */
    public abstract boolean srcValidForCompression(File src);

    /**
     * Returns if the given destination file matches the format of this provider.
     * Implementations usually check the extension of the file.
     * 
     * @param dest
     *            The destination file to be checked.
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
     * @return A List of file extensions this provider can compress. If any file can
     *         be compressed, returns null.
     */
    public default List<String> canCompressFrom() {
        return null;
    }

}
