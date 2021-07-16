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

package com.dscalzi.zipextractor.core.util;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public final class ReflectionUtil {

    // Caches
    private final static Map<String, Class<?>> cachedClasses;
    private final static Map<Class<?>, Map<String, Class<?>>> declaredClasses;

    private final static Map<Class<?>, Map<String, Method>> cachedMethods;

    static {
        cachedClasses = new HashMap<>();
        declaredClasses = new HashMap<>();
        cachedMethods = new HashMap<>();
    }

    public static Class<?> getClass(String declaration) {

        if (cachedClasses.containsKey(declaration))
            return cachedClasses.get(declaration);

        Class<?> clazz;

        try {
            clazz = Class.forName(declaration);
        } catch (Throwable e) {
            e.printStackTrace();
            return cachedClasses.put(declaration, null);
        }

        cachedClasses.put(declaration, clazz);
        return clazz;
    }

    public static Class<?> getDeclaredClass(Class<?> origin, String className) {
        if (!declaredClasses.containsKey(origin))
            declaredClasses.put(origin, new HashMap<>());

        Map<String, Class<?>> classMap = declaredClasses.get(origin);

        if (classMap.containsKey(className))
            return classMap.get(className);

        for(Class<?> clazz : origin.getDeclaredClasses()) {
            if(clazz.getSimpleName().equals(className)) {
                classMap.put(className, clazz);
                declaredClasses.put(origin, classMap);
                return clazz;
            }
        }

        classMap.put(className, null);
        declaredClasses.put(origin, classMap);
        return null;
    }

    public static Method getMethod(Class<?> clazz, String methodName, Class<?>... params) {
        if (!cachedMethods.containsKey(clazz))
            cachedMethods.put(clazz, new HashMap<>());

        Map<String, Method> methods = cachedMethods.get(clazz);

        if (methods.containsKey(methodName))
            return methods.get(methodName);

        try {
            Method method = clazz.getMethod(methodName, params);
            methods.put(methodName, method);
            cachedMethods.put(clazz, methods);
            return method;
        } catch (Throwable e) {
            e.printStackTrace();
            methods.put(methodName, null);
            cachedMethods.put(clazz, methods);
            return null;
        }
    }

}