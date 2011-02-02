// Created by plusminus on 21:31:36 - 25.09.2008
package org.andnav.osm.tileprovider;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.UnknownHostException;

import org.andnav.osm.tileprovider.util.CloudmadeUtil;
import org.andnav.osm.views.util.IOpenStreetMapRendererInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The OpenStreetMapTileDownloader loads tiles from a server and passes them to
 * a OpenStreetMapTileFilesystemProvider.
 * @author Nicolas Gramlich
 * @author Manuel Stahl
 *
 */
public class OpenStreetMapTileDownloader extends OpenStreetMapAsyncTileProvider implements IOpenStreetMapTileProviderCloudmadeTokenCallback {

	// ===========================================================
	// Constants
	// ===========================================================

	private static final Logger logger = LoggerFactory.getLogger(OpenStreetMapTileDownloader.class);

	// ===========================================================
	// Fields
	// ===========================================================

	private final OpenStreetMapTileFilesystemProvider mMapTileFSProvider;
	private String mCloudmadeToken;

	
	static final OSMThreadFactory mThreadFactory = new OSMThreadFactory("Tiles",NUMBER_OF_TILE_DOWNLOAD_THREADS );  
	
	// ===========================================================
	// Constructors
	// ===========================================================

	public OpenStreetMapTileDownloader(final IOpenStreetMapTileProviderCallback pCallback, final OpenStreetMapTileFilesystemProvider aMapTileFSProvider){
		super(pCallback, NUMBER_OF_TILE_DOWNLOAD_THREADS, TILE_DOWNLOAD_MAXIMUM_QUEUE_SIZE);
		this.mMapTileFSProvider = aMapTileFSProvider;
	}

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	// ===========================================================
	// Methods from SuperClass/Interfaces
	// ===========================================================

	@Override
	protected String threadGroupName() {
		return "downloader";
	}

	// final TileLoader mTileLoader = new TileLoader();
	
	@Override
	protected Runnable getTileLoader() {
		// return new TileLoader();
		return new TileLoader();
	};

	// ===========================================================
	// Methods
	// ===========================================================

	private String buildURL(final OpenStreetMapTile tile) throws CloudmadeException {
		return tile.getRenderer().getTileURLString(tile, mCallback, this);
	}

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================

	private class TileLoader extends OpenStreetMapAsyncTileProvider.TileLoader {

		@Override
		protected void loadTile(final long tileId, final IOpenStreetMapRendererInfo renderer) throws CantContinueException {

			InputStream in = null;
			OutputStream out = null;
			OpenStreetMapTile aTile = new OpenStreetMapTile(
					renderer,
					OpenStreetMapTile.decodeTileZoom(tileId),
					OpenStreetMapTile.decodeTileX(tileId),
					OpenStreetMapTile.decodeTileY(tileId));
					

			final File outputFile = mMapTileFSProvider.getOutputFile(aTile);

			try {
				final String tileURLString = buildURL(aTile);

				if(DEBUGMODE)
					logger.debug("Downloading Maptile from url: " + tileURLString);

				in = new BufferedInputStream(new URL(tileURLString).openStream(), StreamUtils.IO_BUFFER_SIZE);

				final ByteArrayOutputStream dataStream = new ByteArrayOutputStream();
				out = new BufferedOutputStream(dataStream, StreamUtils.IO_BUFFER_SIZE);
				StreamUtils.copy(in, out);
				out.flush();

				final byte[] data = dataStream.toByteArray();

				// sanity check - don't save an empty file
				if (data.length == 0) {
					logger.info("Empty maptile not saved: " + tileId);
					tileNotLoaded(aTile);
				} else {
					mMapTileFSProvider.saveFile(aTile, outputFile, data);
					tileLoaded(aTile, true);
					if(DEBUGMODE)
						logger.debug("Maptile saved " + data.length + " bytes : " + tileId);
				}
			} catch (final UnknownHostException e) {
				// no network connection 
				// no need to empty the queue, keep it
				// for when we are connected.
				logger.warn("UnknownHostException downloading MapTile: " + tileId + " : " + e);
				tileNotLoaded(aTile);
				throw new CantContinueException(e);
			} catch(final FileNotFoundException e){
				logger.warn("Tile not found: " + tileId+ " : " + e);
				tileNotLoaded(aTile);
				sleep();
			} catch (final IOException e) {
				logger.warn("IOException downloading MapTile: " + tileId + " : " + e);
				tileNotLoaded(aTile);
				throw new CantContinueException(e);
			} catch (final CloudmadeException e) {
				logger.warn("CloudmadeException downloading MapTile: " + tileId + " : " + e);
				tileNotLoaded(aTile);
				sleep();
			} catch(final Throwable e) {
				logger.error("Error downloading MapTile: " + tileId, e);
				tileNotLoaded(aTile);
				throw new CantContinueException(e);
			} finally {
				StreamUtils.closeStream(in);
				StreamUtils.closeStream(out);
			}

			/* Don't immediately send the tile back.
			 * If we're moving, and the internet is a bit patchy, then by the time
			 * the download has finished we don't need this tile any more.
			 * If we still do need it then the file system provider will get it
			 * again next time it's needed.
			 * That should be immediately because the view is redrawn when it
			 * receives this completion event.
			 */

		}

	}

	@Override
	public String getCloudmadeToken(final String aKey) throws CloudmadeException {

		if (mCloudmadeToken == null) {
			synchronized (this) {
				// check again inside the synchronised block
				if (mCloudmadeToken == null) {
					mCloudmadeToken = CloudmadeUtil.getCloudmadeToken(aKey);
				}
			}
		}

		return mCloudmadeToken;
	};

	private void sleep()
	{
		try {
			Thread.sleep(5 *1000);
		} catch (InterruptedException e) 
		{
		}
	}

	@Override
	protected OSMThreadFactory getOSMThreadFactory() {
		return mThreadFactory;
	}
}
