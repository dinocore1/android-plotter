package com.devsmart.plotterexample.app;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;

import com.devsmart.plotter.Function;
import com.devsmart.plotter.GraphView;
import com.devsmart.plotter.graph.FunctionRenderer;


public class LineGraphExample extends Activity {


    private GraphView mGraphView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_line_graph_example);

        mGraphView = (GraphView) findViewById(R.id.graphview);
        mGraphView.addSeries(createSinFunction());


    }

    private FunctionRenderer createSinFunction() {
        return new FunctionRenderer(new Function() {
            @Override
            public double value(double x) {
                return 3 * Math.sin(x);
            }
        }, Color.GREEN, 2.0f);
    }


}
