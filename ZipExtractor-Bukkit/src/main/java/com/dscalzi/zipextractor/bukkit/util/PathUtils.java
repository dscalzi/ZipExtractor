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

package com.dscalzi.zipextractor.bukkit.util;

import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PathUtils {

    /**
     * Matches '/', '\', '\\'.
     */
    public static final Pattern COMMON_SEPS = Pattern.compile("\\/|\\\\\\\\|\\\\");

    /**
     * Attempts to replace common path separators with the real separator for the
     * current Operating System. The path is matched against the
     * {@link PathUtils#COMMON_SEPS Common Separators} regex.
     * 
     * @param abstractPath
     * @return
     */
    public static String formatPath(String abstractPath, boolean storage) {
        if (abstractPath == null)
            return null;

        abstractPath = COMMON_SEPS.matcher(abstractPath)
                .replaceAll(storage ? "/" : Matcher.quoteReplacement(File.separator));

        return abstractPath;
    }

    /**
     * Check if the given path matches the provided glob pattern.
     * 
     * @param path
     *            The path to test.
     * @param globPattern
     *            The glob pattern to test against.
     * @return True if the path matches the glob pattern, otherwise false.
     */
    public static boolean isValidPath(Path path, String globPattern) {
        PathMatcher m = FileSystems.getDefault().getPathMatcher("glob:" + globPattern);
        return m.matches(path);
    }

    /**
     * Checks if a Path object can be constructed from the given file.
     * 
     * @param f
     *            The file to check.
     * @return True if the path is valid, false otherwise.
     */
    public static boolean validateFilePath(File f) {
        try {
            f.toPath();
        } catch (InvalidPathException e) {
            return false;
        }
        return true;
    }

    /**
     * Checks if a Path object can be constructed from the given string.
     * 
     * @param abstractPath
     *            The abstract path to check.
     * @return True if the path is valid, false otherwise.
     */
    public static boolean validateFilePath(String abstractPath) {
        try {
            Paths.get(abstractPath);
        } catch (InvalidPathException e) {
            return false;
        }
        return true;
    }

}
