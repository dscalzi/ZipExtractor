package com.dscalzi.zipextractor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class ZExecutor implements CommandExecutor{

	private final ConfigManager cm;
	private final MessageManager mm;
	
	private ZipExtractor plugin;
	
	public ZExecutor(ZipExtractor plugin){
		this.plugin = plugin;
		this.cm = ConfigManager.getInstance();
		this.mm = MessageManager.getInstance();
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
				if(args.length > 1 && args[1].matches("^(?iu)(help|unzip|setsrc|setdest|plugindir|reload)")){
					this.cmdMoreInfo(sender, args[1]);
					return true;
				}
				this.cmdList(sender);
				return true;
			}
			if(args[0].equalsIgnoreCase("unzip")){
				this.cmdUnZip(sender);
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
	
	private void cmdUnZip(CommandSender sender){
		if(!sender.hasPermission("zipextractor.admin.unzip")){
			mm.noPermission(sender);
			return;
		}
		this.extract(new File(plugin.formatPath(cm.getSourcePath(), false)), new File(plugin.formatPath(cm.getDestPath(), false)));
	}
	
	private void cmdSetSrc(CommandSender sender, String[] args){
		if(!sender.hasPermission("zipextractor.admin.setsrc")){
			mm.noPermission(sender);
			return;
		}
		if(cm.setSourcePath(plugin.formatPath(args[1], true)))
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
		if(cm.setDestPath(plugin.formatPath(args[1], true)))
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
	
	private void extract(File zipFile, File outputFolder){
		byte[] buffer = new byte[1024];

		try {
			
			File folder = outputFolder;
			if(!folder.exists())
	    		folder.mkdir();

	    	ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile));
	    	ZipEntry ze = zis.getNextEntry();
	    	
	    	while(ze!=null){
	    		
	    		String fileName = ze.getName();
	    		File newFile = new File(outputFolder + File.separator + fileName);
	    		
	    		plugin.getLogger().info("file unzip : "+ newFile.getAbsoluteFile());
	    		
	            //create all non exists folders
	            //else you will hit FileNotFoundException for compressed folder
	            new File(newFile.getParent()).mkdirs();
	            
	            FileOutputStream fos = new FileOutputStream(newFile);
	            
	            int len;
	            while ((len = zis.read(buffer)) > 0) {
	            	fos.write(buffer, 0, len);
	            }
	            
	            fos.close();
	            ze = zis.getNextEntry();
	    	}

	        zis.closeEntry();
	    	zis.close();
	    	
	    	plugin.getLogger().info("Done");

	    } catch(IOException ex){
	    	ex.printStackTrace();
	    }
	}
	
}
