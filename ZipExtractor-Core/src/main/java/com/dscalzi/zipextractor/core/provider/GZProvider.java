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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import com.dscalzi.zipextractor.core.TaskInterruptedException;
import com.dscalzi.zipextractor.core.ZTask;
import com.dscalzi.zipextractor.core.managers.MessageManager;
import com.dscalzi.zipextractor.core.util.ICommandSender;

public class GZProvider implements TypeProvider {

    // Shared pattern by GZProviders
    public static final Pattern PATH_END = Pattern.compile("\\.gz$");
    public static final List<String> SUPPORTED_EXTRACT = new ArrayList<String>(Arrays.asList("gz"));
    public static final List<String> SUPPORTED_COMPRESS = new ArrayList<String>(Arrays.asList("non-directory"));
    
    @Override
    public List<String> scanForExtractionConflicts(ICommandSender sender, File src, File dest) {
        final MessageManager mm = MessageManager.inst();
        mm.scanningForConflics(sender);
        File realDest = new File(dest.getAbsolutePath(), PATH_END.matcher(src.getName()).replaceAll(""));
        List<String> ret = new ArrayList<String>();
        if (realDest.exists()) {
            ret.add(realDest.getAbsolutePath());
        }
        return ret;
    }
    
    @Override
    public void extract(ICommandSender sender, File src, File dest, boolean log) {
        final MessageManager mm = MessageManager.inst();
        mm.startingProcess(sender, ZTask.EXTRACT, src.getName());
        File realDest = new File(dest.getAbsolutePath(), PATH_END.matcher(src.getName()).replaceAll(""));
        try (GZIPInputStream xzis = new GZIPInputStream(new FileInputStream(src));
                FileOutputStream fos = new FileOutputStream(realDest)) {
            if (log)
                mm.info("Extracting : " + src.getAbsoluteFile());
            byte[] buf = new byte[65536];
            int len = 0;
            while ((len = xzis.read(buf)) > 0) {
                if (Thread.interrupted())
                    throw new TaskInterruptedException();
                fos.write(buf, 0, len);
            }
            mm.extractionComplete(sender, realDest);
        } catch (TaskInterruptedException e) {
            mm.taskInterruption(sender, ZTask.EXTRACT);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    @Override
    public void compress(ICommandSender sender, File src, File dest, boolean log, boolean pipe) {
        final MessageManager mm = MessageManager.inst();
        mm.startingProcess(sender, ZTask.COMPRESS, src.getName());
        try (GZIPOutputStream xzos = new GZIPOutputStream(new FileOutputStream(dest));
                FileInputStream fis = new FileInputStream(src)) {
            if (log)
                mm.info("Compressing : " + src.getAbsolutePath());
            byte[] buf = new byte[65536];
            int len = 0;
            while ((len = fis.read(buf)) > 0) {
                if (Thread.interrupted())
                    throw new TaskInterruptedException();
                xzos.write(buf, 0, len);
            }
            if(!pipe)
                mm.compressionComplete(sender, dest);
        } catch (TaskInterruptedException e) {
            mm.taskInterruption(sender, ZTask.COMPRESS);
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
        return !src.isDirectory();
    }

    @Override
    public boolean destValidForCompression(File dest) {
        return validForExtraction(dest);
    }

    @Override
    public List<String> supportedExtractionTypes() {
        return SUPPORTED_EXTRACT;
    }

    @Override
    public List<String> canCompressTo() {
        return SUPPORTED_EXTRACT;
    }

    @Override
    public List<String> canCompressFrom() {
        return SUPPORTED_COMPRESS;
    }

}