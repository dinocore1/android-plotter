package com.devsmart.plotter.axis;


public class LinearAxis implements Axis {


    @Override
    public float toLinearUnit(float x) {
        return x;
    }

    @Override
    public float fromLinearUnit(float x) {
        return x;
    }
}
