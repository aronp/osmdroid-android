package org.andnav.osm.tileprovider;

import org.andnav.osm.tileprovider.IAreWeConnected;
import org.andnav.osm.tileprovider.IOpenStreetMapTileProviderCallback;
import org.andnav.osm.tileprovider.OpenStreetMapAsyncTileProvider;
import org.andnav.osm.tileprovider.OpenStreetMapTile;


public abstract class AsyncHttpProvider extends OpenStreetMapAsyncTileProvider 
{
	final IAreWeConnected mConnCheck;
	
	public AsyncHttpProvider(IOpenStreetMapTileProviderCallback pCallback,
			int aThreadPoolSize, int aPendingQueueSize, IAreWeConnected connCheck) {
		super(pCallback, aThreadPoolSize, aPendingQueueSize);
		mConnCheck = connCheck;
	}

	@Override
	public void loadMapTileAsync(OpenStreetMapTile aTile) {

		if (DEBUGMODE) 
		{
			//TODO commented out
//			final int activeCount =  this.getOSMThreadFactory().getActiveCount();
//			LOG.d(TAG, activeCount + " active threads");
		}
		
		synchronized(mWorking)
		{
			if (mWorking.containsKey(aTile.getTileId()))
			{
				return;
			}
		}
		
		// this will put the tile in the queue, or move it to the front of
		// the queue if it's already present

		mWaiting.put(aTile.getTileId(), aTile.getRenderer());

		if (mConnCheck != null)
		{
			if (mConnCheck.AreWeConnected()) 
			{
				// TODO commented out
				getExecutorService().execute(getTileLoader());
				// getTileLoader().run();
			}
		}
	}

	@Override
	protected String threadGroupName() {
		return "HTTPThreads";
	}
}
