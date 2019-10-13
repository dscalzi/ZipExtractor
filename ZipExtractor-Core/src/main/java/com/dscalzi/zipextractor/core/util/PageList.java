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

package com.dscalzi.zipextractor.core.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class PageList<T> implements Iterable<List<T>> {

    private static final int GLOBAL_DEFAULT_PAGE_SIZE = 5;

    private final int DEFAULT_PAGE_SIZE;
    private final List<List<T>> cStore;

    public PageList() {
        this(null);
    }

    public PageList(List<T> list) {
        this(GLOBAL_DEFAULT_PAGE_SIZE, list);
    }

    public PageList(int pageSize) {
        this(pageSize, null);
    }

    public PageList(int pageSize, List<T> list) {
        this.DEFAULT_PAGE_SIZE = pageSize;
        this.cStore = new ArrayList<>();
        if (list != null)
            this.importFromList(list);
    }

    private void importFromList(List<T> list) {
        if (!cStore.isEmpty())
            cStore.clear();

        for (int i = 0; i < (list.size() / DEFAULT_PAGE_SIZE) + 1; ++i) {
            List<T> page = new ArrayList<>();
            for (int k = 0; k < DEFAULT_PAGE_SIZE; ++k) {
                int realIndex = (i * DEFAULT_PAGE_SIZE) + k;
                if (realIndex < list.size())
                    page.add(list.get(realIndex));
                else
                    break;
            }
            if (!page.isEmpty())
                cStore.add(page);
        }
    }

    public T add(T e) {
        return add(e, false);
    }

    public T add(T e, boolean overflow) {
        if (!cStore.isEmpty()) {
            List<T> page = cStore.get(cStore.size() - 1);
            if (overflow || page.size() < DEFAULT_PAGE_SIZE) {
                page.add(e);
                return e;
            }
        }
        List<T> newPage = new ArrayList<>();
        newPage.add(e);
        cStore.add(newPage);
        return e;
    }

    public List<T> getPage(int page) {
        return getPage(page, true);
    }

    public List<T> getPage(int page, boolean includeNull) {
        try {
            List<T> p = new ArrayList<>(cStore.get(page));
            if (!includeNull)
                p.removeAll(Collections.singleton(null));
            return Collections.unmodifiableList(p);
        } catch (IndexOutOfBoundsException e) {
            throw new IndexOutOfBoundsException("Invalid page number. Index: " + page + ", Size: " + cStore.size());
        }
    }

    public List<T> getMutablePage(int page) {
        try {
            return cStore.get(page);
        } catch (IndexOutOfBoundsException e) {
            throw new IndexOutOfBoundsException("Invalid page number. Index: " + page + ", Size: " + cStore.size());
        }
    }

    public int size() {
        return cStore.size();
    }

    @Override
    public String toString() {
        StringBuilder ret = new StringBuilder("{");
        for (List<T> page : cStore) {
            ret.append(page.toString()).append(",");
        }
        ret = new StringBuilder(ret.substring(0, ret.length() - 1) + "}");
        return ret.toString();
    }

    @Override
    public Iterator<List<T>> iterator() {

        int currentSize = this.size();

        return new Iterator<List<T>>() {

            private int currentIndex = 0;

            @Override
            public boolean hasNext() {
                return currentIndex < currentSize && cStore.get(currentIndex) != null;
            }

            @Override
            public List<T> next() {
                return cStore.get(currentIndex++);
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

}
