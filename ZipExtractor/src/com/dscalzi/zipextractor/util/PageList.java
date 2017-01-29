package com.dscalzi.zipextractor.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class PageList<T> implements Iterable<List<T>>{
	
	private static final int GLOBAL_DEFAULT_PAGE_SIZE = 5;
	
	private final int DEFAULT_PAGE_SIZE;
	private final List<List<T>> cStore;
	
	public PageList(){
		this(null);
	}
	
	public PageList(List<T> list){
		this(GLOBAL_DEFAULT_PAGE_SIZE, list);
	}
	
	public PageList(int pageSize){
		this(pageSize, null);
	}
	
	public PageList(int pageSize, List<T> list){
		this.DEFAULT_PAGE_SIZE = pageSize;
		this.cStore = new ArrayList<List<T>>();
		if(list != null) this.importFromList(list);
	}
	
	private void importFromList(List<T> list){
		if(!cStore.isEmpty())
			cStore.clear();
		
		for(int i=0; i<(list.size()/DEFAULT_PAGE_SIZE)+1; ++i){
			List<T> page = new ArrayList<T>();
			for(int k=0; k<DEFAULT_PAGE_SIZE; ++k){
				int realIndex = (i*DEFAULT_PAGE_SIZE)+k;
				if(realIndex < list.size())
					page.add(list.get(realIndex));
				else
					break;
			}
			if(!page.isEmpty())
				cStore.add(page);
		}
	}
	
	public T add(T e){
		return add(e, false);
	}
	
	public T add(T e, boolean overflow){
		if(cStore.size() > 0){
			List<T> page = cStore.get(cStore.size()-1);
			if(overflow || page.size() < DEFAULT_PAGE_SIZE){
				page.add(e);
				return e;
			}
		}
		List<T> newPage = new ArrayList<T>();
		newPage.add(e);
		cStore.add(newPage);
		return e;
	}
	
	public List<T> getPage(int page){
		return getPage(page, true);
	}
	
	public List<T> getPage(int page, boolean includeNull){
		try{
			List<T> p = new ArrayList<T>(cStore.get(page));
			if(!includeNull)
				p.removeAll(Collections.singleton(null));
			return Collections.unmodifiableList(p);
		} catch (IndexOutOfBoundsException e){
			throw new IndexOutOfBoundsException("Invalid page number. Index: " + page + ", Size: " + cStore.size());
		}
	}
	
	public List<T> getMutablePage(int page){
		try{
			return cStore.get(page);
		} catch (IndexOutOfBoundsException e){
			throw new IndexOutOfBoundsException("Invalid page number. Index: " + page + ", Size: " + cStore.size());
		}
	}
	
	public int size(){
		return cStore.size();
	}
	
	@Override
	public String toString(){
		String ret = "{";
		for(List<T> page : cStore){
			ret += page.toString() + ",";
		}
		ret = ret.substring(0, ret.length()-1) + "}";
		return ret;
	}

	@Override
	public Iterator<List<T>> iterator() {
		
		int currentSize = this.size();
		
		Iterator<List<T>> it = new Iterator<List<T>>() {

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
        return it;
	}
	
}
