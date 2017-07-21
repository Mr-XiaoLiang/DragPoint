package com.liang.lollipop.dragpointlibrary.drawable;

import android.graphics.drawable.Drawable;
import android.view.View;

import com.liang.lollipop.dragpointlibrary.view.DragView;

import java.util.ArrayList;

/**
 * Created by Lollipop on 2017/07/20.
 * 销毁动画的drawable
 */
public abstract class DismissDrawable extends Drawable {

    protected ArrayList<OnAnimationEndListener> endListeners;

    public DismissDrawable() {
        endListeners = new ArrayList<>();
    }

    /**
     * 最后销毁的位置的回调函数
     * @param x 坐标值
     * @param y 坐标值
     */
    public abstract void onLocationChange(DragView dragView, View targetView, float x, float y);

    /**
     * 动画结束的回调监听
     */
    public interface OnAnimationEndListener{
        void onDismissAnimationEnd();
    }

    /**
     * 开始执行
     */
    public abstract void start();

    public void addOnAnimationEndListener(OnAnimationEndListener onAnimationEndListener) {
        if(endListeners==null)
            endListeners = new ArrayList<>();
        if(endListeners.contains(onAnimationEndListener))
            return;
        this.endListeners.add(onAnimationEndListener);
    }
}
