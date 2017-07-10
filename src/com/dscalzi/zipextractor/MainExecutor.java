/*
 * ZipExtractor
 * Copyright (C) 2017 Daniel D. Scalzi
 * See License.txt for license information.
 */
package com.dscalzi.zipextractor;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.regex.Pattern;

import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import com.dscalzi.zipextractor.managers.ConfigManager;
import com.dscalzi.zipextractor.managers.MessageManager;
import com.dscalzi.zipextractor.util.PathUtils;
import com.dscalzi.zipextractor.util.WarnData;
import com.dscalzi.zipextractor.util.ZCompressor;
import com.dscalzi.zipextractor.util.ZExtractor;
import com.dscalzi.zipextractor.util.ZServicer;

public class MainExecutor implements CommandExecutor, TabCompleter{

	public static final Pattern COMMANDS = Pattern.compile("^(?iu)(help|extract|compress|src|dest|setsrc|setdest|plugindir|terminate|forceterminate|reload)");
	public static final Pattern INTEGERS = Pattern.compile("(\\\\d+|-\\\\d+)");
	
	private final MessageManager mm;
	private final ConfigManager cm;
	
	private ZipExtractor plugin;
	
	public MainExecutor(ZipExtractor plugin){
		this.mm = MessageManager.getInstance();
		this.cm = ConfigManager.getInstance();
		this.plugin = plugin;
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		
		if(sender instanceof BlockCommandSender){
			mm.denyCommandBlock(sender);
			return true;
		}
		
		if(args.length > 0 && args[0].equalsIgnoreCase("version")){
			this.cmdVersion(sender);
			return true;
		}
		
		if(!sender.hasPermission("zipextractor.admin.use")){
			mm.noPermissionFull(sender);
			return true;
		}
		
		if(args.length > 0){
			if(args[0].matches("(\\d+|-\\d+)")){
				this.cmdList(sender, Integer.parseInt(args[0]));
				return true;
			}
			if(args[0].equalsIgnoreCase("help")){
				if(args.length > 1 && COMMANDS.matcher(args[1]).matches()){
					this.cmdMoreInfo(sender, args[1]);
					return true;
				}
				if(args.length > 1 && INTEGERS.matcher(args[1]).matches()){
					this.cmdList(sender, Integer.parseInt(args[1]));
					return true;
				}
				this.cmdList(sender, 1);
				return true;
			}
			if(args[0].equalsIgnoreCase("extract")){
				this.cmdExtract(sender, args);
				return true;
			}
			if(args[0].equalsIgnoreCase("compress")){
				this.cmdCompress(sender);
			    return true;
			}
			if(args[0].equalsIgnoreCase("src")){
				this.cmdSrc(sender, args);
				return true;
			}
			if(args[0].equalsIgnoreCase("dest")){
				this.cmdDest(sender, args);
				return true;
			}
			if(args[0].equalsIgnoreCase("setsrc")){
				this.cmdSetSrc(sender, args);
				return true;
			}
			if(args[0].equalsIgnoreCase("setdest")){
				this.cmdSetDest(sender, args);
				return true;
			}
			if(args[0].equalsIgnoreCase("plugindir")){
				this.cmdPluginDir(sender);
				return true;
			}
			if(args[0].equalsIgnoreCase("terminate")){
				this.cmdTerminate(sender, false);
				return true;
			}
			if(args[0].equalsIgnoreCase("forceterminate")){
				this.cmdTerminate(sender, true);
				return true;
			}
			if(args[0].equalsIgnoreCase("reload")){
				this.cmdReload(sender);
				return true;
			}
			if(args[0].equalsIgnoreCase("version")){
				this.cmdVersion(sender);
				return true;
			}
		}
		
		this.cmdList(sender, 1);
		return true;
	}

	private void cmdList(CommandSender sender, int page){
		mm.commandList(sender, --page);
	}
	
	private void cmdMoreInfo(CommandSender sender, String cmd){
		mm.commandInfo(sender, cmd);
	}
	
	private void cmdExtract(CommandSender sender, String[] args){
		if(!sender.hasPermission("zipextractor.admin.extract")){
			mm.noPermission(sender);
			return;
		}
		
		if(args.length >= 2 && args[1].equalsIgnoreCase("view")) {
			Optional<WarnData> dataOpt = ZExtractor.getWarnData(sender);
			if(dataOpt.isPresent()) {
				WarnData d = dataOpt.get();
				
				int page = 0;
				if(args.length >= 3) {
					try {
						page = Integer.parseInt(args[2]);
						if(1 > page || page > d.getFiles().size()) {
							throw new NumberFormatException();
						} else {
							--page;
						}
					} catch(NumberFormatException e) {
						mm.invalidPage(sender);
						return;
					}
				}
				
				mm.formatWarnList(sender, page, d.getFiles());
				
			} else {
				mm.noWarnData(sender);
			}
		} else {
			Optional<File> srcOpt = cm.getSourceFile();
			Optional<File> destOpt = cm.getDestFile();
			if(!srcOpt.isPresent()) {
				mm.invalidPath(sender, cm.getSourceRaw(), "source");
				return;
			}
			if(!destOpt.isPresent()) {
				mm.invalidPath(sender, cm.getDestRaw(), "destination");
				return;
			}
			
			ZExtractor.asyncExtract(sender, srcOpt.get(), destOpt.get(), ZExtractor.wasWarned(sender, srcOpt.get(), destOpt.get()));
		}
	}
	
	private void cmdCompress(CommandSender sender){
		if(!sender.hasPermission("zipextractor.admin.compress")){
			mm.noPermission(sender);
			return;
		}
		
		Optional<File> srcOpt = cm.getSourceFile();
		Optional<File> destOpt = cm.getDestFile();
		if(!srcOpt.isPresent()) {
			mm.invalidPath(sender, cm.getSourceRaw(), "source");
			return;
		}
		if(!destOpt.isPresent()) {
			mm.invalidPath(sender, cm.getDestRaw(), "destination");
			return;
		}
		
		ZCompressor zc = new ZCompressor();
		zc.asyncCompress(sender, srcOpt.get(), destOpt.get());
		
	}
	
	private void cmdSrc(CommandSender sender, String[] args){
		if(!sender.hasPermission("zipextractor.admin.src")){
			mm.noPermission(sender);
			return;
		}
		if(args.length > 1 && args[1].equalsIgnoreCase("-absolute")) {
			Optional<File> srcOpt = cm.getSourceFile();
			if(!srcOpt.isPresent()) {
				mm.invalidPath(sender, cm.getSourceRaw(), "source");
				return;
			}
			mm.sendSuccess(sender, srcOpt.get().getAbsolutePath());
		} else {
			mm.sendSuccess(sender, cm.getSourceRaw());
		}
	}
	
	private void cmdDest(CommandSender sender, String[] args){
		if(!sender.hasPermission("zipextractor.admin.dest")){
			mm.noPermission(sender);
			return;
		}
		if(args.length > 1 && args[1].equalsIgnoreCase("-absolute")) {
			Optional<File> destOpt = cm.getDestFile();
			if(!destOpt.isPresent()) {
				mm.invalidPath(sender, cm.getDestRaw(), "destination");
				return;
			}
			mm.sendSuccess(sender, destOpt.get().getAbsolutePath());
		} else {
			mm.sendSuccess(sender, cm.getDestRaw());
		}
	}
	
	private void cmdSetSrc(CommandSender sender, String[] args){
		if(!sender.hasPermission("zipextractor.admin.setsrc")){
			mm.noPermission(sender);
			return;
		}
		if(args.length < 2) {
			mm.specifyAPath(sender);
			return;
		}
		String path = PathUtils.formatPath(formatInput(args), true);
		if(!PathUtils.validateFilePath(path)) {
			mm.invalidPath(sender, path);
			return;
		}
		if(cm.setSourcePath(path))
			mm.setPathSuccess(sender, "source");
		else
			mm.setPathFailed(sender, "source");
		ConfigManager.reload();
	}
	
	private void cmdSetDest(CommandSender sender, String[] args){
		if(!sender.hasPermission("zipextractor.admin.setdest")){
			mm.noPermission(sender);
			return;
		}
		if(args.length < 2) {
			mm.specifyAPath(sender);
			return;
		}
		String path = PathUtils.formatPath(formatInput(args), true);
		if(!PathUtils.validateFilePath(path)) {
			mm.invalidPath(sender, path);
			return;
		}
		if(cm.setDestPath(path))
			mm.setPathSuccess(sender, "destination");
		else
			mm.setPathFailed(sender, "destination");
		ConfigManager.reload();
	}
	
	private void cmdPluginDir(CommandSender sender){
		if(!sender.hasPermission("zipextractor.admin.plugindir")){
			mm.noPermission(sender);
			return;
		}
		mm.sendMessage(sender, "Plugin Directory - " + plugin.getDataFolder().getAbsolutePath());
	}
	
	private void cmdReload(CommandSender sender){
		if(!sender.hasPermission("zipextractor.admin.reload")){
			mm.noPermission(sender);
			return;
		}
		try{
			if(ConfigManager.reload()){
				ZServicer.getInstance().setMaximumPoolSize(ConfigManager.getInstance().getMaxPoolSize());
				mm.reloadSuccess(sender);
			}else
				mm.reloadFailed(sender);
		} catch (Throwable e){
			mm.reloadFailed(sender);
			mm.getLogger().log(Level.SEVERE, "Error while reloading the plugin", e);
			e.printStackTrace();
		}
	}
	
	private void cmdTerminate(CommandSender sender, boolean force){
		if(!sender.hasPermission(force ? "zipextractor.admin.forceterminate" : "zipextractor.admin.terminate")){
			mm.noPermission(sender);
			return;
		}
		ZServicer e = ZServicer.getInstance();
		if(e.isTerminated()){
			mm.alreadyTerminated(sender);
			return;
		}
		if(e.isTerminating()){
			mm.alreadyTerminating(sender);
			return;
		}
		e.terminate(force, false);
		if(force) mm.terminatingForcibly(sender);
		else mm.terminating(sender);
	}
	
	private void cmdVersion(CommandSender sender){
		mm.cmdVersion(sender);
	}
	
	private String formatInput(String[] args){
		
		String ret = args[1];
		if(args[1].indexOf("\"") == 0 || args[1].indexOf("'") == 0){
			String delimeter = args[1].startsWith("\"") ? "\"" : "'";
			for(int i=2; i<args.length; ++i){
				if(args[i].endsWith(delimeter)){
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
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		List<String> ret = new ArrayList<String>();
		
		if(args.length == 1) {
			ret.addAll(subCommands(sender, args));
		}
		
		if(args.length == 2){
			boolean a = sender.hasPermission("zipextractor.admin.src") && "src".startsWith(args[0].toLowerCase());
			boolean b = sender.hasPermission("zipextractor.admin.dest") && "dest".startsWith(args[0].toLowerCase());
			if(a | b)
				if("-absolute".startsWith(args[1].toLowerCase()))
					ret.add("-absolute");
			if(sender.hasPermission("zipextractor.admin.use") && "help".startsWith(args[0].toLowerCase())) {
				String[] newArgs = new String[args.length-1];
				System.arraycopy(args, 1, newArgs, 0, args.length-1);
				ret.addAll(subCommands(sender, newArgs));
			}
		}
		
		return ret;
	}
	
	private List<String> subCommands(CommandSender sender, String[] args){
		List<String> ret = new ArrayList<String>();
		
		if(args.length == 1) {
			if(sender.hasPermission("zipextractor.admin.use") && "help".startsWith(args[0].toLowerCase()))
				ret.add("help");
			if(sender.hasPermission("zipextractor.admin.extract") && "extract".startsWith(args[0].toLowerCase())) 
				ret.add("extract");
			if(sender.hasPermission("zipextractor.admin.compress") && "compress".startsWith(args[0].toLowerCase())) 
				ret.add("compress");
			if(sender.hasPermission("zipextractor.admin.src") && "src".startsWith(args[0].toLowerCase())) 
				ret.add("src");
			if(sender.hasPermission("zipextractor.admin.dest") && "dest".startsWith(args[0].toLowerCase())) 
				ret.add("dest");
			if(sender.hasPermission("zipextractor.admin.setsrc") && "setsrc".startsWith(args[0].toLowerCase())) 
				ret.add("setsrc");
			if(sender.hasPermission("zipextractor.admin.setdest") && "setdest".startsWith(args[0].toLowerCase())) 
				ret.add("setdest");
			if(sender.hasPermission("zipextractor.admin.plugindir") && "plugindir".startsWith(args[0].toLowerCase())) 
				ret.add("plugindir");
			if(sender.hasPermission("zipextractor.admin.terminate") && "terminate".startsWith(args[0].toLowerCase())) 
				ret.add("terminate");
			if(sender.hasPermission("zipextractor.admin.forceterminate") && "forceterminate".startsWith(args[0].toLowerCase())) 
				ret.add("forceterminate");
			if(sender.hasPermission("zipextractor.admin.reload") && "reload".startsWith(args[0].toLowerCase())) 
				ret.add("reload");
		}
		
		return ret;
	}
	
}
