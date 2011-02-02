package org.andnav.osm.views.overlay;

import org.andnav.osm.DefaultResourceProxyImpl;
import org.andnav.osm.ResourceProxy;
import org.andnav.osm.tileprovider.OpenStreetMapTile;
import org.andnav.osm.util.GeoPoint;
import org.andnav.osm.util.MyMath;
import org.andnav.osm.views.OpenStreetMapView;
import org.andnav.osm.views.OpenStreetMapView.OpenStreetMapViewProjection;
import org.andnav.osm.views.util.IOpenStreetMapRendererInfo;
import org.andnav.osm.views.util.OpenStreetMapTileProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

/**
 * These objects are the principle consumer of map tiles.
 * 
 * see {@link OpenStreetMapTile} for an overview of how tiles are acquired by this overlay.
 * 
 */

public class OpenStreetMapTilesOverlay extends OpenStreetMapViewOverlay {

	private static final Logger logger = LoggerFactory.getLogger(OpenStreetMapTilesOverlay.class);

	protected OpenStreetMapView mOsmv;
	protected IOpenStreetMapRendererInfo mRendererInfo;

	/** Current renderer */
	public final OpenStreetMapTileProvider mTileProvider;
	protected final Paint mPaint = new Paint();
	protected final int extraTiles = 0;

	/* to avoid allocations during draw */
	private final Point mTilePos = new Point();
	private final Rect mViewPort = new Rect();
	
	private final OpenStreetMapTile mTile;
	protected boolean debug_draw = DEBUG_DRAW;
	protected boolean draw_grid = true;
	
	public OpenStreetMapTilesOverlay(
			final OpenStreetMapView aOsmv,
			final IOpenStreetMapRendererInfo aRendererInfo,
			final OpenStreetMapTileProvider aTileProvider,
			final Context aContext) {
		this(aOsmv, aRendererInfo, aTileProvider, new DefaultResourceProxyImpl(aContext));
	}

	public OpenStreetMapTilesOverlay(
			final OpenStreetMapView aOsmv,
			final IOpenStreetMapRendererInfo aRendererInfo,
			final OpenStreetMapTileProvider aTileProvider,
			final ResourceProxy pResourceProxy) {
		super(pResourceProxy);
		this.mOsmv = aOsmv;
		this.mRendererInfo = aRendererInfo;
		this.mTileProvider = aTileProvider; // TODO check for null

		
		mTile = new OpenStreetMapTile(this.mRendererInfo, 1, 1, 1);
	}

	public void detach() {
		this.mTileProvider.detach();
	}

	public IOpenStreetMapRendererInfo getRendererInfo() {
		return mRendererInfo;
	}

	public void setRendererInfo(final IOpenStreetMapRendererInfo aRendererInfo) {
		this.mRendererInfo = aRendererInfo;
		// XXX perhaps we should set the cache capacity back to default here
	}

	public void setAlpha(int a) {
		this.mPaint.setAlpha(a);
	}

	/**
	 * Whether to use the network connection if it's available.
	 */
	public boolean useDataConnection() {
		return mTileProvider.useDataConnection();
	}

	/**
	 * Set whether to use the network connection if it's available.
	 * @param aMode
	 * if true use the network connection if it's available.
	 * if false don't use the network connection even if it's available.
	 */
	public void setUseDataConnection(boolean aMode) {
		mTileProvider.setUseDataConnection(aMode);
	}

	@Override
	protected void onDraw(Canvas c, OpenStreetMapView osmv) {

		if(DEBUGMODE)
			logger.trace("onDraw");

		/*
		 * Do some calculations and drag attributes to local variables to save
		 * some performance.
		 */
		final OpenStreetMapViewProjection pj = osmv.getProjection();
		final int zoomLevel = osmv.getZoomLevel(false);

		c.getClipBounds(mViewPort);
		final int tileSizePx = this.mRendererInfo.maptileSizePx();
		final int tileZoom = this.mRendererInfo.maptileZoom();
		final int worldSize_2 = 1 << (zoomLevel + tileZoom - 1);

		/*
		 * Calculate the amount of tiles needed for each side around the center
		 * one.
		 */
		mViewPort.offset(worldSize_2, worldSize_2);
		final int tileNeededToLeftOfCenter = (mViewPort.left >> tileZoom) - (1 +extraTiles) ;
		final int tileNeededToRightOfCenter = (mViewPort.right >> tileZoom)+ extraTiles;
		final int tileNeededToTopOfCenter = (mViewPort.top >> tileZoom) - (1+extraTiles);
		final int tileNeededToBottomOfCenter = (mViewPort.bottom >> tileZoom) + extraTiles;

		final int mapTileUpperBound = 1 << zoomLevel;

		// make sure the cache is big enough for all the tiles
		final int numNeeded = (tileNeededToBottomOfCenter - tileNeededToTopOfCenter + 1)
							* (tileNeededToRightOfCenter - tileNeededToLeftOfCenter + 1);
		mTileProvider.ensureCapacity(numNeeded);

		/* Draw all the MapTiles (from the upper left to the lower right). */
		for (int y = tileNeededToTopOfCenter; y <= tileNeededToBottomOfCenter; y++) {
			for (int x = tileNeededToLeftOfCenter; x <= tileNeededToRightOfCenter; x++) {
				/* Construct a URLString, which represents the MapTile. */
				final int tileY = MyMath.mod(y, mapTileUpperBound);
				final int tileX = MyMath.mod(x, mapTileUpperBound);
				mTile.populate(this.mRendererInfo, zoomLevel, tileX, tileY);

				pj.toPixels(x, y, mTilePos);
				final Drawable currentMapTile = mTileProvider.getMapTile(mTile);
				if (currentMapTile != null) {
					currentMapTile.setBounds(mTilePos.x, mTilePos.y, mTilePos.x + tileSizePx, mTilePos.y + tileSizePx);
					currentMapTile.draw(c);
				}
				else
				{
					// draw background
//					c.drawLine(mTilePos.x, mTilePos.y, mTilePos.x + tileSizePx, mTilePos.y, mPaint);
//					c.drawLine(mTilePos.x, mTilePos.y, mTilePos.x, mTilePos.y + tileSizePx, mPaint);
					if (draw_grid)
					{
						int ypos = mTilePos.y + tileSizePx/2;
						int xpos = mTilePos.x + tileSizePx/2;

						c.drawLine(mTilePos.x, ypos, mTilePos.x + tileSizePx, ypos, mPaint);
						c.drawLine(xpos, mTilePos.y, xpos, mTilePos.y + tileSizePx, mPaint);
					}

				}

				if (debug_draw) {
					c.drawText(mTile.getX() + "/"+ mTile.getY()+ "/" + mTile.getZoomLevel(), mTilePos.x + 1, mTilePos.y + mPaint.getTextSize(), mPaint);
					c.drawLine(mTilePos.x, mTilePos.y, mTilePos.x + tileSizePx, mTilePos.y, mPaint);
					c.drawLine(mTilePos.x, mTilePos.y, mTilePos.x, mTilePos.y + tileSizePx, mPaint);
				}
			}
		}

		// draw a cross at center in debug mode
		if (debug_draw) {
			final GeoPoint center = osmv.getMapCenter();
			final Point centerPoint = pj.toMapPixels(center, null);
			c.drawLine(centerPoint.x, centerPoint.y - 9, centerPoint.x, centerPoint.y + 9, mPaint);
			c.drawLine(centerPoint.x - 9, centerPoint.y, centerPoint.x + 9, centerPoint.y, mPaint);
		}
		if (debug_draw ) {
			final GeoPoint center = osmv.getMapCenter();
			final Point centerPoint = pj.toMapPixels(center, null);
			c.drawText("Zoom:"+mTile.getZoomLevel(), centerPoint.x, centerPoint.y + mPaint.getTextSize(), mPaint);
		}

	}

	@Override
	protected void onDrawFinished(Canvas c, OpenStreetMapView osmv) {
	}
}
