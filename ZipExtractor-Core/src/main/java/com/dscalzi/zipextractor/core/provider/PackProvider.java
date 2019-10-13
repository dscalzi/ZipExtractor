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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Pack200;
import java.util.regex.Pattern;

import com.dscalzi.zipextractor.core.ZTask;
import com.dscalzi.zipextractor.core.managers.MessageManager;
import com.dscalzi.zipextractor.core.util.ICommandSender;

public class PackProvider implements TypeProvider {

    public static final Pattern PATH_END_EXTRACT = Pattern.compile("\\.pack$");
    public static final Pattern PATH_END_COMPRESS = Pattern.compile("\\.jar$");
    protected static final List<String> SUPPORTED_EXTRACT = new ArrayList<>(Collections.singletonList("pack"));
    protected static final List<String> SUPPORTED_COMPRESS = new ArrayList<>(Collections.singletonList("jar"));

    @Override
    public List<String> scanForExtractionConflicts(ICommandSender sender, File src, File dest, boolean silent) {
        final MessageManager mm = MessageManager.inst();
        if(!silent)
            mm.scanningForConflics(sender);
        File realDest = new File(dest.getAbsolutePath(), PATH_END_EXTRACT.matcher(src.getName()).replaceAll(""));
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
        File realDest = new File(dest.getAbsolutePath(), PATH_END_EXTRACT.matcher(src.getName()).replaceAll(""));
        try (JarOutputStream jarStream = new JarOutputStream(new FileOutputStream(realDest))) {
            if (log)
                mm.info("Extracting : " + src.getAbsoluteFile());
            Pack200.newUnpacker().unpack(src, jarStream);
            if(!pipe)
                mm.extractionComplete(sender, realDest);
            return true;
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
        try (JarFile in = new JarFile(src); OutputStream out = Files.newOutputStream(dest.toPath())) {
            if (log)
                mm.info("Compressing : " + src.getAbsolutePath());
            Pack200.newPacker().pack(in, out);
            if(!pipe)
                mm.compressionComplete(sender, dest);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            mm.genericOperationError(sender, src, ZTask.COMPRESS);
            return false;
        }
    }

    @Override
    public boolean validForExtraction(File src) {
        return PATH_END_EXTRACT.matcher(src.getAbsolutePath()).find();
    }

    @Override
    public boolean srcValidForCompression(File src) {
        return PATH_END_COMPRESS.matcher(src.getAbsolutePath()).find();
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
