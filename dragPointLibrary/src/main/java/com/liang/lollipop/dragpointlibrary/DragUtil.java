package com.liang.lollipop.dragpointlibrary;

import android.content.Context;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.PointF;
import android.graphics.Shader;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.WindowManager;

import com.liang.lollipop.dragpointlibrary.drawable.DefaultDismissDrawable;
import com.liang.lollipop.dragpointlibrary.drawable.DismissDrawable;
import com.liang.lollipop.dragpointlibrary.exception.DragException;
import com.liang.lollipop.dragpointlibrary.view.DragView;

/**
 * Created by Lollipop on 2017/07/19.
 *  拖拽View的对外工具
 */
public class DragUtil {

    private static Builder builder = null;

    public static class Builder{
        private float minFixedRadius = 0;//最小定点的半径
        private float maxDragsRadius = 0;//最大拖拽半径
        private float reConnectionRadius = 0;//重新连接长度
        private DragUtil.OnDragListener onDragListener;//绘制状态的监听器
        private View targetView;//目标的View
        private boolean isDebug = false;//是否debug
        private float targetViewPadding = 0;//目标View的内补白
        private Context context;//上下文
        private Shader shader;//渲染器
        private int jellyColor = Color.RED;//果冻颜色
        private DismissDrawable dismissDrawable;//销毁动画
        private boolean autoColor = false;//自动获取颜色
        private RecyclerView recyclerView;//列表View

        public static Builder dragWith(View view){
            return new Builder(view);
        }

        public static Builder EmptyBuilder(Context context){
            return new Builder(context);
        }

        public Builder setMinFixedRadius(float minFixedRadius) {
            this.minFixedRadius = minFixedRadius;
            return this;
        }

        public Builder setMinFixedRadiusDP(float minFixedRadius) {
            return setMinFixedRadius(dip2px(minFixedRadius));
        }

        public Builder setMaxDragsRadius(float maxDragsRadius) {
            this.maxDragsRadius = maxDragsRadius;
            return this;
        }

        public Builder setMaxDragsRadiusDP(float maxDragsRadius) {
            return setMaxDragsRadius(dip2px(maxDragsRadius));
        }

        public Builder setReConnectionRadius(float reConnectionRadius) {
            this.reConnectionRadius = reConnectionRadius;
            return this;
        }

        public Builder setReConnectionRadiusDP(float reConnectionRadius) {
            return setReConnectionRadius(dip2px(reConnectionRadius));
        }

        public Builder setOnDragListener(OnDragListener onDragListener) {
            this.onDragListener = onDragListener;
            return this;
        }

        public Builder setAutoColor(boolean autoColor) {
            this.autoColor = autoColor;
            return this;
        }

        public Builder withRecyclerView(RecyclerView recyclerView) {
            this.recyclerView = recyclerView;
            return this;
        }

        public Builder setDebug(boolean debug) {
            isDebug = debug;
            return this;
        }

        public Builder setDismissDrawable(DismissDrawable dismissDrawable) {
            this.dismissDrawable = dismissDrawable;
            return this;
        }

        public Builder setJellyColor(int jellyColor) {
            this.jellyColor = jellyColor;
            return this;
        }

        public Builder setTargetViewPadding(float targetViewPadding) {
            this.targetViewPadding = targetViewPadding;
            return this;
        }

        public Builder setTargetViewPaddingDP(float targetViewPadding) {
            return setTargetViewPadding(dip2px(targetViewPadding));
        }

        private Builder(View targetView) {
            this(targetView.getContext());
            this.targetView = targetView;
        }

        private Builder(Context context) {
            this.context = context;
            setTargetViewPadding(0);
            setMaxDragsRadiusDP(80);
            setReConnectionRadiusDP(24);
            setDebug(false);
            setMinFixedRadiusDP(1);
            setJellyColor(Color.RED);
            setDismissDrawable(new DefaultDismissDrawable());
        }

        public Builder linearShader(int[] colors,float[] positions){
            if(maxDragsRadius<1)
                throw new DragException("You must first set maxDragsRadius");
            this.shader = new LinearGradient(0,0,maxDragsRadius,0,colors,positions, Shader.TileMode.CLAMP);
            return this;
        }

        public Builder linearShader(int centerColor,int dragColor){
            return linearShader(new int[]{centerColor,dragColor},new float[]{0,1});
        }

        private float dip2px(float dpValue) {
            float scale = context.getResources().getDisplayMetrics().density;
            return dpValue * scale;
        }

        public Builder createNewBuilder(){
            Builder builder = Builder.EmptyBuilder(context);
            builder.minFixedRadius = minFixedRadius;
            builder.maxDragsRadius = maxDragsRadius;
            builder.reConnectionRadius = reConnectionRadius;
            builder.onDragListener = onDragListener;
            builder.targetView = targetView;
            builder.isDebug = isDebug;
            builder.targetViewPadding = targetViewPadding;
            builder.shader = shader;
            return builder;
        }

        public Builder createNewBuilder(View targetView){
            Builder builder = Builder.EmptyBuilder(targetView.getContext());
            builder.minFixedRadius = minFixedRadius;
            builder.maxDragsRadius = maxDragsRadius;
            builder.reConnectionRadius = reConnectionRadius;
            builder.onDragListener = onDragListener;
            builder.targetView = targetView;
            builder.isDebug = isDebug;
            builder.targetViewPadding = targetViewPadding;
            builder.shader = shader;
            return builder;
        }

        public void build(){
            if(targetView==null)
                throw new DragException("TargetView can not be null");
            targetView.setOnTouchListener(new OnDragTouchListener(this));
        }

        public void build(View targetView){
            if(targetView==null)
                throw new DragException("TargetView can not be null");
            Builder builder = createNewBuilder();
            builder.targetView = targetView;
            builder.context = targetView.getContext();
            targetView.setOnTouchListener(new OnDragTouchListener(builder));
        }

    }

    public static void dragWith(View view,OnDragListener listener){
        Builder builder = getBuilder();
        if(builder==null)
            init(Builder.EmptyBuilder(view.getContext()));
        builder = getBuilder();
        if(builder==null)
            throw new DragException("Unable to create the default Builder");
        builder.setOnDragListener(listener);
        builder.build(view);
    }

    public interface OnDragListener{
        boolean onDismiss(View target,float x,float y);
        void onRestore(View target);
    }

    private static class OnDragTouchListener implements OnTouchListener,DragView.StateListener,RecyclerView.OnItemTouchListener{

        private WindowManager windowManager;
        private DragView dragView;
        private WindowManager.LayoutParams layoutParams;
        private OnDragListener listener;
        private View targetView;
        private RecyclerView recyclerView;
        private boolean isDrag = false;

        OnDragTouchListener(Context context,OnDragListener listener) {
            Builder builder = getBuilder();
            if(builder==null)
                init(Builder.EmptyBuilder(context));
            builder = getBuilder();
            if(builder==null)
                throw new DragException("Unable to create the default Builder");
            builder.setOnDragListener(listener);
            init(builder);
            initDragView(builder);
        }

        OnDragTouchListener(Builder builder) {
            init(builder);
            initDragView(builder);
        }

        private void init(Builder builder){
            this.listener = builder.onDragListener;
            this.recyclerView = builder.recyclerView;
            windowManager = (WindowManager) builder.context.getSystemService(Context.WINDOW_SERVICE);
            layoutParams = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
//                        WindowManager.LayoutParams.TYPE_WALLPAPER,
                    WindowManager.LayoutParams.TYPE_APPLICATION,
//                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|
//                            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL|
                            WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
                            |WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION,
                    WindowManager.LayoutParams.FORMAT_CHANGED);
            if(this.recyclerView!=null)
                this.recyclerView.addOnItemTouchListener(this);
        }

        private void initDragView(Builder builder){
            dragView = new DragView(builder.context);
            dragView.setMaxDragsRadius(builder.maxDragsRadius);
            dragView.setMinFixedRadiusDP(builder.minFixedRadius);
            dragView.setStateListener(this);
            dragView.setDebug(builder.isDebug);
            if(builder.shader!=null)
                dragView.setShader(builder.shader);
            dragView.setReConnectionRadius(builder.reConnectionRadius);
            dragView.setTargetViewPadding(builder.targetViewPadding);
            dragView.setColor(builder.jellyColor);
            dragView.setDismissDrawable(builder.dismissDrawable);
            dragView.setAutoColor(builder.autoColor);
        }

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if(targetView==null)
                targetView = v;
            if(event.getAction()==MotionEvent.ACTION_DOWN){
                dragView.setDragView(v);
                if(dragView.getParent()!=null) {
                    windowManager.removeView(dragView);
                }
                windowManager.addView(dragView,layoutParams);

            }
            dragView.onTouch(v,event);
            return true;
        }

        @Override
        public void onAdhesion(DragView dragView, View targetView, float x, float y) {
            isDrag = true;
        }

        @Override
        public void onHoming(DragView dragView, View targetView) {
            isDrag = false;
        }

        @Override
        public void onRestore(DragView dragView, View targetView) {
            windowManager.removeView(dragView);
            targetView.setVisibility(View.VISIBLE);
            if(listener!=null)
                listener.onRestore(targetView);
        }

        @Override
        public void onOutOf(DragView dragView, View targetView, float x, float y) {

        }

        @Override
        public boolean onDismiss(DragView dragView, View targetView, float x, float y) {
            isDrag = false;
            return(listener==null || listener.onDismiss(targetView,(int)x,(int)y));
        }

        @Override
        public void onEnd(DragView dragView, View targetView, float x, float y) {
            windowManager.removeView(dragView);
            targetView.setVisibility(View.VISIBLE);
        }

        @Override
        public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
//            View childView = recyclerView.findChildViewUnder(e.getX(), e.getY());
//            if (childView != null) {
//                if(childView instanceof ViewGroup){
//                    return checkViewInGroup((ViewGroup) childView,targetView);
//                } else
//                    return childView==targetView;
//            }
//            return false;
//            rv.getFocusedChild()
            return isDrag;
        }

        @Override
        public void onTouchEvent(RecyclerView rv, MotionEvent e) {
            onTouch(targetView,e);
        }

        @Override
        public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {

        }
    }

    private static boolean checkViewInGroup(ViewGroup viewGroup,View view){
        if(viewGroup==null)
            return false;
        int childCount = viewGroup.getChildCount();
        boolean reuslt = false;
        for(int i = 0;i<childCount;i++){
            View child = viewGroup.getChildAt(i);
            Log.e("checkViewInGroup",child.getClass().getName());
            if(child instanceof ViewGroup)
                reuslt = checkViewInGroup((ViewGroup)child,view);
            else
                reuslt = child == view;
            if(reuslt){
                Log.e("checkViewInGroup","true");
                return true;
            }
        }
        return false;
    }

    public static Builder getBuilder() {
        return builder;
    }

    public static Builder createNewBuilder(Context context) {
        Builder builder = getBuilder();
        if(builder!=null)
            return builder.createNewBuilder();
        return Builder.EmptyBuilder(context);
    }

    public static Builder createNewBuilder(View view) {
        Builder builder = getBuilder();
        if(builder!=null)
            return builder.createNewBuilder(view);
        return Builder.dragWith(view);
    }

    public static void init(Builder builder) {
        DragUtil.builder = builder;
    }

    public static Builder init(Context context) {
        DragUtil.builder = Builder.EmptyBuilder(context);
        return DragUtil.builder;
    }

    public static OnDragTouchListener getNewDragTouchListener(Context context,OnDragListener dragListener){
        return new OnDragTouchListener(context,dragListener);
    }

}
