package com.devsmart.plotter;

import java.util.Iterator;

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

		//clear the canvas
		canvas.drawColor(mBackgroundColor);
		
		//drawAxis(canvas, viewPort);

		Matrix matrix = getViewportToScreenMatrix(new RectF(0,0,canvas.getWidth(), canvas.getHeight()), viewPort); 

		RectF pixel = new RectF();

		RectF pixelBin = new RectF();
		float xBinWidth = viewPort.width()/canvas.getWidth();
		pixelBin.left = viewPort.left-xBinWidth;
		pixelBin.right = pixelBin.left;
		pixelBin.bottom = Float.POSITIVE_INFINITY;
		pixelBin.top = Float.NEGATIVE_INFINITY;

		float[] lastpoint = new float[]{Float.NaN, Float.NaN};
		for(Series series : mSeries){
			Iterator<float[]> it = series.createIterator();
			
			while(it.hasNext()){
				float[] point = it.next();
				
				if(point[0] < viewPort.left - xBinWidth) {
					continue;
				}
				
				if(point[0] > viewPort.right + 2*xBinWidth){
					break;
				}

				while(point[0] >= pixelBin.right){

					if(pixelBin.bottom != Float.POSITIVE_INFINITY){
						//draw pixel
						matrix.mapRect(pixel, pixelBin);
						
						
						canvas.drawLine(lastpoint[0], lastpoint[1], pixel.left, pixel.top, mPointPaint);
						
						lastpoint[0] = pixel.left;
						lastpoint[1] = pixel.top;
					}

					pixelBin.offset(xBinWidth, 0);
					pixelBin.bottom = Float.POSITIVE_INFINITY;
					pixelBin.top = Float.NEGATIVE_INFINITY;
				}
				
				pixelBin.bottom = Math.min(pixelBin.bottom, point[1]);
				pixelBin.top = Math.max(pixelBin.top, point[1]);
			}
		}

		//draw pixel
		matrix.mapRect(pixel, pixelBin);
		canvas.drawRect(pixel, mPointPaint);

	}


	private void drawPointData(float[] point, Canvas canvas) {
		canvas.drawPoints(point, mPointPaint);

	}




}
