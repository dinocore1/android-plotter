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
import com.devsmart.plotter.axis.Axis;
import com.devsmart.plotter.axis.LinearAxis;

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
    private Axis mXAxis;
    private Axis mYAxis;


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
		mViewPort.set(0, 1, 1, 0);
		mTransformMatrix.reset();
        mXAxis = new LinearAxis();
        mYAxis = new LinearAxis();
	}

    public void setXAxis(Axis axis) {
        mXAxis = axis;
    }

    public void setYAxis(Axis axis) {
        mYAxis = axis;
    }

	public void addSeries(DataRenderer data) {
		mSeries.add(data);
		drawFrame(getViewport());
	}

	public void removeSeries(DataRenderer data) {
		mSeries.remove(data);
		drawFrame(getViewport());
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		drawFrame(getViewport());
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
        mTransformMatrix.reset();
		drawFrame(viewport);
	}


	@Override
	protected void onDraw(Canvas canvas) {
		if(mFrontBuffer != null){
			canvas.drawBitmap(mFrontBuffer, mTransformMatrix, mDrawPaint);
		}
	}

    public void setViewport(RectF viewport){
        drawFrame(viewport);
    }

    public RectF getViewport() {
        RectF rect = new RectF(0,0,getMeasuredWidth(),getMeasuredHeight());
        Matrix inverse = new Matrix();
        mTransformMatrix.invert(inverse);
        mTransformMatrix.mapRect(rect);

        float[] src = new float[8];
        src[0] = rect.left;
        src[1] = rect.bottom;
        src[2] = rect.right;
        src[3] = rect.bottom;
        src[4] = rect.left;
        src[5] = rect.top;
        src[6] = rect.right;
        src[7] = rect.top;

        float[] dest = new float[8];
        dest[0] = mViewPort.left;
        dest[1] = mViewPort.top;
        dest[2] = mViewPort.right;
        dest[3] = mViewPort.top;
        dest[4] = mViewPort.left;
        dest[5] = mViewPort.bottom;
        dest[6] = mViewPort.right;
        dest[7] = mViewPort.bottom;

        inverse.setPolyToPoly(src, 0, dest, 0, 4);
        inverse.mapRect(rect);

        rect.left = mXAxis.fromLinearUnit(rect.left);
        rect.right = mXAxis.fromLinearUnit(rect.right);
        rect.top = mYAxis.fromLinearUnit(rect.top);
        rect.bottom = mYAxis.fromLinearUnit(rect.bottom);

        return rect;
    }

	private void drawFrame(RectF viewport) {
		if(mBackgroundDrawTask != null){
			mBackgroundDrawTask.mCanceled = true;
		}

		mBackgroundDrawTask = new BackgroundDrawTask(viewport);
		BackgroundTask.runBackgroundTask(mBackgroundDrawTask, mDrawThread);
	}

	private class BackgroundDrawTask extends BackgroundTask {

        private final MultivariateFunction mToScreen;
        private int width;
		private int height;
		private Bitmap mDrawBuffer;
		private boolean mCanceled = false;
		private final RectF viewport;

		public BackgroundDrawTask(RectF viewport){
			this.width = getMeasuredWidth();
			this.height = getMeasuredHeight();
			this.viewport = viewport;

            final Matrix toScreenMatrix = new Matrix();
            toScreenMatrix.setPolyToPoly(
                    new float[]{mXAxis.toLinearUnit(viewport.left), mYAxis.toLinearUnit(viewport.bottom), mXAxis.toLinearUnit(viewport.right), mYAxis.toLinearUnit(viewport.top)}, 0,
                    new float[]{0,height,width,0},0,
                    2);

            mToScreen = new MultivariateFunction() {

                float[] point = new float[2];

                @Override
                public float[] value(float[] frompoint) {
                    point[0] = mXAxis.toLinearUnit(frompoint[0]);
                    point[1] = mYAxis.toLinearUnit(frompoint[1]);
                    toScreenMatrix.mapPoints(point);
                    return point;
                }
            };


        }

		@Override
		public void onBackground() {
			if(!mCanceled){
				mDrawBuffer = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
                Canvas canvas = new Canvas(mDrawBuffer);
                for(DataRenderer r : mSeries){
                    r.draw(canvas, viewport, mToScreen);
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
            drawFrame(getViewport());
			invalidate();
			return true;
		}

		@Override
		public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
			mTransformMatrix.postTranslate(-distanceX, -distanceY);
            drawFrame(getViewport());
			invalidate();
			return true;
		}

	};

	private XYScaleGestureDetector.SimpleOnScaleGestureListener mSimpleScaleGestureListener = new XYScaleGestureDetector.SimpleOnScaleGestureListener(){

		@Override
		public boolean onScale(XYScaleGestureDetector detector) {
			
			mTransformMatrix.postScale(detector.getXScaleFactor(), detector.getYScaleFactor(), detector.getFocusX(), detector.getFocusY());
            drawFrame(getViewport());
			invalidate();
			return true;

		}

	};


	public void zoomInCenter() {
		float scale = 1.3f;
		mTransformMatrix.postScale(scale, scale, getMeasuredWidth()/2, getMeasuredHeight()/2);
        drawFrame(getViewport());
		invalidate();
	}

	public void zoomOutCenter() {
		float scale = 0.7f;
		mTransformMatrix.postScale(scale, scale, getMeasuredWidth()/2, getMeasuredHeight()/2);
        drawFrame(getViewport());
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
