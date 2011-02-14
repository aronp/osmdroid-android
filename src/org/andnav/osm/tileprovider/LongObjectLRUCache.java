package org.andnav.osm.tileprovider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gnu.trove.map.hash.TLongIntHashMap;
import gnu.trove.set.TLongSet;

public class LongObjectLRUCache<T> 
{
	Object [] objArray;
	long [] arrayToKey;
	links<T> [] mLinks;
	
// 	protected static final Logger logger = LoggerFactory.getLogger(LongObjectLRUCache.class);
	private final static boolean DEBUG_QUEUE = false;
	TLongIntHashMap KeyToArray;
	int mSize;
	int mFirstPtr = -1;
	int mLastPtr = -1;
//	AtomicInteger currSize = new AtomicInteger(0);
	int currSize = 0;


	@SuppressWarnings("unchecked")
	public  LongObjectLRUCache(int size)
	{
	//	logger.debug("Created with size " +size);
		objArray = new Object[size];
		arrayToKey = new long [size];

		KeyToArray = new TLongIntHashMap(size, 0.7f,-1,-1);
		mLinks = new links[size];
		for (int i = 0;i < size;i++)
		{
			mLinks[i] = new links<T>();
		}
		mSize = size;
	}

	public synchronized void DebugPrint(String name)
	{
		System.out.println("Name:" + name + " Size:"+currSize);
		System.out.println("First:" + mFirstPtr);
		System.out.println("Last:" + mLastPtr);
		for (int i = 0;i < currSize;i++)
		{
			System.out.println("Links:" + mLinks[i].prev +" "+ mLinks[i].next + " " + arrayToKey[i]);
		}
	}

	@SuppressWarnings("unchecked")
	public synchronized void ensureCapacity(int reqsize)
	{
//		logger.debug("ensureCapacity " +reqsize);
		
		if (reqsize > mSize)
		{

			Object [] tobjArray = new Object[reqsize];
			long [] tarrayToKey = new long[reqsize];
			links [] tmLinks = new links[reqsize];

			KeyToArray.ensureCapacity(reqsize);
			System.arraycopy(objArray, 0, tobjArray, 0, mSize);
			System.arraycopy(arrayToKey, 0, tarrayToKey, 0, mSize);
			System.arraycopy(mLinks, 0, tmLinks, 0, mSize);

			for (int i = mSize;i < reqsize;i++)
			{
				tmLinks[i] = new links<T>();
			}

			objArray = tobjArray;
			arrayToKey = tarrayToKey;
			mLinks = tmLinks;
			mSize = reqsize;
		}
		
		if (DEBUG_QUEUE)
		checkQueue();
	}
	
	public synchronized long getHeadKey()
	{
//		logger.debug("getHeadKey ");
		if (currSize > 0)
		{
			int offset = KeyToArray.get(arrayToKey[mFirstPtr]);
			if (offset != mFirstPtr)
			{
				int problem = 1;
				problem += 1;
			}

			if (DEBUG_QUEUE)
				checkQueue();
			
			return arrayToKey[mFirstPtr];
		}
		else
		{
			if (DEBUG_QUEUE)
				checkQueue();

			return -1;
		}
	}

	public synchronized long getLastKey()
	{
//		logger.debug("getLastKey ");
		if (currSize > 0)
		{
			int offset = KeyToArray.get(arrayToKey[mLastPtr]);
			if (offset != mLastPtr)
			{
				int problem = 1;
				problem += 1;
			}

			if (DEBUG_QUEUE)
				checkQueue();
			
			return arrayToKey[mLastPtr];
		}
		else
		{
			if (DEBUG_QUEUE)
				checkQueue();

			return -1;
		}
	}


	public synchronized T put(long index, T obj)
	{
//		logger.debug("Put " + index );
		int newSize = currSize;
		currSize += 1;

		if (newSize == 0)
		{
			// have to handle first one specially.
			insertHere(index,obj,0);
		}
		else
		{
			// see if we already have item.
			int existingIndex = KeyToArray.get(index);

			if (linkValid(existingIndex))
			{
				// already have this, need to update object and move to front.
				currSize = currSize -1;
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
					currSize = currSize -1;

					if (KeyToArray.get(arrayToKey[mLastPtr]) < 0)
					{
						int problem = 1;
						problem += 1;
					}
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

		if (DEBUG_QUEUE)
			checkQueue();

		return obj;
	}
	

	@SuppressWarnings("unchecked")
	public synchronized T get(long index)
	{
//		logger.debug("Get " + index );
		int arrayIndex =  KeyToArray.get(index);
		if (arrayIndex >= 0)
		{
			MoveToFront(arrayIndex);
			
			if (DEBUG_QUEUE)
				checkQueue();

			return (T) objArray[arrayIndex]; 
		}
		else
		{
			if (DEBUG_QUEUE)
				checkQueue();

			return null;
		}
	}
	
	public synchronized boolean containsKey(long index)
	{
//		logger.debug("Contains Key " + index );
		return KeyToArray.containsKey(index);
	}

	
	public synchronized boolean checkQueue()
	{
		boolean result = checkQueueDetails();
		
		if (result)
		{
		
		}
		else
		{
			int problem = 0;
			problem += 1;
		
		}
		return result;
	}


	public synchronized boolean isEmpty() {
//		logger.debug("Is empty "  );
		return (currSize == 0);
	}

	public synchronized void clear() 
	{
//		logger.debug("Clear "  );
			int currentSize = currSize;
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
			currSize = 0;

			if (DEBUG_QUEUE)
				checkQueue();

	}

	public synchronized TLongSet keySet() {
//		logger.debug("KeySet "  );
		return KeyToArray.keySet();
	}

	public int size() {
		return currSize;
	}

	@SuppressWarnings("unchecked")
	public synchronized T remove(long key) 
	{
//		logger.debug("Remove " + key );
	
		T retval = null;
		// first find index of original
		int index = KeyToArray.get(key);
		if (index < 0)
			return retval;
		
		retval = (T) objArray[index];

		// link up either side.
		links curr = mLinks[index];
		if (linkValid(curr.prev))
		{
			mLinks[curr.prev].next = curr.next;
		}
		
		if (linkValid(curr.next))
		{
			mLinks[curr.next].prev = curr.prev;
		}


		// check on first and lastptr
		if (index == mFirstPtr)
		{
			if (linkValid(curr.prev))
				mFirstPtr = curr.prev;
			else
				mFirstPtr = -1;
		}

		if (index == mLastPtr)
		{
			if (linkValid(curr.next))
				mLastPtr = curr.next;
			else
				mLastPtr = -1;
		}

		

		// move last element here.
		int lastElement = currSize-1;

		// greater than since we dont need to move if only one row. 
		if (lastElement > 0 && lastElement != index)
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
			mLinks[index].next = mLinks[lastElement].next;
			mLinks[index].prev = mLinks[lastElement].prev;
			
			// clear old stuff;
			objArray[lastElement] = null;
			mLinks[lastElement].next = -1;
			mLinks[lastElement].prev = -1;
			arrayToKey[lastElement] = -1; 
			
			
			// move on first and last
			if (lastElement == mFirstPtr)
			{
				mFirstPtr = index;
			}
			
			// move on first and last
			if (lastElement == mLastPtr)
			{
				mLastPtr = index;
			}

		}
		
		KeyToArray.remove(key);
		currSize = currSize -1;

		if (DEBUG_QUEUE)
			checkQueue();
		
		removeResource(retval);
		return retval;

	}

	public void removeResource(T obj)
	{
		
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
		
		if (DEBUG_QUEUE)
			checkQueue();

	}
	
	@SuppressWarnings("unchecked")
	private void replaceHere(long index, Object obj, int arrayIndex)
	{
		removeResource((T) objArray[arrayIndex]);

		objArray[arrayIndex] =  obj;
		arrayToKey[arrayIndex] = index;
		KeyToArray.put(index, arrayIndex);
	}

	private void insertHere(long index, Object obj, int arrayIndex)
	{
		objArray[arrayIndex] = obj;
		arrayToKey[arrayIndex] = index;
		KeyToArray.put(index, arrayIndex);
		// mLinks[arrayIndex] = new links<T>();
		
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

	
	
	
	
	private boolean checkQueueDetails()
	{

		int queueSize = currSize;
		// first check that if size > 1 that first and last are not equal.
		if (queueSize > 1)
		{
			if (mFirstPtr == mLastPtr)
			{
				DebugPrint("First and Last");
				return false;
			}
		}
		
		// check forwards
		{
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
		}
		

		// check backwards
		{
			int start = mFirstPtr;
			int next = start;

			for (int i = 0; i< queueSize - 1;i++)
			{

				next = mLinks[next].prev;
				if (next == -1)
				{
					DebugPrint("Couldnt follow list");
					return false;
				}
			}
		}

		
		
		// check every key is in map.
		
		for (int i = 0; i< queueSize - 1;i++)
		{
			long key = arrayToKey[i];
			if (KeyToArray.containsKey(key))
			{
				int index = KeyToArray.get(key);
				if (index != i)
				{
					DebugPrint("Key not pointing correctly index= " + index +" ,i = "+i);
					return false;
				}
			}
			else
			{					
				DebugPrint("Key not in map " + key);
				return false;
			}
		}
			

		TLongSet keyset = KeyToArray.keySet();

		long keys[]  = keyset.toArray();
		// check that every key in map is in array.
		for (long key : keys)
		{
			int index = KeyToArray.get(key);
			if (index < 0)
			{
				DebugPrint("Key less than 0 ");
				return false;
			}
			if (arrayToKey[index] != key)
			{
				DebugPrint("Mismatched keys ");
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

}
			
