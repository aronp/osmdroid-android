package org.andnav.osm.tileprovider;

import java.io.InputStream;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import org.andnav.osm.tileprovider.constants.OpenStreetMapTileProviderConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.graphics.drawable.Drawable;

/**
 * An abstract child class of {@link OpenStreetMapTileProvider} which acquires tile images
 * asynchronously from some network source.
 * The key unimplemented methods are 'threadGroupname' and 'getTileLoader'.
 */
public abstract class OpenStreetMapAsyncTileProvider implements OpenStreetMapTileProviderConstants {


	public static final ThreadFactory defaultThreadFactory = new ThreadFactory() {
        private final AtomicInteger mCount = new AtomicInteger(1);

        public Thread newThread(Runnable r) {
            return new Thread(r, "OsmAsyncTask #" + mCount.getAndIncrement());
        }
    };

	static final ExecutorService mExeServicenew = 
		Executors.newFixedThreadPool(10, defaultThreadFactory);
	/**
	 * 
	 * @return
	 */
	protected abstract String threadGroupName();

	/**
	 * It is expected that the implementation will construct an internal member which
	 * internally implements a {@link TileLoader}.  This method is expected to return
	 * a that internal member to methods of the parent methods.
	 *  
	 * @return the internal member of this tile provider.
	 */
	protected abstract Runnable getTileLoader();

	protected static final Logger logger = LoggerFactory.getLogger(OpenStreetMapAsyncTileProvider.class);

	private final int mThreadPoolSize;
	protected int mCurrentThreadPoolSize;
	
	protected final ThreadGroup mThreadPool = new ThreadGroup(threadGroupName());
	protected final ConcurrentHashMap<OpenStreetMapTile, Object> mWorking;
	protected final LinkedHashMap<OpenStreetMapTile, Object> mPending;
	protected static final Object PRESENT = new Object();

	protected final IOpenStreetMapTileProviderCallback mCallback;

	public  class SimpleThreadFactory implements ThreadFactory 
	{
		public Thread newThread(Runnable r) {
			Thread t = new Thread(mThreadPool, r);
			t.setPriority(Thread.NORM_PRIORITY);
			return t;
		}
	}

	
	public OpenStreetMapAsyncTileProvider(final IOpenStreetMapTileProviderCallback pCallback, final int aThreadPoolSize, final int aPendingQueueSize) 
	{
		mCallback = pCallback;
		mThreadPoolSize = aThreadPoolSize;
		
		
		mCurrentThreadPoolSize = aThreadPoolSize;
		mWorking = new ConcurrentHashMap<OpenStreetMapTile, Object>();
		mPending = new LinkedHashMap<OpenStreetMapTile, Object>(aPendingQueueSize + 2, 0.1f, true) {
			private static final long serialVersionUID = 6455337315681858866L;
			@Override
			protected boolean removeEldestEntry(Entry<OpenStreetMapTile, Object> pEldest) {
				return size() > aPendingQueueSize;
			}
		};
	}

	public void loadMapTileAsync(final OpenStreetMapTile aTile) {

		final int activeCount = mThreadPool.activeCount();

		synchronized (mPending) {
			// sanity check
			if (activeCount == 0 && !mPending.isEmpty()) {
				logger.warn("Unexpected - no active threads but pending queue not empty");
				
				for (OpenStreetMapTile tile : mPending.keySet())
				{
					logger.warn("Still in queue" + tile.toString());
				}
				for (OpenStreetMapTile tile : mWorking.keySet())
				{
					logger.warn("Thinks we are working on " + tile.toString());
				}

				// clearQueue();
			}

			// this will put the tile in the queue, or move it to the front of
			// the queue if it's already present
			mPending.put(aTile, PRESENT);
		}

		if (DEBUGMODE)
			logger.debug(activeCount + " active threads");

		mExeServicenew.execute(getTileLoader()); 
	}

	private void clearQueue() {
		synchronized (mPending) {
			mPending.clear();
		}
		mWorking.clear();
	}

	/**
	 * Stops all workers - we're shutting down.
	 */
	public void stopWorkers()
	{
		this.clearQueue();
		this.mThreadPool.interrupt();
	}

	/**
	 * Load the requested tile.
     * An abstract internal class whose objects are used by worker threads to acquire tiles from servers.
     * It processes tiles from the 'pending' set to the 'working' set as they become available.
     * The key unimplemented method is 'loadTile'.
	 * 
	 * @param aTile the tile to load
	 * @throws CantContinueException if it is not possible to continue with processing the queue
	 * @throws CloudmadeException if there's an error authorizing for Cloudmade tiles
	 */
	protected abstract class TileLoader implements Runnable {
		
		/**
		 * The key unimplemented method.
		 * 
		 * @param aTile
		 * @throws CantContinueException
		 */
		protected abstract void loadTile(OpenStreetMapTile aTile) throws CantContinueException;

		private OpenStreetMapTile nextTile() {

			synchronized (mPending) {
				OpenStreetMapTile result = null;

				// get the most recently accessed tile
				// - the last item in the iterator that's not already being processed
				Iterator<OpenStreetMapTile> iterator = mPending.keySet().iterator();

				// TODO this iterates the whole list, make this faster...
				while (iterator.hasNext()) {
					try {
						final OpenStreetMapTile tile = iterator.next();
						if (!mWorking.containsKey(tile)) {
							result = tile;
						}
					} catch (final ConcurrentModificationException e) {
						if (DEBUGMODE)
							logger.warn("ConcurrentModificationException break: " + (result != null));

						// if we've got a result return it, otherwise try again
						if (result != null) {
							break;
						} else {
							iterator = mPending.keySet().iterator();
						}
					}
				}

				if (result != null)
				{
					mWorking.put(result, PRESENT);
				}

				if (result == null)
				{
					// need to do some checking see if this is working.
					if (DEBUGMODE)
					{
						if (!mPending.isEmpty())
						{
							logger.info("Returned null with " + mPending.size() + " in queue");
						}
					}
				}
				return result;
			}
		}

		/**
		 * A tile has loaded.
		 * @param aTile the tile that has loaded
		 * @param aTilePath the path of the file.
		 */
		public void tileLoaded(final OpenStreetMapTile aTile, final String aTilePath) {
			synchronized (mPending) {
				mPending.remove(aTile);
			}
			mWorking.remove(aTile);

			mCallback.mapTileRequestCompleted(aTile, aTilePath);
		}

		/**
		 * A tile has loaded.
		 * @param aTile the tile that has loaded
		 * @param aTileInputStream the input stream of the file.
		 */
		public void tileLoaded(final OpenStreetMapTile aTile, final InputStream aTileInputStream) {
			synchronized (mPending) {
				mPending.remove(aTile);
			}
			mWorking.remove(aTile);

			mCallback.mapTileRequestCompleted(aTile, aTileInputStream);
		}

		/**
		 * A tile has loaded.
		 * @param aTile the tile that has loaded
		 * @param aRefresh whether to redraw the screen so that new tiles will be used
		 */
		public void tileLoaded(final OpenStreetMapTile aTile, final boolean aRefresh) {
			synchronized (mPending) {
				mPending.remove(aTile);
			}
			mWorking.remove(aTile);

			mCallback.mapTileRequestCompleted(aTile);
		}

		/**
		 * A tile has loaded.
		 * @param aTile the tile that has loaded
		 * @param aDrawable the drawable that has returned.
		 */
		public void tileLoaded(final OpenStreetMapTile aTile, final Drawable aDrawable) {
			synchronized (mPending) {
				mPending.remove(aTile);
			}
			mWorking.remove(aTile);

			mCallback.mapTileRequestCompleted(aTile, aDrawable);
		}

		
		/**
		 * A tile has not loaded.
		 * @param aTile the tile that has loaded
		 */
		public void tileNotLoaded(final OpenStreetMapTile aTile) 
		{
			if (DEBUGMODE)
				logger.debug("Got a tile not loaded");
			
			mWorking.remove(aTile);
		}
		
		/**
		 * A tile has been requested from another mechanism.
		 * That one will tell screen to refresh
		 * @param aTile the tile that has loaded
		 */

		public void tilePassedOn(final OpenStreetMapTile aTile) 
		{
			synchronized (mPending) {
				mPending.remove(aTile);
			}
			mWorking.remove(aTile);
		}

		
		
		/**
		 * This is a functor class of type Runnable.
		 * The run method is the encapsulated function.
		 */
		@Override
		final public void run() {

			OpenStreetMapTile tile;
			boolean okay = true;
			
			while ((tile = nextTile()) != null && okay) {
				if (DEBUGMODE)
					logger.debug("Next tile: " + tile);
				try {
					loadTile(tile);
					// got a tile so back up and running
					mCurrentThreadPoolSize = mThreadPoolSize;
				} catch (final CantContinueException e) {
					logger.info("Tile loader can't continue", e);
					mCurrentThreadPoolSize = 1;
					sleep();
				} catch (final Throwable e) {
					logger.error("Error downloading tile: " + tile, e);
					mCurrentThreadPoolSize = 1;
					sleep();
				}
				
				if (mThreadPool.activeCount() > mCurrentThreadPoolSize)
				{
					// TODO this doesnt work
				 // okay = false;
				}

			}
			if (DEBUGMODE)
				logger.debug(okay ? "No more tiles" : "Reducing number of threads");
		}
	}

	public class CantContinueException extends Exception {
		private static final long serialVersionUID = 146526524087765133L;

		public CantContinueException(final String aDetailMessage) {
			super(aDetailMessage);
		}

		public CantContinueException(final Throwable aThrowable) {
			super(aThrowable);
		}
	}
	
	private void sleep()
	{
		try {
			Thread.sleep(5 *1000);
		} catch (InterruptedException e) 
		{
		}
	}

}
