/*
 * This file is part of ZipExtractor.
 * Copyright (C) 2016-2019 Daniel D. Scalzi <https://github.com/dscalzi/ZipExtractor>
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

package com.dscalzi.zipextractor.sponge.managers;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

import org.spongepowered.api.asset.Asset;

import com.dscalzi.zipextractor.core.managers.IConfigManager;
import com.dscalzi.zipextractor.core.util.PathUtils;
import com.dscalzi.zipextractor.sponge.ZipExtractorPlugin;

import ninja.leaping.configurate.commented.CommentedConfigurationNode;

public class ConfigManager implements IConfigManager {

    private static boolean initialized;
    private static ConfigManager instance;

    private ZipExtractorPlugin plugin;
    private CommentedConfigurationNode config;


    private ConfigManager(ZipExtractorPlugin plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    public void loadConfig() {
        boolean res = verifyFile();
        if(res) {
            try {
                this.config = this.plugin.getConfigLoader().load();
            } catch (IOException e) {
                plugin.severe("Failed to load config.");
                e.printStackTrace();
            }
        } else {
            this.config = null;
        }
    }

    public boolean verifyFile() {
        Asset asset = plugin.getPlugin().getAsset("zipextractor.conf").orElse(null);
        File file = new File(plugin.getConfigDir(), "zipextractor.conf");

        if (!file.exists()) {
            if(asset != null) {
                try {
                    asset.copyToFile(file.toPath());
                    return true;
                } catch (IOException e) {
                    plugin.severe("Failed to save default config.");
                    e.printStackTrace();
                    return false;
                }
            } else {
                plugin.severe("Failed to locate default config.");
                return false;
            }
        }
        return true;
    }

    public static void initialize(ZipExtractorPlugin plugin) {
        if (!initialized) {
            instance = new ConfigManager(plugin);
            initialized = true;
        }
    }
    
    public static boolean reloadStatic() {
        if (!initialized)
            return false;
        return getInstance().reload();
    }
    
    public boolean reload() {
        try {
            loadConfig();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static ConfigManager getInstance() {
        return ConfigManager.instance;
    }

    /* Configuration Accessors */

    public String getSourceRaw() {
        if(config == null) {
            return null;
        } else {
            return this.config.getNode("file_settings", "source_directory").getString(null);
        }
    }

    public String getDestRaw() {
        if(config == null) {
            return null;
        } else {
            return config.getNode("file_settings", "destination_directory").getString(null);
        }
    }

    private Optional<File> runValidations(String abstractPath) {
        abstractPath = PathUtils.formatPath(abstractPath, false);
        File f = new File(abstractPath);

        return PathUtils.validateFilePath(f) ? Optional.of(f) : Optional.empty();
    }

    public Optional<File> getSourceFile() {
        return runValidations(getSourceRaw());
    }

    public Optional<File> getDestFile() {
        return runValidations(getDestRaw());
    }

    public boolean setSourcePath(String path) {
        if(config != null) {
            config.getNode("file_settings", "source_directory").setValue(path);
            try {
                plugin.getConfigLoader().save(config);
                return true;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }
        return false;
    }

    public boolean setDestPath(String path) {
        if(config != null) {
            config.getNode("file_settings", "destination_directory").setValue(path);
            try {
                plugin.getConfigLoader().save(config);
                return true;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }
        return false;
    }

    public boolean getLoggingProperty() {
        if(config == null) {
            return true;
        } else {
            return config.getNode("general_settings", "log_files").getBoolean(true);
        }
    }

    public boolean warnOnConflitcts() {
        if(config == null) {
            return true;
        } else {
            return config.getNode("general_settings", "warn_on_conflicts").getBoolean(true);
        }
    }
    
    public boolean tabCompleteFiles() {
        if(config == null) {
            return true;
        } else {
            return config.getNode("general_settings", "tab_complete_files").getBoolean(true);
        }
    }

    public boolean waitForTasksOnShutdown() {
        if(config == null) {
            return true;
        } else {
            return this.config.getNode("general_settings", "wait_on_shutdown").getBoolean(true);
        }
    }

    public int getMaxQueueSize() {
        if(config == null) {
            return 3;
        } else {
            int limit = config.getNode("general_settings", "max_queue_size").getInt(3);
            return limit > 0 ? limit : Integer.MAX_VALUE;
        }
    }

    public int getMaxPoolSize() {
        if(config == null) {
            return 1;
        } else {
            int limit = config.getNode("general_settings", "maximum_thread_pool").getInt(1);
            if (limit < 1)
                limit = 1;
            return limit;
        }
    }

    public double getSystemConfigVersion() {
        // TODO Will be implemented in a later version
        return 1.0;
    }

    public double getConfigVersion() {
        if(config == null) {
            return getSystemConfigVersion();
        } else {
            return config.getNode("ConfigVersion").getDouble(getSystemConfigVersion());
        }
    }

}
