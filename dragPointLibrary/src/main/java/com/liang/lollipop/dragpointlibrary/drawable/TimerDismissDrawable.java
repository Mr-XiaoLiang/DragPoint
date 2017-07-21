package com.liang.lollipop.dragpointlibrary.drawable;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PixelFormat;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * Created by Lollipop on 2017/07/20.
 * 计时器的销毁动画绘制器
 */
public abstract class TimerDismissDrawable extends DismissDrawable implements ValueAnimator.AnimatorListener,ValueAnimator.AnimatorUpdateListener {

    private ValueAnimator animator;
    private static final long DURATION = 500L;
    private float progress = 0;

    public TimerDismissDrawable() {
        this(DURATION);
    }

    public TimerDismissDrawable(long duration) {
        super();
        animator = ValueAnimator.ofFloat(0,1);
        animator.setDuration(duration);
        animator.addListener(this);
        animator.addUpdateListener(this);
    }

    @Override
    public void onAnimationEnd(Animator animation) {
        if(endListeners!=null)
            for(OnAnimationEndListener listener:endListeners){
                listener.onDismissAnimationEnd();
            }
    }

    @Override
    public void start() {
        animator.start();
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        draw(canvas,progress);
    }

    public abstract void draw(@NonNull Canvas canvas,float progress);

    public void setDuration(long duration){
        animator.setDuration(duration);
    }

    @Override
    public void onAnimationUpdate(ValueAnimator animation) {
        progress = (float) animation.getAnimatedValue();
        invalidateSelf();
    }

    @Override
    public void onAnimationStart(Animator animation) {

    }

    @Override
    public void onAnimationCancel(Animator animation) {

    }

    @Override
    public void onAnimationRepeat(Animator animation) {

    }
    @Override
    public int getOpacity() {
        return PixelFormat.TRANSPARENT;
    }

    @Override
    public void setAlpha(@IntRange(from = 0, to = 255) int alpha) {

    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {

    }

}
