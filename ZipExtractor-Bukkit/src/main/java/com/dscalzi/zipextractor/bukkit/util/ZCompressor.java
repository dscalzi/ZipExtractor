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

package com.dscalzi.zipextractor.bukkit.util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.command.CommandSender;

import com.dscalzi.zipextractor.bukkit.ZipExtractor;
import com.dscalzi.zipextractor.bukkit.managers.MessageManager;
import com.dscalzi.zipextractor.bukkit.providers.TypeProvider;

public class ZCompressor {

    private static List<String> SUPPORTED;

    public static void asyncCompress(CommandSender sender, File src, File dest, final boolean override) {
        final MessageManager mm = MessageManager.getInstance();
        // If the source does not exist, abort.
        if (!src.exists()) {
            mm.sourceNotFound(sender, src.getAbsolutePath());
            return;
        }

        Runnable task = null;
        for (final TypeProvider p : ZipExtractor.getProviders()) {
            if (p.destValidForCompression(dest)) {
                if (p.srcValidForCompression(src)) {
                    if (dest.exists() && !override) {
                        mm.destExists(sender);
                        return;
                    }
                    task = () -> {
                        p.compress(sender, src, dest);
                    };
                } else {
                    mm.invalidSourceForDest(sender, p.canCompressFrom(), p.canCompressTo());
                    return;
                }
            }
        }

        if (task != null) {
            int result = ZServicer.getInstance().submit(task);
            if (result == 0)
                mm.addToQueue(sender, ZServicer.getInstance().getSize());
            else if (result == 1)
                mm.queueFull(sender);
            else if (result == 2)
                mm.executorTerminated(sender, ZTask.COMPRESS);
        } else {
            mm.invalidCompressionExtension(sender);
        }
    }

    public static List<String> supportedExtensions() {
        if (SUPPORTED == null) {
            SUPPORTED = new ArrayList<String>();
            for (final TypeProvider p : ZipExtractor.getProviders()) {
                SUPPORTED.addAll(p.canCompressTo());
            }
        }
        return SUPPORTED;
    }

}
