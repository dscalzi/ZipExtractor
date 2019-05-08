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
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BooleanSupplier;

import com.dscalzi.zipextractor.core.managers.MessageManager;
import com.dscalzi.zipextractor.core.provider.TypeProvider;
import com.dscalzi.zipextractor.core.util.ICommandSender;
import com.dscalzi.zipextractor.core.util.OpTuple;
import com.dscalzi.zipextractor.core.util.PageList;

public class ZExtractor {

    private static final Map<String, WarnData> WARNED = new HashMap<String, WarnData>();
    private static List<String> SUPPORTED;
    private static List<String> PIPED_RISKS;

    public static void asyncExtract(ICommandSender sender, File src, File dest, boolean log, final boolean override, final boolean pipe, String until) {
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
        
        Deque<OpTuple> pDeque = new ArrayDeque<OpTuple>();
        
        if(pipe) {
            
            Path srcNorm = src.toPath().toAbsolutePath().normalize();
            String[] srcSplit = srcNorm.getFileName().toString().split("\\.", 2);
            String[] srcExts = srcSplit.length > 1 ? srcSplit[1].split("\\.") : new String[0];
            
            if(srcExts.length < 2) {
                TypeProvider p = getApplicableProvider(src);
                if(p == null) {
                    mm.invalidExtractionExtension(sender);
                    return;
                }
                pDeque.add(new OpTuple(src, dest, p));
            } else {
                
                File tSrc = src;
                String queue = srcNorm.toString();
                
                for(int i=srcExts.length-1; i>=0; i--) {
                    if((until != null && srcExts[i].equalsIgnoreCase(until)) || !supportedExtensions().contains(srcExts[i].toLowerCase())) {
                        break;
                    }
                    TypeProvider p = getApplicableProvider(tSrc);
                    if(p == null) {
                        mm.invalidExtractionExtension(sender);
                        return;
                    }
                    pDeque.add(new OpTuple(tSrc, dest, p));
                    queue = queue.substring(0, queue.lastIndexOf('.'));
                    tSrc = new File(dest + File.separator + new File(queue).getName());
                    
                }

            }
            
        } else {
            TypeProvider p = getApplicableProvider(src);
            if(p == null) {
                mm.invalidExtractionExtension(sender);
                return;
            }
            pDeque.add(new OpTuple(src, dest, p));
        }
        
        // Ensure a proper scan can be performed with this piped extraction.
        // This is only needed when the destination directory is not empty.
        // There can never be a conflict with an empty destination.
        if(pipe && dest.list().length > 0) {
            // The first source can be fully scanned for conflicts since it is already
            // in its final state on the disk.
            boolean first = true;
            for(final OpTuple op : pDeque) {
                if(!first && !op.getProvider().canDetectPipedConflicts()) {
                    mm.pipedConflictRisk(sender);
                    return;
                }
                first = false;
            }
        }

        // Fully scan the piped chain for extraction conflicts.
        // This is so that we can detect ALL conflicts in every
        // stage of the operation, and report the full list to the user.
        if(!override && pipe) {
            List<String> atRisk = new ArrayList<String>();
            for(final OpTuple op : pDeque) {
                atRisk.addAll(op.getProvider().scanForExtractionConflicts(sender, op.getSrc(), op.getDest()));
            }
            if(atRisk.size() > 0) {
                WARNED.put(sender.getName(), new WarnData(src, dest, new PageList<String>(4, atRisk)));
                mm.warnOfConflicts(sender, atRisk.size());
                return;
            }
        }
        
        // Prepare the tasks.
        // We will still check for conflicts in this stage for added security.
        // If any exist, the operation will be terminated.
        Runnable task = null;
        int c = 0;
        boolean piped = false;
        final BooleanSupplier[] pipes = new BooleanSupplier[pDeque.size()];
        for(final OpTuple op : pDeque) {
            final boolean interOp = c != pDeque.size()-1;

            if(piped) {
                pipes[c] = () -> {
                    List<String> atRisk = new ArrayList<String>();
                    if (!override) {
                        atRisk = op.getProvider().scanForExtractionConflicts(sender, op.getSrc(), op.getDest());
                    }
                    if (atRisk.size() == 0 || override) {
                        op.getProvider().extract(sender, op.getSrc(), op.getDest(), log, interOp);
                        op.getSrc().delete();
                        return true;
                    } else {
                        WARNED.put(sender.getName(), new WarnData(op.getSrc(), op.getDest(), new PageList<String>(4, atRisk)));
                        mm.warnOfConflicts(sender, atRisk.size());
                        return false;
                    }
                };
            } else {
                pipes[c] = () -> {
                    List<String> atRisk = new ArrayList<String>();
                    if (!override) {
                        atRisk = op.getProvider().scanForExtractionConflicts(sender, op.getSrc(), op.getDest());
                    }
                    if (atRisk.size() == 0 || override) {
                        op.getProvider().extract(sender, op.getSrc(), op.getDest(), log, interOp);
                        return true;
                    } else {
                        WARNED.put(sender.getName(), new WarnData(op.getSrc(), op.getDest(), new PageList<String>(4, atRisk)));
                        mm.warnOfConflicts(sender, atRisk.size());
                        return false;
                    }
                };
            }
            
            piped = true;
            c++;
        }
            
        task = () -> {
            for(BooleanSupplier r : pipes) {
                if(!r.getAsBoolean()) {
                    // Conflicts
                    break;
                }
            }
        };
        
        int result = ZServicer.getInstance().submit(task);
        if (result == 0)
            mm.addToQueue(sender, ZServicer.getInstance().getSize());
        else if (result == 1)
            mm.queueFull(sender, ZServicer.getInstance().getMaxQueueSize());
        else if (result == 2)
            mm.executorTerminated(sender, ZTask.EXTRACT);
    }

    private static TypeProvider getApplicableProvider(File src) {
        TypeProvider provider = null;
        for (final TypeProvider p : TypeProvider.getProviders()) {
            if (p.validForExtraction(src)) {
                provider = p;
            }
        }
        return provider;
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
    
    public static List<String> pipedConflictRiskExtensions(){
        if(PIPED_RISKS == null) {
            PIPED_RISKS = new ArrayList<String>();
            for(final TypeProvider p : TypeProvider.getProviders()) {
                if(!p.canDetectPipedConflicts()) {
                    PIPED_RISKS.addAll(p.supportedExtractionTypes());
                }
            }
        }
        return PIPED_RISKS;
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
