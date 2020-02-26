/*
 * This file is part of ZipExtractor.
 * Copyright (C) 2016-2020 Daniel D. Scalzi <https://github.com/dscalzi/ZipExtractor>
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

package com.dscalzi.zipextractor.bukkit;

import com.dscalzi.zipextractor.bukkit.managers.ConfigManager;
import com.dscalzi.zipextractor.bukkit.util.BukkitCommandSender;
import com.dscalzi.zipextractor.core.ZServicer;
import com.dscalzi.zipextractor.core.managers.MessageManager;
import com.dscalzi.zipextractor.core.util.ICommandSender;
import com.dscalzi.zipextractor.core.util.IPlugin;
import org.bstats.bukkit.Metrics;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class ZipExtractor extends JavaPlugin implements IPlugin {

    @SuppressWarnings("unused")
    private Metrics metrics;

    @Override
    public void onEnable() {
        ConfigManager.initialize(this);
        MessageManager.initialize(this);
        ZServicer.initalize(ConfigManager.getInstance().getMaxQueueSize(),
                ConfigManager.getInstance().getMaxPoolSize());
        this.getCommand("zipextractor").setExecutor(new MainExecutor(this));
        metrics = new Metrics(this, 1117);
    }

    @Override
    public void onDisable() {
        boolean wait = ConfigManager.getInstance().waitForTasksOnShutdown();
        ZServicer.getInstance().terminate(!wait, wait);
    }

    @Override
    public String getVersion() {
        return this.getDescription().getVersion();
    }

    @Override
    public List<? extends ICommandSender> getOnlinePlayers() {
        List<BukkitCommandSender> l = new ArrayList<>();
        this.getServer().getOnlinePlayers().forEach(p -> l.add(new BukkitCommandSender(p)));
        return l;
    }

    @Override
    public void info(String msg) {
        this.getLogger().info(msg);
    }

    @Override
    public void warn(String msg) {
        this.getLogger().warning(msg);
    }

    @Override
    public void severe(String msg) {
        this.getLogger().severe(msg);
    }

    @Override
    public void severe(String msg, Throwable t) {
        this.getLogger().log(Level.SEVERE, msg, t);
    }

    @Override
    public String getPluginDirectory() {
        return this.getDataFolder().getAbsolutePath();
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