package org.andnav.osm.tileprovider;

import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gnu.trove.map.hash.TLongIntHashMap;
import gnu.trove.set.TLongSet;

public class LongObjectLRUCache<T> 
{
	
	protected static final Logger logger = LoggerFactory.getLogger(LongObjectLRUCache.class);

	Object [] objArray;
	long [] arrayToKey;
	links<T> [] mLinks;
	
	TLongIntHashMap KeyToArray;
	int mSize;
	int mFirstPtr = -1;
	int mLastPtr = -1;
	AtomicInteger currSize = new AtomicInteger(0);


	@SuppressWarnings("unchecked")
	public  LongObjectLRUCache(int size)
	{
		objArray = new Object[size];
		arrayToKey = new long [size];

		KeyToArray = new TLongIntHashMap(size, 0.7f,-1,-1);
		mLinks = new links[size];
		mSize = size;
	}

	public void DebugPrint(String name)
	{
		System.out.println("Name:" + name + " Size:"+currSize.get());
		System.out.println("First:" + mFirstPtr);
		System.out.println("Last:" + mLastPtr);
		for (int i = 0;i < currSize.get();i++)
		{
			System.out.println("Links:" + mLinks[i].prev +" "+ mLinks[i].next);
		}
	}

	@SuppressWarnings("unchecked")
	public synchronized void ensureCapacity(int reqsize)
	{
		if (reqsize > mSize)
		{
//			logger.debug("Ensure " + reqsize + " Current size" + mSize);

			Object [] tobjArray = new Object[reqsize];
			long [] tarrayToKey = new long[reqsize];
			links [] tmLinks = new links[reqsize];

			synchronized(this)
			{
				KeyToArray.ensureCapacity(reqsize);
				System.arraycopy(objArray, 0, tobjArray, 0, mSize);
				System.arraycopy(arrayToKey, 0, tarrayToKey, 0, mSize);
				System.arraycopy(mLinks, 0, tmLinks, 0, mSize);
				
				objArray = tobjArray;
				arrayToKey = tarrayToKey;
				mLinks = tmLinks;
				mSize = reqsize;
			}
		}
	}
	
	private void MoveToFront(int arrayIndex)
	{
		if (mFirstPtr == arrayIndex)
		{
			// already first in queue.
			return;
		}
		
		links<T> curr = mLinks[arrayIndex];
		
		// if we are moving the last one dont update previous
		if (mLastPtr != arrayIndex)
		{
			if (linkValid(curr.prev))
			{
				mLinks[curr.prev].next = curr.next;
			}
		}
		
		if (linkValid(curr.next))
		{
			mLinks[curr.next].prev = curr.prev;
		}
		
		// move on last ptr
		if (mLastPtr == arrayIndex)
		{
			if (linkValid(curr.next))
			{
				mLastPtr = curr.next;
			}
			else
			{
				// only have one in list.
			}
			
		}

		// move on first ptr
		if (mFirstPtr != arrayIndex)
		{
			links<T> first = mLinks[mFirstPtr];
			first.next = arrayIndex;
		}

		
		mLinks[arrayIndex].next = -1;
		mLinks[arrayIndex].prev = mFirstPtr;
		// move on first ptr
		mFirstPtr = arrayIndex;
	}
	
	private void replaceHere(long index, Object obj, int arrayIndex)
	{
		objArray[arrayIndex] =  obj;
		arrayToKey[arrayIndex] = index;
		KeyToArray.put(index, arrayIndex);
	}

	private void insertHere(long index, Object obj, int arrayIndex)
	{
		objArray[arrayIndex] = obj;
		arrayToKey[arrayIndex] = index;
		KeyToArray.put(index, arrayIndex);
		mLinks[arrayIndex] = new links<T>();
		
		mLinks[arrayIndex].prev = mFirstPtr;

		if (mFirstPtr >=0)
		{
			links<T> firstLink = this.mLinks[mFirstPtr];
			firstLink.next = arrayIndex;
		}

		mFirstPtr = arrayIndex;

		if (mLastPtr < 0)
		{
			mLastPtr = arrayIndex;
		}
	}

	
	public synchronized void put(long index, T obj)
	{
		synchronized(this)
		{

		int newSize = currSize.getAndAdd(1);
		
		if (newSize == 0)
		{
			// have to handle first one specially.
			synchronized(this)
			{
				insertHere(index,obj,0);
			}
		}
		else
		{
			// see if we already have item.
			int existingIndex = KeyToArray.get(index);
			
			if (linkValid(existingIndex))
			{
				// already have this, need to update object and move to front.
				currSize.getAndAdd(-1);
				objArray[existingIndex] = obj;
					MoveToFront(existingIndex);
			}
			else
			{
				// new item, add to end
				// 
				// First check if we are full.
				if (newSize >= mSize)
				{
					// decrease currSize back.
					currSize.getAndAdd(-1);
					
						// remove key of mLastPtr
						KeyToArray.remove(arrayToKey[mLastPtr]);
						replaceHere(index,obj,mLastPtr);
						MoveToFront(mLastPtr);
				}
				else
				{
						insertHere(index,obj,newSize);
				}
			}
		}
		}
	}
	
	@SuppressWarnings("unchecked")
	public synchronized T get(long index)
	{
		synchronized(this)
		{
			int arrayIndex =  KeyToArray.get(index);
			if (arrayIndex >= 0)
			{
				MoveToFront(arrayIndex);

//				logger.debug("Request " + index + " Found");

				return (T) objArray[arrayIndex]; 
			}
			else
			{
//				logger.debug("Request " + index + " Not Found");
				return null;
			}
		}
	}
	
	public synchronized boolean containsKey(long index)
	{
		return KeyToArray.containsKey(index);
	}

	
	public boolean checkQueue()
	{

		int queueSize = currSize.get();
		// first check that if size > 1 that first and last are not equal.
		if (queueSize > 1)
		{
			if (mFirstPtr == mLastPtr)
			{
				DebugPrint("First and Last");
				return false;
			}
		}
		
		int start = mLastPtr;
		int next = start;
		
		
		for (int i = 0; i< queueSize - 1;i++)
		{
		
			next = mLinks[next].next;
			if (next == -1)
			{
				DebugPrint("Couldnt follow list");
				return false;
			}
		}
		
		
		return true;
	}
	
	boolean linkValid(int link)
	{
		return (link >= 0);
	}
	
	@SuppressWarnings("hiding")
	class links<T> 
	{
		int prev = -1;
		int next = -1;
	}

	public synchronized boolean isEmpty() {
		return (this.KeyToArray.size() == 0);
	}

	public synchronized void clear() 
	{
			int currentSize = currSize.get();
			for (int i =0; i< currentSize;i++)
			{
				objArray[i] = null;
				arrayToKey[i] = -1;
				mLinks[i].next =  -1;
				mLinks[i].prev =  -1;
			}
			KeyToArray.clear();
			mFirstPtr = -1;
			mLastPtr = -1;
			currSize.set(0);
	}

	public synchronized TLongSet keySet() {
		return KeyToArray.keySet();
	}

	public int size() {
		return currSize.get();
	}

	@SuppressWarnings("unchecked")
	public synchronized void remove(long key) {
		
		// first find index of original
		int index = KeyToArray.get(key);
		if (index < 0)
			return;
		
		synchronized(this)
		{
		// link up either side.
			links curr = mLinks[index];
			if (linkValid(curr.prev))
				mLinks[curr.prev].next = curr.next;

			if (linkValid(curr.next))
				mLinks[curr.next].prev = curr.prev;

			// move last element here.
			int lastElement = currSize.get()-1;
			
			// greater than since we dont need to move if only one row. 
			if (lastElement > 0)
			{
				links endLink = mLinks[lastElement];

				if (linkValid(endLink.prev))
				{
					mLinks[endLink.prev].next = index;
				}

				if (linkValid(endLink.next))
				{
					mLinks[endLink.next].prev = index;
				}
				objArray[index] = objArray[lastElement];
				arrayToKey[index] = arrayToKey[lastElement];
				KeyToArray.put(arrayToKey[lastElement], index);
			}
			KeyToArray.remove(key);
			currSize.getAndDecrement();
		}
		
		
	}
}
			
