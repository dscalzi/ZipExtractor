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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.regex.Pattern;

import com.dscalzi.zipextractor.core.ZTask;
import com.dscalzi.zipextractor.core.managers.MessageManager;
import com.dscalzi.zipextractor.core.util.ICommandSender;
import com.dscalzi.zipextractor.core.util.JavaUtil;
import com.dscalzi.zipextractor.core.util.ReflectionUtil;

// Pack200 deprecated in JDK 13, removed in JDK 14.
// https://openjdk.java.net/jeps/367
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
            try {
                this.unpack(src, jarStream);
            } catch(Throwable t) {
                t.printStackTrace();
                mm.genericOperationError(sender, src, ZTask.EXTRACT);
                return false;
            }
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
            try {
                this.pack(in, out);
            } catch(Throwable t) {
                t.printStackTrace();
                mm.genericOperationError(sender, src, ZTask.COMPRESS);
                return false;
            }
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

    @Override
    public boolean isSupported() {
        return JavaUtil.getJavaVersion() < 14;
    }

    @Override
    public String getUnsupportedMessage() {
        return "Pack200 support is only enabled on JDK 13 and below.";
    }

    /* Access Pack200 Reflectively */

    protected Class<?> getPack200Class() {
        return ReflectionUtil.getClass("java.util.jar.Pack200");
    }

    protected Object getUnpacker() throws InvocationTargetException, IllegalAccessException {
        Class<?> Pack200Class = this.getPack200Class();
        Method newUnpacker = Objects.requireNonNull(ReflectionUtil.getMethod(Pack200Class, "newUnpacker"));
        return newUnpacker.invoke(null);
    }

    protected Object getPacker() throws InvocationTargetException, IllegalAccessException {
        Class<?> Pack200Class = this.getPack200Class();
        Method newPacker = Objects.requireNonNull(ReflectionUtil.getMethod(Pack200Class, "newPacker"));
        return newPacker.invoke(null);
    }

    protected Method getUnpackMethod() {
        Class<?> Pack200Class = this.getPack200Class();
        Class<?> UnpackerClass = ReflectionUtil.getDeclaredClass(Pack200Class, "Unpacker");
        // void unpack(File in, JarOutputStream out) throws IOException;
        return ReflectionUtil.getMethod(UnpackerClass, "unpack", File.class, JarOutputStream.class);
    }

    protected Method getPackMethod() {
        Class<?> Pack200Class = this.getPack200Class();
        Class<?> PackerClass = ReflectionUtil.getDeclaredClass(Pack200Class, "Packer");
        // void pack(JarFile in, OutputStream out) throws IOException;
        return ReflectionUtil.getMethod(PackerClass, "pack", JarFile.class, OutputStream.class);
    }

    protected void unpack(File in, JarOutputStream out) throws InvocationTargetException, IllegalAccessException {
        // Pack200.newUnpacker().unpack(src, jarStream);
        Object unpacker = this.getUnpacker();
        Method unpackMethod = this.getUnpackMethod();
        unpackMethod.invoke(unpacker, in, out);
    }

    protected void pack(JarFile in, OutputStream out) throws InvocationTargetException, IllegalAccessException {
        // Pack200.newPacker().pack(in, out);
        Object packer = this.getPacker();
        Method packMethod = this.getPackMethod();
        packMethod.invoke(packer, in, out);
    }

}
