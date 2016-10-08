package com.dscalzi.zipextractor;

import java.io.File;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import com.dscalzi.zipextractor.util.ZCompressor;
import com.dscalzi.zipextractor.util.ZExtractor;

public class ZExecutor implements CommandExecutor{

	private final MessageManager mm;
	private final ConfigManager cm;
	
	private ZipExtractor plugin;
	
	public ZExecutor(ZipExtractor plugin){
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
			if(args[0].equalsIgnoreCase("help")){
				if(args.length > 1 && args[1].matches("^(?iu)(help|extract|compress|setsrc|setdest|plugindir|reload)")){
					this.cmdMoreInfo(sender, args[1]);
					return true;
				}
				this.cmdList(sender);
				return true;
			}
			if(args[0].equalsIgnoreCase("extract")){
				this.cmdExtract(sender);
				return true;
			}
			if(args[0].equalsIgnoreCase("compress")){
				this.cmdCompress(sender);
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
			if(args[0].equalsIgnoreCase("reload")){
				this.cmdReload(sender);
				return true;
			}
			if(args[0].equalsIgnoreCase("version")){
				this.cmdVersion(sender);
				return true;
			}
		}
		
		this.cmdList(sender);
		return true;
	}

	private void cmdList(CommandSender sender){
		mm.commandList(sender);
	}
	
	private void cmdMoreInfo(CommandSender sender, String cmd){
		mm.commandInfo(sender, cmd);
	}
	
	private void cmdExtract(CommandSender sender){
		if(!sender.hasPermission("zipextractor.admin.extract")){
			mm.noPermission(sender);
			return;
		}
		
		ZExtractor ze = new ZExtractor(plugin);
		ze.asyncExtract(sender, new File(plugin.formatPath(cm.getSourcePath(), false)), new File(plugin.formatPath(cm.getDestPath(), false)));
	}
	
	private void cmdCompress(CommandSender sender){
		if(!sender.hasPermission("zipextractor.admin.compress")){
			mm.noPermission(sender);
			return;
		}
		
		ZCompressor zc = new ZCompressor(plugin);
		zc.asyncCompress(sender, new File(plugin.formatPath(cm.getSourcePath(), false)), new File(plugin.formatPath(cm.getDestPath(), false)));
		
	}
	
	private void cmdSetSrc(CommandSender sender, String[] args){
		if(!sender.hasPermission("zipextractor.admin.setsrc")){
			mm.noPermission(sender);
			return;
		}
		if(cm.setSourcePath(plugin.formatPath(formatInput(args), true)))
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
		if(cm.setDestPath(plugin.formatPath(formatInput(args), true)))
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
		if(ConfigManager.reload())
			mm.reloadSuccess(sender);
		else
			mm.reloadFailed(sender);
	}
	
	private void cmdVersion(CommandSender sender){
		mm.sendMessage(sender, "Zip Extractor version " + plugin.getDescription().getVersion());
	}
	
	private String formatInput(String[] args){
		if(args.length < 2) return null;
		
		String ret = args[1];
		if(args[1].indexOf("\"") == 0 || args[1].indexOf("'") == 0){
			for(int i=2; i<args.length; ++i){
				if(args[i].contains("\"") || args[i].contains("'")){
					ret += " " + args[i].substring(0, args[i].indexOf("\""));
					break;
				}
				ret += " " + args[i];
			}
		}
		
		ret = ret.replace("\"", "").replace("'", "");
		
		return ret;
	}
	
}
