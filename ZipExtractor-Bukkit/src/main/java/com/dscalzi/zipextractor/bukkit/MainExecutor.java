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

import java.util.List;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import com.dscalzi.zipextractor.bukkit.managers.ConfigManager;
import com.dscalzi.zipextractor.bukkit.util.BukkitCommandSender;
import com.dscalzi.zipextractor.core.command.CommandAdapter;
import com.dscalzi.zipextractor.core.managers.MessageManager;

public class MainExecutor implements CommandExecutor, TabCompleter {

    private CommandAdapter adapter;

    public MainExecutor(ZipExtractor plugin) {
        this.adapter = new CommandAdapter(plugin, MessageManager.inst(), ConfigManager.getInstance());
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        adapter.resolve(new BukkitCommandSender(sender), args);
        
        return true;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return adapter.tabComplete(new BukkitCommandSender(sender), args);
    }

}
