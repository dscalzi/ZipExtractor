/*
 * This file is part of ZipExtractor.
 * Copyright (C) 2016-2020 Daniel D. Scalzi <https://github.com/dscalzi/ZipExtractor>
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
import org.tukaani.xz.LZMA2Options;
import org.tukaani.xz.XZFormatException;
import org.tukaani.xz.XZInputStream;
import org.tukaani.xz.XZOutputStream;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public class XZProvider implements TypeProvider {

    // Shared pattern by XZProviders
    public static final Pattern PATH_END = Pattern.compile("\\.xz$");
    protected static final List<String> SUPPORTED_EXTRACT = new ArrayList<>(Collections.singletonList("xz"));
    protected static final List<String> SUPPORTED_COMPRESS = new ArrayList<>(Collections.singletonList("non-directory"));

    @Override
    public List<String> scanForExtractionConflicts(ICommandSender sender, File src, File dest, boolean silent) {
        final MessageManager mm = MessageManager.inst();
        if(!silent)
            mm.scanningForConflics(sender);
        File realDest = new File(dest.getAbsolutePath(), PATH_END.matcher(src.getName()).replaceAll(""));
        List<String> ret = new ArrayList<>();
        if (realDest.exists()) {
            ret.add(realDest.getAbsolutePath());
        }
        return ret;
    }

    @Override
    public boolean canDetectPipedConflicts() {
        return true;
    }
    
    @Override
    public boolean extract(ICommandSender sender, File src, File dest, boolean log, boolean pipe) {
        final MessageManager mm = MessageManager.inst();
        mm.startingProcess(sender, ZTask.EXTRACT, src.getName());
        File realDest = new File(dest.getAbsolutePath(), PATH_END.matcher(src.getName()).replaceAll(""));
        try (XZInputStream xzis = new XZInputStream(new FileInputStream(src));
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
            if(!pipe)
                mm.extractionComplete(sender, realDest);
            return true;
        } catch(XZFormatException e) {
            mm.extractionFormatError(sender, src, "XZ");
            return false;
        } catch (TaskInterruptedException e) {
            mm.taskInterruption(sender, ZTask.EXTRACT);
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            mm.genericOperationError(sender, src, ZTask.EXTRACT);
            return false;
        }
    }

    @Override
    public boolean compress(ICommandSender sender, File src, File dest, boolean log, boolean pipe) {
        final MessageManager mm = MessageManager.inst();
        mm.startingProcess(sender, ZTask.COMPRESS, src.getName());
        try (XZOutputStream xzos = new XZOutputStream(new FileOutputStream(dest), new LZMA2Options());
                FileInputStream fis = new FileInputStream(src)) {
            if (log)
                mm.info("Compressing : " + src.getAbsolutePath());
            byte[] buf = new byte[8*1024];
            int len = 0;
            while ((len = fis.read(buf)) > 0) {
                if (Thread.interrupted())
                    throw new TaskInterruptedException();
                xzos.write(buf, 0, len);
            }
            if(!pipe)
                mm.compressionComplete(sender, dest);
            return true;
        } catch (TaskInterruptedException e) {
            mm.taskInterruption(sender, ZTask.COMPRESS);
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            mm.genericOperationError(sender, src, ZTask.COMPRESS);
            return false;
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
