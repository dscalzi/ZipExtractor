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

package com.dscalzi.zipextractor.sponge;

import java.util.List;
import java.util.Optional;
import org.spongepowered.api.command.CommandCallable;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import com.dscalzi.zipextractor.core.command.CommandAdapter;
import com.dscalzi.zipextractor.core.managers.MessageManager;
import com.dscalzi.zipextractor.sponge.managers.ConfigManager;
import com.dscalzi.zipextractor.sponge.util.SpongeCommandSender;

public class MainExecutor implements CommandCallable {

    private CommandAdapter adapter;

    public MainExecutor(ZipExtractorPlugin plugin) {
        this.adapter = new CommandAdapter(plugin, MessageManager.inst(), ConfigManager.getInstance());
    }

    @Override
    public CommandResult process(CommandSource source, String arguments) {
        
        final String[] args = arguments.isEmpty() ? new String[0] : arguments.replaceAll("\\s{2,}", " ").split(" ");
        
        adapter.resolve(new SpongeCommandSender(source), args);
        
        return CommandResult.success();
    }

    @Override
    public List<String> getSuggestions(CommandSource source, String arguments, Location<World> targetPosition) {
        
        String[] argsDirty = arguments.replaceAll("\\s{2,}", " ").split(" ");
        String[] args = arguments.endsWith(" ") ? new String[argsDirty.length + 1] : argsDirty;
        if(args != argsDirty) {
            System.arraycopy(argsDirty, 0, args, 0, argsDirty.length);
            args[args.length-1] = "";
        }
        
        return adapter.tabComplete(new SpongeCommandSender(source), args);
    }

    @Override
    public boolean testPermission(CommandSource source) {
        return true;
    }

    @Override
    public Optional<Text> getShortDescription(CommandSource source) {
        return Optional.of(Text.of("ZipExtractor main command."));
    }

    @Override
    public Optional<Text> getHelp(CommandSource source) {
        return Optional.of(Text.of("Run /ZipExtractor to view usage."));
    }

    @Override
    public Text getUsage(CommandSource source) {
        return Text.of("/ZipExtractor <args>");
    }

}
