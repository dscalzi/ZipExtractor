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

package com.dscalzi.zipextractor.sponge;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import org.spongepowered.api.command.CommandCallable;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.source.CommandBlockSource;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import com.dscalzi.zipextractor.core.WarnData;
import com.dscalzi.zipextractor.core.ZCompressor;
import com.dscalzi.zipextractor.core.ZExtractor;
import com.dscalzi.zipextractor.core.ZServicer;
import com.dscalzi.zipextractor.core.managers.MessageManager;
import com.dscalzi.zipextractor.core.util.BaseCommandSender;
import com.dscalzi.zipextractor.core.util.PathUtils;
import com.dscalzi.zipextractor.sponge.managers.ConfigManager;
import com.dscalzi.zipextractor.sponge.util.SpongeCommandSender;

public class MainExecutor implements CommandCallable {

    public static final Pattern COMMANDS = Pattern.compile(
            "^(?iu)(help|extract|compress|src|dest|setsrc|setdest|status|plugindir|terminate|forceterminate|reload|version)");
    public static final Pattern INTEGERS = Pattern.compile("(\\\\d+|-\\\\d+)");

    private MessageManager mm;
    private ConfigManager cm;

    private ZipExtractorPlugin plugin;

    public MainExecutor(ZipExtractorPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public CommandResult process(CommandSource source, String arguments) {
        
        final String[] args = arguments.isEmpty() ? new String[0] : arguments.replaceAll("\\s{2,}", " ").split(" ");
        this.mm = MessageManager.inst();
        this.cm = ConfigManager.getInstance();

        BaseCommandSender sender = new SpongeCommandSender(source);
        
        if (source instanceof CommandBlockSource) {
            mm.denyCommandBlock(sender);
            return CommandResult.success();
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("version")) {
            this.cmdVersion(sender);
            return CommandResult.success();
        }

        if (!sender.hasPermission("zipextractor.admin.use")) {
            mm.noPermissionFull(sender);
            return CommandResult.success();
        }

        if (args.length > 0) {
            if (args[0].matches("(\\d+|-\\d+)")) {
                this.cmdList(sender, Integer.parseInt(args[0]));
                return CommandResult.success();
            }
            if (args[0].equalsIgnoreCase("help")) {
                if (args.length > 1 && COMMANDS.matcher(args[1]).matches()) {
                    this.cmdMoreInfo(sender, args[1]);
                    return CommandResult.success();
                }
                if (args.length > 1 && INTEGERS.matcher(args[1]).matches()) {
                    this.cmdList(sender, Integer.parseInt(args[1]));
                    return CommandResult.success();
                }
                this.cmdList(sender, 1);
                return CommandResult.success();
            }
            if (args[0].equalsIgnoreCase("extract")) {
                this.cmdExtract(sender, args);
                return CommandResult.success();
            }
            if (args[0].equalsIgnoreCase("compress")) {
                this.cmdCompress(sender, args);
                return CommandResult.success();
            }
            if (args[0].equalsIgnoreCase("src")) {
                this.cmdSrc(sender, args);
                return CommandResult.success();
            }
            if (args[0].equalsIgnoreCase("dest")) {
                this.cmdDest(sender, args);
                return CommandResult.success();
            }
            if (args[0].equalsIgnoreCase("setsrc")) {
                this.cmdSetSrc(sender, args);
                return CommandResult.success();
            }
            if (args[0].equalsIgnoreCase("setdest")) {
                this.cmdSetDest(sender, args);
                return CommandResult.success();
            }
            if (args[0].equalsIgnoreCase("status")) {
                this.cmdStatus(sender);
                return CommandResult.success();
            }
            if (args[0].equalsIgnoreCase("plugindir")) {
                this.cmdPluginDir(sender);
                return CommandResult.success();
            }
            if (args[0].equalsIgnoreCase("terminate")) {
                this.cmdTerminate(sender, false);
                return CommandResult.success();
            }
            if (args[0].equalsIgnoreCase("forceterminate")) {
                this.cmdTerminate(sender, true);
                return CommandResult.success();
            }
            if (args[0].equalsIgnoreCase("reload")) {
                this.cmdReload(sender);
                return CommandResult.success();
            }
            if (args[0].equalsIgnoreCase("version")) {
                this.cmdVersion(sender);
                return CommandResult.success();
            }
        }

        this.cmdList(sender, 1);
        return CommandResult.success();
    }

    private void cmdList(BaseCommandSender sender, int page) {
        mm.commandList(sender, --page);
    }

    private void cmdMoreInfo(BaseCommandSender sender, String cmd) {
        mm.commandInfo(sender, cmd);
    }

    private void cmdExtract(BaseCommandSender sender, String[] args) {
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
            if (args.length >= 2 && args[1].equalsIgnoreCase("-override")) {
                if (!sender.hasPermission("zipextractor.admin.override.extract")) {
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

            ZExtractor.asyncExtract(sender, srcOpt.get(), destOpt.get(), cm.getLoggingProperty(), override);
        }
    }

    private void cmdCompress(BaseCommandSender sender, String[] args) {
        if (!sender.hasPermission("zipextractor.admin.compress")) {
            mm.noPermission(sender);
            return;
        }

        boolean override = !cm.warnOnConflitcts();
        if (args.length >= 2 && args[1].equalsIgnoreCase("-override")) {
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

    private void cmdSrc(BaseCommandSender sender, String[] args) {
        if (!sender.hasPermission("zipextractor.admin.src")) {
            mm.noPermission(sender);
            return;
        }
        if (args.length > 1 && args[1].equalsIgnoreCase("-absolute")) {
            Optional<File> srcOpt = cm.getSourceFile();
            if (!srcOpt.isPresent()) {
                mm.invalidPath(sender, cm.getSourceRaw(), "source");
                return;
            }
            mm.sendSuccess(sender, srcOpt.get().getAbsolutePath());
        } else {
            mm.sendSuccess(sender, cm.getSourceRaw());
        }
    }

    private void cmdDest(BaseCommandSender sender, String[] args) {
        if (!sender.hasPermission("zipextractor.admin.dest")) {
            mm.noPermission(sender);
            return;
        }
        if (args.length > 1 && args[1].equalsIgnoreCase("-absolute")) {
            Optional<File> destOpt = cm.getDestFile();
            if (!destOpt.isPresent()) {
                mm.invalidPath(sender, cm.getDestRaw(), "destination");
                return;
            }
            mm.sendSuccess(sender, destOpt.get().getAbsolutePath());
        } else {
            mm.sendSuccess(sender, cm.getDestRaw());
        }
    }

    private void cmdSetSrc(BaseCommandSender sender, String[] args) {
        if (!sender.hasPermission("zipextractor.admin.setsrc")) {
            mm.noPermission(sender);
            return;
        }
        if (args.length < 2) {
            mm.specifyAPath(sender);
            return;
        }
        String path = PathUtils.formatPath(formatInput(args), true);
        if (!PathUtils.validateFilePath(path)) {
            mm.invalidPath(sender, path);
            return;
        }
        if (cm.setSourcePath(path))
            mm.setPathSuccess(sender, "source");
        else
            mm.setPathFailed(sender, "source");
        ConfigManager.reload();
    }

    private void cmdSetDest(BaseCommandSender sender, String[] args) {
        if (!sender.hasPermission("zipextractor.admin.setdest")) {
            mm.noPermission(sender);
            return;
        }
        if (args.length < 2) {
            mm.specifyAPath(sender);
            return;
        }
        String path = PathUtils.formatPath(formatInput(args), true);
        if (!PathUtils.validateFilePath(path)) {
            mm.invalidPath(sender, path);
            return;
        }
        if (cm.setDestPath(path))
            mm.setPathSuccess(sender, "destination");
        else
            mm.setPathFailed(sender, "destination");
        ConfigManager.reload();
    }

    private void cmdPluginDir(BaseCommandSender sender) {
        if (!sender.hasPermission("zipextractor.admin.plugindir")) {
            mm.noPermission(sender);
            return;
        }
        mm.sendMessage(sender, "Plugin Directory - " + plugin.getConfigDir().getAbsolutePath());
    }

    private void cmdReload(BaseCommandSender sender) {
        if (!sender.hasPermission("zipextractor.admin.reload")) {
            mm.noPermission(sender);
            return;
        }
        try {
            if (ConfigManager.reload()) {
                ZServicer.getInstance().setMaximumPoolSize(ConfigManager.getInstance().getMaxPoolSize());
                mm.reloadSuccess(sender);
            } else
                mm.reloadFailed(sender);
        } catch (Throwable e) {
            mm.reloadFailed(sender);
            mm.severe("Error while reloading the plugin", e);
            e.printStackTrace();
        }
    }

    private void cmdTerminate(BaseCommandSender sender, boolean force) {
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

    private void cmdStatus(BaseCommandSender sender) {
        if (!sender.hasPermission("zipextractor.harmless.status")) {
            mm.noPermission(sender);
            return;
        }
        mm.cmdStatus(sender);
    }

    private void cmdVersion(BaseCommandSender sender) {
        mm.cmdVersion(sender, false);
    }

    private String formatInput(String[] args) {

        String ret = args[1];
        if (args[1].indexOf("\"") == 0 || args[1].indexOf("'") == 0) {
            String delimeter = args[1].startsWith("\"") ? "\"" : "'";
            for (int i = 2; i < args.length; ++i) {
                if (args[i].endsWith(delimeter)) {
                    ret += " " + args[i];
                    break;
                }
                ret += " " + args[i];
            }
        }

        ret = ret.replace("\"", "").replace("'", "");

        return ret;
    }

    @Override
    public List<String> getSuggestions(CommandSource source, String arguments, Location<World> targetPosition) {
        
        String[] argsDirty = arguments.replaceAll("\\s{2,}", " ").split(" ");
        String[] args = arguments.endsWith(" ") ? new String[argsDirty.length + 1] : argsDirty;
        if(args != argsDirty) {
            System.arraycopy(argsDirty, 0, args, 0, argsDirty.length);
            args[args.length-1] = new String();
        }
        
        List<String> ret = new ArrayList<String>();

        if (args.length == 1) {
            ret.addAll(subCommands(source, args));
        }

        if (args.length == 2) {
            boolean a = source.hasPermission("zipextractor.admin.src") && "src".startsWith(args[0].toLowerCase());
            boolean b = source.hasPermission("zipextractor.admin.dest") && "dest".startsWith(args[0].toLowerCase());
            boolean c = source.hasPermission("zipextractor.admin.extract")
                    && "extract".startsWith(args[0].toLowerCase());
            boolean d = source.hasPermission("zipextractor.admin.compress")
                    && "compress".startsWith(args[0].toLowerCase());
            if (a | b)
                if ("-absolute".startsWith(args[1].toLowerCase()))
                    ret.add("-absolute");
            if (source.hasPermission("zipextractor.admin.use") && "help".startsWith(args[0].toLowerCase())) {
                String[] newArgs = new String[args.length - 1];
                System.arraycopy(args, 1, newArgs, 0, args.length - 1);
                ret.addAll(subCommands(source, newArgs));
            }

            if (c && ZExtractor.getWarnData(source.getName()).isPresent() && "view".startsWith(args[1].toLowerCase())) {
                ret.add("view");
            }
            if (((c && source.hasPermission("zipextractor.admin.override.extract"))
                    || (d && source.hasPermission("zipextractor.admin.override.compress")))
                    && "-override".startsWith(args[1].toLowerCase())) {
                ret.add("-override");
            }
        }

        return ret;
    }

    private List<String> subCommands(CommandSource source, String[] args) {
        List<String> ret = new ArrayList<String>();

        if (args.length == 1) {
            if (source.hasPermission("zipextractor.admin.use") && "help".startsWith(args[0].toLowerCase()))
                ret.add("help");
            if (source.hasPermission("zipextractor.admin.extract") && "extract".startsWith(args[0].toLowerCase()))
                ret.add("extract");
            if (source.hasPermission("zipextractor.admin.compress") && "compress".startsWith(args[0].toLowerCase()))
                ret.add("compress");
            if (source.hasPermission("zipextractor.admin.src") && "src".startsWith(args[0].toLowerCase()))
                ret.add("src");
            if (source.hasPermission("zipextractor.admin.dest") && "dest".startsWith(args[0].toLowerCase()))
                ret.add("dest");
            if (source.hasPermission("zipextractor.admin.setsrc") && "setsrc".startsWith(args[0].toLowerCase()))
                ret.add("setsrc");
            if (source.hasPermission("zipextractor.admin.setdest") && "setdest".startsWith(args[0].toLowerCase()))
                ret.add("setdest");
            if (source.hasPermission("zipextractor.harmless.status") && "status".startsWith(args[0].toLowerCase()))
                ret.add("status");
            if (source.hasPermission("zipextractor.admin.plugindir") && "plugindir".startsWith(args[0].toLowerCase()))
                ret.add("plugindir");
            if (source.hasPermission("zipextractor.admin.terminate") && "terminate".startsWith(args[0].toLowerCase()))
                ret.add("terminate");
            if (source.hasPermission("zipextractor.admin.forceterminate")
                    && "forceterminate".startsWith(args[0].toLowerCase()))
                ret.add("forceterminate");
            if (source.hasPermission("zipextractor.admin.reload") && "reload".startsWith(args[0].toLowerCase()))
                ret.add("reload");
            if ("version".startsWith(args[0].toLowerCase()))
                ret.add("version");
        }

        return ret;
    }

    @Override
    public boolean testPermission(CommandSource source) {
        return true;
    }

    @Override
    public Optional<Text> getShortDescription(CommandSource source) {
        return Optional.of(Text.of("ZipExtractor main command."));
    }

    @Override
    public Optional<Text> getHelp(CommandSource source) {
        return Optional.of(Text.of("Run /ZipExtractor to view usage."));
    }

    @Override
    public Text getUsage(CommandSource source) {
        return Text.of("/ZipExtractor <args>");
    }

}
