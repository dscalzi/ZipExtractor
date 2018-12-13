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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import com.dscalzi.zipextractor.core.TaskInterruptedException;
import com.dscalzi.zipextractor.core.ZTask;
import com.dscalzi.zipextractor.core.managers.MessageManager;
import com.dscalzi.zipextractor.core.util.ICommandSender;

public class ZipProvider implements TypeProvider {

    // Shared pattern by ZipProviders
    public static final Pattern PATH_END = Pattern.compile("\\.zip$");
    public static final List<String> SUPPORTED = new ArrayList<String>(Arrays.asList("zip"));

    @Override
    public List<String> scanForExtractionConflicts(ICommandSender sender, File src, File dest) {
        List<String> existing = new ArrayList<String>();
        final MessageManager mm = MessageManager.inst();
        mm.scanningForConflics(sender);
        try (FileInputStream fis = new FileInputStream(src); ZipInputStream zis = new ZipInputStream(fis);) {
            ZipEntry ze = zis.getNextEntry();

            while (ze != null) {
                if (Thread.interrupted())
                    throw new TaskInterruptedException();

                File newFile = new File(dest + File.separator + ze.getName());
                if (newFile.exists()) {
                    existing.add(ze.getName());
                }
                ze = zis.getNextEntry();
            }

            zis.closeEntry();
        } catch (TaskInterruptedException e) {
            mm.taskInterruption(sender, ZTask.EXTRACT);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return existing;
    }

    @Override
    public void extract(ICommandSender sender, File src, File dest, boolean log) {
        final MessageManager mm = MessageManager.inst();
        byte[] buffer = new byte[1024];
        mm.startingProcess(sender, ZTask.EXTRACT, src.getName());
        try (FileInputStream fis = new FileInputStream(src); ZipInputStream zis = new ZipInputStream(fis);) {
            ZipEntry ze = zis.getNextEntry();

            while (ze != null) {
                if (Thread.interrupted())
                    throw new TaskInterruptedException();

                File newFile = new File(dest + File.separator + ze.getName());
                if (log)
                    mm.info("Extracting : " + newFile.getAbsoluteFile());
                File parent = newFile.getParentFile();
                if (!parent.exists() && !parent.mkdirs()) {
                    throw new IllegalStateException("Couldn't create dir: " + parent);
                }
                if (ze.isDirectory()) {
                    newFile.mkdir();
                    ze = zis.getNextEntry();
                    continue;
                }
                try (FileOutputStream fos = new FileOutputStream(newFile)) {
                    int len;
                    while ((len = zis.read(buffer)) > 0) {
                        fos.write(buffer, 0, len);
                    }
                }
                ze = zis.getNextEntry();
            }
            zis.closeEntry();
            mm.extractionComplete(sender, dest);
        } catch (AccessDeniedException e) {
            mm.fileAccessDenied(sender, ZTask.EXTRACT, e.getMessage());
        } catch (TaskInterruptedException e) {
            mm.taskInterruption(sender, ZTask.EXTRACT);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void compress(ICommandSender sender, File src, File dest, boolean log, boolean pipe) {
        final MessageManager mm = MessageManager.inst();
        mm.startingProcess(sender, ZTask.COMPRESS, src.getName());
        try (OutputStream os = Files.newOutputStream(dest.toPath()); ZipOutputStream zs = new ZipOutputStream(os);) {
            Path pp = src.toPath();
            Files.walk(pp).filter(path -> !Files.isDirectory(path)).forEach(path -> {
                if (Thread.interrupted())
                    throw new TaskInterruptedException();
                // Prevent recursive compressions
                if (path.equals(dest.toPath()))
                    return;
                String sp = path.toAbsolutePath().toString().replace(pp.toAbsolutePath().toString(), "");
                if (sp.length() > 0)
                    sp = sp.substring(1);
                ZipEntry zipEntry = new ZipEntry(pp.getFileName() + ((sp.length() > 0) ? (File.separator + sp) : ""));
                try {
                    if (log)
                        mm.info("Compressing : " + zipEntry.toString());
                    zs.putNextEntry(zipEntry);
                    zs.write(Files.readAllBytes(path));
                    zs.closeEntry();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            if(!pipe)
                mm.compressionComplete(sender, dest);
        } catch (AccessDeniedException e) {
            mm.fileAccessDenied(sender, ZTask.COMPRESS, e.getMessage());
        } catch (TaskInterruptedException e) {
            mm.taskInterruption(sender, ZTask.COMPRESS);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean validForExtraction(File src) {
        return PATH_END.matcher(src.getAbsolutePath()).find();
    }

    @Override
    public boolean srcValidForCompression(File src) {
        // Any source file can be compressed to a zip.
        return true;
    }

    @Override
    public boolean destValidForCompression(File dest) {
        return validForExtraction(dest);
    }

    @Override
    public List<String> supportedExtractionTypes() {
        return SUPPORTED;
    }

    @Override
    public List<String> canCompressTo() {
        return SUPPORTED;
    }

}
