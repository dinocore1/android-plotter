package com.devsmart.plotter;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;

import com.devsmart.PeekableIterator;

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
		final float xBinWidth = viewPort.width()/canvas.getWidth();
		pixelBin.left = viewPort.left-xBinWidth;
		pixelBin.right = viewPort.left;
		pixelBin.bottom = Float.POSITIVE_INFINITY;
		pixelBin.top = Float.NEGATIVE_INFINITY;

		float[] lastpoint = new float[]{Float.NaN, Float.NaN};
		for(Series series : mSeries){
			PeekableIterator<float[]> it = new PeekableIterator<float[]>(series.createIterator());
			
			
			//findPixelBinLessThan(pixelBin, it);
			while(it.hasNext()){
				float[] point = it.next();
				lastpoint[0] = point[0];
				lastpoint[1] = point[1];
				if(it.peek()[0] > viewPort.left){
					break;
				}
			}
			matrix.mapPoints(lastpoint);
			
			boolean findOneMore = false;
			while(it.hasNext()){
				pixelBin.offset(xBinWidth, 0);
				pixelBin.bottom = Float.POSITIVE_INFINITY;
				pixelBin.top = Float.NEGATIVE_INFINITY;
				
				if(fillPixelBin(pixelBin, it)){
					//draw pixel
					matrix.mapRect(pixel, pixelBin);
					canvas.drawLine(lastpoint[0], lastpoint[1], pixel.left, pixel.top, mPointPaint);
					lastpoint[0] = pixel.left;
					lastpoint[1] = pixel.top;
					if(findOneMore) {
						break;
					}
				}
				if(it.peek()[0] > viewPort.right){
					findOneMore = true;
				}
				
			}
		}

	}
	
	private boolean findPixelBinLessThan(RectF pixelBin, PeekableIterator<float[]> it) {
		boolean retval = false;
		float[] point;
		
		while(it.hasNext()){
			
			point = it.next();
			pixelBin.bottom = Math.min(pixelBin.bottom, point[1]);
			pixelBin.top = Math.max(pixelBin.top, point[1]);
			
			point = it.peek();
			
			if(point[0] >= pixelBin.right){
				break;
			}
			
			if(point[0] >= pixelBin.left && point[0] < pixelBin.right){
				pixelBin.bottom = Float.POSITIVE_INFINITY;
				pixelBin.top = Float.NEGATIVE_INFINITY;
			}
			
			
			retval = true;
		}
		
		return retval;
	}
	
	private boolean fillPixelBin(RectF pixelBin, PeekableIterator<float[]> it) {
		boolean retval = false;
		float[] point;
		while(it.hasNext()){
			point = it.peek();
			if(point[0] > pixelBin.right) {
				break;
			}
			
			if(point[0] >= pixelBin.left) {
				pixelBin.bottom = Math.min(pixelBin.bottom, point[1]);
				pixelBin.top = Math.max(pixelBin.top, point[1]);
				retval = true;
			}
			it.next();
		}
		return retval;
	}


}
