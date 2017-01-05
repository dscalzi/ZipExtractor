package com.dscalzi.zipextractor;

import java.io.File;
import java.util.logging.Level;

import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import com.dscalzi.zipextractor.managers.ConfigManager;
import com.dscalzi.zipextractor.managers.MessageManager;
import com.dscalzi.zipextractor.util.ZCompressor;
import com.dscalzi.zipextractor.util.ZExtractor;
import com.dscalzi.zipextractor.util.ZServicer;

public class MainExecutor implements CommandExecutor{

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
				if(args.length > 1 && args[1].matches("^(?iu)(help|extract|compress|setsrc|setdest|plugindir|terminate|forceterminate|reload)")){
					this.cmdMoreInfo(sender, args[1]);
					return true;
				}
				if(args.length > 1 && args[1].matches("(\\d+|-\\d+)")){
					this.cmdList(sender, Integer.parseInt(args[1]));
					return true;
				}
				this.cmdList(sender, 1);
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
