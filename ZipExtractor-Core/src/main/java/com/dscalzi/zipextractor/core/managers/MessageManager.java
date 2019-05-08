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

package com.dscalzi.zipextractor.core.managers;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.dscalzi.zipextractor.core.ZCompressor;
import com.dscalzi.zipextractor.core.ZExtractor;
import com.dscalzi.zipextractor.core.ZServicer;
import com.dscalzi.zipextractor.core.ZTask;
import com.dscalzi.zipextractor.core.util.ICommandSender;
import com.dscalzi.zipextractor.core.util.IPlugin;
import com.dscalzi.zipextractor.core.util.PageList;

public class MessageManager {

    private static final char BULLET = (char) 8226;

    private static boolean initialized;
    private static MessageManager instance;

    private IPlugin plugin;
    private final String prefix;
    private final String cPrimary;
    private final String cTrim;
    private final String cSuccess;
    private final String cError;

    private MessageManager(IPlugin plugin) {
        this.plugin = plugin;
        this.cPrimary = "&7";
        this.cTrim = "&3";
        this.cSuccess = "&a";
        this.cError = "&c";
        this.prefix = cPrimary + "| " + cTrim + "ZipExtractor" + cPrimary + " |" + "&r";

        this.plugin.info(plugin.getName() + " is loading.");
    }

    public static void initialize(IPlugin plugin) {
        if (!initialized) {
            instance = new MessageManager(plugin);
            initialized = true;
        }
    }

    public static MessageManager inst() {
        return MessageManager.instance;
    }

    /* Message Distribution */

    public void sendMessage(ICommandSender sender, String message) {
        sender.sendMessage(prefix + " " + message);
    }

    public void sendSuccess(ICommandSender sender, String message) {
        sender.sendMessage(prefix + cSuccess + " " + message);
    }

    public void sendError(ICommandSender sender, String message) {
        sender.sendMessage(prefix + cError + " " + message);
    }

    public void sendGlobal(String message, String permission) {
        for (ICommandSender p : plugin.getOnlinePlayers()) {
            if (p.hasPermission(permission)) {
                sendMessage(p, message);
            }
        }
    }
    
    /* Logging */
    
    public void info(String msg) {
        plugin.info(msg);
    }
    
    public void warn(String msg) {
        plugin.warn(msg);
    }
    
    public void severe(String msg) {
        plugin.severe(msg);
    }
    
    public void severe(String msg, Throwable t) {
        plugin.severe(msg, t);
    }

    /* Accessors */

    public String getPrefix() {
        return this.prefix;
    }

    /* Messages */

    public void noPermission(ICommandSender sender) {
        sendError(sender, "You do not have permission to execute this command.");
    }

    public void noPermissionFull(ICommandSender sender) {
        sendError(sender, "You do not have permission to use this plugin.");
    }

    public void noInfoPermission(ICommandSender sender) {
        sendError(sender, "You do not have permission to view details about this command.");
    }

    public void reloadSuccess(ICommandSender sender) {
        sendSuccess(sender, "Configuration successfully reloaded.");
    }

    public void reloadFailed(ICommandSender sender) {
        sendError(sender, "Failed to reload the configuration file, see the console for details.");
    }

    public void setPathSuccess(ICommandSender sender, String action) {
        sendSuccess(sender, "Successfully updated the " + action + " file path.");
    }

    public void setPathFailed(ICommandSender sender, String action) {
        sendError(sender, "Failed to update the " + action + " file path, see the console for details.");
    }

    public void warnOfConflicts(ICommandSender sender, int amt) {
        sendError(sender,
                "Warning, this extraction will override " + "&o" + Integer.toString(amt) + cError + " file"
                        + (amt == 1 ? "" : "s") + ". To view " + (amt == 1 ? "this" : "these") + " file"
                        + (amt == 1 ? "" : "s") + " run the command " + "&o" + "/ze extract view [page]");
        sendError(sender, "To proceed with the extraction: " + "&o" + "/ze extract --override");
    }

    public void destExists(ICommandSender sender) {
        sendError(sender, "Warning, the destination of this compression already exists.");
        sendError(sender, "To proceed with the compression: " + "&o" + "/ze compress --override");
    }
    
    public void destExistsPiped(ICommandSender sender, File path) {
        sendError(sender, "Warning, an intermediate destination of this piped compression already exists.");
        sendError(sender, "&o" + path.toPath().toAbsolutePath().normalize().toString());
        sendError(sender, "To proceed with the compression: " + "&o" + "/ze compress --override");
    }
    
    public void pipedConflictRisk(ICommandSender sender) {
        sendError(sender, "Piped extraction cannot proceed. The destination directory is not empty and one or more of the intermediate file types cannot be scanned for conflicts.");
        sendError(sender, "This operation MUST be done with an empty destination directory.");
    }

    public void noWarnData(ICommandSender sender) {
        sendError(sender, "You have no data to view!");
    }

    public void fileNotFound(ICommandSender sender, String path) {
        if (!sender.isConsole()) {
            sendError(sender, "An error occurred during extraction. Could not locate the source file: "
                    + "&o" + path);
        }
        plugin.severe("An error occurred during extraction. Could not locate the source file: " + path);
    }

    public void destNotDirectory(ICommandSender sender, String filePath) {
        sendError(sender, "The destination path must be a directory:");
        sendError(sender, "&o" + filePath);
    }

    public void sourceNotFound(ICommandSender sender, String filePath) {
        sendError(sender, "Source file not found:");
        sendError(sender, "&o" + filePath);
    }

    public void sourceNoExt(ICommandSender sender, String filePath) {
        sendError(sender, "The source file must have an extension:");
        sendError(sender, "&o" + filePath);
    }

    public void fileAccessDenied(ICommandSender sender, ZTask t, String path) {
        if (!sender.isConsole()) {
            sendError(sender, "Error during " + t.getProcessName() + ". Access is denied to " + path);
        }
        plugin.severe("Error during " + t.getProcessName() + ". Access is denied to " + path);
    }

    public void invalidExtractionExtension(ICommandSender sender) {
        sendError(sender, "Currently extractions are only supported for "
                + listToString(ZExtractor.supportedExtensions()) + " files.");
    }

    public void invalidCompressionExtension(ICommandSender sender) {
        sendError(sender, "Currently you may only compress to the " + listToString(ZCompressor.supportedExtensions())
                + " format" + (ZCompressor.supportedExtensions().size() > 1 ? "s" : "") + ".");
    }

    public void invalidSourceForDest(ICommandSender sender, List<String> sources, List<String> dests) {
        sendError(sender,
                "Only " + listToString(sources) + " files can be compressed to " + listToString(dests) + " files.");
    }

    public void invalidPath(ICommandSender sender, String path, String type) {
        if (path == null || path.isEmpty()) {
            sendError(sender, "A " + type + " path must be specified.");
        } else {
            sendError(sender, "Invalid " + type + " path:");
            sendError(sender, "&o" + path);
        }
    }

    public void invalidPath(ICommandSender sender, String path) {
        if (path == null || path.isEmpty()) {
            sendError(sender, "A path must be specified.");
        } else {
            sendError(sender, "Invalid path:");
            sendError(sender, "&o" + path);
        }
    }

    public void invalidPathIsSet(ICommandSender sender, String path) {
        if (path == null || path.isEmpty()) {
            sendError(sender, "No path is set.");
        } else {
            sendError(sender, "An invalid path is currently set:");
            sendError(sender, "&o" + path);
        }
    }

    public void scanningForConflics(ICommandSender sender) {
        sendSuccess(sender, "Scanning for file conflicts..");
    }

    public void specifyAPath(ICommandSender sender) {
        sendError(sender, "Please specify a path.");
    }

    public void addToQueue(ICommandSender sender, int position) {
        String ordinal;
        if (position == 1 || position == 0)
            ordinal = "next";
        else
            ordinal = ordinal(position);
        sendSuccess(sender, "Your task has been added to the queue. It is currently " + ordinal + ".");
    }

    public void queueFull(ICommandSender sender, int maxQueueSize) {
        sendError(sender, "Unable to add your task to the queue, the limit of "
                + maxQueueSize + " has been reached.");
    }

    public void executorTerminated(ICommandSender sender, ZTask task) {
        sendError(sender, "The execution servicer has been shutdown and has therefore rejected your "
                + task.getProcessName() + " request.");
    }

    public void alreadyTerminated(ICommandSender sender) {
        sendError(sender, "The execution servicer has already been shutdown. This cannot be repeated or undone.");
    }

    public void alreadyTerminating(ICommandSender sender) {
        sendError(sender, "The execution servicer is currently shutting down, no further requests can be made.");
    }

    public void terminating(ICommandSender sender) {
        sendSuccess(sender,
                "Execution servicer is being shutdown. All queued tasks will be completed, although no further tasks will be accepted.");
    }

    public void terminatingForcibly(ICommandSender sender) {
        sendSuccess(sender,
                "Forcibly shutting down the execution servicer. All running and queued tasks will be interrupted and terminated.");
    }

    public void taskInterruption(ICommandSender sender, ZTask task) {
        if (!sender.isConsole())
            sendError(sender, "Channel closed during " + task.getProcessName()
                    + ", unable to continue. This is most likely due to a forced termination of the execution servicer.");
        plugin.warn("Channel closed during " + task.getProcessName()
                + ", unable to continue. This is most likely due to a forced termination of the execution servicer.");
    }

    public void startingProcess(ICommandSender sender, ZTask task, String fileName) {
        if (!sender.isConsole()) {
            sendSuccess(sender,
                    "Starting " + task.getProcessName() + " of '" + fileName + "'..");
        }
        plugin.info("Starting asynchronous " + task.getProcessName() + " of the file '" + fileName + "'..");
    }

    public void extractionComplete(ICommandSender sender, File dest) {
        if (!sender.isConsole()) {
            sendSuccess(sender, "Extraction complete (See console for details).");
        }
        plugin.info("---------------------------------------------------");
        plugin.info("Extraction complete.");
        plugin.info("The archive's contents have been extracted to\n" + dest.toPath().toAbsolutePath().normalize().toString());
        plugin.info("---------------------------------------------------");
    }

    public void compressionComplete(ICommandSender sender, File dest) {
        if (!sender.isConsole()) {
            sendSuccess(sender, "Compression complete (See console for details).");
        }
        plugin.info("---------------------------------------------------");
        plugin.info("Compression complete.");
        plugin.info("The folder's contents have been compressed to\n" + dest.toPath().toAbsolutePath().normalize().toString());
        plugin.info("---------------------------------------------------");
    }

    public void denyCommandBlock(ICommandSender sender) {
        sendError(sender, "Command blocks are blocked from accessing this command for security purposes.");
    }

    public void commandFormat(ICommandSender sender, String cmd) {
        if (cmd.equalsIgnoreCase("setsrc") || cmd.equalsIgnoreCase("setdest")) {
            sendError(sender, "Proper usage is /" + cmd.toLowerCase() + " <File Path>");
        }
    }
    
    public void untilMissingType(ICommandSender sender) {
        sendError(sender, "You must provide a type (--until <type>).");
    }

    public void invalidPage(ICommandSender sender) {
        sendError(sender, "Page does not exist.");
    }

    public void formatWarnList(ICommandSender sender, int page, PageList<String> files) {
        final String listPrefix = cError + " " + BULLET + " ";
        final String header = prefix + cError + " The following files would be overriden:";

        List<String> p = new ArrayList<String>(files.getPage(page));
        p.replaceAll(s -> listPrefix + s);

        String footer = cPrimary + "Page " + "&8" + (page + 1) + cPrimary + " of " + "&8"
                + files.size();

        sender.sendMessage(header);
        for (String s : p)
            sender.sendMessage(s);
        sender.sendMessage(footer);
    }
    
    public void commandList(ICommandSender sender, int page) {
        final String listPrefix = cPrimary + " " + BULLET + " ";

        PageList<String> cmds = new PageList<String>(7);
        String header = prefix + cPrimary + " Command List - <Required> [Optional]";
        if (sender.hasPermission("zipextractor.admin.use")) {
            cmds.add(listPrefix + "/ZipExtractor help [cmd] " + cTrim + "- View command list or info.");
        }
        if (sender.hasPermission("zipextractor.admin.extract"))
            cmds.add(listPrefix + "/ZipExtractor extract " + cTrim + "- Extract the specified file.");
        if (sender.hasPermission("zipextractor.admin.compress"))
            cmds.add(listPrefix + "/ZipExtractor compress " + cTrim + "- Compress the specified file.");
        if (sender.hasPermission("zipextractor.admin.src"))
            cmds.add(listPrefix + "/ZipExtractor src [--absolute] " + cTrim + "- View the source filepath.");
        if (sender.hasPermission("zipextractor.admin.dest"))
            cmds.add(listPrefix + "/ZipExtractor dest [--absolute] " + cTrim + "- View the destination filepath.");
        if (sender.hasPermission("zipextractor.admin.setsrc"))
            cmds.add(listPrefix + "/ZipExtractor setsrc <path> " + cTrim + "- Set the source's filepath.");
        if (sender.hasPermission("zipextractor.admin.setdest"))
            cmds.add(listPrefix + "/ZipExtractor setdest <path> " + cTrim + "- Set the destination's filepath.");
        if (sender.hasPermission("zipextractor.harmless.status"))
            cmds.add(listPrefix + "/ZipExtractor status " + cTrim + "- View the executor's status.");
        if (sender.hasPermission("zipextractor.admin.plugindir"))
            cmds.add(listPrefix + "/ZipExtractor plugindir " + cTrim + "- Get the plugin's full filepath.");
        if (sender.hasPermission("zipextractor.admin.terminate"))
            cmds.add(listPrefix + "/ZipExtractor terminate " + cTrim
                    + "- Shutdown the plugin's executor and allow all outstanding tasks to complete.");
        if (sender.hasPermission("zipextractor.admin.forceterminate"))
            cmds.add(listPrefix + "/ZipExtractor forceterminate " + cTrim
                    + "- Immediately shutdown the plugin's executor and terminate all outstanding tasks.");
        if (sender.hasPermission("zipextractor.admin.reload"))
            cmds.add(listPrefix + "/ZipExtractor reload " + cTrim + "- Reload the config.yml.");
        cmds.add(listPrefix + "/ZipExtractor version " + cTrim + "- View plugin version info.");

        String footer = cPrimary + "Page " + "&8" + (page + 1) + cPrimary + " of " + "&8"
                + cmds.size();

        if (page >= cmds.size() || page < 0) {
            invalidPage(sender);
            return;
        }

        sender.sendMessage(header);
        for (String s : cmds.getPage(page))
            sender.sendMessage(s);
        sender.sendMessage(footer);
    }

    public void commandInfo(ICommandSender sender, String cmd) {
        if (cmd.equalsIgnoreCase("help")) {
            if (!sender.hasPermission("zipextractor.admin.use")) {
                noInfoPermission(sender);
                return;
            }
            sendMessage(sender, cPrimary + "This command will provide information on the plugin's functions.");
            return;
        }
        if (cmd.equalsIgnoreCase("extract")) {
            if (!sender.hasPermission("zipextractor.admin.extract")) {
                noInfoPermission(sender);
                return;
            }
            sendMessage(sender, cPrimary
                    + "This command will extract the archive specified in the config.yml. That value can be edited directly in the file or via the command /ZipExtractor setsrc <File Path>. The zip contents will be extracted to the destination folder specified in the config.yml. That value can be edited directly in the file or via the command /ZipExtractor setdest <File Path>.");
            return;
        }
        if (cmd.equalsIgnoreCase("compress")) {
            if (!sender.hasPermission("zipextractor.admin.compress")) {
                noInfoPermission(sender);
                return;
            }
            sendMessage(sender, cPrimary
                    + "This command will compress the folder specified in the config.yml. That value can be edited directly in the file or via the command /ZipExtractor setsrc <File Path>. The contents will be compressed into a new archive at the location specified specified in the config.yml. That value can be edited directly in the file or via the command /ZipExtractor setdest <File Path>.");
            return;
        }
        if (cmd.equalsIgnoreCase("src")) {
            if (!sender.hasPermission("zipextractor.admin.src")) {
                noInfoPermission(sender);
                return;
            }
            sendMessage(sender, cPrimary
                    + "View the currently set source file path. To view the absolute path, run the command as /ZipExtractor src --absolute");
            return;
        }
        if (cmd.equalsIgnoreCase("dest")) {
            if (!sender.hasPermission("zipextractor.admin.dest")) {
                noInfoPermission(sender);
                return;
            }
            sendMessage(sender, cPrimary
                    + "View the currently set destitation file path. To view the absolute path, run the command as /ZipExtractor dest --absolute");
            return;
        }
        if (cmd.equalsIgnoreCase("setsrc")) {
            if (!sender.hasPermission("zipextractor.admin.setsrc")) {
                noInfoPermission(sender);
                return;
            }
            sendMessage(sender, cPrimary
                    + "This command will directly update the 'source_directory' field in the configuration file. \nPlease use / for a file separator.\nSyntax is /ZipExtractor setsrc <File Path>");
            return;
        }
        if (cmd.equalsIgnoreCase("setdest")) {
            if (!sender.hasPermission("zipextractor.admin.setdest")) {
                noInfoPermission(sender);
                return;
            }
            sendMessage(sender, cPrimary
                    + "This command will directly update the 'destination_directory' field in the configuration file. \nPlease use / for a file separator.\nSyntax is /ZipExtractor setdest <File Path>");
            return;
        }
        if (cmd.equalsIgnoreCase("status")) {
            if (!sender.hasPermission("zipextractor.harmless.status")) {
                noInfoPermission(sender);
                return;
            }
            sendMessage(sender, cPrimary
                    + "This command will display the status of the executor service. If the service has not been terminated, the number of active and queued processes will be displayed.");
            return;
        }
        if (cmd.equalsIgnoreCase("plugindir")) {
            if (!sender.hasPermission("zipextractor.admin.plugindir")) {
                noInfoPermission(sender);
                return;
            }
            sendMessage(sender, cPrimary
                    + "This command will tell you the full path of this plugin's data folder on your server. It can be easily accessed using the shortcut *plugindir*.");
            return;
        }
        if (cmd.equalsIgnoreCase("terminate")) {
            if (!sender.hasPermission("zipextractor.admin.terminate")) {
                noInfoPermission(sender);
                return;
            }
            sendMessage(sender, cPrimary
                    + "This command will initiate the shutdown proccess for the plugin's execution servicer. Any queued tasks at the time of shutdown will be allowed to finish. It is recommended not to shutdown or restart the server until this has finished.");
            return;
        }
        if (cmd.equalsIgnoreCase("forceterminate")) {
            if (!sender.hasPermission("zipextractor.admin.forceterminate")) {
                noInfoPermission(sender);
                return;
            }
            sendMessage(sender, cPrimary
                    + "This command will forcibly shutdown the plugin's execution servicer and send a request to interrupt and terminate any queued and proccessing tasks. This type of termination is not recommended.");
            return;
        }
        if (cmd.equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("zipextractor.admin.reload")) {
                noInfoPermission(sender);
                return;
            }
            sendMessage(sender, cPrimary
                    + "This command will reload the configuration file. It's necessary to do this after you edit the config file directly. If you use the built-in commands it's automatically reloaded after each edit.");
            return;
        }
        if (cmd.equalsIgnoreCase("version")) {
            sendMessage(sender, cPrimary
                    + "Displays the plugin's version information and provides links to the source code and metrics page.");
            return;
        }

    }

    public void cmdStatus(ICommandSender sender) {
        ZServicer zs = ZServicer.getInstance();
        if (zs == null) {
            sendMessage(sender, "Executor Status | " + "&c" + "UNINITIALIZED");
        } else if (zs.isTerminated()) {
            sendMessage(sender, "Executor Status | " + "&c" + "TERMINATED");
        } else if (zs.isTerminating()) {
            sendMessage(sender, "Executor Status | " + "&c" + "TERMINATING");
        } else {
            String status = zs.isQueueFull() ? "&c" + "FULL" : "&a" + "READY";
            sendMessage(sender, "Executor Status | " + status + "&r" + " | Active : " + zs.getActive()
                    + " | Queued : " + zs.getQueued());
        }
    }

    public void cmdVersion(ICommandSender sender, boolean bukkit) {
        sendMessage(sender,
                "Zip Extractor version " + plugin.getVersion() + "\n" + cPrimary + "| " + cTrim
                        + "Source" + cPrimary + " | " + "&r" + "https://github.com/dscalzi/ZipExtractor"
                        + "\n" + cPrimary + "| " + cTrim + "Metrics" + cPrimary + " | " + "&r"
                        + "https://bstats.org/plugin/" + (bukkit ? "bukkit" : "sponge") + "/ZipExtractor");
    }

    public String ordinal(int i) {
        int mod100 = i % 100;
        int mod10 = i % 10;
        if (mod10 == 1 && mod100 != 11)
            return i + "st";
        else if (mod10 == 2 && mod100 != 12)
            return i + "nd";
        else if (mod10 == 3 && mod100 != 13)
            return i + "rd";
        else
            return i + "th";
    }

    public <T> String listToString(List<T> c) {
        if (c == null)
            return "";
        if (c.size() == 1) {
            return c.get(0).toString();
        } else if (c.size() == 2) {
            return c.get(0).toString() + " and " + c.get(1).toString();
        } else {
            String vals = "";
            int tracker = 0;
            for (final T t : c) {
                if (tracker == c.size() - 1) {
                    vals += "and " + t.toString();
                    break;
                }
                vals += t.toString() + ", ";
                ++tracker;
            }
            return vals;
        }
    }
}
