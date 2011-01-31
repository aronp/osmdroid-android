package org.andnav.osm.tileprovider;

import java.util.concurrent.atomic.AtomicInteger;

import gnu.trove.map.hash.TLongIntHashMap;
import gnu.trove.set.TLongSet;

public class LongObjectQueue<T> {
	
	Object [] objArray;
	long [] arrayToKey;
	TLongIntHashMap KeyToArray;
	AtomicInteger currFreePointer = new AtomicInteger(0);
	int mSize;
	
	public LongObjectQueue(int size)
	{
		objArray = new Object [size];
		arrayToKey = new long [size];
		KeyToArray = new TLongIntHashMap(size, 0.7f,-1,-1);
		mSize = size;
	}

	public LongObjectQueue()
	{
		this(100);
	}
	
	public synchronized void ensureCapacity(int newSize)
	{
		if (newSize > mSize)
		{
				Object [] newArray = new Object [newSize];
				long [] newarrayToKey = new long[newSize];

				for (int i = 0; i < mSize;i++)
				{
					newArray[i] = objArray[i];
					newarrayToKey[i] = arrayToKey[i];
				}
				objArray = newArray;
				arrayToKey = newarrayToKey;
				mSize = newSize;
		}
	}

	void putNew(long index, T obj)
	{
		int currPointer = (currFreePointer.getAndAdd(1));
		int myPointer = currPointer % mSize;
		
		long removeKey = arrayToKey[myPointer];
		
		// only start removing once we have looped.
		if (currPointer >= mSize)
		{
			KeyToArray.remove(removeKey);
		}
		
		arrayToKey[myPointer] = index;
		KeyToArray.put(index,myPointer);
		objArray[myPointer] = obj;
	}
	
	public synchronized void put(long index, T obj)
	{
			int arrayOffset = KeyToArray.get(index);
			
			if (arrayOffset >= 0)
			{
				objArray[arrayOffset]=obj;
			}
			else
			{
				putNew(index, obj);
			}
	}
	
	public synchronized boolean containsKey(long index)
	{
		return KeyToArray.containsKey(index);
	}
	
	@SuppressWarnings("unchecked")
	public synchronized T get(long index)
	{
		Object obj;
		int arrayOffset = KeyToArray.get(index);
		if (arrayOffset >= 0)
		{
			obj = objArray[arrayOffset];
		}
		else
		{
			obj = null;
		}
		return   (T)obj;
	}

	public synchronized TLongSet  keySet() {
		// TODO need to check
		return KeyToArray.keySet();
	}

	public synchronized void clear() {
		// TODO need to check clear.
		KeyToArray.clear();
		currFreePointer.set(0);
	}

}
