package com.devsmart.plotter;

import com.devsmart.plotter.GraphView.Axis;

public interface AxisLabelRenderer {

	public String renderAxisLabel(Axis axis, float value);
	
}
