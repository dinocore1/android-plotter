package com.devsmart.plotter;


import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

import java.util.Arrays;

public class OversampleFunctionRenderer implements DataRenderer {

    private static class MinMax {
        double min = Double.NaN;
        double max = Double.NaN;

        public void addValue(double value){
            if(value < min || Double.isNaN(min)){
                min = value;
            }

            if(value > max || Double.isNaN(max)){
                max = value;
            }
        }

        public void clear() {
            min = Double.NaN;
            max = Double.NaN;
        }

    }

    private final FunctionRenderer2.GraphFunction mFunction;
    private double[] mSamplePoints;
    private double mSampleRate;
    protected Paint mPaint = new Paint();
    private MinMax mMinMax = new MinMax();
    private float[] mPoints = new float[4];

    public OversampleFunctionRenderer(FunctionRenderer2.GraphFunction f, double[] samplePoints, int color) {
        mFunction = f;
        mSamplePoints = new double[samplePoints.length];
        System.arraycopy(samplePoints, 0, mSamplePoints, 0, mSamplePoints.length);
        Arrays.sort(mSamplePoints);
        mPaint.setColor(color);
        mPaint.setStrokeWidth(2.0f);
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.STROKE);
    }

    public OversampleFunctionRenderer(FunctionRenderer2.GraphFunction f, double sampleRate, int color){
        mFunction = f;
        mSampleRate = sampleRate;
        mPaint.setColor(color);
        mPaint.setStrokeWidth(2.0f);
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.STROKE);
    }

    @Override
    public void draw(Canvas canvas, RectF viewPort, CoordinateSystem coordSystem) {
        if(mSamplePoints != null){
            drawUsingSamplePoints(canvas, viewPort, coordSystem);
        } else {
            drawUsingSamplerate(canvas, viewPort, coordSystem);
        }
    }

    private void drawUsingSamplerate(Canvas canvas, RectF viewPort, CoordinateSystem coordSystem) {
        float[] points = new float[2];
        final double pixelWidth = viewPort.width() / (double)canvas.getWidth();

        mMinMax.clear();
        double lastX = viewPort.left-pixelWidth;

        Path p = new Path();

        points[0] = viewPort.left;
        points[1] = (float) mFunction.value(viewPort.left);
        coordSystem.mapPoints(points);
        p.moveTo(points[0], points[1]);

        for(double x=viewPort.left;x<=viewPort.right;x+=mSampleRate){
            final double y = mFunction.value(x);
            mMinMax.addValue(y);

            if(x >= lastX+pixelWidth){
                drawLine(p, (float) x, coordSystem);
                lastX = x;
                mMinMax.clear();
            }
        }

        canvas.drawPath(p, mPaint);
    }

    private void drawLine(Path path, float x, CoordinateSystem coordSystem) {
        mPoints[0] = x;
        mPoints[1] = (float)mMinMax.min;
        mPoints[2] = x;
        mPoints[3] = (float)mMinMax.max;

        coordSystem.mapPoints(mPoints);
        if(FunctionRenderer2.isRealNumber(mPoints[0]) && FunctionRenderer2.isRealNumber(mPoints[1])) {
            path.lineTo(mPoints[0], mPoints[1]);
        }
        if(FunctionRenderer2.isRealNumber(mPoints[2]) && FunctionRenderer2.isRealNumber(mPoints[3])) {
            path.lineTo(mPoints[2], mPoints[3]);
        }

        path.moveTo(mPoints[0], mPoints[1]);
    }

    private void drawUsingSamplePoints(Canvas canvas, RectF viewPort, CoordinateSystem coordSystem) {
        float[] points = new float[2];
        final double pixelWidth = viewPort.width() / (double)canvas.getWidth();

        int start = Arrays.binarySearch(mSamplePoints, viewPort.left);
        if(start < 0) {
            start = -start - 2;
        }
        if(start < 0){
            start = 0;
        }

        int end = Arrays.binarySearch(mSamplePoints, viewPort.right);
        if(end < 0) {
            end = -end;
        }
        if(end >= mSamplePoints.length) {
            end = mSamplePoints.length;
        }

        Path p = new Path();
        mMinMax.clear();

        double lastX = mSamplePoints[start];
        points[0] = (float) lastX;
        points[1] = (float) mFunction.value(lastX);
        coordSystem.mapPoints(points);
        p.moveTo(points[0], points[1]);

        for(int i=start+1;i<end;i++){
            double x = mSamplePoints[i];
            final double y = mFunction.value(x);
            mMinMax.addValue(y);

            if(x >= lastX+pixelWidth){
                drawLine(p, (float) x, coordSystem);
                lastX = x;
                mMinMax.clear();
            }
        }

        canvas.drawPath(p, mPaint);
    }
}
