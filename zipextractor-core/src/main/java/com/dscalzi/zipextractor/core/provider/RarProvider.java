/*
 * This file is part of ZipExtractor.
 * Copyright (C) 2016-2021 Daniel D. Scalzi <https://github.com/dscalzi/ZipExtractor>
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

import com.dscalzi.zipextractor.core.TaskInterruptedException;
import com.dscalzi.zipextractor.core.ZTask;
import com.dscalzi.zipextractor.core.managers.MessageManager;
import com.dscalzi.zipextractor.core.util.ICommandSender;
import com.github.junrar.Archive;
import com.github.junrar.exception.NotRarArchiveException;
import com.github.junrar.exception.RarException;
import com.github.junrar.exception.UnsupportedRarV5Exception;
import com.github.junrar.rarfile.FileHeader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public class RarProvider implements TypeProvider {

    // Shared pattern by RarProviders
    public static final Pattern PATH_END = Pattern.compile("\\.rar$");
    protected static final List<String> SUPPORTED = new ArrayList<>(Collections.singletonList("rar"));

    @Override
    public List<String> scanForExtractionConflicts(ICommandSender sender, File src, File dest, boolean silent) {
        List<String> existing = new ArrayList<>();
        final MessageManager mm = MessageManager.inst();

        try (Archive a = new Archive(src)) {

            if(!silent)
                mm.scanningForConflics(sender);
            FileHeader fh = a.nextFileHeader();
            while (fh != null) {
                if (Thread.interrupted())
                    throw new TaskInterruptedException();
                File newFile = Paths.get(dest + File.separator + fh.getFileName()).toFile();
                if (newFile.exists()) {
                    existing.add(fh.getFileName());
                }
                fh = a.nextFileHeader();
            }

        } catch (TaskInterruptedException e) {
            mm.taskInterruption(sender, ZTask.EXTRACT);
        } catch (IOException | RarException e) {
            e.printStackTrace();
        }

        return existing;
    }
    
    @Override
    public boolean canDetectPipedConflicts() {
        return false;
    }

    @Override
    public boolean extract(ICommandSender sender, File src, File dest, boolean log, boolean pipe) {
        final MessageManager mm = MessageManager.inst();
        //noinspection TryWithIdenticalCatches
        try (Archive a = new Archive(src)) {
            FileHeader fh = a.nextFileHeader();
            mm.startingProcess(sender, ZTask.EXTRACT, src.getName());
            while (fh != null) {
                if (Thread.interrupted())
                    throw new TaskInterruptedException();
                try (InputStream is = a.getInputStream(fh)) {
                    Path p = Paths.get(dest + File.separator + fh.getFileName());
                    File parent = p.toFile().getParentFile();
                    if (!parent.exists() && !parent.mkdirs()) {
                        throw new IllegalStateException("Couldn't create dir: " + parent);
                    }
                    try {
                        if (log)
                            mm.info("Extracting : " + p);
                        Files.copy(is, p, StandardCopyOption.REPLACE_EXISTING);
                    } catch (DirectoryNotEmptyException e) {
                        fh = a.nextFileHeader();
                        continue;
                    }
                } catch (AccessDeniedException e) {
                    mm.fileAccessDenied(sender, ZTask.EXTRACT, e.getMessage());
                } catch (InterruptedIOException e) {
                    throw new TaskInterruptedException();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                fh = a.nextFileHeader();
            }
            if(!pipe)
                mm.extractionComplete(sender, dest);
            return true;
        } catch (TaskInterruptedException e) {
            mm.taskInterruption(sender, ZTask.EXTRACT);
            return false;
        } catch(RarException e) {
            if(e instanceof NotRarArchiveException) {
                mm.extractionFormatError(sender, src, "Rar");
            }
            else if(e instanceof UnsupportedRarV5Exception) {
                final String msg = "Rar v5 is currently unsupported. Consider using a different compression format.";
                mm.sendError(sender, msg);
                mm.severe(msg);
            }
            else {
                mm.sendError(sender, "Failed: RarException of type " + e.getClass().getSimpleName() + " thrown.");
                mm.severe("Failed: RarException of type " + e.getClass().getSimpleName() + " thrown.");
            }
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            mm.genericOperationError(sender, src, ZTask.EXTRACT);
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            mm.genericOperationError(sender, src, ZTask.EXTRACT);
            return false;
        }
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
        return new ArrayList<>();
    }

}
