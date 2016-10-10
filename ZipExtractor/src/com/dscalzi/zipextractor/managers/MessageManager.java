package com.dscalzi.zipextractor.managers;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;

import com.dscalzi.zipextractor.ZipExtractor;
import com.dscalzi.zipextractor.util.ZTask;

public class MessageManager {

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
		this.cPrimary = ChatColor.DARK_AQUA;
		this.cTrim = ChatColor.WHITE;
		this.cSuccess = ChatColor.GREEN;
		this.cError = ChatColor.RED;
		this.prefix = cPrimary + "[" + cTrim + "ZipExtractor" + cPrimary + "]" + ChatColor.RESET;
		
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
	
	public void fileNotFound(CommandSender sender, String path){
		if(!(sender instanceof ConsoleCommandSender)){
			sendError(sender, "An error occurred during extraction. Could not locate the source file: " + ChatColor.ITALIC + path);
		}
		getLogger().severe("An error occurred during extraction. Could not locate the source file: " + path);
	}
	
	public void destNotDirectory(CommandSender sender){
		sendError(sender, "The destination you've selected is not a directory, aborting.");
	}
	
	public void invalidPath(CommandSender sender, String action){
		sendError(sender, "The " + action + " file path you've selected is not valid, aborting.");
	}
	
	public void fileAccessDenied(CommandSender sender, ZTask t, String path){
		if(!(sender instanceof ConsoleCommandSender)){
			sendError(sender, "Error during " + t.getProcessName() + ". Access is denied to " + path);
		}
		getLogger().severe("Error during " + t.getProcessName() + ". Access is denied to " + path);
	}
	
	public void invalidExtension(CommandSender sender, String extension){
		if(extension.length() >= 1)
			extension = Character.toUpperCase(extension.charAt(0)) + extension.substring(1).toLowerCase();
		sendError(sender, extension + " files are not currently supported.");
	}
	
	public void addToQueue(CommandSender sender, int position){
		String ordinal;
		if(position == 1) ordinal = "next";
		else ordinal = ordinal(position);
		sendSuccess(sender, "Your task has been added to the queue. It is currently " + ordinal + ".");
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
	
	public void commandList(CommandSender sender){
		final String listPrefix = cPrimary + " • ";
		
		List<String> cmds = new ArrayList<String>();
		cmds.add(prefix + cPrimary + " Command List - <Required> [Optional]");
		if(sender.hasPermission("zipextractor.admin.use")){
			cmds.add(listPrefix + "/ZipExtractor help [cmd]" + cTrim + "- View command list or info.");
		}
		if(sender.hasPermission("zipextractor.admin.extract"))
			cmds.add(listPrefix + "/ZipExtractor extract " + cTrim + "- Extract the specified file.");
		if(sender.hasPermission("zipextractor.admin.compress"))
			cmds.add(listPrefix + "/ZipExtractor compress " + cTrim + "- Compress the specified file.");
		if(sender.hasPermission("zipextractor.admin.setsrc"))
			cmds.add(listPrefix + "/ZipExtractor setsrc <path> " + cTrim + "- Set the source's filepath.");
		if(sender.hasPermission("zipextractor.admin.setdest"))
			cmds.add(listPrefix + "/ZipExtractor setdest <path> " + cTrim + "- Set the destination's filepath.");
		if(sender.hasPermission("zipextractor.admin.plugindir"))
			cmds.add(listPrefix + "/ZipExtractor plugindir " + cTrim + "- Get the plugin's full filepath.");
		if(sender.hasPermission("zipextractor.admin.reload"))
			cmds.add(listPrefix + "/ZipExtractor reload " + cTrim + "- Reload the config.yml.");
		cmds.add(listPrefix + "/ZipExtractor version " + cTrim + "- View plugin version info.");
		
		for(String s : cmds) sender.sendMessage(s);
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
			sendMessage(sender, cPrimary + "This command will extract the archive you specified in the config.yml. That value can be edited directly in the file or via the command /ZipExtractor setsrc <File Path>. The zip contents will be copied into the folder specified in the config.yml. That value can be edited directly in the file or via the command /ZipExtractor setdest <File Path>.");
			return;
		}
		if(cmd.equalsIgnoreCase("compress")){
			if(!sender.hasPermission("zipextractor.admin.compress")){
				noInfoPermission(sender);
				return;
			}
			sendMessage(sender, cPrimary + "This command will compress the folder you specified in the config.yml. That value can be edited directly in the file or via the command /ZipExtractor setsrc <File Path>. The folder contents will be compressed into the archive at the location specified specified in the config.yml. That value can be edited directly in the file or via the command /ZipExtractor setdest <File Path>.");
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
		if(cmd.equalsIgnoreCase("plugindir")){
			if(!sender.hasPermission("zipextractor.admin.plugindir")){
				noInfoPermission(sender);
				return;
			}
			sendMessage(sender, cPrimary + "This command will tell you the full path of your minecraft server's plugin directory.");
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
}
