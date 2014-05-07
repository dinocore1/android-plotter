package com.devsmart.plotter.axis;


public interface Axis {

    float toLinearUnit(float x);
    float fromLinearUnit(float x);
}
