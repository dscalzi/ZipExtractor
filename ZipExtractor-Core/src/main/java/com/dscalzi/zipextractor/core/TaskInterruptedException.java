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

package com.dscalzi.zipextractor.core;

public class TaskInterruptedException extends RuntimeException {

    private static final long serialVersionUID = 3942881135656327340L;

    public TaskInterruptedException() {

    }

    public TaskInterruptedException(String paramString) {
        super(paramString);
    }

    public TaskInterruptedException(String paramString, Throwable paramThrowable) {
        super(paramString, paramThrowable);
    }

    public TaskInterruptedException(Throwable paramThrowable) {
        super(paramThrowable);
    }

}
