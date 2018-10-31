package com.sxw.shimmerlayout.shimmer;

import android.graphics.Canvas;

/**
 * Administrator on 2018/10/30/030 19:27
 */
public interface ShimmerCallBack {

    int getViewWidth();

    int getViewHeight();

    void doInvalidate();

    void startAnimation();

    void stopAnimation();

    void superDispatchDraw(Canvas canvas);
}
