package com.devsmart.plotter;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

public class FunctionRenderer implements DataRenderer {
	
	public interface GraphFunction {
		float value(float x);
	}

	private GraphFunction mFunction;
	protected Paint mPointPaint = new Paint();
	
	public FunctionRenderer(GraphFunction f, int color) {
		mFunction = f;
		mPointPaint.setColor(color);
		mPointPaint.setStrokeWidth(2.0f);
	}

	float[] points = new float[4];
	float[] drawPoints = new float[4];
	
	@Override
	public void draw(Canvas canvas, RectF viewPort, CoordinateSystem coordSystem) {
		
		final float xDiff = viewPort.width() / (float)canvas.getWidth();
		
		points[0] = viewPort.left;
		points[1] = mFunction.value(viewPort.left);
		
		for(float x=viewPort.left+xDiff;x<=viewPort.right;x = x + xDiff){
			
			points[2] = x;
			points[3] = mFunction.value(x);
			
			coordSystem.mapPoints(drawPoints, points);
			canvas.drawLines(drawPoints, mPointPaint);
			
			points[0] = points[2];
			points[1] = points[3];
		}

	}

}
