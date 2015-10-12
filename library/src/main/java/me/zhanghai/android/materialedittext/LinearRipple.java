/*
 * Copyright (c) 2015 Zhang Hai <Dreaming.in.Code.ZH@Gmail.com>
 * All Rights Reserved.
 */

package me.zhanghai.android.materialedittext;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
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
 * @see <a href="https://github.com/android/platform_frameworks_base/blob/f872ee0057ed247aa93589347f1b53afc99517f8/graphics/java/android/graphics/drawable/RippleForeground.java">RippleForeground@f872ee</a>
 */
public class LinearRipple {

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

    private float mTweenAll;
    private float mOpacity;

    private Animator mAnimator;
    private boolean mAnimationEnded;

    public LinearRipple(Drawable owner, Rect bounds, float position, float density) {

        mWaveTouchDownAcceleration = WAVE_TOUCH_DOWN_ACCELERATION_DP * density;
        mWaveTouchUpAcceleration = WAVE_TOUCH_UP_ACCELERATION_DP * density;

        mOwner = owner;
        onBoundsChange(bounds);
        mStartingPosition = position;
    }

    public void onBoundsChange(Rect bounds) {
        mBounds = bounds;
        mTargetPosition = mTargetRadius = mBounds.width() / 2f;
    }

    public void enter() {

        cancelAnimation();

        mAnimator = createEnterAnimation();
        mAnimator.start();
    }

    private Animator createEnterAnimation() {

        ObjectAnimator tweenAll = ObjectAnimator.ofFloat(this, TWEEN_ALL, 1);
        int duration = (int) (1000 * Math.sqrt(mTargetRadius / mWaveTouchDownAcceleration) + 0.5);
        //tweenAll.setAutoCancel(true);
        tweenAll.setDuration(duration);
        tweenAll.setInterpolator(LINEAR_INTERPOLATOR);
        tweenAll.setStartDelay(RIPPLE_ENTER_DELAY);

        ObjectAnimator opacity = ObjectAnimator.ofFloat(this, OPACITY, 1);
        //opacity.setAutoCancel(true);
        opacity.setDuration(OPACITY_ENTER_DURATION);
        opacity.setInterpolator(LINEAR_INTERPOLATOR);

        AnimatorSet set = new AnimatorSet();
        set.play(tweenAll).with(opacity);

        return set;
    }

    public void moveTo(float position) {
        mStartingPosition = position;
    }

    public void exit() {

        cancelAnimation();

        mAnimator = createEndAnimation(true);
        mAnimator.start();
    }

    public void finish() {

        cancelAnimation();

        mAnimator = createEndAnimation(false);
        mAnimator.start();
    }

    private int getRadiusExitDuration() {
        final float radius = MathUtils.lerp(0, mTargetRadius, mTweenAll);
        final float remaining = mTargetRadius - radius;
        return (int) (1000 * Math.sqrt(remaining /
                (mWaveTouchUpAcceleration + mWaveTouchDownAcceleration)) + 0.5);
    }

    private int getOpacityExitDuration() {
        return (int) (1000 * mOpacity / WAVE_OPACITY_DECAY_VELOCITY + 0.5f);
    }

    private Animator createEndAnimation(boolean exit) {

        ObjectAnimator tweenAll = ObjectAnimator.ofFloat(this, TWEEN_ALL, 1);
        //tweenAll.setAutoCancel(true);
        tweenAll.setDuration(getRadiusExitDuration());
        tweenAll.setInterpolator(DECELERATE_INTERPOLATOR);

        Animator animator;
        if (exit) {

            ObjectAnimator opacity = ObjectAnimator.ofFloat(this, OPACITY, 0);
            //opacity.setAutoCancel(true);
            opacity.setDuration(getOpacityExitDuration());
            opacity.setInterpolator(LINEAR_INTERPOLATOR);

            AnimatorSet set = new AnimatorSet();
            set.play(tweenAll).with(opacity);
            animator = set;
        } else {
            animator = tweenAll;
        }
        animator.addListener(mAnimationListener);

        return animator;
    }

    public void cancelAnimation() {
        stopAnimation(true);
    }

    public void endAnimation() {
        stopAnimation(false);
    }

    private void stopAnimation(boolean cancel) {
        if (mAnimator != null) {
            if (cancel) {
                mAnimator.cancel();
            } else {
                mAnimator.end();
            }
            mAnimator = null;
        }
    }

    public boolean isAnimationEnded() {
        return mAnimationEnded;
    }

    public void draw(Canvas canvas, Paint paint) {

        int origAlpha = paint.getAlpha();
        int alpha = (int) (origAlpha * mOpacity + 0.5f);
        float radius = MathUtils.lerp(0, mTargetRadius, mTweenAll);
        if (alpha == 0 || radius == 0) {
            return;
        }

        float position = MathUtils.lerp(mStartingPosition, mTargetPosition, mTweenAll);
        float left = MathUtils.constrain(position - radius, mBounds.left, mBounds.right);
        float right = MathUtils.constrain(position + radius, mBounds.left, mBounds.right);
        paint.setAlpha(alpha);
        canvas.drawRect(left, mBounds.top, right, mBounds.bottom, paint);
        paint.setAlpha(origAlpha);
    }

    private void invalidateSelf() {
        mOwner.invalidateSelf();
    }

    private final Animator.AnimatorListener mAnimationListener = new AnimatorListenerAdapter() {
        @Override
        public void onAnimationEnd(Animator animator) {
            mAnimationEnded = true;
        }
    };

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
    private static final FloatProperty<LinearRipple> TWEEN_ALL =
            new FloatProperty<LinearRipple>("tweenAll") {

                @Override
                public Float get(LinearRipple object) {
                    return object.mTweenAll;
                }

                @Override
                public void setValue(LinearRipple object, float value) {
                    object.mTweenAll = value;
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
