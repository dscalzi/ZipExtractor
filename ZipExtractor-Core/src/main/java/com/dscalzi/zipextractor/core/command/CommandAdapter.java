/*
 * This file is part of ZipExtractor.
 * Copyright (C) 2016-2019 Daniel D. Scalzi <https://github.com/dscalzi/ZipExtractor>
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

package com.dscalzi.zipextractor.core.command;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import com.dscalzi.zipextractor.core.WarnData;
import com.dscalzi.zipextractor.core.ZCompressor;
import com.dscalzi.zipextractor.core.ZExtractor;
import com.dscalzi.zipextractor.core.ZServicer;
import com.dscalzi.zipextractor.core.managers.IConfigManager;
import com.dscalzi.zipextractor.core.managers.MessageManager;
import com.dscalzi.zipextractor.core.util.ICommandSender;
import com.dscalzi.zipextractor.core.util.IPlugin;
import com.dscalzi.zipextractor.core.util.PathUtils;

public class CommandAdapter {
    
    public static final Pattern COMMANDS = Pattern.compile(
            "^(?iu)(help|extract|compress|src|dest|setsrc|setdest|status|plugindir|terminate|forceterminate|reload|version)");
    public static final Pattern INTEGERS = Pattern.compile("(\\\\d+|-\\\\d+)");
    
    MessageManager mm;
    IConfigManager cm;
    
    IPlugin plugin;
    
    public CommandAdapter(IPlugin plugin, MessageManager mm, IConfigManager cm) {
        this.mm = mm;
        this.cm = cm;
        this.plugin = plugin;
    }
    
    public void resolve(ICommandSender sender, String[] args) {
        
        if (sender.isCommandBlock()) {
            mm.denyCommandBlock(sender);
            return;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("version")) {
            this.cmdVersion(sender);
            return;
        }

        if (!sender.hasPermission("zipextractor.admin.use")) {
            mm.noPermissionFull(sender);
            return;
        }

        if (args.length > 0) {
            if (args[0].matches("(\\d+|-\\d+)")) {
                this.cmdList(sender, Integer.parseInt(args[0]));
                return;
            }
            if (args[0].equalsIgnoreCase("help")) {
                if (args.length > 1 && COMMANDS.matcher(args[1]).matches()) {
                    this.cmdMoreInfo(sender, args[1]);
                    return;
                }
                if (args.length > 1 && INTEGERS.matcher(args[1]).matches()) {
                    this.cmdList(sender, Integer.parseInt(args[1]));
                    return;
                }
                this.cmdList(sender, 1);
                return;
            }
            if (args[0].equalsIgnoreCase("extract")) {
                this.cmdExtract(sender, args);
                return;
            }
            if (args[0].equalsIgnoreCase("compress")) {
                this.cmdCompress(sender, args);
                return;
            }
            if (args[0].equalsIgnoreCase("src")) {
                this.cmdSrc(sender, args);
                return;
            }
            if (args[0].equalsIgnoreCase("dest")) {
                this.cmdDest(sender, args);
                return;
            }
            if (args[0].equalsIgnoreCase("setsrc")) {
                this.cmdSetSrc(sender, args);
                return;
            }
            if (args[0].equalsIgnoreCase("setdest")) {
                this.cmdSetDest(sender, args);
                return;
            }
            if (args[0].equalsIgnoreCase("status")) {
                this.cmdStatus(sender);
                return;
            }
            if (args[0].equalsIgnoreCase("plugindir")) {
                this.cmdPluginDir(sender, plugin);
                return;
            }
            if (args[0].equalsIgnoreCase("terminate")) {
                this.cmdTerminate(sender, false);
                return;
            }
            if (args[0].equalsIgnoreCase("forceterminate")) {
                this.cmdTerminate(sender, true);
                return;
            }
            if (args[0].equalsIgnoreCase("reload")) {
                this.cmdReload(sender, plugin);
                return;
            }
            if (args[0].equalsIgnoreCase("version")) {
                this.cmdVersion(sender);
                return;
            }
        }

        this.cmdList(sender, 1);
    }

    public void cmdList(ICommandSender sender, int page) {
        mm.commandList(sender, --page);
    }

    public void cmdMoreInfo(ICommandSender sender, String cmd) {
        mm.commandInfo(sender, cmd);
    }

    public void cmdExtract(ICommandSender sender, String[] args) {
        if (!sender.hasPermission("zipextractor.admin.extract")) {
            mm.noPermission(sender);
            return;
        }

        if (args.length >= 2 && args[1].equalsIgnoreCase("view")) {
            Optional<WarnData> dataOpt = ZExtractor.getWarnData(sender.getName());
            if (dataOpt.isPresent()) {
                WarnData d = dataOpt.get();

                int page = 0;
                if (args.length >= 3) {
                    try {
                        page = Integer.parseInt(args[2]);
                        if (1 > page || page > d.getFiles().size()) {
                            throw new NumberFormatException();
                        } else {
                            --page;
                        }
                    } catch (NumberFormatException e) {
                        mm.invalidPage(sender);
                        return;
                    }
                }

                mm.formatWarnList(sender, page, d.getFiles());

            } else {
                mm.noWarnData(sender);
            }
        } else {
            
            boolean override = !cm.warnOnConflitcts();
            boolean pipe = false;
            String until = null;

            if(args.length >= 2) {
                for(int i=1; i<args.length; i++) {
                    
                    if (args[i].equalsIgnoreCase("--override")) {
                        if (!sender.hasPermission("zipextractor.admin.override.extract")) {
                            mm.noPermission(sender);
                            return;
                        }
                        override = true;
                    } else if(args[i].equalsIgnoreCase("--all")) {
                        pipe = true;
                    } else if(args[i].equalsIgnoreCase("--until")) {
                        // Expect a parameter follow-up.
                        if(++i < args.length) {
                            until = args[i].toLowerCase();
                            pipe = true;
                        } else {
                            mm.untilMissingType(sender);
                            return;
                        }
                    }
                    
                }
            }

            Optional<File> srcOpt = cm.getSourceFile();
            Optional<File> destOpt = cm.getDestFile();
            if (!srcOpt.isPresent()) {
                mm.invalidPath(sender, cm.getSourceRaw(), "source");
                return;
            }
            if (!destOpt.isPresent()) {
                mm.invalidPath(sender, cm.getDestRaw(), "destination");
                return;
            }

            ZExtractor.asyncExtract(sender, srcOpt.get(), destOpt.get(), cm.getLoggingProperty(), override, pipe, until);
        }
    }

    public void cmdCompress(ICommandSender sender, String[] args) {
        if (!sender.hasPermission("zipextractor.admin.compress")) {
            mm.noPermission(sender);
            return;
        }

        boolean override = !cm.warnOnConflitcts();
        if (args.length >= 2 && args[1].equalsIgnoreCase("--override")) {
            if (!sender.hasPermission("zipextractor.admin.override.compress")) {
                mm.noPermission(sender);
                return;
            }
            override = true;
        }

        Optional<File> srcOpt = cm.getSourceFile();
        Optional<File> destOpt = cm.getDestFile();
        if (!srcOpt.isPresent()) {
            mm.invalidPath(sender, cm.getSourceRaw(), "source");
            return;
        }
        if (!destOpt.isPresent()) {
            mm.invalidPath(sender, cm.getDestRaw(), "destination");
            return;
        }

        ZCompressor.asyncCompress(sender, srcOpt.get(), destOpt.get(), cm.getLoggingProperty(), override);

    }

    public void cmdSrc(ICommandSender sender, String[] args) {
        if (!sender.hasPermission("zipextractor.admin.src")) {
            mm.noPermission(sender);
            return;
        }
        if (args.length > 1 && args[1].equalsIgnoreCase("--absolute")) {
            Optional<File> srcOpt = cm.getSourceFile();
            if (!srcOpt.isPresent()) {
                mm.invalidPath(sender, cm.getSourceRaw(), "source");
                return;
            }
            mm.sendSuccess(sender, srcOpt.get().toPath().toAbsolutePath().normalize().toString());
        } else {
            mm.sendSuccess(sender, cm.getSourceRaw());
        }
    }

    public void cmdDest(ICommandSender sender, String[] args) {
        if (!sender.hasPermission("zipextractor.admin.dest")) {
            mm.noPermission(sender);
            return;
        }
        if (args.length > 1 && args[1].equalsIgnoreCase("--absolute")) {
            Optional<File> destOpt = cm.getDestFile();
            if (!destOpt.isPresent()) {
                mm.invalidPath(sender, cm.getDestRaw(), "destination");
                return;
            }
            mm.sendSuccess(sender, destOpt.get().toPath().toAbsolutePath().normalize().toString());
        } else {
            mm.sendSuccess(sender, cm.getDestRaw());
        }
    }

    public void cmdSetSrc(ICommandSender sender, String[] args) {
        if (!sender.hasPermission("zipextractor.admin.setsrc")) {
            mm.noPermission(sender);
            return;
        }
        if (args.length < 2) {
            mm.specifyAPath(sender);
            return;
        }
        String path = PathUtils.formatPath(PathUtils.join(args, ' ', 1, args.length).trim(), true);
        if (!PathUtils.validateFilePath(path)) {
            mm.invalidPath(sender, path);
            return;
        }
        if (cm.setSourcePath(path))
            mm.setPathSuccess(sender, "source");
        else
            mm.setPathFailed(sender, "source");
        cm.reload();
    }

    public void cmdSetDest(ICommandSender sender, String[] args) {
        if (!sender.hasPermission("zipextractor.admin.setdest")) {
            mm.noPermission(sender);
            return;
        }
        if (args.length < 2) {
            mm.specifyAPath(sender);
            return;
        }
        String path = PathUtils.formatPath(PathUtils.join(args, ' ', 1, args.length).trim(), true);
        if (!PathUtils.validateFilePath(path)) {
            mm.invalidPath(sender, path);
            return;
        }
        if (cm.setDestPath(path))
            mm.setPathSuccess(sender, "destination");
        else
            mm.setPathFailed(sender, "destination");
        cm.reload();
    }

    public void cmdReload(ICommandSender sender, IPlugin plugin) {
        if (!sender.hasPermission("zipextractor.admin.reload")) {
            mm.noPermission(sender);
            return;
        }
        try {
            if(plugin.reload()) {
                mm.reloadSuccess(sender);
            } else {
                mm.reloadFailed(sender);
            }   
        } catch (Throwable e) {
            mm.reloadFailed(sender);
            mm.severe("Error while reloading the plugin", e);
            e.printStackTrace();
        }
    }
    
    public void cmdPluginDir(ICommandSender sender, IPlugin plugin) {
        if (!sender.hasPermission("zipextractor.admin.plugindir")) {
            mm.noPermission(sender);
            return;
        }
        mm.sendMessage(sender, "Plugin Directory - " + plugin.getPluginDirectory());
    }

    public void cmdTerminate(ICommandSender sender, boolean force) {
        if (!sender.hasPermission(force ? "zipextractor.admin.forceterminate" : "zipextractor.admin.terminate")) {
            mm.noPermission(sender);
            return;
        }
        ZServicer e = ZServicer.getInstance();
        if (e.isTerminated()) {
            mm.alreadyTerminated(sender);
            return;
        }
        if (e.isTerminating()) {
            mm.alreadyTerminating(sender);
            return;
        }
        e.terminate(force, false);
        if (force)
            mm.terminatingForcibly(sender);
        else
            mm.terminating(sender);
    }

    public void cmdStatus(ICommandSender sender) {
        if (!sender.hasPermission("zipextractor.harmless.status")) {
            mm.noPermission(sender);
            return;
        }
        mm.cmdStatus(sender);
    }

    public void cmdVersion(ICommandSender sender) {
        mm.cmdVersion(sender, true);
    }
    
    public List<String> tabComplete(ICommandSender sender, String[] args) {
        List<String> ret = new ArrayList<>();

        if (args.length == 1) {
            ret.addAll(subCommands(sender, args));
        }

        if (args.length >= 2) {
            
            final String arg0Normal = args[0].toLowerCase();
            
            if((arg0Normal.equals("setdest") && sender.hasPermission("zipextractor.admin.setdest")) || (arg0Normal.equals("setsrc") && sender.hasPermission("zipextractor.admin.setsrc"))) {
                
                if(cm.tabCompleteFiles()) {
                    ret.addAll(PathUtils.tabCompletePath(args));
                }
                
            } else {
                
                boolean c = sender.hasPermission("zipextractor.admin.extract")
                        && "extract".startsWith(arg0Normal);
                
                
                if(args.length == 2) {
                    boolean a = sender.hasPermission("zipextractor.admin.src") && "src".startsWith(arg0Normal);
                    boolean b = sender.hasPermission("zipextractor.admin.dest") && "dest".startsWith(arg0Normal);
                    
                    boolean d = sender.hasPermission("zipextractor.admin.compress")
                            && "compress".startsWith(arg0Normal);
                    
                    if (a || b)
                        if ("--absolute".startsWith(args[1].toLowerCase()))
                            ret.add("--absolute");
                    if (sender.hasPermission("zipextractor.admin.use") && "help".startsWith(arg0Normal)) {
                        String[] newArgs = new String[args.length - 1];
                        System.arraycopy(args, 1, newArgs, 0, args.length - 1);
                        ret.addAll(subCommands(sender, newArgs));
                    }

                    if (c && ZExtractor.getWarnData(sender.getName()).isPresent() && "view".startsWith(args[1].toLowerCase())) {
                        ret.add("view");
                    }
                    if (d && sender.hasPermission("zipextractor.admin.override.compress") && "--override".startsWith(args[1].toLowerCase())) {
                        ret.add("--override");
                    }
                }
                
                if(args.length >= 2) {
                    if(c) {
                        if(args[args.length-2].equalsIgnoreCase("--until")) {
                            ret.addAll(ZExtractor.supportedExtensions());
                        } else {
                            if("--all".startsWith(args[args.length-1]))
                                ret.add("--all");
                            if("--until".startsWith(args[args.length-1]))
                                ret.add("--until");
                            if(c && sender.hasPermission("zipextractor.admin.override.extract") && "--override".startsWith(args[args.length-1].toLowerCase())){
                                ret.add("--override");
                            }
                        }
                    }
                    
                    
                }
            }
        }

        return ret;
    }

    private List<String> subCommands(ICommandSender sender, String[] args) {
        List<String> ret = new ArrayList<>();

        if (args.length == 1) {
            if (sender.hasPermission("zipextractor.admin.use") && "help".startsWith(args[0].toLowerCase()))
                ret.add("help");
            if (sender.hasPermission("zipextractor.admin.extract") && "extract".startsWith(args[0].toLowerCase()))
                ret.add("extract");
            if (sender.hasPermission("zipextractor.admin.compress") && "compress".startsWith(args[0].toLowerCase()))
                ret.add("compress");
            if (sender.hasPermission("zipextractor.admin.src") && "src".startsWith(args[0].toLowerCase()))
                ret.add("src");
            if (sender.hasPermission("zipextractor.admin.dest") && "dest".startsWith(args[0].toLowerCase()))
                ret.add("dest");
            if (sender.hasPermission("zipextractor.admin.setsrc") && "setsrc".startsWith(args[0].toLowerCase()))
                ret.add("setsrc");
            if (sender.hasPermission("zipextractor.admin.setdest") && "setdest".startsWith(args[0].toLowerCase()))
                ret.add("setdest");
            if (sender.hasPermission("zipextractor.harmless.status") && "status".startsWith(args[0].toLowerCase()))
                ret.add("status");
            if (sender.hasPermission("zipextractor.admin.plugindir") && "plugindir".startsWith(args[0].toLowerCase()))
                ret.add("plugindir");
            if (sender.hasPermission("zipextractor.admin.terminate") && "terminate".startsWith(args[0].toLowerCase()))
                ret.add("terminate");
            if (sender.hasPermission("zipextractor.admin.forceterminate")
                    && "forceterminate".startsWith(args[0].toLowerCase()))
                ret.add("forceterminate");
            if (sender.hasPermission("zipextractor.admin.reload") && "reload".startsWith(args[0].toLowerCase()))
                ret.add("reload");
            if ("version".startsWith(args[0].toLowerCase()))
                ret.add("version");
        }

        return ret;
    }
    
}
