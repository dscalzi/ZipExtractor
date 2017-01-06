package com.dscalzi.zipextractor.managers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.configuration.file.FileConfiguration;

import com.dscalzi.zipextractor.ZipExtractor;

public class ConfigManager {

	private static boolean initialized;
	private static ConfigManager instance;
	
	//TODO Will be implemented in a later version
	private final double configVersion = 1.9;
	private ZipExtractor plugin;
	private FileConfiguration config;
	
	private ConfigManager(ZipExtractor plugin){
		this.plugin = plugin;
		loadConfig();
	}
	
	public void loadConfig(){
    	verifyFile();
    	this.plugin.reloadConfig();
		this.config = this.plugin.getConfig(); 
    }
	
	public void verifyFile(){
    	File file = new File(this.plugin.getDataFolder(), "config.yml");
		if (!file.exists()){
			this.plugin.saveDefaultConfig();
		}
    }
	
	public static void initialize(ZipExtractor plugin){
		if(!initialized){
			instance = new ConfigManager(plugin);
			initialized = true;
		}
	}
	
	public static boolean reload(){
		if(!initialized) return false;
		try{
			getInstance().loadConfig();
			return true;
		} catch(Exception e){
			e.printStackTrace();
			return false;
		}
	}
	
	public static ConfigManager getInstance(){
		return ConfigManager.instance;
	}
	
	/* Configuration Accessors */
	
	public String getSourcePath(){
		return this.config.getString("file_settings.source_directory", null);
	}
	
	public String getDestPath(){
		return this.config.getString("file_settings.destination_directory", null);
	}
	
	public boolean setSourcePath(String path){
		boolean ret = this.updateValue("file_settings.source_directory", path);
		this.plugin.saveDefaultConfig();
		return ret;
	}
	
	public boolean setDestPath(String path){
		boolean ret = this.updateValue("file_settings.destination_directory", path);
		this.plugin.saveDefaultConfig();
		return ret;
	}
	
	public boolean getLoggingProperty(){
		return this.config.getBoolean("general_settings.log_files", false);
	}
	
	public boolean waitForTasksOnShutdown(){
		return this.config.getBoolean("general_settings.wait_on_shutdown", true);
	}
	
	public int getMaxQueueSize(){
		int limit = this.config.getInt("general_settings.max_queue_size", 3);
		return limit > 0 ? limit : Integer.MAX_VALUE;
	}
	
	public int getMaxPoolSize(){
		int limit = this.config.getInt("general_settings.maximum_thread_pool", 1);
		if(limit < 1) limit = 1;
		return limit > 0 ? limit : 1;
	}
	
	public double getVersion(){
		return this.configVersion;
	}
	
	public boolean updateValue(String path, String value) {
	    try {
	        BufferedReader file = new BufferedReader(new FileReader(this.plugin.getDataFolder() + File.separator + "config.yml"));
	        String line;
	        String input = "";

	        List<String> paths = new ArrayList<String>(Arrays.asList(path.split("\\.")));
	        
	        while ((line = file.readLine()) != null) {
	        	String lline = line.toLowerCase();
	        	if(paths.size() > 0){
	        		if(lline.contains(paths.get(0).toLowerCase())){
	        			paths.remove(0);
	        			if(paths.size() == 0){
		        			int firstIndex = line.indexOf("\"");
		        			int lastIndex = line.lastIndexOf("\"");
		        			if(firstIndex == -1){
		        				line = line.replaceAll(" +$", "");
		        				line += " \"";
		        				firstIndex = line.indexOf("\"");
		        			}
		        			if(lastIndex == -1){
		        				line += "\"";
		        				lastIndex = line.lastIndexOf("\"");
		        			}
		        			line = line.substring(0, firstIndex+1) + value + line.substring(lastIndex);
		        		}
	        		}
	        	}
	        	input += line + '\n';
	        }
	        

	        file.close();
	        
	        if(paths.size() > 0) return false;
	        
	        FileOutputStream fileOut = new FileOutputStream(this.plugin.getDataFolder() + File.separator + "config.yml");
	        fileOut.write(input.getBytes());
	        fileOut.close();
	        return true;
	    } catch (Exception e) {
	        e.printStackTrace();
	        return false;
	    }
	}
}
