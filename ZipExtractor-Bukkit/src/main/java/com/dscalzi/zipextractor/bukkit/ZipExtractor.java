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

package com.dscalzi.zipextractor.bukkit;

import org.bstats.bukkit.Metrics;
import org.bukkit.plugin.java.JavaPlugin;

import com.dscalzi.zipextractor.bukkit.managers.ConfigManager;
import com.dscalzi.zipextractor.bukkit.managers.MessageManager;
import com.dscalzi.zipextractor.bukkit.providers.JarProvider;
import com.dscalzi.zipextractor.bukkit.providers.PackProvider;
import com.dscalzi.zipextractor.bukkit.providers.RarProvider;
import com.dscalzi.zipextractor.bukkit.providers.TypeProvider;
import com.dscalzi.zipextractor.bukkit.providers.XZProvider;
import com.dscalzi.zipextractor.bukkit.providers.ZipProvider;
import com.dscalzi.zipextractor.bukkit.util.ZServicer;

public class ZipExtractor extends JavaPlugin {

    @SuppressWarnings("unused")
    private Metrics metrics;
    private static final TypeProvider[] PROVIDERS = { new ZipProvider(), new RarProvider(), new JarProvider(),
            new PackProvider(), new XZProvider() };

    @Override
    public void onEnable() {
        ConfigManager.initialize(this);
        MessageManager.initialize(this);
        ZServicer.initalize(ConfigManager.getInstance().getMaxQueueSize(),
                ConfigManager.getInstance().getMaxPoolSize());
        this.getCommand("zipextractor").setExecutor(new MainExecutor(this));
        metrics = new Metrics(this);
    }

    @Override
    public void onDisable() {
        boolean wait = ConfigManager.getInstance().waitForTasksOnShutdown();
        ZServicer.getInstance().terminate(!wait, wait);
    }

    public static TypeProvider[] getProviders() {
        return PROVIDERS;
    }

}