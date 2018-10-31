package com.sxw.shimmerlayout.shimmer;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.util.Log;

import com.sxw.shimmerlayout.R;


/**
 * 孙贤武 on 2018/10/30/030 18:15
 */
public class ShimmerHelper {
    private static final String TAG = "ShimmerHelper";
    private static final PorterDuffXfermode DST_IN_PORTER_DUFF_XFERMODE = new PorterDuffXfermode(PorterDuff.Mode.DST_IN);

    public enum MaskShape {
        LINEAR,
        RADIAL
    }

    public enum MaskAngle {
        CW_0, // left to right
        CW_90, // top to bottom
        CW_180, // right to left
        CW_270, // bottom to top
    }

    private static class Mask {

        public MaskAngle angle;
        public float tilt;
        public float dropoff;
        public int fixedWidth;
        public int fixedHeight;
        public float intensity;
        public float relativeWidth;
        public float relativeHeight;
        public MaskShape shape;

        public int maskWidth(int width) {
            return fixedWidth > 0 ? fixedWidth : (int) (width * relativeWidth);
        }

        public int maskHeight(int height) {
            return fixedHeight > 0 ? fixedHeight : (int) (height * relativeHeight);
        }

        /**
         * Get the array of colors to be distributed along the gradient of the mask bitmap
         *
         * @return An array of black and transparent colors
         */
        public int[] getGradientColors() {
            switch (shape) {
                default:
                case LINEAR:
                    return new int[]{Color.TRANSPARENT, Color.BLACK, Color.BLACK, Color.TRANSPARENT};
                case RADIAL:
                    return new int[]{Color.BLACK, Color.BLACK, Color.TRANSPARENT};
            }
        }

        /**
         * Get the array of relative positions [0..1] of each corresponding color in the colors array
         *
         * @return A array of float values in the [0..1] range
         */
        public float[] getGradientPositions() {
            switch (shape) {
                default:
                case LINEAR:
                    return new float[]{
                            Math.max((1.0f - intensity - dropoff) / 2, 0.0f),
                            Math.max((1.0f - intensity) / 2, 0.0f),
                            Math.min((1.0f + intensity) / 2, 1.0f),
                            Math.min((1.0f + intensity + dropoff) / 2, 1.0f)};
                case RADIAL:
                    return new float[]{
                            0.0f,
                            Math.min(intensity, 1.0f),
                            Math.min(intensity + dropoff, 1.0f)};
            }
        }
    }

    // struct for storing the mask translation animation values
    private static class MaskTranslation {

        public int fromX;
        public int fromY;
        public int toX;
        public int toY;

        public void set(int fromX, int fromY, int toX, int toY) {
            this.fromX = fromX;
            this.fromY = fromY;
            this.toX = toX;
            this.toY = toY;
        }
    }

    private Paint mAlphaPaint;
    private Paint mMaskPaint;

    private Mask mMask;
    private MaskTranslation mMaskTranslation;

    private Bitmap mRenderMaskBitmap;
    private Bitmap mRenderUnmaskBitmap;

    private boolean mAutoStart;
    private int mDuration;
    private int mRepeatCount;
    private int mRepeatDelay;
    private int mRepeatMode;

    private int mMaskOffsetX;
    private int mMaskOffsetY;

    private boolean mAnimationStarted;

    protected ValueAnimator mAnimator;
    protected Bitmap mMaskBitmap;
    private ShimmerCallBack mShimmerCallBack;

    public boolean isAnimationStarted() {
        return mAnimationStarted;
    }

    public ShimmerHelper(ShimmerCallBack mShimmerCallBack, Context context, AttributeSet attrs) {
        this.mShimmerCallBack = mShimmerCallBack;
        mMask = new Mask();
        mAlphaPaint = new Paint();
        mMaskPaint = new Paint();
        mMaskPaint.setAntiAlias(true);
        mMaskPaint.setDither(true);
        mMaskPaint.setFilterBitmap(true);
        mMaskPaint.setXfermode(DST_IN_PORTER_DUFF_XFERMODE);
        useDefaults();

        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ShimmerFrameLayout, 0, 0);
            try {
                if (a.hasValue(R.styleable.ShimmerFrameLayout_auto_start)) {
                    setAutoStart(a.getBoolean(R.styleable.ShimmerFrameLayout_auto_start, false));
                }
                if (a.hasValue(R.styleable.ShimmerFrameLayout_base_alpha)) {
                    setBaseAlpha(a.getFloat(R.styleable.ShimmerFrameLayout_base_alpha, 0));
                }
                if (a.hasValue(R.styleable.ShimmerFrameLayout_duration)) {
                    setDuration(a.getInt(R.styleable.ShimmerFrameLayout_duration, 0));
                }
                if (a.hasValue(R.styleable.ShimmerFrameLayout_repeat_count)) {
                    setRepeatCount(a.getInt(R.styleable.ShimmerFrameLayout_repeat_count, 0));
                }
                if (a.hasValue(R.styleable.ShimmerFrameLayout_repeat_delay)) {
                    setRepeatDelay(a.getInt(R.styleable.ShimmerFrameLayout_repeat_delay, 0));
                }
                if (a.hasValue(R.styleable.ShimmerFrameLayout_repeat_mode)) {
                    setRepeatMode(a.getInt(R.styleable.ShimmerFrameLayout_repeat_mode, 0));
                }

                if (a.hasValue(R.styleable.ShimmerFrameLayout_angle)) {
                    int angle = a.getInt(R.styleable.ShimmerFrameLayout_angle, 0);
                    switch (angle) {
                        default:
                        case 0:
                            mMask.angle = MaskAngle.CW_0;
                            break;
                        case 90:
                            mMask.angle = MaskAngle.CW_90;
                            break;
                        case 180:
                            mMask.angle = MaskAngle.CW_180;
                            break;
                        case 270:
                            mMask.angle = MaskAngle.CW_270;
                            break;
                    }
                }

                if (a.hasValue(R.styleable.ShimmerFrameLayout_shape)) {
                    int shape = a.getInt(R.styleable.ShimmerFrameLayout_shape, 0);
                    switch (shape) {
                        default:
                        case 0:
                            mMask.shape = MaskShape.LINEAR;
                            break;
                        case 1:
                            mMask.shape = MaskShape.RADIAL;
                            break;
                    }
                }

                if (a.hasValue(R.styleable.ShimmerFrameLayout_dropoff)) {
                    mMask.dropoff = a.getFloat(R.styleable.ShimmerFrameLayout_dropoff, 0);
                }
                if (a.hasValue(R.styleable.ShimmerFrameLayout_fixed_width)) {
                    mMask.fixedWidth = a.getDimensionPixelSize(R.styleable.ShimmerFrameLayout_fixed_width, 0);
                }
                if (a.hasValue(R.styleable.ShimmerFrameLayout_fixed_height)) {
                    mMask.fixedHeight = a.getDimensionPixelSize(R.styleable.ShimmerFrameLayout_fixed_height, 0);
                }
                if (a.hasValue(R.styleable.ShimmerFrameLayout_intensity)) {
                    mMask.intensity = a.getFloat(R.styleable.ShimmerFrameLayout_intensity, 0);
                }
                if (a.hasValue(R.styleable.ShimmerFrameLayout_relative_width)) {
                    mMask.relativeWidth = a.getFloat(R.styleable.ShimmerFrameLayout_relative_width, 0);
                }
                if (a.hasValue(R.styleable.ShimmerFrameLayout_relative_height)) {
                    mMask.relativeHeight = a.getFloat(R.styleable.ShimmerFrameLayout_relative_height, 0);
                }
                if (a.hasValue(R.styleable.ShimmerFrameLayout_tilt)) {
                    mMask.tilt = a.getFloat(R.styleable.ShimmerFrameLayout_tilt, 0);
                }
            } finally {
                a.recycle();
            }
        }

    }

    public void useDefaults() {
        // Set defaults
        setAutoStart(false);
        setDuration(1000);
        setRepeatCount(ObjectAnimator.INFINITE);
        setRepeatDelay(0);
        setRepeatMode(ObjectAnimator.RESTART);

        mMask.angle = MaskAngle.CW_0;
        mMask.shape = MaskShape.LINEAR;
        mMask.dropoff = 0.5f;
        mMask.fixedWidth = 0;
        mMask.fixedHeight = 0;
        mMask.intensity = 0.0f;
        mMask.relativeWidth = 1.0f;
        mMask.relativeHeight = 1.0f;
        mMask.tilt = 20;

        mMaskTranslation = new MaskTranslation();

        setBaseAlpha(0.3f);

        resetAll();
    }

    public boolean isAutoStart() {
        return mAutoStart;
    }

    public void setAutoStart(boolean autoStart) {
        mAutoStart = autoStart;
        resetAll();
    }

    public void setDuration(int duration) {
        mDuration = duration;
        resetAll();
    }

    public void setRepeatCount(int repeatCount) {
        mRepeatCount = repeatCount;
        resetAll();
    }

    public void setRepeatDelay(int repeatDelay) {
        mRepeatDelay = repeatDelay;
        resetAll();
    }

    public void setRepeatMode(int repeatMode) {
        mRepeatMode = repeatMode;
        resetAll();
    }

    public void setBaseAlpha(float alpha) {
        mAlphaPaint.setAlpha((int) (clamp(0, 1, alpha) * 0xff));
        resetAll();
    }

    public void resetAll() {
        stopShimmerAnimation();
        resetMaskBitmap();
        resetRenderedView();
    }

    public void stopShimmerAnimation() {
        if (mAnimator != null) {
            mAnimator.end();
            mAnimator.removeAllUpdateListeners();
            mAnimator.cancel();
        }
        mAnimator = null;
        mAnimationStarted = false;
    }

    private void resetMaskBitmap() {
        if (mMaskBitmap != null) {
            mMaskBitmap.recycle();
            mMaskBitmap = null;
        }
    }

    private void resetRenderedView() {
        if (mRenderUnmaskBitmap != null) {
            mRenderUnmaskBitmap.recycle();
            mRenderUnmaskBitmap = null;
        }

        if (mRenderMaskBitmap != null) {
            mRenderMaskBitmap.recycle();
            mRenderMaskBitmap = null;
        }
    }

    private static float clamp(float min, float max, float value) {
        return Math.min(max, Math.max(min, value));
    }

    public boolean dispatchDrawUsingBitmap(Canvas canvas) {
        Bitmap unmaskBitmap = tryObtainRenderUnmaskBitmap();
        Bitmap maskBitmap = tryObtainRenderMaskBitmap();
        if (unmaskBitmap == null || maskBitmap == null) {
            return false;
        }
        // First draw a desaturated version
        drawUnmasked(new Canvas(unmaskBitmap));
        canvas.drawBitmap(unmaskBitmap, 0, 0, mAlphaPaint);

        // Then draw the masked version
        drawMasked(new Canvas(maskBitmap));
        canvas.drawBitmap(maskBitmap, 0, 0, null);

        return true;
    }

    private void drawUnmasked(Canvas renderCanvas) {
        mShimmerCallBack.superDispatchDraw(renderCanvas);
    }

    private void drawMasked(Canvas renderCanvas) {
        Bitmap maskBitmap = getMaskBitmap();
        if (maskBitmap == null) {
            return;
        }

        renderCanvas.clipRect(
                mMaskOffsetX,
                mMaskOffsetY,
                mMaskOffsetX + maskBitmap.getWidth(),
                mMaskOffsetY + maskBitmap.getHeight());
        mShimmerCallBack.superDispatchDraw(renderCanvas);

        renderCanvas.drawBitmap(maskBitmap, mMaskOffsetX, mMaskOffsetY, mMaskPaint);
    }

    public Bitmap tryObtainRenderUnmaskBitmap() {
        if (mRenderUnmaskBitmap == null) {
            mRenderUnmaskBitmap = tryCreateRenderBitmap();
        }
        return mRenderUnmaskBitmap;
    }

    public Bitmap tryObtainRenderMaskBitmap() {
        if (mRenderMaskBitmap == null) {
            mRenderMaskBitmap = tryCreateRenderBitmap();
        }
        return mRenderMaskBitmap;
    }

    private Bitmap tryCreateRenderBitmap() {
        int width = mShimmerCallBack.getViewWidth();
        int height = mShimmerCallBack.getViewHeight();
        try {
            return createBitmapAndGcIfNecessary(width, height);
        } catch (OutOfMemoryError e) {
            String logMessage = "ShimmerFrameLayout failed to create working bitmap";
            StringBuilder logMessageStringBuilder = new StringBuilder(logMessage);
            logMessageStringBuilder.append(" (width = ");
            logMessageStringBuilder.append(width);
            logMessageStringBuilder.append(", height = ");
            logMessageStringBuilder.append(height);
            logMessageStringBuilder.append(")\n\n");
            for (StackTraceElement stackTraceElement :
                    Thread.currentThread().getStackTrace()) {
                logMessageStringBuilder.append(stackTraceElement.toString());
                logMessageStringBuilder.append("\n");
            }
            logMessage = logMessageStringBuilder.toString();
            Log.d(TAG, logMessage);
        }
        return null;
    }

    protected static Bitmap createBitmapAndGcIfNecessary(int width, int height) {
        try {
            return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        } catch (OutOfMemoryError e) {
            System.gc();
            return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        }
    }

    private Bitmap getMaskBitmap() {
        if (mMaskBitmap != null) {
            return mMaskBitmap;
        }

        int width = mMask.maskWidth(mShimmerCallBack.getViewWidth());
        int height = mMask.maskHeight(mShimmerCallBack.getViewHeight());

        mMaskBitmap = createBitmapAndGcIfNecessary(width, height);
        Canvas canvas = new Canvas(mMaskBitmap);
        Shader gradient;
        switch (mMask.shape) {
            default:
            case LINEAR: {
                int x1, y1;
                int x2, y2;
                switch (mMask.angle) {
                    default:
                    case CW_0:
                        x1 = 0;
                        y1 = 0;
                        x2 = width;
                        y2 = 0;
                        break;
                    case CW_90:
                        x1 = 0;
                        y1 = 0;
                        x2 = 0;
                        y2 = height;
                        break;
                    case CW_180:
                        x1 = width;
                        y1 = 0;
                        x2 = 0;
                        y2 = 0;
                        break;
                    case CW_270:
                        x1 = 0;
                        y1 = height;
                        x2 = 0;
                        y2 = 0;
                        break;
                }
                gradient =
                        new LinearGradient(
                                x1, y1,
                                x2, y2,
                                mMask.getGradientColors(),
                                mMask.getGradientPositions(),
                                Shader.TileMode.REPEAT);
                break;
            }
            case RADIAL: {
                int x = width / 2;
                int y = height / 2;
                gradient =
                        new RadialGradient(
                                x,
                                y,
                                (float) (Math.max(width, height) / Math.sqrt(2)),
                                mMask.getGradientColors(),
                                mMask.getGradientPositions(),
                                Shader.TileMode.REPEAT);
                break;
            }
        }
        canvas.rotate(mMask.tilt, width / 2, height / 2);
        Paint paint = new Paint();
        paint.setShader(gradient);
        // We need to increase the rect size to account for the tilt
        int padding = (int) (Math.sqrt(2) * Math.max(width, height)) / 2;
        canvas.drawRect(-padding, -padding, width + padding, height + padding, paint);

        return mMaskBitmap;
    }

    public void startShimmerAnimation() {
        if (mAnimationStarted) {
            return;
        }
        Animator animator = getShimmerAnimation();
        animator.start();
        mAnimationStarted = true;
    }

    private Animator getShimmerAnimation() {
        if (mAnimator != null) {
            return mAnimator;
        }
        int width = mShimmerCallBack.getViewWidth();
        int height = mShimmerCallBack.getViewHeight();
        switch (mMask.shape) {
            default:
            case LINEAR:
                switch (mMask.angle) {
                    default:
                    case CW_0:
                        mMaskTranslation.set(-width, 0, width, 0);
                        break;
                    case CW_90:
                        mMaskTranslation.set(0, -height, 0, height);
                        break;
                    case CW_180:
                        mMaskTranslation.set(width, 0, -width, 0);
                        break;
                    case CW_270:
                        mMaskTranslation.set(0, height, 0, -height);
                        break;
                }
        }
        mAnimator = ValueAnimator.ofFloat(0.0f, 1.0f + (float) mRepeatDelay / mDuration);
        mAnimator.setDuration(mDuration + mRepeatDelay);
        mAnimator.setRepeatCount(mRepeatCount);
        mAnimator.setRepeatMode(mRepeatMode);
        mAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float value = Math.max(0.0f, Math.min(1.0f, (Float) animation.getAnimatedValue()));
                setMaskOffsetX((int) (mMaskTranslation.fromX * (1 - value) + mMaskTranslation.toX * value));
                setMaskOffsetY((int) (mMaskTranslation.fromY * (1 - value) + mMaskTranslation.toY * value));
            }
        });
        return mAnimator;
    }

    public void setMaskOffsetX(int maskOffsetX) {
        if (mMaskOffsetX == maskOffsetX) {
            return;
        }
        mMaskOffsetX = maskOffsetX;
        mShimmerCallBack.doInvalidate();
    }

    public void setMaskOffsetY(int maskOffsetY) {
        if (mMaskOffsetY == maskOffsetY) {
            return;
        }
        mMaskOffsetY = maskOffsetY;
        mShimmerCallBack.doInvalidate();
    }
}
