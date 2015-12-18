package com.devsmart.plotter;

public class LinearFunction implements AxisFunction {
	
	private float a = 1;
	private float m = 0;
	
	public LinearFunction(){
		
	}
	
	public LinearFunction(float a, float m) {
		this.a = a;
		this.m = m;
	}

	@Override
	public float value(float x) {
		return a * x + m;
	}

	@Override
	public AxisFunction inverse() {
		return new LinearFunction(1/a, -m/a);
	}

	@Override
	public void interpolate(float[] x, float[] y) {
		float top = y[1]-y[0];
		float bottom = x[1]-x[0];
		
		this.a = top / bottom;
		this.m = -a*x[0];
		
	}

	@Override
	public AxisFunction copy() {
		return new LinearFunction(a, m);
	}

}
