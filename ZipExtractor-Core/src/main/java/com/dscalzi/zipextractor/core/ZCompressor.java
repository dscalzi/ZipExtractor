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

package com.dscalzi.zipextractor.core;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.dscalzi.zipextractor.core.managers.MessageManager;
import com.dscalzi.zipextractor.core.provider.TypeProvider;
import com.dscalzi.zipextractor.core.util.ICommandSender;

public class ZCompressor {

    private static List<String> SUPPORTED;

    public static void asyncCompress(ICommandSender sender, File src, File dest, boolean log, final boolean override) {
        final MessageManager mm = MessageManager.inst();
        // If the source does not exist, abort.
        if (!src.exists()) {
            mm.sourceNotFound(sender, src.getAbsolutePath());
            return;
        }
        
        String[] srcSplit = src.toPath().toAbsolutePath().normalize().toString().split("\\.", 2);
        String[] destSplit = dest.toPath().toAbsolutePath().normalize().toString().split("\\.", 2);
        
        // We need an extension.
        if(destSplit.length < 2) {
            mm.invalidCompressionExtension(sender);
            return;
        }
        
        String[] srcExts = srcSplit.length > 1 ? srcSplit[1].split("\\.") : new String[0];
        String[] destExts = destSplit[1].split("\\.");
        
        Map<File, File> pMap = new LinkedHashMap<File, File>();
        if(destExts.length < 2) {
            pMap.put(src, dest);
        } else {
            File dTemp = dest;
            File sTemp = src;
            String extAcc = "";
            for (int i=0; i<destExts.length; i++) {
                extAcc += '.' + destExts[i];
                if (i<srcExts.length && !srcExts[i].equals(destExts[i]) || !(i<srcExts.length)) {
                    dTemp = new File(destSplit[0] + extAcc);
                    pMap.put(sTemp, dTemp);
                    sTemp = dTemp;
                }
            }
        }
        
        Runnable task = null;
        int c = 0;
        boolean piped = false;
        final Runnable[] pipes = new Runnable[pMap.size()];
        for (final Map.Entry<File, File> e : pMap.entrySet()) {
            for (final TypeProvider p : TypeProvider.getProviders()) {
                if (p.destValidForCompression(e.getValue())) {
                    if (p.srcValidForCompression(e.getKey())) {
                        if (e.getValue().exists() && !override) {
                            mm.destExists(sender);
                            return;
                        }
                        final boolean printFinish = c != pMap.size()-1;
                        if(piped) {
                            pipes[c] = () -> {
                                p.compress(sender, e.getKey(), e.getValue(), log, printFinish);
                                e.getKey().delete();
                            };
                        } else {
                            pipes[c] = () -> {
                                p.compress(sender, e.getKey(), e.getValue(), log, printFinish);
                            };
                        }
                        piped = true;
                    } else {
                        mm.invalidSourceForDest(sender, p.canCompressFrom(), p.canCompressTo());
                        return;
                    }
                }
            }
            // If we can't process this phase, cancel the operation.
            if(pipes[c] == null) {
                mm.invalidCompressionExtension(sender);
                return;
            }
            c++;
        }
        
        task = () -> {
            for(Runnable r : pipes) {
                r.run();
            }
        };

        int result = ZServicer.getInstance().submit(task);
        if (result == 0)
            mm.addToQueue(sender, ZServicer.getInstance().getSize());
        else if (result == 1)
            mm.queueFull(sender, ZServicer.getInstance().getMaxQueueSize());
        else if (result == 2)
            mm.executorTerminated(sender, ZTask.COMPRESS);
    }

    public static List<String> supportedExtensions() {
        if (SUPPORTED == null) {
            SUPPORTED = new ArrayList<String>();
            for (final TypeProvider p : TypeProvider.getProviders()) {
                SUPPORTED.addAll(p.canCompressTo());
            }
        }
        return SUPPORTED;
    }

}
