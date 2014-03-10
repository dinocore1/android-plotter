package com.devsmart.plotter;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

public class FunctionRenderer implements DataRenderer {

    public interface GraphFunction {
		double value(double x);
	}

	private GraphFunction mFunction;
    private final double mSampleRate;
	protected Paint mPointPaint = new Paint();
	
	public FunctionRenderer(GraphFunction f, double samplerate, int color) {
		mFunction = f;
        mSampleRate = samplerate;
		mPointPaint.setColor(color);
		mPointPaint.setStrokeWidth(2.0f);
        mPointPaint.setAntiAlias(true);
	}

	float[] points = new float[4];
	float[] drawPoints = new float[4];
    double[] yMinMax = new double[2];
	
	@Override
	public void draw(Canvas canvas, RectF viewPort, CoordinateSystem coordSystem) {

        //reset min max
        yMinMax[0] = Double.MAX_VALUE;
        yMinMax[1] = Double.MIN_VALUE;

        final double pixelWidth = viewPort.width() / (double)canvas.getWidth();
        double startPix = viewPort.left;
        Path p = new Path();
        p.moveTo(0, 0);

        points[0] = viewPort.left;
        points[1] = (float)mFunction.value(viewPort.left);
        coordSystem.mapPoints(points);
        p.lineTo(points[0], points[1]);

        for(double x=viewPort.left;x<viewPort.right;x+= 1.0/ mSampleRate) {
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

}
