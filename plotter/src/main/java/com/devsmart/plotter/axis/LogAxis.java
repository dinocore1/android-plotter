package com.devsmart.plotter.axis;


public class LogAxis implements Axis {

    public final float mBase;

    public LogAxis(float base) {
        mBase = base;
    }

    @Override
    public float toLinearUnit(float x) {
        return (float) (Math.log(x) / Math.log(mBase));
    }

    @Override
    public float fromLinearUnit(float x) {
        return (float) Math.pow(mBase, x);
    }
}
