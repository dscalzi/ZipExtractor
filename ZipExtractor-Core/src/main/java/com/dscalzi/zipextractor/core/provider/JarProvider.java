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
import java.nio.file.AccessDeniedException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import com.dscalzi.zipextractor.core.TaskInterruptedException;
import com.dscalzi.zipextractor.core.ZTask;
import com.dscalzi.zipextractor.core.manager.MessageManager;
import com.dscalzi.zipextractor.core.util.BaseCommandSender;

public class JarProvider implements TypeProvider {

    // Shared pattern by JarProviders
    public static final Pattern PATH_END = Pattern.compile("\\.jar$");
    public static final List<String> SUPPORTED = new ArrayList<String>(Arrays.asList("jar"));

    @Override
    public List<String> scanForExtractionConflicts(BaseCommandSender sender, File src, File dest) {
        
        List<String> existing = new ArrayList<String>();
        final MessageManager mm = MessageManager.inst();
        mm.scanningForConflics(sender);
        try (FileInputStream fis = new FileInputStream(src); JarInputStream jis = new JarInputStream(fis);) {
            JarEntry je = jis.getNextJarEntry();

            while (je != null) {
                if (Thread.interrupted())
                    throw new TaskInterruptedException();

                File newFile = new File(dest + File.separator + je.getName());
                if (newFile.exists()) {
                    existing.add(je.getName());
                }
                je = jis.getNextJarEntry();
            }

            jis.closeEntry();
        } catch (TaskInterruptedException e) {
            mm.taskInterruption(sender, ZTask.EXTRACT);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return existing;
    }

    @Override
    public void extract(BaseCommandSender sender, File src, File dest, boolean log) {
        final MessageManager mm = MessageManager.inst();
        final Logger logger = mm.getLogger();
        byte[] buffer = new byte[1024];
        mm.startingProcess(sender, ZTask.EXTRACT, src.getName());
        try (FileInputStream fis = new FileInputStream(src); JarInputStream jis = new JarInputStream(fis);) {
            JarEntry je = jis.getNextJarEntry();
            
            while(je != null) {
                if (Thread.interrupted())
                    throw new TaskInterruptedException();

                File newFile = new File(dest + File.separator + je.getName());
                if (log)
                    logger.info("Extracting : " + newFile.getAbsoluteFile());
                File parent = newFile.getParentFile();
                if (!parent.exists() && !parent.mkdirs()) {
                    throw new IllegalStateException("Couldn't create dir: " + parent);
                }
                if (je.isDirectory()) {
                    newFile.mkdir();
                    je = jis.getNextJarEntry();
                    continue;
                }
                try (FileOutputStream fos = new FileOutputStream(newFile)) {
                    int len;
                    while ((len = jis.read(buffer)) > 0) {
                        fos.write(buffer, 0, len);
                    }
                }
                je = jis.getNextJarEntry();
            }
            jis.closeEntry();
            mm.extractionComplete(sender, dest.getAbsolutePath());
        } catch (AccessDeniedException e) {
            mm.fileAccessDenied(sender, ZTask.EXTRACT, e.getMessage());
        } catch (TaskInterruptedException e) {
            mm.taskInterruption(sender, ZTask.EXTRACT);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean validForExtraction(File src) {
        return PATH_END.matcher(src.getAbsolutePath()).find();
    }

    @Override
    public boolean srcValidForCompression(File src) {
        return false; // Compression to Jars is not supported.
    }

    @Override
    public boolean destValidForCompression(File dest) {
        return false; // Compression to Jars is not supported.
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
