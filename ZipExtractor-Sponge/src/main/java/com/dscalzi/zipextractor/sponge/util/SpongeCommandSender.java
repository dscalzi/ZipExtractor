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

package com.dscalzi.zipextractor.sponge.util;

import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.source.ConsoleSource;
import org.spongepowered.api.text.serializer.TextSerializers;

import com.dscalzi.zipextractor.core.util.BaseCommandSender;

public class SpongeCommandSender implements BaseCommandSender {

    private CommandSource cs;
    
    public SpongeCommandSender(CommandSource cs) {
        this.cs = cs;
    }
    
    @Override
    public void sendMessage(String msg) {
        cs.sendMessage(TextSerializers.FORMATTING_CODE.deserialize(msg));
    }

    @Override
    public boolean isConsole() {
        return cs instanceof ConsoleSource;
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
