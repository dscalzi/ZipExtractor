package com.dscalzi.zipextractor;

import java.io.File;
import org.bukkit.plugin.java.JavaPlugin;

public class ZipExtractor extends JavaPlugin{ 

    @Override
    public void onEnable(){
    	ConfigManager.initialize(this);
    	MessageManager.initialize(this);
    	this.getCommand("zipextractor").setExecutor(new ZExecutor(this));
    }
    
    @Override
    public void onDisable(){
    	
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
    		path = path.replace("%sep%", File.separator);
    	
    	return path;
    }
    
}