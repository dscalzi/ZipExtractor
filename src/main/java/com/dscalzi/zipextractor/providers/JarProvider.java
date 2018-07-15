/*
 * ZipExtractor
 * Copyright (C) 2016-2018 Daniel D. Scalzi
 * See LICENSE for license information.
 */
package com.dscalzi.zipextractor.providers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import org.bukkit.command.CommandSender;

import com.dscalzi.zipextractor.managers.ConfigManager;
import com.dscalzi.zipextractor.managers.MessageManager;
import com.dscalzi.zipextractor.util.TaskInterruptedException;
import com.dscalzi.zipextractor.util.ZTask;

public class JarProvider implements TypeProvider {

    // Shared pattern by JarProviders
    public static final Pattern PATH_END = Pattern.compile("\\.jar$");
    public static final List<String> SUPPORTED = new ArrayList<String>(Arrays.asList("jar"));

    @Override
    public List<String> scanForExtractionConflicts(CommandSender sender, File src, File dest) {
        
        List<String> existing = new ArrayList<String>();
        final MessageManager mm = MessageManager.getInstance();
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
    public void extract(CommandSender sender, File src, File dest) {
        final ConfigManager cm = ConfigManager.getInstance();
        final MessageManager mm = MessageManager.getInstance();
        final Logger logger = mm.getLogger();
        final boolean log = cm.getLoggingProperty();
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
        } catch (FileNotFoundException e) {
            mm.fileNotFound(sender, src.getAbsolutePath());
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
