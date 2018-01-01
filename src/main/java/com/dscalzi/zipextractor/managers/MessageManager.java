/*
 * ZipExtractor
 * Copyright (C) 2016-2018 Daniel D. Scalzi
 * See License.txt for license information.
 */
package com.dscalzi.zipextractor.managers;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import com.dscalzi.zipextractor.ZipExtractor;
import com.dscalzi.zipextractor.util.PageList;
import com.dscalzi.zipextractor.util.ZCompressor;
import com.dscalzi.zipextractor.util.ZExtractor;
import com.dscalzi.zipextractor.util.ZServicer;
import com.dscalzi.zipextractor.util.ZTask;

public class MessageManager {

	private static final char BULLET = (char)8226;
	
	private static boolean initialized;
	private static MessageManager instance;
	
	private ZipExtractor plugin;
	private final Logger logger;
	private final String prefix;
	private final ChatColor cPrimary;
	private final ChatColor cTrim;
	private final ChatColor cSuccess;
	private final ChatColor cError;
	
	private MessageManager(ZipExtractor plugin){
		this.plugin = plugin;
		this.logger = plugin.getLogger();
		this.cPrimary = ChatColor.GRAY;
		this.cTrim = ChatColor.DARK_AQUA;
		this.cSuccess = ChatColor.GREEN;
		this.cError = ChatColor.RED;
		this.prefix = cPrimary + "| " + cTrim + "ZipExtractor" + cPrimary + " |" + ChatColor.RESET;
		
		this.plugin.getLogger().info(plugin.getDescription().getName() + " is loading.");
	}
	
	public static void initialize(ZipExtractor plugin){
		if(!initialized){
			instance = new MessageManager(plugin);
			initialized = true;
		}
	}
	
	public static MessageManager getInstance(){
		return MessageManager.instance;
	}
	
	/* Message Distribution */
	
	public void sendMessage(CommandSender sender, String message){
		sender.sendMessage(prefix + " " + message);
	}
	
	public void sendSuccess(CommandSender sender, String message){
		sender.sendMessage(prefix + cSuccess + " " + message);
	}
	
	public void sendError(CommandSender sender, String message){
		sender.sendMessage(prefix + cError + " " + message);
	}
	
	public void sendGlobal(String message, String permission){
		for(Player p : plugin.getServer().getOnlinePlayers()){
			if(p.hasPermission(permission)){
				sendMessage(p, message);
			}
		}
	}
	
	/* Accessors */
	
	public String getPrefix(){
		return this.prefix;
	}
	
	public Logger getLogger(){
		return this.logger;
	}
	
	/* Messages */
	
	public void noPermission(CommandSender sender){
		sendError(sender, "You do not have permission to execute this command.");
	}
	
	public void noPermissionFull(CommandSender sender){
		sendError(sender, "You do not have permission to use this plugin.");
	}
	
	public void noInfoPermission(CommandSender sender){
		sendError(sender, "You do not have permission to view details about this command.");
	}
	
	public void reloadSuccess(CommandSender sender){
		sendSuccess(sender, "Configuration successfully reloaded.");
	}
	
	public void reloadFailed(CommandSender sender){
		sendError(sender, "Failed to reload the configuration file, see the console for details.");
	}
	
	public void setPathSuccess(CommandSender sender, String action){
		sendSuccess(sender, "Successfully updated the " + action + " file path.");
	}
	
	public void setPathFailed(CommandSender sender, String action){
		sendError(sender, "Failed to update the " + action + " file path, see the console for details.");
	}
	
	public void warnOfConflicts(CommandSender sender, int amt) {
		sendError(sender, "Warning, this extraction will override " + ChatColor.ITALIC + Integer.toString(amt) + cError + " file"
				+ (amt == 1 ? "" : "s") + ". To view " + (amt == 1 ? "this" : "these") + " file" + (amt == 1 ? "" : "s") + " run the command " + ChatColor.ITALIC + "/ze extract view [page]");
		sendError(sender, "To proceed with the extraction: " + ChatColor.ITALIC + "/ze extract -override");
	}
	
	public void destExists(CommandSender sender) {
		sendError(sender, "Warning, the destination of this compression already exists.");
		sendError(sender, "To proceed with the compression: " + ChatColor.ITALIC + "/ze compress -override");
	}
	
	public void noWarnData(CommandSender sender) {
		sendError(sender, "You have no data to view!");
	}
	
	public void fileNotFound(CommandSender sender, String path){
		if(!(sender instanceof ConsoleCommandSender)){
			sendError(sender, "An error occurred during extraction. Could not locate the source file: " + ChatColor.ITALIC + path);
		}
		getLogger().severe("An error occurred during extraction. Could not locate the source file: " + path);
	}
	
	public void destNotDirectory(CommandSender sender, String filePath){
		sendError(sender, "The destination path must be a directory:");
		sendError(sender, ChatColor.ITALIC + filePath);
	}
	
	public void sourceNotFound(CommandSender sender, String filePath) {
		sendError(sender, "Source file not found:");
		sendError(sender, ChatColor.ITALIC + filePath);
	}
	
	public void sourceNoExt(CommandSender sender, String filePath) {
		sendError(sender, "The source file must have an extension:");
		sendError(sender, ChatColor.ITALIC + filePath);
	}
	
	public void fileAccessDenied(CommandSender sender, ZTask t, String path){
		if(!(sender instanceof ConsoleCommandSender)){
			sendError(sender, "Error during " + t.getProcessName() + ". Access is denied to " + path);
		}
		getLogger().severe("Error during " + t.getProcessName() + ". Access is denied to " + path);
	}
	
	public void invalidExtractionExtension(CommandSender sender){
		sendError(sender, "Currently extractions are only supported for " + listToString(ZExtractor.supportedExtensions()) + " files.");
	}
	
	public void invalidCompressionExtension(CommandSender sender) {
		sendError(sender, "Currently you may only compress to the " + listToString(ZCompressor.supportedExtensions()) + " format" + (ZCompressor.supportedExtensions().size() > 1 ? "s" : "" ) + ".");
	}
	
	public void invalidSourceForDest(CommandSender sender, List<String> sources, List<String> dests) {
		sendError(sender, "Only " + listToString(sources) + " files can be compressed to " + listToString(dests) + " files.");
	}
	
	public void invalidPath(CommandSender sender, String path, String type) {
		if(path == null || path.isEmpty()) {
			sendError(sender, "A " + type + " path must be specified.");
		} else {
			sendError(sender, "Invalid " + type + " path:");
			sendError(sender, ChatColor.ITALIC + path);
		}
	}
	
	public void invalidPath(CommandSender sender, String path) {
		if(path == null || path.isEmpty()) {
			sendError(sender, "A path must be specified.");
		} else {
			sendError(sender, "Invalid path:");
			sendError(sender, ChatColor.ITALIC + path);
		}
	}
	
	public void invalidPathIsSet(CommandSender sender, String path) {
		if(path == null || path.isEmpty()) {
			sendError(sender, "No path is set.");
		} else {
			sendError(sender, "An invalid path is currently set:");
			sendError(sender, ChatColor.ITALIC + path);
		}
	}
	
	public void scanningForConflics(CommandSender sender) {
		sendSuccess(sender, "Scanning for file conflicts..");
	}
	
	public void specifyAPath(CommandSender sender){
		sendError(sender, "Please specify a path.");
	}
	
	public void addToQueue(CommandSender sender, int position){
		String ordinal;
		if(position == 1 || position == 0) ordinal = "next";
		else ordinal = ordinal(position);
		sendSuccess(sender, "Your task has been added to the queue. It is currently " + ordinal + ".");
	}
	
	public void queueFull(CommandSender sender){
		sendError(sender, "Unable to add your task to the queue, the limit of " + ConfigManager.getInstance().getMaxQueueSize() + " has been reached.");
	}
	
	public void executorTerminated(CommandSender sender, ZTask task){
		sendError(sender, "The execution servicer has been shutdown and has therefore rejected your " + task.getProcessName() + " request.");
	}
	
	public void alreadyTerminated(CommandSender sender){
		sendError(sender, "The execution servicer has already been shutdown. This cannot be repeated or undone.");
	}
	
	public void alreadyTerminating(CommandSender sender){
		sendError(sender, "The execution servicer is currently shutting down, no further requests can be made.");
	}
	
	public void terminating(CommandSender sender){
		sendSuccess(sender, "Execution servicer is being shutdown. All queued tasks will be completed, although no further tasks will be accepted.");
	}
	
	public void terminatingForcibly(CommandSender sender){
		sendSuccess(sender, "Forcibly shutting down the execution servicer. All running and queued tasks will be interrupted and terminated.");
	}
	
	public void taskInterruption(CommandSender sender, ZTask task){
		if(!(sender instanceof ConsoleCommandSender))
			sendError(sender, "Channel closed during " + task.getProcessName() + ", unable to continue. This is most likely due to a forced termination of the execution servicer.");
		logger.log(Level.WARNING, "Channel closed during " + task.getProcessName() + ", unable to continue. This is most likely due to a forced termination of the execution servicer.");
	}
	
	public void startingProcess(CommandSender sender, ZTask task, String fileName){
		if(!(sender instanceof ConsoleCommandSender)){
			sendSuccess(sender, "Starting " + task.getProcessName() + " of '" + fileName + "'.. See the console for more details.");
		}
		getLogger().info("Starting asynchronous " + task.getProcessName() + " of the file '" + fileName + "'..");
	}
	
	public void extractionComplete(CommandSender sender, String destPath){
		if(!(sender instanceof ConsoleCommandSender)){
			sendSuccess(sender, "Extraction complete.");
		}
		getLogger().info("---------------------------------------------------");
		getLogger().info("Extraction complete.");
		getLogger().info("The archive's contents have been extracted to\n" + destPath);
		getLogger().info("---------------------------------------------------");
	}
	
	public void compressionComplete(CommandSender sender, String destPath){
		if(!(sender instanceof ConsoleCommandSender)){
			sendSuccess(sender, "Compression complete.");
		}
		getLogger().info("---------------------------------------------------");
		getLogger().info("Compression complete.");
		getLogger().info("The folder's contents have been compressed to\n" + destPath);
		getLogger().info("---------------------------------------------------");
	}
	
	public void denyCommandBlock(CommandSender sender){
		sendError(sender, "Command blocks are blocked from accessing this command for security purposes.");
	}
	
	public void commandFormat(CommandSender sender, String cmd){
		if(cmd.equalsIgnoreCase("setsrc") || cmd.equalsIgnoreCase("setdest")){
			sendError(sender, "Proper usage is /" + cmd.toLowerCase() + " <File Path>");
		}
	}
	
	public void invalidPage(CommandSender sender) {
		sendError(sender, "Page does not exist.");
	}
	
	public void formatWarnList(CommandSender sender, int page, PageList<String> files) {
		final String listPrefix = cError + " " + BULLET + " ";
		final String header = prefix + cError + " The following files would be overriden:";
		
		List<String> p = new ArrayList<String>(files.getPage(page));
		p.replaceAll(s -> listPrefix + s);
		
		String footer = cPrimary + "Page " + ChatColor.DARK_GRAY + (page+1) + cPrimary + " of " + ChatColor.DARK_GRAY + files.size();
	
		sender.sendMessage(header);
		for(String s : p)
			sender.sendMessage(s);
		sender.sendMessage(footer);
	}
	
	public void commandList(CommandSender sender, int page){
		final String listPrefix = cPrimary + " " + BULLET + " ";
		
		PageList<String> cmds = new PageList<String>(7);
		String header = prefix + cPrimary + " Command List - <Required> [Optional]";
		if(sender.hasPermission("zipextractor.admin.use")){
			cmds.add(listPrefix + "/ZipExtractor help [cmd] " + cTrim + "- View command list or info.");
		}
		if(sender.hasPermission("zipextractor.admin.extract"))
			cmds.add(listPrefix + "/ZipExtractor extract " + cTrim + "- Extract the specified file.");
		if(sender.hasPermission("zipextractor.admin.compress"))
			cmds.add(listPrefix + "/ZipExtractor compress " + cTrim + "- Compress the specified file.");
		if(sender.hasPermission("zipextractor.admin.src"))
			cmds.add(listPrefix + "/ZipExtractor src [-absolute] " + cTrim + "- View the source filepath.");
		if(sender.hasPermission("zipextractor.admin.dest"))
			cmds.add(listPrefix + "/ZipExtractor dest [-absolute] " + cTrim + "- View the destination filepath.");
		if(sender.hasPermission("zipextractor.admin.setsrc"))
			cmds.add(listPrefix + "/ZipExtractor setsrc <path> " + cTrim + "- Set the source's filepath.");
		if(sender.hasPermission("zipextractor.admin.setdest"))
			cmds.add(listPrefix + "/ZipExtractor setdest <path> " + cTrim + "- Set the destination's filepath.");
		if(sender.hasPermission("zipextractor.harmless.status"))
			cmds.add(listPrefix + "/ZipExtractor status " + cTrim + "- View the executor's status.");
		if(sender.hasPermission("zipextractor.admin.plugindir"))
			cmds.add(listPrefix + "/ZipExtractor plugindir " + cTrim + "- Get the plugin's full filepath.");
		if(sender.hasPermission("zipextractor.admin.terminate"))
			cmds.add(listPrefix + "/ZipExtractor terminate " + cTrim + "- Shutdown the plugin's executor and allow all outstanding tasks to complete.");
		if(sender.hasPermission("zipextractor.admin.forceterminate"))
			cmds.add(listPrefix + "/ZipExtractor forceterminate " + cTrim + "- Immediately shutdown the plugin's executor and terminate all outstanding tasks.");
		if(sender.hasPermission("zipextractor.admin.reload"))
			cmds.add(listPrefix + "/ZipExtractor reload " + cTrim + "- Reload the config.yml.");
		cmds.add(listPrefix + "/ZipExtractor version " + cTrim + "- View plugin version info.");
		
		String footer = cPrimary + "Page " + ChatColor.DARK_GRAY + (page+1) + cPrimary + " of " + ChatColor.DARK_GRAY + cmds.size();
		
		if(page >= cmds.size() || page < 0){
			invalidPage(sender);
			return;
		}
		
		sender.sendMessage(header);
		for(String s : cmds.getPage(page))
			sender.sendMessage(s);
		sender.sendMessage(footer);
	}
	
	public void commandInfo(CommandSender sender, String cmd){
		if(cmd.equalsIgnoreCase("help")){
			if(!sender.hasPermission("zipextractor.admin.use")){
				noInfoPermission(sender);
				return;
			}
			sendMessage(sender, cPrimary + "This command will provide information on the plugin's functions.");
			return;
		}
		if(cmd.equalsIgnoreCase("extract")){
			if(!sender.hasPermission("zipextractor.admin.extract")){
				noInfoPermission(sender);
				return;
			}
			sendMessage(sender, cPrimary + "This command will extract the archive specified in the config.yml. That value can be edited directly in the file or via the command /ZipExtractor setsrc <File Path>. The zip contents will be extracted to the destination folder specified in the config.yml. That value can be edited directly in the file or via the command /ZipExtractor setdest <File Path>.");
			return;
		}
		if(cmd.equalsIgnoreCase("compress")){
			if(!sender.hasPermission("zipextractor.admin.compress")){
				noInfoPermission(sender);
				return;
			}
			sendMessage(sender, cPrimary + "This command will compress the folder specified in the config.yml. That value can be edited directly in the file or via the command /ZipExtractor setsrc <File Path>. The contents will be compressed into a new archive at the location specified specified in the config.yml. That value can be edited directly in the file or via the command /ZipExtractor setdest <File Path>.");
			return;
		}
		if(cmd.equalsIgnoreCase("src")){
			if(!sender.hasPermission("zipextractor.admin.src")){
				noInfoPermission(sender);
				return;
			}
			sendMessage(sender, cPrimary + "View the currently set source file path. To view the absolute path, run the command as /ZipExtractor src -absolute");
			return;
		}
		if(cmd.equalsIgnoreCase("dest")){
			if(!sender.hasPermission("zipextractor.admin.dest")){
				noInfoPermission(sender);
				return;
			}
			sendMessage(sender, cPrimary + "View the currently set destitation file path. To view the absolute path, run the command as /ZipExtractor dest -absolute");
			return;
		}
		if(cmd.equalsIgnoreCase("setsrc")){
			if(!sender.hasPermission("zipextractor.admin.setsrc")){
				noInfoPermission(sender);
				return;
			}
			sendMessage(sender, cPrimary + "This command will directly update the 'source_directory' field in the configuration file. \nPlease use / for a file separator.\nSyntax is /ZipExtractor setsrc <File Path>");
			return;
		}
		if(cmd.equalsIgnoreCase("setdest")){
			if(!sender.hasPermission("zipextractor.admin.setdest")){
				noInfoPermission(sender);
				return;
			}
			sendMessage(sender, cPrimary + "This command will directly update the 'destination_directory' field in the configuration file. \nPlease use / for a file separator.\nSyntax is /ZipExtractor setdest <File Path>");
			return;
		}
		if(cmd.equalsIgnoreCase("status")){
			if(!sender.hasPermission("zipextractor.harmless.status")){
				noInfoPermission(sender);
				return;
			}
			sendMessage(sender, cPrimary + "This command will display the status of the executor service. If the service has not been terminated, the number of active and queued processes will be displayed.");
			return;
		}
		if(cmd.equalsIgnoreCase("plugindir")){
			if(!sender.hasPermission("zipextractor.admin.plugindir")){
				noInfoPermission(sender);
				return;
			}
			sendMessage(sender, cPrimary + "This command will tell you the full path of this plugin's data folder on your server. It can be easily accessed using the shortcut *plugindir*.");
			return;
		}
		if(cmd.equalsIgnoreCase("terminate")){
			if(!sender.hasPermission("zipextractor.admin.terminate")){
				noInfoPermission(sender);
				return;
			}
			sendMessage(sender, cPrimary + "This command will initiate the shutdown proccess for the plugin's execution servicer. Any queued tasks at the time of shutdown will be allowed to finish. It is recommended not to shutdown or restart the server until this has finished.");
			return;
		}
		if(cmd.equalsIgnoreCase("forceterminate")){
			if(!sender.hasPermission("zipextractor.admin.forceterminate")){
				noInfoPermission(sender);
				return;
			}
			sendMessage(sender, cPrimary + "This command will forcibly shutdown the plugin's execution servicer and send a request to interrupt and terminate any queued and proccessing tasks. This type of termination is not recommended.");
			return;
		}
		if(cmd.equalsIgnoreCase("reload")){
			if(!sender.hasPermission("zipextractor.admin.reload")){
				noInfoPermission(sender);
				return;
			}
			sendMessage(sender, cPrimary + "This command will reload the configuration file. It's necessary to do this after you edit the config file directly. If you use the built-in commands it's automatically reloaded after each edit.");
			return;
		}
		if(cmd.equalsIgnoreCase("version")){
			sendMessage(sender, cPrimary + "Displays the plugin's version information and provides links to the source code and metrics page.");
			return;
		}
		
	}
	
	public void cmdStatus(CommandSender sender) {
		ZServicer zs = ZServicer.getInstance();
		if(zs == null) {
			sendMessage(sender, "Executor Status | " + ChatColor.RED + "UNINITIALIZED");
		} else if(zs.isTerminated()) {
			sendMessage(sender, "Executor Status | " + ChatColor.RED + "TERMINATED");
		} else if(zs.isTerminating()) {
			sendMessage(sender, "Executor Status | " + ChatColor.RED + "TERMINATING");
		} else {
			String status = zs.isQueueFull() ? ChatColor.RED + "FULL" : ChatColor.GREEN + "READY";
			sendMessage(sender, "Executor Status | " + status + ChatColor.RESET + " | Active : " + zs.getActive() + " | Queued : " + zs.getQueued());
		}
	}
	
	public void cmdVersion(CommandSender sender){
		sendMessage(sender, "Zip Extractor version " + plugin.getDescription().getVersion() +
				"\n" + cPrimary + "| " + cTrim + "Source" + cPrimary + " | " + ChatColor.RESET + "bitbucket.org/AventiumSoftworks/zip-extractor" +
				"\n" + cPrimary + "| " + cTrim + "Metrics" + cPrimary + " | " + ChatColor.RESET + "https://bstats.org/plugin/bukkit/ZipExtractor");
	}
	
	public String ordinal(int i) {
	    int mod100 = i % 100;
	    int mod10 = i % 10;
	    if(mod10 == 1 && mod100 != 11)
	        return i + "st";
	    else if(mod10 == 2 && mod100 != 12)
	        return i + "nd";
	    else if(mod10 == 3 && mod100 != 13)
	        return i + "rd";
	    else
	        return i + "th";
	}
	
	public <T> String listToString(List<T> c) {
		if(c == null) return "";
		if(c.size() == 1) {
			return c.get(0).toString();
		} else if(c.size() == 2) {
			return c.get(0).toString() + " and " + c.get(1).toString();
		} else {
			String vals = "";
			int tracker = 0;
			for(final T t : c) {
				if(tracker == c.size()-1) {
					vals += "and " +  t.toString();
					break;
				}
				vals += t.toString() + ", ";
				++tracker;
			}
			return vals;
		}
	}
}
