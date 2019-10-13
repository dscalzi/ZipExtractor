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

package com.dscalzi.zipextractor.core.util;

import java.util.List;

public interface IPlugin {

    String getVersion();
    
    void info(String msg);
    
    void warn(String msg);
    
    void severe(String msg);
    
    void severe(String msg, Throwable t);
    
    String getName();
    
    String getPluginDirectory();
    
    boolean reload();
    
    List<? extends ICommandSender> getOnlinePlayers();
    
}
