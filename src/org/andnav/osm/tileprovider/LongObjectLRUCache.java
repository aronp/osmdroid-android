package org.andnav.osm.tileprovider;

import java.util.concurrent.atomic.AtomicInteger;

import gnu.trove.map.hash.TLongIntHashMap;

public class LongObjectLRUCache 
{
	Object [] objArray;
	long [] arrayToKey;
	links [] mLinks;
//	long [] prevLinks;
//	long [] nextLinks;
	
	TLongIntHashMap KeyToArray;
	int mSize;
	int mFirstPtr = -1;
	int mLastPtr = -1;
	AtomicInteger currSize = new AtomicInteger(0);

	boolean linkValid(int link)
	{
		return (link >= 0);
	}
	
	class links 
	{
		int prev = -1;
		int next = -1;
	};

	public LongObjectLRUCache(int size)
	{
		objArray = new Object[size];
		arrayToKey = new long [size];

//		prevLinks = new long [size];
//		nextLinks = new long [size];

		KeyToArray = new TLongIntHashMap(size, 0.7f,-1,-1);
		mLinks = new links[size];
		mSize = size;
		for (int i = 0;i<size;i++)
		{
			mLinks[i] = new links();
//			prevLinks[i] = -1;
//			nextLinks[i] = -1;
		}
	}

	public void DebugPrint()
	{
		System.out.println("First:" + mFirstPtr);
		System.out.println("Last:" + mLastPtr);
		for (int i = 0;i < currSize.get();i++)
		{
			System.out.println("Links:" + mLinks[i].prev +" "+ mLinks[i].next);
		}
	}

	void ensureCapacity(int size)
	{
		// objArray.ensureCapacity(size);

	}
	
	private void MoveToFront(int arrayIndex)
	{
		links curr = mLinks[arrayIndex];
		
		if (linkValid(curr.prev))
		{
			mLinks[curr.prev].next = curr.next;
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
			links first = mLinks[mFirstPtr];
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
		
		mLinks[arrayIndex].prev = mFirstPtr;

		if (mFirstPtr >=0)
		{
			links firstLink = this.mLinks[mFirstPtr];
			firstLink.next = arrayIndex;
		}

		mFirstPtr = arrayIndex;

		if (mLastPtr < 0)
		{
			mLastPtr = arrayIndex;
		}
	}

	
	public void put(long index, Object obj)
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
				objArray[existingIndex] = obj;
				synchronized(this)
				{
					MoveToFront(existingIndex);
				}
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
					
					synchronized(this)
					{
						// remove key of mLastPtr
						KeyToArray.remove(arrayToKey[mLastPtr]);
						replaceHere(index,obj,mLastPtr);
						MoveToFront(mLastPtr);
					}
				}
				else
				{
					synchronized(this)
					{
						insertHere(index,obj,newSize);
					}
				}
			}
		}
	}
	
	public Object get(long index)
	{
		int arrayIndex =  KeyToArray.get(index);
		if (arrayIndex >= 0)
		{
			synchronized(this)
			{
				MoveToFront(arrayIndex);
			}
			
			return objArray[arrayIndex]; 
		}
		else
		{
			return null;
		}
	}
	
	public boolean containsKey(long index)
	{
		return KeyToArray.containsKey(index);
	}

	}
			
