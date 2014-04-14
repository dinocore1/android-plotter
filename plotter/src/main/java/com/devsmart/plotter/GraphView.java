package com.devsmart.plotter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.view.MotionEventCompat;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ZoomButtonsController;

import com.devsmart.BackgroundTask;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GraphView extends View {

	private static final String KEY_VIEWPORT = "viewport";
	private static final String KEY_SUPERINSTANCE = "superinstance";

	private ExecutorService mDrawThread = Executors.newSingleThreadExecutor();

	private RectF mViewPort = new RectF();
	protected List<DataRenderer> mSeries = new CopyOnWriteArrayList<DataRenderer>();
	private Bitmap mFrontBuffer;
	private Matrix mTransformMatrix = new Matrix();
	private Paint mDrawPaint = new Paint();
	private BackgroundDrawTask mBackgroundDrawTask;
	private GestureDetector mPanGestureDetector;
	private XYScaleGestureDetector mScaleGestureDetector;
    private CoordanateSystem mCoordanateSyste;


	public GraphView(Context context) {
		super(context);
		init();
	}

	public GraphView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	private void init() {
		mPanGestureDetector = new GestureDetector(mSimpleGestureListener);
		mScaleGestureDetector = new XYScaleGestureDetector(getContext(), mSimpleScaleGestureListener);
		mDrawPaint.setFilterBitmap(true);
		mViewPort.set(-1, -1, 1, 1);
		mTransformMatrix.reset();
        mCoordanateSyste = CoordanateSystem.linearSystem();

        mCoordanateSyste.xAxis.interpolate(new double[]{-1, 0}, new double[]{1, 480});
        mCoordanateSyste.yAxis.interpolate(new double[]{-1, 0}, new double[]{1, 800});

	}

	@Override
	protected Parcelable onSaveInstanceState() {
		
		Bundle retval = new Bundle();
		retval.putParcelable(KEY_SUPERINSTANCE, super.onSaveInstanceState());

		float[] viewportvalues = new float[4];
		viewportvalues[0] = mViewPort.left;
		viewportvalues[1] = mViewPort.top;
		viewportvalues[2] = mViewPort.right;
		viewportvalues[3] = mViewPort.bottom;
		retval.putFloatArray(KEY_VIEWPORT, viewportvalues);
		return retval;
	}

	protected void onRestoreInstanceState (Parcelable state) {
		Bundle bundle = (Bundle) state;
		super.onRestoreInstanceState(bundle.getParcelable(KEY_SUPERINSTANCE));

		float[] viewportvalues = bundle.getFloatArray(KEY_VIEWPORT);
		mViewPort.left = viewportvalues[0];
		mViewPort.top = viewportvalues[1];
		mViewPort.right = viewportvalues[2];
		mViewPort.bottom = viewportvalues[3];
		drawFrame();
	}

	public void addSeries(DataRenderer data) {
		mSeries.add(data);
		drawFrame();
	}

	public void removeSeries(DataRenderer data) {
		mSeries.remove(data);
		drawFrame();
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		drawFrame();
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {

		final int action = MotionEventCompat.getActionMasked(event);
		switch(action){
		case MotionEvent.ACTION_UP:
            invalidate();
			break;
		}

		boolean retval = mPanGestureDetector.onTouchEvent(event);
		retval |= mScaleGestureDetector.onTouchEvent(event);
		return retval;
	}
	
	public void setDisplayViewPort(RectF viewport) {
        mViewPort = viewport;
        mTransformMatrix.reset();
		drawFrame();
	}


	@Override
	protected void onDraw(Canvas canvas) {
		if(mFrontBuffer != null){
			canvas.drawBitmap(mFrontBuffer, mTransformMatrix, mDrawPaint);
		}
	}

	private void drawFrame() {
		if(mBackgroundDrawTask != null){
			mBackgroundDrawTask.mCanceled = true;
		}

        RectF newViewPort = new RectF(0, 0, getMeasuredWidth(), getMeasuredHeight());
        Matrix matrix = new Matrix();
        mTransformMatrix.invert(matrix);
        matrix.mapRect(newViewPort);

        newViewPort.left = (float) mCoordanateSyste.xAxis.fromScreen.value(newViewPort.left);
        newViewPort.right = (float) mCoordanateSyste.xAxis.fromScreen.value(newViewPort.right);
        newViewPort.top = (float) mCoordanateSyste.yAxis.fromScreen.value(newViewPort.top);
        newViewPort.bottom = (float) mCoordanateSyste.yAxis.fromScreen.value(newViewPort.bottom);


		mBackgroundDrawTask = new BackgroundDrawTask(getMeasuredWidth(), getMeasuredHeight(), newViewPort);
		BackgroundTask.runBackgroundTask(mBackgroundDrawTask, mDrawThread);
	}

	private class BackgroundDrawTask extends BackgroundTask {

		private int width;
		private int height;
		private Bitmap mDrawBuffer;
		private boolean mCanceled = false;
		private final RectF viewport;

		public BackgroundDrawTask(int width, int height, RectF viewport){
			this.width = width;
			this.height = height;
			this.viewport = viewport;
		}

		@Override
		public void onBackground() {
			if(!mCanceled){
                CoordanateSystem coordanateSystem = mCoordanateSyste.copy();
                coordanateSystem.xAxis.interpolate(new double[]{viewport.left, 0}, new double[]{viewport.right, width});
                coordanateSystem.yAxis.interpolate(new double[]{viewport.bottom, 0}, new double[]{viewport.top, height});
				mDrawBuffer = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
                Canvas canvas = new Canvas(mDrawBuffer);
                for(DataRenderer r : mSeries){
                    r.draw(canvas, mViewPort, coordanateSystem);
                }

			}
		}

		@Override
		public void onAfter() {
			if(!mCanceled){
				mFrontBuffer = mDrawBuffer;
				mViewPort = viewport;
				mTransformMatrix.reset();
				invalidate();
			} else if(mDrawBuffer != null) {
				mDrawBuffer.recycle();
				mDrawBuffer = null;
			}
		}
	}

	private GestureDetector.SimpleOnGestureListener mSimpleGestureListener = new GestureDetector.SimpleOnGestureListener(){

		@Override
		public boolean onDown(MotionEvent e) {
			return true;
		}



		@Override
		public boolean onDoubleTap(MotionEvent e) {
			//autoScaleDomainAndRange();
			mTransformMatrix.postScale(1.3f, 1.3f, e.getX(), e.getY());
            drawFrame();
			invalidate();
			return true;
		}

		@Override
		public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
			mTransformMatrix.postTranslate(-distanceX, -distanceY);
            drawFrame();
			invalidate();
			return true;
		}

	};

	private XYScaleGestureDetector.SimpleOnScaleGestureListener mSimpleScaleGestureListener = new XYScaleGestureDetector.SimpleOnScaleGestureListener(){

		@Override
		public boolean onScale(XYScaleGestureDetector detector) {
			
			mTransformMatrix.postScale(detector.getXScaleFactor(), detector.getYScaleFactor(), detector.getFocusX(), detector.getFocusY());
            drawFrame();
			invalidate();
			return true;

		}

	};


	public void zoomInCenter() {
		float scale = 1.3f;
		mTransformMatrix.postScale(scale, scale, getMeasuredWidth()/2, getMeasuredHeight()/2);
        drawFrame();
		invalidate();
	}

	public void zoomOutCenter() {
		float scale = 0.7f;
		mTransformMatrix.postScale(scale, scale, getMeasuredWidth()/2, getMeasuredHeight()/2);
        drawFrame();
		invalidate();
	}

	private ZoomButtonsController.OnZoomListener mZoomButtonListener = new ZoomButtonsController.OnZoomListener(){

		@Override
		public void onVisibilityChanged(boolean visible) {}

		@Override
		public void onZoom(boolean zoomIn) {
			if(zoomIn) {
				zoomInCenter();
			} else {
				zoomOutCenter();
			}

		}

	};



}
