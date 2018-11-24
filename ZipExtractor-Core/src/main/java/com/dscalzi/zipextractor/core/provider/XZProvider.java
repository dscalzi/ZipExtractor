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
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import org.tukaani.xz.LZMA2Options;
import org.tukaani.xz.XZInputStream;
import org.tukaani.xz.XZOutputStream;

import com.dscalzi.zipextractor.core.TaskInterruptedException;
import com.dscalzi.zipextractor.core.ZTask;
import com.dscalzi.zipextractor.core.manager.MessageManager;
import com.dscalzi.zipextractor.core.util.BaseCommandSender;

public class XZProvider implements TypeProvider {

    // Shared pattern by XZProviders
    public static final Pattern PATH_END = Pattern.compile("\\.xz$");
    public static final List<String> SUPPORTED_EXTRACT = new ArrayList<String>(Arrays.asList("xz"));
    public static final List<String> SUPPORTED_COMPRESS = new ArrayList<String>(Arrays.asList("non-directory"));

    @Override
    public List<String> scanForExtractionConflicts(BaseCommandSender sender, File src, File dest) {
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
    public void extract(BaseCommandSender sender, File src, File dest, boolean log) {
        final MessageManager mm = MessageManager.inst();
        final Logger logger = mm.getLogger();
        mm.startingProcess(sender, ZTask.EXTRACT, src.getName());
        File realDest = new File(dest.getAbsolutePath(), PATH_END.matcher(src.getName()).replaceAll(""));
        try (FileInputStream fis = new FileInputStream(src);
                XZInputStream xzis = new XZInputStream(fis);
                FileOutputStream fos = new FileOutputStream(realDest)) {
            if (log)
                logger.info("Extracting : " + src.getAbsoluteFile());
            byte[] buf = new byte[65536];
            int read = xzis.read(buf);
            while (read >= 1) {
                if (Thread.interrupted())
                    throw new TaskInterruptedException();
                fos.write(buf, 0, read);
                read = xzis.read(buf);
            }
            mm.extractionComplete(sender, realDest.getAbsolutePath());
        } catch (TaskInterruptedException e) {
            mm.taskInterruption(sender, ZTask.EXTRACT);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void compress(BaseCommandSender sender, File src, File dest, boolean log) {
        final MessageManager mm = MessageManager.inst();
        final Logger logger = mm.getLogger();
        mm.startingProcess(sender, ZTask.COMPRESS, src.getName());
        try (FileOutputStream fos = new FileOutputStream(dest);
                XZOutputStream xzos = new XZOutputStream(fos, new LZMA2Options());) {
            if (log)
                logger.info("Compressing : " + src.getAbsolutePath());
            xzos.write(Files.readAllBytes(src.toPath()));
            mm.compressionComplete(sender, dest.getAbsolutePath());
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
