package org.andnav.osm.views.util;

import java.text.DecimalFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TimingStats {

	public double sumx;
	public double sumx2;
	public double numsum;
	public double max;
	public double min;
	
	private double startSecs;
	private double endSecs;
	private final String mName;

	private static DecimalFormat df = new DecimalFormat("#.######");

	private static final Logger logger = LoggerFactory.getLogger(TimingStats.class);

	public TimingStats(String tname)
	{
		mName = tname;
		reset();
	}

	public void reset()
	{
		 sumx = 0f;
		 sumx2 = 0f;
		 numsum = 0f;
		 max = 0f;
		 min = -1f;
	}
	
	public void start()
	{
		startSecs = System.currentTimeMillis()/1000f;
	}


	public void stop()
	{
		endSecs = System.currentTimeMillis()/1000f;
		
		double time = endSecs - startSecs;
		sumx += time;
		sumx2 += time*time;
		numsum += 1;
	}

	public void OutputStats()
	{
		if (numsum > 0)
		{
			logger.debug(mName + ": Average time " +df.format((sumx/numsum))+ " std " + df.format(Math.sqrt((sumx2/numsum) -(sumx/numsum)*(sumx/numsum))) + " Num " + numsum + " Mean +- "+  df.format(Math.sqrt( ((sumx2/numsum) -(sumx/numsum)*(sumx/numsum))/numsum )));
		}
	}
}
