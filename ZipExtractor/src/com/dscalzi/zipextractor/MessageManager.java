package com.dscalzi.zipextractor;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class MessageManager {

	private static boolean initialized;
	private static MessageManager instance;
	
	private ZipExtractor plugin;
	private final String prefix;
	private final ChatColor main;
	private final ChatColor trim;
	
	private MessageManager(ZipExtractor plugin){
		this.plugin = plugin;
		this.main = ChatColor.DARK_AQUA;
		this.trim = ChatColor.WHITE;
		this.prefix = main + "[" + trim + "ZipExtractor" + main + "]" + ChatColor.RESET;
		
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
		sender.sendMessage(prefix + " " + message + trim);
	}
	
	/* Accessors */
	
	public String getPrefix(){
		return this.prefix;
	}
	
	public Logger getLogger(){
		return this.plugin.getLogger();
	}
	
	/* Messages */
	
	public void noPermission(CommandSender sender){
		sendMessage(sender, ChatColor.RED + "You do not have permission to execute this command.");
	}
	
	public void noPermissionFull(CommandSender sender){
		sendMessage(sender, ChatColor.RED + "You do not have permission to use this plugin.");
	}
	
	public void noInfoPermission(CommandSender sender){
		sendMessage(sender, ChatColor.RED + "You do not have permission to view details about this command.");
	}
	
	public void reloadSuccess(CommandSender sender){
		sendMessage(sender, ChatColor.GREEN + "Configuration successfully reloaded.");
	}
	
	public void reloadFailed(CommandSender sender){
		sendMessage(sender, ChatColor.RED + "Failed to reload the configuration file, see the console for details.");
	}
	
	public void setPathSuccess(CommandSender sender, String action){
		sendMessage(sender, ChatColor.GREEN + "Successfully updated the " + action + " file path.");
	}
	
	public void setPathFailed(CommandSender sender, String action){
		sendMessage(sender, ChatColor.RED + "Failed to update the " + action + " file path, see the console for details.");
	}
	
	public void denyCommandBlock(CommandSender sender){
		sendMessage(sender, ChatColor.RED + "Command blocks are blocked from accessing this command for security purposes.");
	}
	
	public void commandFormat(CommandSender sender, String cmd){
		if(cmd.equalsIgnoreCase("setsrc") || cmd.equalsIgnoreCase("setdest")){
			sendMessage(sender, ChatColor.RED + "Proper usage is /" + cmd.toLowerCase() + " <File Path>");
		}
		sendMessage(sender, "");
	}
	
	public void commandList(CommandSender sender){
		final String listPrefix = main + " • ";
		
		List<String> cmds = new ArrayList<String>();
		cmds.add(prefix + main + " Command List - <Required> [Optional]");
		if(sender.hasPermission("zipextractor.admin.use")){
			cmds.add(listPrefix + "/ZipExtractor help [cmd]" + trim + "- View command list or info.");
		}
		if(sender.hasPermission("zipextractor.admin.unzip"))
			cmds.add(listPrefix + "/ZipExtractor unzip " + trim + "- Unzip the file you specified.");
		if(sender.hasPermission("zipextractor.admin.setsrc"))
			cmds.add(listPrefix + "/ZipExtractor setsrc <path> " + trim + "- Set the filepath of the zip.");
		if(sender.hasPermission("zipextractor.admin.setdest"))
			cmds.add(listPrefix + "/ZipExtractor setdest <path> " + trim + "- Set the filepath of the destination.");
		if(sender.hasPermission("zipextractor.admin.plugindir"))
			cmds.add(listPrefix + "/ZipExtractor plugindir " + trim + "- Get the plugin's full filepath.");
		if(sender.hasPermission("zipextractor.admin.reload"))
			cmds.add(listPrefix + "/ZipExtractor reload " + trim + "- Reload the config.yml.");
		cmds.add(listPrefix + "/ZipExtractor version " + trim + "- View plugin version info.");
		
		for(String s : cmds) sender.sendMessage(s);
	}
	
	public void commandInfo(CommandSender sender, String cmd){
		if(cmd.equalsIgnoreCase("help")){
			if(!sender.hasPermission("zipextractor.admin.use")){
				noInfoPermission(sender);
				return;
			}
			sendMessage(sender, main + "This command will provide information on the plugin's functions.");
			return;
		}
		if(cmd.equalsIgnoreCase("unzip")){
			if(!sender.hasPermission("zipextractor.admin.unzip")){
				noInfoPermission(sender);
				return;
			}
			sendMessage(sender, main + "This command will unzip the archive you specified in the config.yml. That value can be edited directly in the file or via the command /ZipExtractor setsrc <File Path>. The zip contents will be copied into the folder specified in the config.yml. That value can be edited directly in the file or via the command /ZipExtractor setdest <File Path>.");
			return;
		}
		if(cmd.equalsIgnoreCase("setsrc")){
			if(!sender.hasPermission("zipextractor.admin.setsrc")){
				noInfoPermission(sender);
				return;
			}
			sendMessage(sender, main + "This command will directly update the 'zip_directory' field in the configuration file. \nPlease use \\\\ for a file separator.\nSyntax is /ZipExtractor setsrc <File Path>");
			return;
		}
		if(cmd.equalsIgnoreCase("setdest")){
			if(!sender.hasPermission("zipextractor.admin.setdest")){
				noInfoPermission(sender);
				return;
			}
			sendMessage(sender, main + "This command will directly update the 'destination_directory' field in the configuration file. \nPlease use \\\\ for a file separator.\nSyntax is /ZipExtractor setdest <File Path>");
			return;
		}
		if(cmd.equalsIgnoreCase("plugindir")){
			if(!sender.hasPermission("zipextractor.admin.plugindir")){
				noInfoPermission(sender);
				return;
			}
			sendMessage(sender, main + "This command will tell you the full path of your minecraft server's plugin directory.");
			return;
		}
		if(cmd.equalsIgnoreCase("reload")){
			if(!sender.hasPermission("zipextractor.admin.reload")){
				noInfoPermission(sender);
				return;
			}
			sendMessage(sender, main + "This command will reload the configuration file. It's necessary to do this after you edit the config file directly. If you use the built-in commands it's automatically reloaded after each edit.");
			return;
		}
		
	}
}
