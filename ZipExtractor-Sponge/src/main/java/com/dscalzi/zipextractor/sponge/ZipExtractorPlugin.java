/*
 * This file is part of ZipExtractor.
 * Copyright (C) 2016-2018 Daniel D. Scalzi <https://github.com/dscalzi/ZipExtractor>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.dscalzi.zipextractor.sponge;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.bstats.sponge.Metrics2;
import org.slf4j.Logger;
import org.spongepowered.api.Game;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.config.DefaultConfig;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.GameReloadEvent;
import org.spongepowered.api.event.game.state.GamePostInitializationEvent;
import org.spongepowered.api.event.game.state.GamePreInitializationEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.service.permission.PermissionDescription;
import org.spongepowered.api.service.permission.PermissionDescription.Builder;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.text.Text;

import com.dscalzi.zipextractor.core.ZServicer;
import com.dscalzi.zipextractor.core.managers.MessageManager;
import com.dscalzi.zipextractor.core.util.ICommandSender;
import com.dscalzi.zipextractor.core.util.IPlugin;
import com.dscalzi.zipextractor.sponge.managers.ConfigManager;
import com.dscalzi.zipextractor.sponge.util.SpongeCommandSender;
import com.google.inject.Inject;

import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.loader.ConfigurationLoader;

@Plugin(id = "zipextractor")
public class ZipExtractorPlugin implements IPlugin {

    @Inject private PluginContainer plugin;
    @Inject private Logger logger;
    @Inject private Game game;
    @SuppressWarnings("unused")
    @Inject private Metrics2 metrics;
    
    @Inject
    @DefaultConfig(sharedRoot = false)
    private ConfigurationLoader<CommentedConfigurationNode> configLoader;
    @Inject
    @ConfigDir(sharedRoot = false)
    private File configDir;
    
    public PluginContainer getPlugin() {
        return plugin;
    }
    
    public ConfigurationLoader<CommentedConfigurationNode> getConfigLoader(){
        return configLoader;
    }
    
    public File getConfigDir() {
        return configDir;
    }
    
    public void disable() {
        game.getEventManager().unregisterPluginListeners(this);
        game.getCommandManager().getOwnedBy(this).forEach(game.getCommandManager()::removeMapping);
        game.getScheduler().getScheduledTasks(this).forEach(Task::cancel);
    }
    
    @Listener
    public void onGamePreInitialization(GamePreInitializationEvent e){
        logger.info("Enabling " + plugin.getName() + " version " + plugin.getVersion().orElse("dev") + ".");
        
        ConfigManager.initialize(this);
        MessageManager.initialize(this);
        ZServicer.initalize(ConfigManager.getInstance().getMaxQueueSize(), ConfigManager.getInstance().getMaxPoolSize());
        
        Sponge.getCommandManager().register(this, new MainExecutor(this), Arrays.asList("zipextractor", "ze"));
    }

    
    @Listener
    public void onPostInit(GamePostInitializationEvent event) {
        Optional<PermissionService> ops = Sponge.getServiceManager().provide(PermissionService.class);
        if (ops.isPresent()) {
            Builder opdb = ops.get().newDescriptionBuilder(this);
            if (opdb != null) {
                opdb.assign(PermissionDescription.ROLE_ADMIN, true).description(Text.of("Full access to ZipExtractor.")).id(plugin.getId()).register();
                opdb.assign(PermissionDescription.ROLE_ADMIN, true).description(Text.of("Access to administrator commands.")).id(plugin.getId() + ".admin").register();
                opdb.assign(PermissionDescription.ROLE_ADMIN, true).description(Text.of("Allow usage of /ZipExtractor.")).id(plugin.getId() + ".admin.use").register();
                opdb.assign(PermissionDescription.ROLE_ADMIN, true).description(Text.of("Allow usage of /ZipExtractor extract.")).id(plugin.getId() + ".admin.extract").register();
                opdb.assign(PermissionDescription.ROLE_ADMIN, true).description(Text.of("Allow usage of /ZipExtractor compress.")).id(plugin.getId() + ".admin.compress").register();
                opdb.assign(PermissionDescription.ROLE_ADMIN, true).description(Text.of("Allow usage of /ZipExtractor extract --override.")).id(plugin.getId() + ".admin.override.extract").register();
                opdb.assign(PermissionDescription.ROLE_ADMIN, true).description(Text.of("Allow usage of /ZipExtractor compress --override.")).id(plugin.getId() + ".admin.override.compress").register();
                opdb.assign(PermissionDescription.ROLE_ADMIN, true).description(Text.of("Allow usage of /ZipExtractor src.")).id(plugin.getId() + ".admin.src").register();
                opdb.assign(PermissionDescription.ROLE_ADMIN, true).description(Text.of("Allow usage of /ZipExtractor dest.")).id(plugin.getId() + ".admin.dest").register();
                opdb.assign(PermissionDescription.ROLE_ADMIN, true).description(Text.of("Allow usage of /ZipExtractor setsrc.")).id(plugin.getId() + ".admin.setsrc").register();
                opdb.assign(PermissionDescription.ROLE_ADMIN, true).description(Text.of("Allow usage of /ZipExtractor setdest.")).id(plugin.getId() + ".admin.setdest").register();
                opdb.assign(PermissionDescription.ROLE_ADMIN, true).description(Text.of("Allow usage of /ZipExtractor plugindir.")).id(plugin.getId() + ".admin.plugindir").register();
                opdb.assign(PermissionDescription.ROLE_ADMIN, true).description(Text.of("Allow usage of /ZipExtractor terminate.")).id(plugin.getId() + ".admin.terminate").register();
                opdb.assign(PermissionDescription.ROLE_ADMIN, true).description(Text.of("Allow usage of /ZipExtractor forceterminate.")).id(plugin.getId() + ".admin.forceterminate").register();
                opdb.assign(PermissionDescription.ROLE_ADMIN, true).description(Text.of("Allow usage of /ZipExtractor reload.")).id(plugin.getId() + ".admin.reload").register();
                opdb.assign(PermissionDescription.ROLE_ADMIN, true).description(Text.of("Access to harmless commands.")).id(plugin.getId() + ".harmless").register();
                opdb.assign(PermissionDescription.ROLE_ADMIN, true).description(Text.of("User will be notified if the plugin broadcasts a message.")).id(plugin.getId() + ".harmless.notify").register();
                opdb.assign(PermissionDescription.ROLE_ADMIN, true).description(Text.of("Allow usage of /ZipExtractor status.")).id(plugin.getId() + ".harmless.status").register();
            }
        }
    }

    
    @Listener
    public void onReload(GameReloadEvent e){
        reload();
    }

    @Override
    public String getVersion() {
        return plugin.getVersion().orElse("dev");
    }

    @Override
    public String getName() {
        return plugin.getName();
    }

    @Override
    public List<? extends ICommandSender> getOnlinePlayers() {
        List<SpongeCommandSender> l = new ArrayList<SpongeCommandSender>();
        for(Player p : game.getServer().getOnlinePlayers()) {
            l.add(new SpongeCommandSender(p));
        }
        return l;
    }

    @Override
    public void info(String msg) {
        logger.info(msg);
    }

    @Override
    public void warn(String msg) {
        logger.warn(msg);
    }

    @Override
    public void severe(String msg) {
        logger.error(msg);
    }

    @Override
    public void severe(String msg, Throwable t) {
        logger.trace(msg, t);
    }

    @Override
    public String getPluginDirectory() {
        return configDir.getAbsolutePath();
    }

    @Override
    public boolean reload() {
        if (ConfigManager.reloadStatic()) {
            ZServicer.getInstance().setMaximumPoolSize(ConfigManager.getInstance().getMaxPoolSize());
            return true;
        }
        return false;
    }
    
}
