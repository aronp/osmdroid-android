package org.andnav.osm.tileprovider;

import org.andnav.osm.views.util.IOpenStreetMapRendererInfo;

/**
 * A map tile is distributed using the observer pattern.
 * The tile is delivered by a tile provider 
 * (i.e. a descendant of {@link OpenStreetMapAsyncTileProvider}  or {@link OpenStreetMapTileProvider}
 * to a consumer of tiles (e.g.  descendant of {@link OpenStreetMapTilesOverlay}).
 * Tiles are typically images (e.g. png or jpeg).
 */
public class OpenStreetMapTile {

	public static final int MAPTILE_SUCCESS_ID = 0;
	public static final int MAPTILE_FAIL_ID = MAPTILE_SUCCESS_ID + 1;

	static final long zoomBits = 5;
	static final long yBits = 20;
	static final long xBits = 20;
	static final long ySignBit = 1;
	static final long xSignBit = 1;
	static final long maskZoom = (1L << zoomBits) -1 ;
	static final long ymask = ((1L << yBits) -1) *(1L << zoomBits);
	static final long xmask = ((1L << xBits) -1) *(1L << (zoomBits+yBits));
	static final long ysignmask = (1L << (zoomBits+yBits+xBits));
	static final long xsignmask = ysignmask << 1L;

	
	// This class must be immutable because it's used as the key in the cache hash map
	// (ie all the fields are final).
	private  IOpenStreetMapRendererInfo renderer;
	private  int x;
	private  int y;
	private  int zoomLevel;

	public OpenStreetMapTile(IOpenStreetMapRendererInfo renderer, int zoomLevel, int tileX, int tileY) {
		populate( renderer,  zoomLevel,  tileX,  tileY);
	}

	@Override
	public OpenStreetMapTile clone() {
		return new OpenStreetMapTile(renderer, zoomLevel, x, y);
	}

	public void populate(IOpenStreetMapRendererInfo renderer, int zoomLevel, int tileX, int tileY) 
	{
		this.renderer = renderer;
		this.zoomLevel = zoomLevel;
		this.x = tileX;
		this.y = tileY;
	}
	public IOpenStreetMapRendererInfo getRenderer() {
		return renderer;
	}

	public int getZoomLevel() {
		return zoomLevel;
	}

	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}

	@Override
	public String toString() {
		return renderer.name() + "/" + zoomLevel + "/" + x + "/" + y;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) return false;
		if (obj == this) return true;
		if (obj.getClass() != getClass()) return false;
		final OpenStreetMapTile rhs = (OpenStreetMapTile)obj;
		return zoomLevel == rhs.zoomLevel && x == rhs.x && y == rhs.y && renderer.equals(rhs.renderer);
	}

	@Override
	public int hashCode() {
		int code = 17;
		code *= 37 + x;
		code *= 37 + y;
		code *= 37 + renderer.hashCode();
		code *= 37 + zoomLevel;
		return code;
	}
	public long getTileId()
	{
		return getTileId(x,y,zoomLevel);
	}

	public static long getTileId(int x, int y, int zoom)
	{
		long retval = 0;
		long absx;
		long absy;
		long numMask = ((1 << zoom) -1L);

		if (x < 0)
		{
			absx = (-x) & numMask;
			retval = retval | xsignmask;
		}
		else
		{
			absx = x & numMask;
		}

		if (y < 0)
		{
			absy = (-y) & numMask;
			retval = retval | ysignmask;
		}
		else
		{
			absy = y & numMask;
		}

		retval = retval | ((zoom & maskZoom) | ((absy << zoomBits) ) | (((absx << (zoomBits+yBits)) )));


		return retval;
	}
	
	public static int decodeTileX(long tileId)
	{
		long x =  (((tileId & xmask) >>> (zoomBits + yBits)));

		if ((tileId & xsignmask) != 0)
		{
			x = -x;
		}

		return (int)x;
	}

	public static int decodeTileY(long tileId)
	{
		long y =  (((tileId & ymask) >>> zoomBits));

		if ((tileId & ysignmask) != 0)
		{
			y = -y;
		}

		return (int)y;
	}

	public static int decodeTileZoom(long tileId)
	{
		int zoom = (int) (tileId & maskZoom);
		return zoom;
	}

	// TODO implement equals and hashCode in renderer

}
