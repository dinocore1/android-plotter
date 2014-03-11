package com.devsmart.plotter;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

public class FunctionRenderer implements DataRenderer {

    public interface GraphFunction {
        double value(double x);
    }

    private double[] mSampleLocations;
	private GraphFunction mFunction;
    private double mSampleRate = 2;
	protected Paint mPointPaint = new Paint();

    public FunctionRenderer(GraphFunction f, double[] sampleLocations, int color) {
        mFunction = f;
        mSampleLocations = sampleLocations;
        mPointPaint.setColor(color);
        mPointPaint.setStrokeWidth(2.0f);
        mPointPaint.setAntiAlias(true);
    }
	
	public FunctionRenderer(GraphFunction f, double samplerate, int color) {
		mFunction = f;
        mSampleRate = samplerate;
		mPointPaint.setColor(color);
		mPointPaint.setStrokeWidth(2.0f);
        mPointPaint.setAntiAlias(true);
	}


	float[] points = new float[6];
	float[] drawPoints = new float[4];
    float[] lastPoint = new float[2];
    double[] yMinMax = new double[2];

    private void drawFixedSample(Canvas canvas, RectF viewPort, CoordinateSystem coordSystem) {
        //reset min max
        yMinMax[0] = Double.MAX_VALUE;
        yMinMax[1] = Double.MIN_VALUE;

        final double pixelWidth = viewPort.width() / (double)canvas.getWidth();
        final double stepWidth = Math.min(pixelWidth, 1.0/mSampleRate);

        Path p = new Path();
        p.moveTo(0, 0);

        points[0] = viewPort.left;
        points[1] = (float)mFunction.value(viewPort.left);
        coordSystem.mapPoints(points);
        p.lineTo(points[0], points[1]);

        double startPix = viewPort.left;
        for(double x=startPix;x<viewPort.right;x+=stepWidth) {
            final double y = mFunction.value(x);
            yMinMax[0] = Math.min(yMinMax[0], y);
            yMinMax[1] = Math.max(yMinMax[1], y);

            if(x >= startPix+pixelWidth){

                //min
                points[0] = (float) startPix;
                points[1] = (float) yMinMax[0];

                //max
                points[2] = (float) startPix;
                points[3] = (float) yMinMax[1];

                coordSystem.mapPoints(points);

                p.lineTo(points[0], points[1]);
                p.lineTo(points[2], points[3]);

                //reset min max
                yMinMax[0] = Double.MAX_VALUE;
                yMinMax[1] = Double.MIN_VALUE;

                startPix = x;
            }
        }

        points[0] = viewPort.right;
        points[1] = (float)mFunction.value(viewPort.right);
        coordSystem.mapPoints(points);
        p.lineTo(points[0], points[1]);

        p.lineTo(canvas.getWidth(), 0);

        p.close();

        canvas.drawPath(p, mPointPaint);
    }

    private class MinMax {
        double min;
        double max;

        public MinMax() {
            reset();
        }

        public void reset() {
            min = Double.MAX_VALUE;
            max = Double.MIN_VALUE;
        }

        public void add(float value) {
            min = Math.min(min, value);
            max = Math.max(max, value);
        }
    }

    private void drawAtSampleLocations(Canvas canvas, RectF viewPort, CoordinateSystem coordSystem) {

        int sampleindex = 0;
        for(int i=0;i<mSampleLocations.length;i++){
            if(mSampleLocations[i] >= viewPort.left){
                sampleindex = i;
                if(i>0){
                    sampleindex = i-1;
                }
                break;
            }
        }

        //reset min max
        yMinMax[0] = Double.MAX_VALUE;
        yMinMax[1] = Double.MIN_VALUE;
        MinMax xminmax = new MinMax();
        MinMax yminmax = new MinMax();

        final double pixelWidth = viewPort.width() / (double)canvas.getWidth();

        Path p = new Path();

        points[0] = viewPort.left;
        points[1] = (float)mFunction.value(viewPort.left);
        xminmax.add(points[0]);
        yminmax.add(points[1]);

        coordSystem.mapPoints(points);
        p.moveTo(points[0], points[1]);
        lastPoint[0] = points[0];
        lastPoint[1] = points[1];

        double startPix = mSampleLocations[sampleindex];
        double x = 0;
        while(true){
            if(sampleindex >= mSampleLocations.length || (x=mSampleLocations[sampleindex++]) > viewPort.right){
                break;
            }
            final double y = mFunction.value(x);
            yMinMax[0] = Math.min(yMinMax[0], y);
            yMinMax[1] = Math.max(yMinMax[1], y);

            if(x >= startPix+pixelWidth){

                //min
                points[0] = (float) startPix;
                points[1] = (float) yMinMax[0];

                //max
                points[2] = (float) startPix;
                points[3] = (float) yMinMax[1];

                xminmax.add(points[0]);
                yminmax.add(points[1]);
                xminmax.add(points[2]);
                yminmax.add(points[3]);

                coordSystem.mapPoints(points);

                p.quadTo((lastPoint[0]+points[0])/2, coordSystem.yValue((lastPoint[0]+points[0])/2), points[0], points[1]);
                lastPoint[0] = points[0];
                lastPoint[1] = points[1];

                p.quadTo((lastPoint[0]+points[2])/2, coordSystem.yValue((lastPoint[0]+points[2])/2), points[2], points[3]);
                lastPoint[0] = points[2];
                lastPoint[1] = points[3];

                //reset min max
                yMinMax[0] = Double.MAX_VALUE;
                yMinMax[1] = Double.MIN_VALUE;

                startPix = x;
            }
        }

        points[0] = viewPort.right;
        points[1] = (float)mFunction.value(viewPort.right);
        xminmax.add(points[0]);
        yminmax.add(points[1]);
        coordSystem.mapPoints(points);
        p.quadTo((lastPoint[0] + points[0]) / 2, (lastPoint[1] + points[1]) / 2, points[0], points[1]);


        //p.lineTo(xminmax.max, yminmax.min);
        //p.lineTo(xminmax.min, yminmax.min);


        p.close();

        canvas.drawPath(p, mPointPaint);
    }
	
	@Override
	public void draw(Canvas canvas, RectF viewPort, CoordinateSystem coordSystem) {
        if(mSampleLocations == null) {
            drawFixedSample(canvas, viewPort, coordSystem);
        } else {
            drawAtSampleLocations(canvas, viewPort, coordSystem);
        }


	}



}
