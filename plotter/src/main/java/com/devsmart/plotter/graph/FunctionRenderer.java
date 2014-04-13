package com.devsmart.plotter.graph;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

import com.devsmart.plotter.CoordanateSystem;
import com.devsmart.plotter.DataRenderer;
import com.devsmart.plotter.Function;

public class FunctionRenderer implements DataRenderer {

    public Function mGraphFunction;
    public Paint mPathPaint;

    public FunctionRenderer(Function graphFunction, int lineColor, float lineWidth) {
        mGraphFunction = graphFunction;
        mPathPaint = new Paint();
        mPathPaint.setColor(lineColor);
        mPathPaint.setStrokeWidth(lineWidth);
    }

    @Override
    public void draw(Canvas canvas, RectF viewPort, CoordanateSystem coordanateSystem) {

        final double pixelWidth = viewPort.width() / Math.abs(coordanateSystem.xAxis.toScreen.value(viewPort.right) - coordanateSystem.xAxis.toScreen.value(viewPort.left));
        final double stepWidth = pixelWidth;

        Path p = new Path();

        double x = viewPort.left;
        double y = mGraphFunction.value(x);

        p.moveTo((float)coordanateSystem.xAxis.toScreen.value(x),
                (float)coordanateSystem.yAxis.toScreen.value(y));

        for(x=x+stepWidth; x<=viewPort.right; x+=stepWidth) {
            y = mGraphFunction.value(x);

            p.lineTo((float)coordanateSystem.xAxis.toScreen.value(x),
                    (float)coordanateSystem.yAxis.toScreen.value(y));


        }

        canvas.drawPath(p, mPathPaint);

    }
}
