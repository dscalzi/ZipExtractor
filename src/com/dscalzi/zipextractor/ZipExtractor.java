/*
 * ZipExtractor
 * Copyright (C) 2017 Daniel D. Scalzi
 * See License.txt for license information.
 */
package com.dscalzi.zipextractor;

import org.bstats.Metrics;
import org.bukkit.plugin.java.JavaPlugin;

import com.dscalzi.zipextractor.managers.ConfigManager;
import com.dscalzi.zipextractor.managers.MessageManager;
import com.dscalzi.zipextractor.providers.JarProvider;
import com.dscalzi.zipextractor.providers.PackProvider;
import com.dscalzi.zipextractor.providers.RarProvider;
import com.dscalzi.zipextractor.providers.TypeProvider;
import com.dscalzi.zipextractor.providers.ZipProvider;
import com.dscalzi.zipextractor.util.ZServicer;

public class ZipExtractor extends JavaPlugin{ 

	@SuppressWarnings("unused")
	private Metrics metrics;
	private static final TypeProvider[] PROVIDERS = {
			new ZipProvider(),
			new RarProvider(),
			new JarProvider(),
			new PackProvider()
	};
	
    @Override
    public void onEnable(){
    	ConfigManager.initialize(this);
    	MessageManager.initialize(this);
    	ZServicer.initalize(ConfigManager.getInstance().getMaxQueueSize(), ConfigManager.getInstance().getMaxPoolSize());
    	this.getCommand("zipextractor").setExecutor(new MainExecutor(this));
    	metrics = new Metrics(this);
    }
    
    @Override
    public void onDisable(){
    	boolean wait = ConfigManager.getInstance().waitForTasksOnShutdown();
    	ZServicer.getInstance().terminate(!wait, wait);
    }
    
    public static TypeProvider[] getProviders() {
    	return PROVIDERS;
    }
    
}