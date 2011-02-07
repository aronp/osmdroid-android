// Created by plusminus on 17:58:57 - 25.09.2008
package org.andnav.osm.views.util;

import org.andnav.osm.tileprovider.LongObjectLRUCache;
import org.andnav.osm.tileprovider.OpenStreetMapTile;
import org.andnav.osm.views.util.constants.OpenStreetMapViewConstants;

import android.graphics.drawable.Drawable;

/**
 *
 * @author Nicolas Gramlich
 *
 */
public final class OpenStreetMapTileCache extends LRUMapTileCache implements OpenStreetMapViewConstants 
{
	// ===========================================================
	// Constants
	// ===========================================================

	// ===========================================================
	// Fields
	// ===========================================================

	// protected LRUMapTileCache mCachedTiles;
//	protected LongObjectLRUCache<Drawable> mCachedTiles; 
	public TimingStats getStats = new TimingStats("CacheGet");


	private final boolean DEBUG_STATS = false;
	
	// ===========================================================
	// Constructors
	// ===========================================================

	public OpenStreetMapTileCache() {
		this(CACHE_MAPTILECOUNT_DEFAULT);
	}

	/**
	 * @param aMaximumCacheSize Maximum amount of MapTiles to be hold within.
	 */
	public OpenStreetMapTileCache(final int aMaximumCacheSize){
		super(aMaximumCacheSize);
		// this.mCachedTiles = new LongObjectLRUCache<Drawable>(aMaximumCacheSize);
	}

	// ===========================================================
	// Getter & Setter
	// ===========================================================

//	public synchronized void ensureCapacity(final int aCapacity) {
//		mCachedTiles.ensureCapacity(aCapacity);
//	}

	public synchronized Drawable getMapTile(final OpenStreetMapTile aTile) {

		if (DEBUG_STATS)
			getStats.start();

		Drawable retval =  super.get(aTile.getTileId());

		if (DEBUG_STATS)
			getStats.stop();

		return retval;


//		return this.mCachedTiles.get(aTile.getTileId());
	}

	public synchronized void putTile(final OpenStreetMapTile aTile, final Drawable aDrawable) {
		if (aDrawable != null) {
			super.put(aTile.getTileId(), aDrawable);
		}
	}

	// ===========================================================
	// Methods from SuperClass/Interfaces
	// ===========================================================

	// ===========================================================
	// Methods
	// ===========================================================

	public synchronized boolean containsTile(final OpenStreetMapTile aTile) {
		return super.containsKey(aTile.getTileId());
	}

	public synchronized void clear() {
		super.clear();
//		this.mCachedTiles.clear();
		// TODO clear map.
	}

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================
}
