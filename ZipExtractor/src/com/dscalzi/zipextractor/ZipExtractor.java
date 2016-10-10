package com.dscalzi.zipextractor;

import java.io.File;
import java.io.IOException;

import org.bukkit.plugin.java.JavaPlugin;
import org.mcstats.Metrics;

import com.dscalzi.zipextractor.managers.ConfigManager;
import com.dscalzi.zipextractor.managers.MessageManager;
import com.dscalzi.zipextractor.util.ZServicer;

public class ZipExtractor extends JavaPlugin{ 

    @Override
    public void onEnable(){
    	ZServicer.initalize();
    	ConfigManager.initialize(this);
    	MessageManager.initialize(this);
    	logMetrics();
    	this.getCommand("zipextractor").setExecutor(new MainExecutor(this));
    }
    
    @Override
    public void onDisable(){
    	
    }
    
    private void logMetrics(){
    	try {
            Metrics metrics = new Metrics(this);
            metrics.start();
        } catch (IOException e) {
            getLogger().severe("Unable to connect to MCStats.org, ");
        }
    }
    
    public String formatPath(String path, boolean forStorage){
    	
    	if(path == null) return null;
    	
    	if(path.contains("*plugindir*")) path = path.replace("*plugindir*", this.getDataFolder().getAbsolutePath());
    	
    	path = path.replaceAll("/|\\\\\\\\|\\\\", "/");
    	
    	String[] cleaner = path.split("\\/");
    	path = "";
    	for(int i=0; i<cleaner.length; ++i){
    		cleaner[i] = cleaner[i].trim();
    		path += cleaner[i] + "/";
    	}
    	path = path.substring(0, (path.lastIndexOf("/") != -1) ? path.lastIndexOf("/") : path.length());
    		
    	
    	if(!forStorage)
    		path = path.replace("/", File.separator);
    	
    	return path;
    }
    
    public String getFileExtension(File f){
    	String fileExtension = "";
    	String path = f.getAbsolutePath();
		if(path.lastIndexOf(".") != -1 && !f.isDirectory()) 
			fileExtension = path.substring(path.lastIndexOf(".")).toLowerCase();
		return fileExtension;
    }
    
}