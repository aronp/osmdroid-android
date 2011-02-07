package org.andnav.osm.views.util;


import org.andnav.osm.tileprovider.LongObjectLRUCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

public class LRUMapTileCache extends LongObjectLRUCache<Drawable> {

	private static final Logger logger = LoggerFactory.getLogger(LRUMapTileCache.class);

	private static final long serialVersionUID = -541142277575493335L;

	private int mCapacity;

	public LRUMapTileCache(final int aCapacity) {
		super(aCapacity);
		mCapacity = aCapacity;
	}

	public void ensureCapacity(final int aCapacity) {
		if (aCapacity > mCapacity) {
			logger.info("Tile cache increased from " + mCapacity + " to " + aCapacity);
			mCapacity = aCapacity;
			super.ensureCapacity(aCapacity);
		}
	}

//	@Override
//	public Drawable remove(final long aKey) {
//		final Drawable drawable = super.remove(aKey);
//		return drawable;
//	}

	@Override
	public void removeResource(Drawable drawable) {
		if (drawable instanceof BitmapDrawable) {
			final Bitmap bitmap = ((BitmapDrawable)drawable).getBitmap();
			if (bitmap != null) {
				bitmap.recycle();
			}
		}
	}

	
	@Override
	public void clear() {
		// remove them all individually so that they get recycled
		while(size() > 0) {
			remove(keySet().iterator().next());
		}

		// and then clear
		super.clear();
	}
}
