/*
 * This file is part of ZipExtractor.
 * Copyright (C) 2016-2021 Daniel D. Scalzi <https://github.com/dscalzi/ZipExtractor>
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

package com.dscalzi.zipextractor.bukkit.managers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.bukkit.configuration.file.FileConfiguration;

import com.dscalzi.zipextractor.bukkit.ZipExtractor;
import com.dscalzi.zipextractor.core.managers.IConfigManager;
import com.dscalzi.zipextractor.core.util.PathUtils;

public class ConfigManager implements IConfigManager {

    private static boolean initialized;
    private static ConfigManager instance;

    private ZipExtractor plugin;
    private FileConfiguration config;

    private ConfigManager(ZipExtractor plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    public void loadConfig() {
        verifyFile();
        this.plugin.reloadConfig();
        this.config = this.plugin.getConfig();
    }

    public void verifyFile() {
        File file = new File(this.plugin.getDataFolder(), "config.yml");
        if (!file.exists()) {
            this.plugin.saveDefaultConfig();
        }
    }

    public static void initialize(ZipExtractor plugin) {
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
        return this.config.getString("file_settings.source_directory", null);
    }

    public String getDestRaw() {
        return this.config.getString("file_settings.destination_directory", null);
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
        boolean ret = this.updateValue("file_settings.source_directory", path);
        this.plugin.saveDefaultConfig();
        return ret;
    }

    public boolean setDestPath(String path) {
        boolean ret = this.updateValue("file_settings.destination_directory", path);
        this.plugin.saveDefaultConfig();
        return ret;
    }

    public boolean getLoggingProperty() {
        return this.config.getBoolean("general_settings.log_files", true);
    }

    public boolean warnOnConflitcts() {
        return this.config.getBoolean("general_settings.warn_on_conflicts", true);
    }
    
    @Override
    public boolean tabCompleteFiles() {
        return this.config.getBoolean("general_settings.tab_complete_files", true);
    }

    public boolean waitForTasksOnShutdown() {
        return this.config.getBoolean("general_settings.wait_on_shutdown", true);
    }

    public int getMaxQueueSize() {
        int limit = this.config.getInt("general_settings.max_queue_size", 3);
        return limit > 0 ? limit : Integer.MAX_VALUE;
    }

    public int getMaxPoolSize() {
        int limit = this.config.getInt("general_settings.maximum_thread_pool", 1);
        if (limit < 1)
            limit = 1;
        return limit;
    }

    public double getSystemConfigVersion() {
        // TODO Will be implemented in a later version
        return 1.9;
    }

    public double getConfigVersion() {
        return config.getDouble("ConfigVersion", getSystemConfigVersion());
    }

    public boolean updateValue(String path, String value) {
        try (BufferedReader file = new BufferedReader(new FileReader(this.plugin.getDataFolder() + File.separator + "config.yml"))) {
            String line;
            StringBuilder input = new StringBuilder();

            List<String> paths = new ArrayList<>(Arrays.asList(path.split("\\.")));

            while ((line = file.readLine()) != null) {
                String lline = line.toLowerCase();
                if (!paths.isEmpty()) {
                    if (lline.contains(paths.get(0).toLowerCase())) {
                        paths.remove(0);
                        if (paths.isEmpty()) {
                            int firstIndex = line.indexOf('\"');
                            int lastIndex = line.lastIndexOf('\"');
                            if (firstIndex == -1) {
                                line = line.replaceAll(" +$", "");
                                line += " \"";
                                firstIndex = line.indexOf('\"');
                            }
                            if (lastIndex == -1) {
                                line += "\"";
                                lastIndex = line.lastIndexOf('\"');
                            }
                            line = line.substring(0, firstIndex + 1) + value + line.substring(lastIndex);
                        }
                    }
                }
                input.append(line).append('\n');
            }

            if (!paths.isEmpty())
                return false;

            try (FileOutputStream fileOut = new FileOutputStream(this.plugin.getDataFolder() + File.separator + "config.yml")) {
                fileOut.write(input.toString().getBytes());
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

}
