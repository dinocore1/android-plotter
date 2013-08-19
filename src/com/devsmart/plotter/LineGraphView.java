package com.devsmart.plotter;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;

public class LineGraphView extends GraphView {

	protected Paint mPointPaint = new Paint();

	public LineGraphView(Context context) {
		super(context);
		init();
	}
	
	public LineGraphView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	protected void init() {
		mPointPaint.setColor(Color.GREEN);
		mPointPaint.setStrokeWidth(2.0f);
	}

	protected void drawGraph(Canvas canvas, RectF viewPort){

		drawAxis(canvas, viewPort);
		
		Matrix matrix = getViewportToScreenMatrix(canvas.getWidth(), canvas.getHeight(), viewPort); 
		
		float[] points = new float[2];
		for(Series series : mSeries){
			for(int i=0;i<series.getLength();i++){
				series.getPoint(i, points);
				matrix.mapPoints(points);
				drawPointData(points, canvas);
			}
		}
		
	}
	

	private void drawPointData(float[] point, Canvas canvas) {
		canvas.drawPoints(point, mPointPaint);
		
	}

	


}
