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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.dscalzi.zipextractor.core.managers.MessageManager;
import com.dscalzi.zipextractor.core.provider.TypeProvider;
import com.dscalzi.zipextractor.core.util.ICommandSender;
import com.dscalzi.zipextractor.core.util.PageList;

public class ZExtractor {

    private static final Map<String, WarnData> WARNED = new HashMap<String, WarnData>();
    private static List<String> SUPPORTED;

    public static void asyncExtract(ICommandSender sender, File src, File dest, boolean log, final boolean override) {
        final MessageManager mm = MessageManager.inst();

        // If the user was warned, clear it.
        if (WARNED.containsKey(sender.getName())) {
            WARNED.remove(sender.getName());
        }

        // If the source file does not exist, abort.
        if (!src.exists()) {
            mm.sourceNotFound(sender, src.getAbsolutePath());
            return;
        }

        // If the destination directory does not exist, create it.
        if (!dest.exists()) {
            dest.mkdir();
        }

        // If the destination exists and it's not a directory, abort.
        if (dest.exists()) {
            if (!dest.isDirectory()) {
                mm.destNotDirectory(sender, dest.getAbsolutePath());
                return;
            }
        }

        Runnable task = null;
        for (final TypeProvider p : TypeProvider.getProviders()) {
            if (p.validForExtraction(src)) {
                task = () -> {
                    List<String> atRisk = new ArrayList<String>();
                    if (!override) {
                        atRisk = p.scanForExtractionConflicts(sender, src, dest);
                    }
                    if (atRisk.size() == 0 || override) {
                        p.extract(sender, src, dest, log);
                    } else {
                        WARNED.put(sender.getName(), new WarnData(src, dest, new PageList<String>(4, atRisk)));
                        mm.warnOfConflicts(sender, atRisk.size());
                    }
                };
                break;
            }
        }
        if (task != null) {
            int result = ZServicer.getInstance().submit(task);
            if (result == 0)
                mm.addToQueue(sender, ZServicer.getInstance().getSize());
            else if (result == 1)
                mm.queueFull(sender, ZServicer.getInstance().getMaxQueueSize());
            else if (result == 2)
                mm.executorTerminated(sender, ZTask.EXTRACT);
        } else {
            mm.invalidExtractionExtension(sender);
        }
    }

    public static List<String> supportedExtensions() {
        if (SUPPORTED == null) {
            SUPPORTED = new ArrayList<String>();
            for (final TypeProvider p : TypeProvider.getProviders()) {
                SUPPORTED.addAll(p.supportedExtractionTypes());
            }
        }
        return SUPPORTED;
    }

    public static boolean wasWarned(String name, File src, File dest) {
        if (WARNED.containsKey(name)) {
            final WarnData data = WARNED.get(name);
            return data.getSrc().equals(src) && data.getDest().equals(dest);
        }
        return false;
    }

    public static Optional<WarnData> getWarnData(String name) {
        WarnData data = WARNED.get(name);
        return data != null ? Optional.of(data) : Optional.empty();
    }

}
