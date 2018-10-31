// Copyright 2004-present Facebook. All Rights Reserved.

package com.sxw.shimmerlayout.shimmer;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.ViewTreeObserver;
import android.widget.RelativeLayout;

public class ShimmerRelativeLayout extends RelativeLayout implements ShimmerCallBack {
    private ShimmerHelper mShimmerHelper;
    private ViewTreeObserver.OnGlobalLayoutListener mOnGlobalLayoutListener;

    public ShimmerRelativeLayout(Context context) {
        this(context, null, 0);
    }

    public ShimmerRelativeLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ShimmerRelativeLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        setWillNotDraw(false);
        mShimmerHelper = new ShimmerHelper(this, context, attrs);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (mOnGlobalLayoutListener == null) {
            mOnGlobalLayoutListener = getLayoutListener();
        }
        getViewTreeObserver().addOnGlobalLayoutListener(mOnGlobalLayoutListener);
    }

    private ViewTreeObserver.OnGlobalLayoutListener getLayoutListener() {
        return new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                startAnimation();
            }
        };
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mOnGlobalLayoutListener != null) {
            getViewTreeObserver().removeOnGlobalLayoutListener(mOnGlobalLayoutListener);
        }
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        if (!mShimmerHelper.isAnimationStarted() || getWidth() <= 0 || getHeight() <= 0) {
            super.dispatchDraw(canvas);
            return;
        }
        mShimmerHelper.dispatchDrawUsingBitmap(canvas);
    }

    @Override
    public int getViewWidth() {
        return getWidth();
    }

    @Override
    public int getViewHeight() {
        return getHeight();
    }

    @Override
    public void doInvalidate() {
        invalidate();
    }

    @Override
    public void startAnimation() {
        if (mShimmerHelper != null) {
            mShimmerHelper.startShimmerAnimation();
        }
    }

    @Override
    public void stopAnimation() {
        if (mShimmerHelper != null) {
            mShimmerHelper.stopShimmerAnimation();
        }
    }

    @Override
    public void superDispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
    }
}
