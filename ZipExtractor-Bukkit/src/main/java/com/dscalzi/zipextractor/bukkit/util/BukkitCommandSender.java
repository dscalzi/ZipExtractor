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

package com.dscalzi.zipextractor.bukkit.util;

import org.bukkit.ChatColor;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;

import com.dscalzi.zipextractor.core.util.ICommandSender;

public class BukkitCommandSender implements ICommandSender {

    private CommandSender cs;
    
    public BukkitCommandSender(CommandSender sender) {
        cs = sender;
    }
    
    @Override
    public void sendMessage(String msg) {
        cs.sendMessage(ChatColor.translateAlternateColorCodes('&', msg));
    }

    @Override
    public boolean isConsole() {
        return cs instanceof ConsoleCommandSender;
    }
    
    @Override
    public boolean isCommandBlock() {
        return cs instanceof BlockCommandSender;
    }

    @Override
    public boolean hasPermission(String perm) {
        return cs.hasPermission(perm);
    }

    @Override
    public String getName() {
        return cs.getName();
    }

}
