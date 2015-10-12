/*
 * Copyright (c) 2015 Zhang Hai <Dreaming.in.Code.ZH@Gmail.com>
 * All Rights Reserved.
 */

package me.zhanghai.android.materialedittext;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.annotation.NonNull;

import me.zhanghai.android.materialedittext.internal.ThemeUtils;

public class EditTextDrawable extends DrawableBase {

    private static final int INTRINSIC_PADDING_BOTTOM_DP = 4;

    private static final int DEFAULT_HEIGHT_DP = 1;
    private static final int ACTIVATED_HEIGHT_DP = 2;

    // As android.graphics.drawable.RippleDrawable.MAX_RIPPLES.
    private static final int MAX_RIPPLES = 10;

    private final Rect mPadding;

    private final int mDefaultHeight;
    private final int mActivatedHeight;

    private final int mColorHint;
    private final float mDisabledAlpha;

    private float mDensity;

    private Rect mDefaultRect = new Rect();
    private Rect mActivatedRect = new Rect();

    private Paint mDefaultPaint;

    private boolean mEnabled;
    private boolean mPressed;
    private boolean mFocused;

    private boolean mRippleActive = false;

    private boolean mHasPendingRipple = false;
    private float mPendingRipplePosition;

    private LinearRipple mRipple;

    private LinearRipple[] mExitingRipples;
    private int mExitingRipplesCount;

    public EditTextDrawable(Context context) {
        super(context);

        Resources resources = context.getResources();
        mDensity = resources.getDisplayMetrics().density;

        int paddingHorizontal = resources.getDimensionPixelOffset(
                R.dimen.abc_edit_text_inset_horizontal_material);
        int paddingTop = resources.getDimensionPixelOffset(
                R.dimen.abc_edit_text_inset_top_material);
        float paddingBottomF = resources.getDimension(R.dimen.abc_edit_text_inset_bottom_material);
        paddingBottomF += INTRINSIC_PADDING_BOTTOM_DP * mDensity;
        // As in android.util.TypedValue.complexToDimensionPixelOffset().
        int paddingBottom = (int) paddingBottomF;
        mPadding = new Rect(paddingHorizontal, paddingTop, paddingHorizontal, paddingBottom);

        mDefaultHeight = (int) (DEFAULT_HEIGHT_DP * mDensity + 0.5f);
        mActivatedHeight = (int) (ACTIVATED_HEIGHT_DP * mDensity + 0.5f);

        mColorHint = ThemeUtils.getColorFromAttrRes(android.R.attr.textColorHint, context);
        mDisabledAlpha = ThemeUtils.getFloatFromAttrRes(android.R.attr.disabledAlpha, context);
    }

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
        int drawingRectTop = bounds.bottom - mActivatedHeight;
        int drawingRectRight = bounds.right - mPadding.right;
        mDefaultRect.set(drawingRectLeft, drawingRectTop, drawingRectRight,
                drawingRectTop + mDefaultHeight);
        mActivatedRect.set(drawingRectLeft, drawingRectTop, drawingRectRight, bounds.bottom);

        if (mRipple != null) {
            mRipple.onBoundsChange(mActivatedRect);
        }

        invalidateSelf();
    }

    @Override
    public void getOutline(@NonNull Outline outline) {
        outline.setRect(mRippleActive ? mActivatedRect : mDefaultRect);
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

        boolean rippleActive = mEnabled && mPressed && mFocused;
        // FIXME: Take enabled into account.
        boolean changed = mRippleActive != rippleActive;
        setRippleActive(rippleActive);

        return changed;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void jumpToCurrentState() {
        //mRipple.finish();
        //cancelExitingRipples();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean setVisible(boolean visible, boolean restart) {

        boolean changed = super.setVisible(visible, restart);

        if (!visible) {
            clearRipples();
        } else {
            // If we just became visible, ensure the ripple visibilities are consistent with their
            // internal states.
            if (changed && mRippleActive) {
                tryRippleEnter();
            }
        }
        if (changed) {
            // Skip animations, just show the correct final states.
            jumpToCurrentState();
        }

        return changed;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setHotspot(float x, float y) {
        x -= mActivatedRect.left;
        if (mRipple == null) {
            mPendingRipplePosition = x;
            mHasPendingRipple = true;
        } else {
            mRipple.moveTo(x);
        }
    }

    private void setRippleActive(boolean active) {
        if (mRippleActive != active) {
            mRippleActive = active;
            if (mRippleActive) {
                tryRippleEnter();
            } else {
                tryRippleExit();
            }
        }
    }

    /**
     * Attempts to start an enter animation for the active hotspot. Fails if
     * there are too many animating ripples.
     */
    private void tryRippleEnter() {

        if (mExitingRipplesCount >= MAX_RIPPLES) {
            // This should never happen unless the user is tapping like a maniac
            // or there is a bug that's preventing ripples from being removed.
            return;
        }

        if (mRipple == null) {
            float position;
            if (mHasPendingRipple) {
                mHasPendingRipple = false;
                position = mPendingRipplePosition;
            } else {
                position = mActivatedRect.exactCenterX();
            }
            mRipple = new LinearRipple(this, mActivatedRect, position, mDensity);
        }

        mRipple.enter();
    }

    /**
     * Attempts to start an exit animation for the active hotspot. Fails if
     * there is no active hotspot.
     */
    private void tryRippleExit() {
        if (mRipple != null) {
            if (mExitingRipples == null) {
                mExitingRipples = new LinearRipple[MAX_RIPPLES];
            }
            mExitingRipples[mExitingRipplesCount++] = mRipple;
            mRipple.exit();
            mRipple = null;
        }
    }

    private void cancelExitingRipples() {

        int count = mExitingRipplesCount;
        for (int i = 0; i < count; i++) {
            mExitingRipples[i].finish();
        }

        if (mExitingRipples != null) {
            Arrays.fill(mExitingRipples, 0, count, null);
        }
        mExitingRipplesCount = 0;

        // Always draw an additional "clean" frame after canceling animations.
        invalidateSelf();
    }

    /**
     * Cancels and removes the active ripple, all exiting ripples. Nothing will be drawn after this
     * method is called.
     */
    private void clearRipples() {
        if (mRipple != null) {
            mRipple.finish();
            mRipple = null;
            mRippleActive = false;
        }

        cancelExitingRipples();
    }

    @Override
    protected void onPreparePaint(Paint paint) {
        paint.setStyle(Paint.Style.FILL);
    }

    @Override
    protected void onDraw(Canvas canvas, Paint paint) {

        pruneRipples();

        drawDefault(canvas);
        drawRipples(canvas);
    }

    private void drawDefault(Canvas canvas) {

        if (mDefaultPaint == null) {
            mDefaultPaint = new Paint();
            mDefaultPaint.setAntiAlias(true);
            mDefaultPaint.setColor(mColorHint);
        }
        int alpha = mEnabled ? mAlpha : (int) (mDisabledAlpha * mAlpha + 0.5f);
        mDefaultPaint.setAlpha(alpha);

        canvas.drawRect(mDefaultRect, mDefaultPaint);
    }

    private void pruneRipples() {

        // Move remaining entries into pruned spaces.
        int remaining = 0;
        for (int i = 0; i < mExitingRipplesCount; i++) {
            if (!mExitingRipples[i].isAnimationEnded()) {
                mExitingRipples[remaining++] = mExitingRipples[i];
            }
        }

        // Null out the remaining entries.
        for (int i = remaining; i < mExitingRipplesCount; i++) {
            mExitingRipples[i] = null;
        }

        mExitingRipplesCount = remaining;
    }

    private void drawRipples(Canvas canvas) {
        final LinearRipple active = mRipple;
        final int count = mExitingRipplesCount;
        if (active == null && count <= 0) {
            // Move along, nothing to draw here.
            return;
        }

        // Grab the color for the current state and cut the alpha channel in
        // half so that the ripple and background together yield full alpha.
        final int color = mState.mColor.getColorForState(getState(), Color.BLACK);
        final int halfAlpha = (Color.alpha(color) / 2) << 24;
        final Paint p = getRipplePaint();

        final int halfAlphaColor = (color & 0xFFFFFF) | halfAlpha;
        p.setColor(halfAlphaColor);
        p.setColorFilter(null);
        p.setShader(null);

        if (count > 0) {
            final LinearRipple[] ripples = mExitingRipples;
            for (int i = 0; i < count; i++) {
                ripples[i].draw(canvas, p);
            }
        }

        if (active != null) {
            active.draw(canvas, p);
        }
    }
}
