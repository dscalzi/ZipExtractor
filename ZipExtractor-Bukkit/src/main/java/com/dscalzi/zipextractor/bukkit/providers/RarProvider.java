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

package com.dscalzi.zipextractor.bukkit.providers;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import org.bukkit.command.CommandSender;

import com.dscalzi.zipextractor.bukkit.managers.ConfigManager;
import com.dscalzi.zipextractor.bukkit.managers.MessageManager;
import com.dscalzi.zipextractor.bukkit.util.TaskInterruptedException;
import com.dscalzi.zipextractor.bukkit.util.ZTask;
import com.github.junrar.Archive;
import com.github.junrar.exception.RarException;
import com.github.junrar.impl.FileVolumeManager;
import com.github.junrar.rarfile.FileHeader;

public class RarProvider implements TypeProvider {

    // Shared pattern by RarProviders
    public static final Pattern PATH_END = Pattern.compile("\\.rar$");
    public static final List<String> SUPPORTED = new ArrayList<String>(Arrays.asList("rar"));

    @Override
    public List<String> scanForExtractionConflicts(CommandSender sender, File src, File dest) {
        List<String> existing = new ArrayList<String>();
        final MessageManager mm = MessageManager.getInstance();

        try (Archive a = new Archive(new FileVolumeManager(src))) {

            if (a != null) {
                mm.scanningForConflics(sender);
                FileHeader fh = a.nextFileHeader();
                while (fh != null) {
                    if (Thread.interrupted())
                        throw new TaskInterruptedException();
                    File newFile = Paths.get(dest + File.separator + fh.getFileNameString()).toFile();
                    if (newFile.exists()) {
                        existing.add(fh.getFileNameString());
                    }
                    fh = a.nextFileHeader();
                }
            }

        } catch (TaskInterruptedException e) {
            mm.taskInterruption(sender, ZTask.EXTRACT);
        } catch (IOException | RarException e) {
            e.printStackTrace();
        }

        return existing;
    }

    @Override
    public void extract(CommandSender sender, File src, File dest) {
        final ConfigManager cm = ConfigManager.getInstance();
        final MessageManager mm = MessageManager.getInstance();
        final Logger logger = mm.getLogger();
        final boolean log = cm.getLoggingProperty();
        try (Archive a = new Archive(new FileVolumeManager(src))) {
            if (a != null) {
                FileHeader fh = a.nextFileHeader();
                mm.startingProcess(sender, ZTask.EXTRACT, src.getName());
                while (fh != null) {
                    if (Thread.interrupted())
                        throw new TaskInterruptedException();
                    try (InputStream is = a.getInputStream(fh)) {
                        Path p = Paths.get(dest + File.separator + fh.getFileNameString());
                        File parent = p.toFile().getParentFile();
                        if (!parent.exists() && !parent.mkdirs()) {
                            throw new IllegalStateException("Couldn't create dir: " + parent);
                        }
                        try {
                            if (log)
                                logger.info("Extracting : " + p.toString());
                            Files.copy(is, p, StandardCopyOption.REPLACE_EXISTING);
                        } catch (DirectoryNotEmptyException e) {
                            fh = a.nextFileHeader();
                            continue;
                        }
                    } catch (AccessDeniedException e) {
                        mm.fileAccessDenied(sender, ZTask.EXTRACT, e.getMessage());
                    } catch (InterruptedIOException e) {
                        throw new TaskInterruptedException();
                    } catch (RarException | IOException e) {
                        e.printStackTrace();
                    }
                    fh = a.nextFileHeader();
                }
            }
        } catch (TaskInterruptedException e) {
            mm.taskInterruption(sender, ZTask.EXTRACT);
            return;
        } catch (RarException | IOException e) {
            e.printStackTrace();
        }
        mm.extractionComplete(sender, dest.getAbsolutePath());
    }

    @Override
    public boolean validForExtraction(File src) {
        return PATH_END.matcher(src.getAbsolutePath()).find();
    }

    @Override
    public boolean srcValidForCompression(File src) {
        return false; // Compression to RAR not supported.
    }

    @Override
    public boolean destValidForCompression(File dest) {
        return false; // Compression to RAR not supported.
    }

    @Override
    public List<String> supportedExtractionTypes() {
        return SUPPORTED;
    }

    @Override
    public List<String> canCompressTo() {
        return new ArrayList<String>();
    }

}
