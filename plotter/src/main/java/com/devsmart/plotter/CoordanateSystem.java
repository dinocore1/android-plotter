package com.devsmart.plotter;

import android.graphics.RectF;

public class CoordanateSystem {

    public static class Axis {
        public Function toScreen;
        public Function fromScreen;
    }

    public Axis xAxis;
    public Axis yAxis;

    public static class LinearFunction implements Function {

        private final double mSlope;
        private final double mYOffset;

        public LinearFunction(double[] from, double[] to) {
            mSlope = (from[1]-to[1]) / (from[0]-to[0]);
            mYOffset = to[1] - mSlope*to[0];
        }

        @Override
        public double value(double x) {
            return mSlope*x + mYOffset;
        }
    }


    public static CoordanateSystem linearSystem(RectF viewPort, RectF screen) {
        CoordanateSystem retval = new CoordanateSystem();

        retval.xAxis.toScreen = new LinearFunction(new double[]{viewPort.right, screen.right}, new double[]{viewPort.left, screen.left});
        retval.xAxis.fromScreen = new LinearFunction(new double[]{screen.right, viewPort.right}, new double[]{screen.left, viewPort.left});
        retval.yAxis.toScreen = new LinearFunction(new double[]{viewPort.bottom, screen.bottom}, new double[]{viewPort.top, screen.top});
        retval.yAxis.fromScreen = new LinearFunction(new double[]{screen.bottom, viewPort.bottom}, new double[]{screen.top, viewPort.top});

        return retval;
    }
}
