package com.devsmart.plotter;

import java.util.ArrayList;

public class SimpleSeries implements Series {
	
	protected ArrayList<float[]> mData = new ArrayList<float[]>();

	@Override
	public int getLength() {
		return mData.size();
	}

	@Override
	public void getPoint(int index, float[] point) {
		float[] dp = mData.get(index);
		point[0] = dp[0];
		point[1] = dp[1];

	}

}
