package com.devsmart.plotter;

import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Matrix.ScaleToFit;
import android.support.v4.view.MotionEventCompat;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import com.devsmart.BackgroundTask;

public abstract class GraphView extends View {

	private ExecutorService mDrawThread = Executors.newSingleThreadExecutor();

	private RectF mViewPort = new RectF();
	protected LinkedList<Series> mSeries = new LinkedList<Series>();

	private Bitmap mFrontBuffer;
	private Matrix mTransformMatrix = new Matrix();
	private Paint mDrawPaint = new Paint();
	private Future<?> mDrawFuture;
	private GestureDetector mGestureDetector;

	//draw prefs
	protected boolean mDrawXAxis;
	protected boolean mDrawYAxis;
	protected int mAxisColor;
	protected Paint mAxisLabelPaint = new Paint();
	protected Rect mPlotMargins = new Rect();
	protected int mBackgroundColor;

	public GraphView(Context context) {
		super(context);
		init();
	}

	public GraphView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	private void init() {
		mGestureDetector = new GestureDetector(mSimpleGestureListener);
		mDrawPaint.setFilterBitmap(true);
		mViewPort.set(-1, -1, 1, 1);
		mTransformMatrix.reset();

		//defaults
		mDrawXAxis = true;
		mDrawYAxis = true;
		mPlotMargins.set(10, 10, 10, 10);
		mAxisColor = Color.DKGRAY;
		mAxisLabelPaint.setColor(Color.DKGRAY);
		mAxisLabelPaint.setTextSize(20.0f);
		
		mBackgroundColor = Color.WHITE;
	}

	public void addSeries(Series series) {
		mSeries.add(series);
		drawFrame(mViewPort);
	}

	public void removeSeries(Series series) {
		mSeries.remove(series);
		drawFrame(mViewPort);
	}

	protected Matrix getViewportToScreenMatrix(
			final int canvasWidth, final int canvasHeight,
			RectF viewPort){

		Matrix matrix = new Matrix();
		matrix.setRectToRect(viewPort, 
				//new RectF(mPlotMargins.left, mPlotMargins.top, canvasWidth-mPlotMargins.right, canvasHeight-mPlotMargins.bottom),
				new RectF(0,0,canvasWidth,canvasHeight),
				ScaleToFit.FILL);
		
		matrix.postScale(1, -1);
		matrix.postTranslate(0, canvasHeight);
		
		return matrix;
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		drawFrame(mViewPort);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		
		final int action = MotionEventCompat.getActionMasked(event);
		switch(action){
		case MotionEvent.ACTION_UP:
			updateViewport();
			break;
		}
		
		boolean retval = mGestureDetector.onTouchEvent(event);
		return retval;
	}

	protected void updateViewport(){	
		
		Matrix m = new Matrix();
		mTransformMatrix.invert(m);
		
		RectF screen = new RectF(0,0, mFrontBuffer.getWidth(), mFrontBuffer.getHeight());
		m.mapRect(screen);
		
		Matrix viewPortTransform = getViewportToScreenMatrix(getMeasuredWidth(), getMeasuredHeight(), mViewPort);
		Matrix screenToViewPort = new Matrix();
		viewPortTransform.invert(screenToViewPort);
		
		screenToViewPort.mapRect(screen);
		drawFrame(screen);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		if(mFrontBuffer != null){
			canvas.drawBitmap(mFrontBuffer, mTransformMatrix, mDrawPaint);
		}
	}

	protected abstract void drawGraph(Canvas canvas, RectF viewPort);
	

	private void drawFrame(final RectF viewport) {
		if(mDrawFuture != null){
			mDrawFuture.cancel(false);
		}
		final int width = getMeasuredWidth();
		final int height = getMeasuredHeight();
		mDrawFuture = BackgroundTask.runBackgroundTask(new BackgroundTask() {
			private Bitmap mDrawBuffer;

			@Override
			public void onBackground() {
				mDrawBuffer = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
				drawGraph(new Canvas(mDrawBuffer), viewport);
			}

			@Override
			public void onFinished() {
				mFrontBuffer = mDrawBuffer;
				mViewPort = viewport;
				mTransformMatrix.reset();
				invalidate();
			}

		}, mDrawThread);
	}
	
	private GestureDetector.SimpleOnGestureListener mSimpleGestureListener = new GestureDetector.SimpleOnGestureListener(){

		@Override
		public boolean onDown(MotionEvent e) {
			return true;
		}

		@Override
		public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
			
			mTransformMatrix.postTranslate(-distanceX, -distanceY);
			invalidate();
			return true;
		}
		
	};



}
