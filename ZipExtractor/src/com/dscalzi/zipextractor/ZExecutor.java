package com.dscalzi.zipextractor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import com.github.junrar.Archive;
import com.github.junrar.exception.RarException;
import com.github.junrar.impl.FileVolumeManager;
import com.github.junrar.rarfile.FileHeader;

public class ZExecutor implements CommandExecutor{

	private final ConfigManager cm;
	private final MessageManager mm;
	
	private volatile boolean extracting;
	
	private ZipExtractor plugin;
	
	public ZExecutor(ZipExtractor plugin){
		this.plugin = plugin;
		this.extracting = false;
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
				if(args.length > 1 && args[1].matches("^(?iu)(help|extract|setsrc|setdest|plugindir|reload)")){
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
		this.asyncExecute(sender, new File(plugin.formatPath(cm.getSourcePath(), false)), new File(plugin.formatPath(cm.getDestPath(), false)));
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
	
	private void asyncExecute(CommandSender sender, File sourceFile, File destFolder){
		String fileExtension = "";
		String sourcePath = sourceFile.getAbsolutePath();
		if(sourcePath.lastIndexOf(".") != -1 && !sourceFile.isDirectory()) fileExtension = sourcePath.substring(sourcePath.lastIndexOf(".")).toLowerCase();
		if(fileExtension.length() == 0 || !sourceFile.exists()) {
			mm.invalidPath(sender, "source");
			return;
		}
		
		try{
			Paths.get(destFolder.getAbsolutePath());
		} catch(InvalidPathException e){
			mm.invalidPath(sender, "destination");
			return;
		}
		
		if(!destFolder.exists())
    		destFolder.mkdir();
		
		if(destFolder.exists()){
			if(!destFolder.isDirectory()){
				mm.destNotDirectory(sender);
				return;
			}
		}
		
		if(extracting){
			mm.extractionInProccess(sender);
			return;
		}
		
		mm.startingExtraction(sender, sourceFile.getName());
		extracting = true;
		
		switch(fileExtension){
		case ".jar":
			new Thread(() -> extractJar(sender, sourceFile, destFolder)).start();
			break;
		case ".rar":
			new Thread(() -> extractRar(sender, sourceFile, destFolder)).start();
			break;
		default:
			new Thread(() -> extract(sender, sourceFile, destFolder)).start();
			break;
		}
		
	}
	
	private void extract(CommandSender sender, File sourceFile, File destFolder){
		Logger logger = MessageManager.getInstance().getLogger();
		byte[] buffer = new byte[1024];
		try {
			ZipInputStream zis = new ZipInputStream(new FileInputStream(sourceFile));
	    	ZipEntry ze = zis.getNextEntry();
	    	
	    	while(ze!=null){
	    		String fileName = ze.getName();
	    		File newFile = new File(destFolder + File.separator + fileName);	    		
	    		if(cm.getLoggingProperty())
	    			logger.info("Extracting : "+ newFile.getAbsoluteFile());
	    		if(newFile.getParentFile() != null)
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
	    	MessageManager.getInstance().extractionComplete(sender, destFolder.getAbsolutePath());
	    } catch(IOException ex){
	    	ex.printStackTrace();
	    }
		extracting = false;
	}
	
	private void extractJar(CommandSender sender, File sourceFile, File destFolder){
		Logger logger = MessageManager.getInstance().getLogger();
		try{			
			JarFile jar = new JarFile(sourceFile);
			Enumeration<JarEntry> enumEntries = jar.entries();
			while (enumEntries.hasMoreElements()) {
			    JarEntry file = enumEntries.nextElement();
			    File f = new File(destFolder + File.separator + file.getName());
			    if(cm.getLoggingProperty())
			    	logger.info("Extracting : "+ f.getAbsolutePath());
			    if (file.isDirectory()) {
			        f.mkdir();
			        continue;
			    }
			    InputStream is = jar.getInputStream(file);
			    FileOutputStream fos = new FileOutputStream(f);
			    while (is.available() > 0) fos.write(is.read());
			    fos.close();
			    is.close();
			}
			jar.close();
			MessageManager.getInstance().extractionComplete(sender, destFolder.getAbsolutePath());
		} catch (IOException e){
			e.printStackTrace();
		}
		extracting = false;
	}
	
	private void extractRar(CommandSender sender, File sourceFile, File destFolder){
		Logger logger = MessageManager.getInstance().getLogger();
		Archive a = null;
		try {
			a = new Archive(new FileVolumeManager(sourceFile));
		} catch (RarException | IOException e) {
			e.printStackTrace();
		}
		if (a != null) {
			FileHeader fh = a.nextFileHeader();
			while (fh != null) {
				try {
					InputStream is = a.getInputStream(fh);
					Path p = Paths.get(destFolder + File.separator + fh.getFileNameString()); 
					File parent = p.toFile().getParentFile();
					if(!parent.exists() && !parent.mkdirs()){
					    throw new IllegalStateException("Couldn't create dir: " + parent);
					}
					try{
						if(cm.getLoggingProperty())
					    	logger.info("Extracting : "+ p.toString());
						Files.copy(is, p, StandardCopyOption.REPLACE_EXISTING);
					} catch (DirectoryNotEmptyException e){
						fh = a.nextFileHeader();
						continue;
					}
					is.close();
				} catch (RarException | IOException e) {
					e.printStackTrace();
				}
				fh = a.nextFileHeader();
			}
		}
		MessageManager.getInstance().extractionComplete(sender, destFolder.getAbsolutePath());
		extracting = false;
	}
	
}
