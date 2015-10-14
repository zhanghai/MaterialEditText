/*
 * Copyright (c) 2015 Zhang Hai <Dreaming.in.Code.ZH@Gmail.com>
 * All Rights Reserved.
 */

package me.zhanghai.android.materialedittext;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.EditText;

import me.zhanghai.android.materialedittext.internal.ThemeUtils;

/**
 * A Material Design background {@link Drawable} for {@link EditText}.
 */
public class MaterialEditTextBackgroundDrawable extends DrawableBase {

    private static final String TAG = MaterialEditTextBackgroundDrawable.class.getName();

    private static final int INTRINSIC_WIDTH_DP = 20;
    private static final int INTRINSIC_HEIGHT_DP = 24;

    private static final int INTRINSIC_PADDING_HORIZONTAL = 4;
    private static final int INTRINSIC_PADDING_TOP = 4;
    private static final int INTRINSIC_PADDING_BOTTOM = 13;

    private static final int DRAWING_RECT_TOP_FROM_BOTTOM_DP = 6;
    private static final int DEFAULT_HEIGHT_DP = 1;
    private static final int ACTIVATED_HEIGHT_DP = 2;

    // As android.graphics.drawable.RippleDrawable.MAX_RIPPLES.
    private static final int MAX_RIPPLES = 10;

    private final Rect mPadding;

    private final int mIntrinsicWidth;
    private final int mIntrinsicHeight;
    private final int mDrawingRectTopFromBottom;

    private final int mDefaultHeight;
    private final int mActivatedHeight;

    private final int mColorHint;
    private final float mColorHintAlpha;
    private final float mDisabledAlpha;

    private float mDensity;

    private Rect mDefaultRect = new Rect();
    private Rect mActivatedRect = new Rect();

    private Paint mDefaultPaint;

    private boolean mEnabled;
    private boolean mPressed;
    private boolean mFocused;
    private boolean mError;

    private boolean mHasPendingRipple = false;
    private float mPendingRipplePosition;

    private LinearRipple mEnteringRipple;
    private LinearRipple[] mFillingRipples = new LinearRipple[MAX_RIPPLES];
    private int mFillingRippleCount;
    private LinearRipple mFilledRipple;
    private LinearRipple[] mExitingRipples = new LinearRipple[MAX_RIPPLES];
    private int mExitingRippleCount;

    public MaterialEditTextBackgroundDrawable(Context context) {
        super(context);

        Resources resources = context.getResources();
        mDensity = resources.getDisplayMetrics().density;

        mIntrinsicWidth = (int) (INTRINSIC_WIDTH_DP * mDensity + 0.5f);
        mIntrinsicHeight = (int) (INTRINSIC_HEIGHT_DP * mDensity + 0.5f);

        // As in android.util.TypedValue.complexToDimensionPixelOffset().
        int paddingHorizontal = (int) (INTRINSIC_PADDING_HORIZONTAL * mDensity);
        int paddingTop = (int) (INTRINSIC_PADDING_TOP * mDensity);
        int paddingBottom = (int) (INTRINSIC_PADDING_BOTTOM * mDensity);
        mPadding = new Rect(paddingHorizontal, paddingTop, paddingHorizontal, paddingBottom);

        mDrawingRectTopFromBottom = (int) (DRAWING_RECT_TOP_FROM_BOTTOM_DP * mDensity + 0.5f);
        mDefaultHeight = (int) (DEFAULT_HEIGHT_DP * mDensity + 0.5f);
        mActivatedHeight = (int) (ACTIVATED_HEIGHT_DP * mDensity + 0.5f);

        mColorHint = ThemeUtils.getColorFromAttrRes(android.R.attr.textColorHint, context);
        mColorHintAlpha = (float) Color.alpha(mColorHint) / 0xFF;
        mDisabledAlpha = ThemeUtils.getFloatFromAttrRes(android.R.attr.disabledAlpha, context);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getIntrinsicWidth() {
        return mIntrinsicWidth;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getIntrinsicHeight() {
        return mIntrinsicHeight;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean getPadding(@NonNull Rect padding) {
        padding.set(mPadding);
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onBoundsChange(Rect bounds) {

        int drawingRectLeft = bounds.left + mPadding.left;
        int drawingRectTop = bounds.bottom - mDrawingRectTopFromBottom;
        int drawingRectRight = bounds.right - mPadding.right;
        mDefaultRect.set(drawingRectLeft, drawingRectTop, drawingRectRight,
                drawingRectTop + mDefaultHeight);
        mActivatedRect.set(drawingRectLeft, drawingRectTop, drawingRectRight,
                drawingRectTop + mActivatedHeight);

        if (mEnteringRipple != null) {
            mEnteringRipple.onBoundsChange(mActivatedRect);
        }
        for (int i = 0; i < mFillingRippleCount; ++i) {
            mFillingRipples[i].onBoundsChange(mActivatedRect);
        }
        if (mFilledRipple != null) {
            mFilledRipple.onBoundsChange(mActivatedRect);
        }
        for (int i = 0; i < mExitingRippleCount; ++i) {
            mExitingRipples[i].onBoundsChange(mActivatedRect);
        }

        invalidateSelf();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void getOutline(@NonNull Outline outline) {
        outline.setRect(hasRipple() ? mActivatedRect : mDefaultRect);
    }

    private boolean hasRipple() {
        return mEnteringRipple != null || mFillingRippleCount > 0 || mFilledRipple != null
                || mExitingRippleCount >= 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setHotspot(float x, float y) {
        x -= mActivatedRect.left;
        if (mEnteringRipple == null) {
            mPendingRipplePosition = x;
            mHasPendingRipple = true;
        } else {
            mEnteringRipple.moveTo(x);
        }
    }

    /**
     * Get whether this drawable is in error state. The default is {@code false}.
     *
     * @return Whether this drawable is in error state.
     */
    public boolean hasError() {
        return mError;
    }

    /**
     * Set whether this drawable is in error state. The default is {@code false}.
     *
     * @param error Whether this drawable should be in error state.
     */
    public void setError(boolean error) {
        if (mError != error) {
            mError = error;
            onStateChanged();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isStateful() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean onStateChange(int[] stateSet) {

        mEnabled = false;
        mPressed = false;
        mFocused = false;
        for (int state : stateSet) {
            switch (state) {
                case android.R.attr.state_enabled:
                    mEnabled = true;
                    break;
                case android.R.attr.state_pressed:
                    mPressed = true;
                    break;
                case android.R.attr.state_focused:
                    mFocused = true;
                    break;
            }
        }

        onStateChanged();

        // Be safe.
        return true;
    }

    private void onStateChanged() {

        updateRipples();

        // Branch into states and operate on each non-exiting group of ripple.
        if (!mEnabled || (!mPressed && !mFocused && !mError)) {
            // Disabled, or enabled and unpressed and unfocused.
            // Exit non-exiting ripples.
            exitRipples();
        } else if (mPressed) {
            // Enabled and pressed.
            // Enter a new ripple if no ripple is entering or filled; leave filling, filled ripples
            // unchanged.
            if (mEnteringRipple == null && mFilledRipple == null) {
                createAndEnterRipple();
            }
        } else {
            // Enabled, unpressed, focused || error.
            // Fill the entering ripple if exists.
            if (mEnteringRipple != null) {
                fillEnteringRipple();
            }
            // Ensure a filling or filled ripple.
            if (mFillingRippleCount == 0 && mFilledRipple == null) {
                createFillingRipple();
            }
        }

        invalidateSelf();
    }

    private void removeEnteringRipple() {
        if (mEnteringRipple != null) {
            mEnteringRipple.endAnimation();
            mEnteringRipple = null;
        }
    }

    private void removeFillingRipples() {
        for (int i = 0; i < mFillingRippleCount; ++i) {
            mFillingRipples[i].endAnimation();
            mFillingRipples[i] = null;
        }
        mFillingRippleCount = 0;
    }

    private void removeFilledRipple() {
        mFilledRipple = null;
    }

    private void removeExitingRipples() {
        for (int i = 0; i < mExitingRippleCount; ++i) {
            mExitingRipples[i].endAnimation();
            mExitingRipples[i] = null;
        }
        mExitingRippleCount = 0;
    }

    private void updateRipples() {

        // Update filled ripple.
        if (mFilledRipple == null) {
            if (mEnteringRipple != null && mEnteringRipple.hasFilled()) {
                mFilledRipple = mEnteringRipple;
            }
        }
        if (mFilledRipple == null) {
            for (int i = 0; i < mFillingRippleCount; ++i) {
                LinearRipple fillingRipple = mFillingRipples[i];
                if (fillingRipple.hasFilled()) {
                    mFilledRipple = mFillingRipples[i];
                    break;
                }
            }
        }

        if (mFilledRipple != null) {

            // Clear all ripples except for the filled one.
            removeEnteringRipple();
            removeFillingRipples();
            removeExitingRipples();

        } else {

            // Remove exited ripples.
            int remaining = 0;
            for (int i = 0; i < mExitingRippleCount; ++i) {
                if (!mExitingRipples[i].hasExited()) {
                    mExitingRipples[remaining++] = mExitingRipples[i];
                }
            }
            for (int i = remaining; i < mExitingRippleCount; ++i) {
                mExitingRipples[i] = null;
            }
            mExitingRippleCount = remaining;
        }
    }

    private void exitRipple(LinearRipple ripple) {
        ripple.exit();
        mExitingRipples[mExitingRippleCount++] = ripple;
    }

    private void exitRipples() {

        if (mEnteringRipple != null) {
            exitRipple(mEnteringRipple);
            mEnteringRipple = null;
        }

        for (int i = 0; i < mFillingRippleCount; ++i) {
            exitRipple(mFillingRipples[i]);
            mFillingRipples[i] = null;
        }
        mFillingRippleCount = 0;

        if (mFilledRipple != null) {
            exitRipple(mFilledRipple);
            mFilledRipple = null;
        }
    }

    private LinearRipple createRipple() {

        if (mFillingRippleCount + mExitingRippleCount >= MAX_RIPPLES) {
            Log.w(TAG, "Too many ripples alive, skipping ripple creation");
            return null;
        }

        float position;
        if (mHasPendingRipple) {
            mHasPendingRipple = false;
            position = mPendingRipplePosition;
        } else {
            position = mActivatedRect.exactCenterX();
        }
        return new LinearRipple(this, mActivatedRect, position, mDensity);
    }

    private void createAndEnterRipple() {

        // DEBUG: Remove this.
        if (mEnteringRipple != null) {
            throw new IllegalStateException("createAndEnterRipple() when mEnteringRipple is not null");
        }

        mEnteringRipple = createRipple();
        if (mEnteringRipple != null) {
            mEnteringRipple.enter();
        }
    }

    private void fillRipple(LinearRipple ripple) {
        ripple.fill();
        mFillingRipples[mFillingRippleCount++] = ripple;
    }

    private void fillEnteringRipple() {

        // DEBUG: Remove this.
        if (mEnteringRipple == null) {
            throw new IllegalStateException("fillEnteringRipple() when mEnteringRipple is not null");
        }

        fillRipple(mEnteringRipple);
        mEnteringRipple = null;
    }

    private void createFillingRipple() {
        LinearRipple ripple = createRipple();
        fillRipple(ripple);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void jumpToCurrentState() {

        updateRipples();

        removeEnteringRipple();
        removeFillingRipples();
        removeExitingRipples();

        if (mEnabled && (mPressed || mFocused)) {
            if (mFilledRipple == null) {
                createFilledRipple();
            }
        } else {
            removeFilledRipple();
        }

        invalidateSelf();
    }

    private void createFilledRipple() {
        LinearRipple ripple = createRipple();
        if (ripple != null) {
            ripple.makeFilled();
            mFilledRipple = ripple;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean setVisible(boolean visible, boolean restart) {

        updateRipples();

        boolean changed = super.setVisible(visible, restart);
        if (changed && !visible) {
            jumpToCurrentState();
        }
        return changed;
    }

    @Override
    protected void onDraw(Canvas canvas, Paint paint) {
        drawDefault(canvas);
        drawRipples(canvas, paint);
    }

    private void drawDefault(Canvas canvas) {

        if (mDefaultPaint == null) {
            mDefaultPaint = new Paint();
            mDefaultPaint.setAntiAlias(true);
            mDefaultPaint.setColor(mColorHint);
        }
        int alpha = (int) ((mEnabled ? 1 : mDisabledAlpha) * mColorHintAlpha * mAlpha + 0.5f);
        mDefaultPaint.setAlpha(alpha);

        canvas.drawRect(mDefaultRect, mDefaultPaint);
    }

    @Override
    protected void onPreparePaint(Paint paint) {
        paint.setStyle(Paint.Style.FILL);
    }

    private void drawRipples(Canvas canvas, Paint paint) {

        updateRipples();

        if (mFilledRipple != null) {
            mFilledRipple.draw(canvas, paint);
        } else {

            for (int i = 0; i < mExitingRippleCount; ++i) {
                mExitingRipples[i].draw(canvas, paint);
            }

            for (int i = 0; i < mFillingRippleCount; ++i) {
                mFillingRipples[i].draw(canvas, paint);
            }

            if (mEnteringRipple != null) {
                mEnteringRipple.draw(canvas, paint);
            }
        }
    }
}
