package com.devsmart.plotter;

import java.util.Iterator;

public interface Series {
    Iterator<float[]> createIterator();
}