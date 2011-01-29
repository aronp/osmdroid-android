package org.andnav.osm.tileprovider;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class OSMThreadFactory {
	public final ThreadFactory mDefaultThreadFactory;
	public final ThreadGroup mThreadPool;
	public final ExecutorService mExeServicenew;
	public final int mNumber;
	public final String mName;
	
	public OSMThreadFactory(final String name, int number)
	{
		mDefaultThreadFactory =
			new ThreadFactory() {
	        private final AtomicInteger mCount = new AtomicInteger(1);

	        public Thread newThread(Runnable r) {
	        	Thread t =new Thread(mThreadPool, r, name+ "#" + mCount.getAndIncrement()); 
	        	t.setPriority(Thread.NORM_PRIORITY-1);
	            return t;
	        }
	    };
	    mThreadPool = new ThreadGroup(name);
	    
		mExeServicenew = 
			Executors.newFixedThreadPool(number, mDefaultThreadFactory);

	    mNumber = number;
	    mName = name;
	}

	public int numThreads()
	{
		return mNumber;
	}
	
	public ExecutorService getExecutorService()
	{
		return mExeServicenew;
	}
	
	public int numberActive()
	{
		return mThreadPool.activeCount();
	}

	public int getActiveCount() {
		return mThreadPool.activeCount();
		
	}

	public boolean areThreadsFree() {
		
		
		return getActiveCount() < mNumber;
		
	}
}
