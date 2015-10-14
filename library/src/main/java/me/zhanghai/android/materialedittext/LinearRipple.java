/*
 * Copyright (c) 2015 Zhang Hai <Dreaming.in.Code.ZH@Gmail.com>
 * All Rights Reserved.
 */

package me.zhanghai.android.materialedittext;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.animation.LinearInterpolator;

import me.zhanghai.android.materialedittext.internal.FloatProperty;
import me.zhanghai.android.materialedittext.internal.MathUtils;

/**
 * A linear ripple implemented according to the framework implementation of
 * {@code RippleForeground}.
 *
 * @see <a href="https://github.com/android/platform_frameworks_base/blob/f872ee0057ed247aa93589347f1b53afc99517f8/graphics/java/android/graphics/drawable/RippleForeground.java">RippleForeground@f872ee</a>
 */
class LinearRipple {

    private static final float WAVE_TOUCH_DOWN_ACCELERATION_DP = 1024;
    private static final float WAVE_TOUCH_UP_ACCELERATION_DP = 3400;
    private static final float WAVE_OPACITY_DECAY_VELOCITY = 3;

    private static final int RIPPLE_ENTER_DELAY = 80;
    private static final int OPACITY_ENTER_DURATION = 120;

    private static final TimeInterpolator LINEAR_INTERPOLATOR = new LinearInterpolator();
    private static final TimeInterpolator DECELERATE_INTERPOLATOR = new LogDecelerateInterpolator(
            400f, 1.4f, 0);

    private final float mWaveTouchDownAcceleration;
    private final float mWaveTouchUpAcceleration;

    private Drawable mOwner;
    private Rect mBounds;
    private float mStartingPosition;

    private float mTargetPosition;
    private float mTargetRadius;

    private float mTweenRipple;
    private float mOpacity = 1;

    private Animator mAnimator;

    public LinearRipple(Drawable owner, Rect bounds, float position, float density) {

        mWaveTouchDownAcceleration = WAVE_TOUCH_DOWN_ACCELERATION_DP * density;
        mWaveTouchUpAcceleration = WAVE_TOUCH_UP_ACCELERATION_DP * density;

        mOwner = owner;
        onBoundsChange(bounds);
        mStartingPosition = position;
    }

    public void onBoundsChange(Rect bounds) {
        mBounds = bounds;
        mTargetRadius = mBounds.width() / 2f;
        mTargetPosition = mBounds.left + mTargetRadius;
    }

    public void enter() {
        mAnimator = createEnterAnimation();
        mAnimator.start();
    }

    private int getRippleEnterDuration() {
        return (int) (1000 * Math.sqrt(mTargetRadius / mWaveTouchDownAcceleration) + 0.5);
    }

    private Animator createEnterAnimation() {

        ObjectAnimator tweenRipple = ObjectAnimator.ofFloat(this, TWEEN_RIPPLE, 1);
        int duration = getRippleEnterDuration();
        //tweenRipple.setAutoCancel(true);
        tweenRipple.setDuration(duration);
        tweenRipple.setInterpolator(LINEAR_INTERPOLATOR);
        tweenRipple.setStartDelay(RIPPLE_ENTER_DELAY);

        ObjectAnimator opacity = ObjectAnimator.ofFloat(this, OPACITY, 1);
        //opacity.setAutoCancel(true);
        opacity.setDuration(OPACITY_ENTER_DURATION);
        opacity.setInterpolator(LINEAR_INTERPOLATOR);

        AnimatorSet set = new AnimatorSet();
        set.play(tweenRipple).with(opacity);

        return set;
    }

    public void moveTo(float position) {
        mStartingPosition = position;
    }

    public void fill() {

        cancelAnimation();

        if (hasFilled()) {
            return;
        }

        mAnimator = createFillAnimation();
        mAnimator.start();
    }

    public boolean hasFilled() {
        return mTweenRipple == 1 && mOpacity == 1;
    }

    private float getCurrentRadius() {
        return MathUtils.lerp(0, mTargetRadius, mTweenRipple);
    }

    private int getRippleFillOrExitDuration() {
        float radius = getCurrentRadius();
        float remaining = mTargetRadius - radius;
        return (int) (1000 * Math.sqrt(2 * remaining /
                (mWaveTouchUpAcceleration + mWaveTouchDownAcceleration)) + 0.5);
    }

    private Animator createFillAnimation() {
        ObjectAnimator tweenRipple = ObjectAnimator.ofFloat(this, TWEEN_RIPPLE, 1);
        //tweenRipple.setAutoCancel(true);
        tweenRipple.setDuration(getRippleFillOrExitDuration());
        tweenRipple.setInterpolator(DECELERATE_INTERPOLATOR);
        return tweenRipple;
    }

    public void makeFilled() {

        mTweenRipple = 1;
        mOpacity = 1;

        invalidateSelf();
    }

    public void exit() {

        cancelAnimation();

        if (hasExited()) {
            return;
        }

        mAnimator = createExitAnimation();
        mAnimator.start();
    }

    public boolean hasExited() {
        return mOpacity == 0;
    }

    private int getOpacityExitDuration() {
        return (int) (1000 * mOpacity / WAVE_OPACITY_DECAY_VELOCITY + 0.5f);
    }

    private Animator createExitAnimation() {

        ObjectAnimator opacity = ObjectAnimator.ofFloat(this, OPACITY, 0);
        //opacity.setAutoCancel(true);
        opacity.setDuration(getOpacityExitDuration());
        opacity.setInterpolator(LINEAR_INTERPOLATOR);

        if (hasFilled()) {
            return opacity;
        } else {

            ObjectAnimator tweenRipple = ObjectAnimator.ofFloat(this, TWEEN_RIPPLE, 1);
            //tweenRipple.setAutoCancel(true);
            tweenRipple.setDuration(getRippleFillOrExitDuration());
            tweenRipple.setInterpolator(DECELERATE_INTERPOLATOR);

            AnimatorSet set = new AnimatorSet();
            set.play(tweenRipple).with(opacity);
            return set;
        }
    }

    public void cancelAnimation() {
        stopAnimation(true);
    }

    public void endAnimation() {
        stopAnimation(false);
    }

    private void stopAnimation(boolean cancel) {
        if (mAnimator == null) {
            return;
        }

        if (cancel) {
            mAnimator.cancel();
        } else {
            mAnimator.end();
        }
        mAnimator = null;
    }

    public void draw(Canvas canvas, Paint paint) {

        int origAlpha = paint.getAlpha();
        int alpha = (int) (origAlpha * mOpacity + 0.5f);
        float radius = getCurrentRadius();
        if (alpha <= 0 || radius <= 0) {
            return;
        }

        float position = MathUtils.lerp(mStartingPosition, mTargetPosition, mTweenRipple);
        float left = MathUtils.constrain(position - radius, mBounds.left, mBounds.right);
        float right = MathUtils.constrain(position + radius, mBounds.left, mBounds.right);
        paint.setAlpha(alpha);
        canvas.drawRect(left, mBounds.top, right, mBounds.bottom, paint);
        paint.setAlpha(origAlpha);
    }

    private void invalidateSelf() {
        mOwner.invalidateSelf();
    }

    /**
     * From {@code android.graphics.drawable.RippleForeground.LogDecelerateInterpolator}.
     *
     * Interpolator with a smooth log deceleration.
     */
    private static class LogDecelerateInterpolator implements TimeInterpolator {

        private float mBase;
        private float mDrift;
        private float mTimeScale;
        private float mOutputScale;

        public LogDecelerateInterpolator(float base, float timeScale, float drift) {
            mBase = base;
            mDrift = drift;
            mTimeScale = 1f / timeScale;
            mOutputScale = 1f / computeLog(1f);
        }

        private float computeLog(float input) {
            return 1f - MathUtils.pow(mBase, -input * mTimeScale) + (mDrift * input);
        }

        @Override
        public float getInterpolation(float input) {
            return computeLog(input) * mOutputScale;
        }
    }

    /**
     * Property for animating position and radius between its initial and target values.
     */
    private static final FloatProperty<LinearRipple> TWEEN_RIPPLE =
            new FloatProperty<LinearRipple>("tweenRipple") {

                @Override
                public Float get(LinearRipple object) {
                    return object.mTweenRipple;
                }

                @Override
                public void setValue(LinearRipple object, float value) {
                    object.mTweenRipple = value;
                    object.invalidateSelf();
                }
            };

    /**
     * Property for animating opacity between 0 and its target value.
     */
    private static final FloatProperty<LinearRipple> OPACITY =
            new FloatProperty<LinearRipple>("opacity") {

                @Override
                public Float get(LinearRipple object) {
                    return object.mOpacity;
                }

                @Override
                public void setValue(LinearRipple object, float value) {
                    object.mOpacity = value;
                    object.invalidateSelf();
                }
            };
}
