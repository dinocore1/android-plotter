package com.devsmart.plotter;

import android.graphics.RectF;

public class CoordanateSystem {

    public static abstract class Axis {
        public Function toScreen;
        public Function fromScreen;

        public abstract void interpolate(double[] from, double[] to);
        public abstract Axis copy();
    }

    public Axis xAxis;
    public Axis yAxis;

    public CoordanateSystem copy() {
        CoordanateSystem retval = new CoordanateSystem();
        retval.xAxis = xAxis.copy();
        retval.yAxis = yAxis.copy();
        return retval;
    }

    public static class LinearAxis extends Axis {

        public static class LinearFunction implements Function {

            double mSlope;
            double mYOffset;

            public LinearFunction() {}
            public LinearFunction(LinearFunction function) {
                mSlope = function.mSlope;
                mYOffset = function.mYOffset;
            }

            void interpolate(double[] from, double[] to) {
                mSlope = (from[1]-to[1]) / (from[0]-to[0]);
                mYOffset = to[1] - mSlope*to[0];
            }

            @Override
            public double value(double x) {
                return mSlope*x + mYOffset;
            }
        }

        public LinearAxis() {
            toScreen = new LinearFunction();
            fromScreen = new LinearFunction();
        }

        @Override
        public void interpolate(double[] from, double[] to) {
            ((LinearFunction)toScreen).interpolate(from, to);
            ((LinearFunction)fromScreen).mSlope = 1/((LinearFunction) toScreen).mSlope;
            ((LinearFunction) fromScreen).mYOffset = -((LinearFunction) fromScreen).mYOffset / ((LinearFunction) fromScreen).mSlope;
        }

        @Override
        public Axis copy() {
            LinearAxis retval = new LinearAxis();
            retval.toScreen = new LinearFunction((LinearFunction) toScreen);
            retval.fromScreen = new LinearFunction((LinearFunction)fromScreen);

            return retval;
        }
    }


    public static CoordanateSystem linearSystem() {
        CoordanateSystem retval = new CoordanateSystem();

        retval.xAxis = new LinearAxis();
        retval.yAxis = new LinearAxis();

        return retval;
    }
}
