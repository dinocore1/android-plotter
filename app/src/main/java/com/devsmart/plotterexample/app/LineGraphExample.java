package com.devsmart.plotterexample.app;

import android.app.Activity;
import android.os.Bundle;

import com.devsmart.plotter.GraphView;


public class LineGraphExample extends Activity {


    private GraphView mGraphView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_line_graph_example);

        mGraphView = (GraphView) findViewById(R.id.graphview);


    }


}
