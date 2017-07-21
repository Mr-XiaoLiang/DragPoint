package com.liang.lollipop.dragpointlibrary.view;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.Shader;
import android.support.annotation.Nullable;
import android.support.v7.graphics.Palette;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.OvershootInterpolator;

import com.liang.lollipop.dragpointlibrary.drawable.DismissDrawable;

/**
 * Created by Lollipop on 2017/07/19.
 * 拖拽效果的View
 */
public class DragView extends View implements View.OnTouchListener,
        DismissDrawable.OnAnimationEndListener,Palette.PaletteAsyncListener,
ValueAnimator.AnimatorListener,ValueAnimator.AnimatorUpdateListener{

    private Paint paint = null;//画笔
    private Paint dragPaint = null;//拖拽物的，用于绘制拖拽View的笔
    private Paint debugPaint = null;//debug用的画笔

    private PointF fixedPoint = null;//定点位置
    private PointF dragPoint = null;//拖拽的位置
    private PointF offsetSize = null;//偏移量的值
    private PointF lashDragPoint = null;//最后的拖拽的位置

    private RectF dragViewRect = null;//被拖拽View的范围属性
    private Path adhesionPath = null;//绘制的粘连部分的曲线
    private StateListener stateListener;//状态变化的回调监听
    private View targetView;//目标的View
    private DismissDrawable dismissDrawable;//销毁动画的绘制drawable
    private ValueAnimator restoreAnimator;//恢复用的动画控件

    private float fixedRadius = 0;//定点的半径
    private float minFixedRadius = 0;//最小定点的半径
    private float maxDragsRadius = 0;//最大拖拽半径
    private float reConnectionRadius = 0;//重新连接长度
    private float targetViewPadding = 0;//目标View的内补白

    private boolean isDebug = false;//是否debug
    private boolean isFracture = false;//是否断裂了
    private boolean isTouchUp = false;//是否手指离开
    private boolean autoColor = false;//自动获取颜色


    public DragView(Context context) {
        this(context,null);
    }

    public DragView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs,0);
    }

    public DragView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init(){
        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setDither(true);
        paint.setColor(Color.RED);
        dragPaint = new Paint();
        dragPaint.setAntiAlias(true);
        dragPaint.setDither(true);
        dragPaint.setColor(Color.RED);

        dragPoint = new PointF();
        fixedPoint = new PointF();
        offsetSize = new PointF();
        lashDragPoint = new PointF();
        dragViewRect = new RectF();

        restoreAnimator = ValueAnimator.ofFloat(0,1);
        restoreAnimator.setInterpolator(new OvershootInterpolator());
        restoreAnimator.addUpdateListener(this);
        restoreAnimator.addListener(this);
        restoreAnimator.setDuration(300L);
    }

//    @Override
//    public boolean onTouchEvent(MotionEvent event) {
//        return onTouch(this,event);
//    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        //如果处于脱离状态同时手指离开，说明进入销毁动画
        if(isFracture&&isTouchUp)
            return;
        //绘制粘连部分
        drawAdhesion(canvas);
        //绘制拖拽View部分
        drawDragView(canvas);
        if(isDebug){
            drawDebug(canvas);
        }
    }

    private void drawDebug(Canvas canvas){
        if(debugPaint==null){
            debugPaint = new Paint();
            debugPaint.setColor(Color.RED);
            debugPaint.setStyle(Paint.Style.STROKE);
        }
        canvas.drawCircle(fixedPoint.x,fixedPoint.y,maxDragsRadius,debugPaint);
        canvas.drawCircle(fixedPoint.x,fixedPoint.y,reConnectionRadius,debugPaint);
    }

    //绘制粘连部分
    private void drawAdhesion(Canvas canvas){
        //断开状态则不绘制
        if(isFracture)
            return;
        //得到当前拖拽的长度
        float spacing = getPointSpacing(dragPoint,fixedPoint,offsetSize);
        //如果达到了最大长度，那么就不再绘制粘连部分
        if(spacing>maxDragsRadius)
            return;
        //计算缩小后的半径
        float r = (1-(spacing/maxDragsRadius))*(fixedRadius-minFixedRadius)+minFixedRadius;
        r -= targetViewPadding;
        //保存画布属性
        canvas.save();
        //计算用户手指拖拽角度
        float angle = getTouchAngle(dragPoint,fixedPoint,offsetSize);
        //移动画布
        canvas.translate(fixedPoint.x,fixedPoint.y);
        //旋转画布角度
        canvas.rotate(angle);
        //绘制固定圆
        canvas.drawCircle(0,0,r,paint);
        //绘制果冻曲线
        canvas.drawPath(getAdhesionPath(r,spacing,fixedRadius-targetViewPadding),paint);
        //恢复画布
        canvas.restore();
    }

    //绘制拖拽View部分
    private void drawDragView(Canvas canvas){
        //保存坐标属性
        canvas.save();
        //移动位置到左上角（Bitmap渲染的硬伤，强行从起点位置绘制，因此将坐标系挪到目标位置）
        canvas.translate((dragViewRect.width()/2-dragPoint.x+offsetSize.x)*-1,(dragViewRect.height()/2-dragPoint.y+offsetSize.y)*-1);
        //绘制目标View大小的矩形，防止多余绘制
        canvas.drawRect(0,0,dragViewRect.width(),dragViewRect.height(),dragPaint);
        //恢复画布坐标系
        canvas.restore();
    }

    //获取两点之间的距离
    private float getPointSpacing(PointF drag,PointF center,PointF offset){
        return getPointSpacing(drag.x-offset.x,drag.y-offset.y,center.x,center.y);
    }
    //获取两点之间的距离
    private float getPointSpacing(float x1,float y1,float x2,float y2){
        return (float) Math.sqrt((x1 - x2) * (x1 - x2)+(y1 - y2) * (y1 - y2));
    }
    /**
     * 获取度数
     */
    private float getTouchAngle(PointF point,PointF center,PointF offset){
        return getTouchAngle(point.x-offset.x,point.y-offset.y,center.x,center.y);
    }
    /**
     * 获取度数
     */
    private float getTouchAngle(float pointX,float pointY,float centerX,float centerY){
        float x = pointX - centerX;
        float y = pointY - centerY;
        double pa;
        //分象限计算角度值
        if (y > 0) {//第三、四象限
            if (x < 0) {//第三象限
                pa = 180-Math.toDegrees(Math.atan(y / -x));
            } else {//第四象限
                pa = Math.toDegrees(Math.atan(y/x));
            }
        }else{//第一、二象限
            if (x < 0) {//第二象限
                pa = 180+Math.toDegrees(Math.atan(-y / -x));
            } else {//第一象限
                pa = 360-Math.toDegrees(Math.atan(-y / x));
            }
        }
        pa %= 360;
        if(isDebug)
            Log.e("getTouchAngle","Y:"+y+",X:"+x+",pa:"+pa);
        return (float) pa;
    }

    //获取粘连部分的路径
    private Path getAdhesionPath(float r,float spacing,float r2){
        //如果为空，则创建一个
        if(adhesionPath==null)
            adhesionPath = new Path();
        //重置路径
        adhesionPath.reset();

        //移动到定点圆的上方边缘
        adhesionPath.moveTo(0,-r);
        //贝塞尔曲线连接到动圆的同侧边缘
        adhesionPath.cubicTo(spacing*0.3f,0,spacing*0.5f,0,spacing,-r2);
        //直线连接到动圆另一侧
        adhesionPath.lineTo(spacing,r2);
        //贝塞尔曲线连接到定圆同侧
        adhesionPath.cubicTo(spacing*0.5f,0,spacing*0.3f,0,0,r);
        //闭合路径（连接定圆两侧）
        adhesionPath.close();

        return adhesionPath;
    }


    public float getMinFixedRadius() {
        return minFixedRadius;
    }

    public void setMinFixedRadius(float minFixedRadius) {
        this.minFixedRadius = minFixedRadius;
        invalidate();
    }

    public void setMinFixedRadiusDP(float minFixedRadius) {
        setMinFixedRadius(dip2px(minFixedRadius));
    }

    public float getMaxDragsRadius() {
        return maxDragsRadius;
    }

    public void setMaxDragsRadius(float maxDragsRadius) {
        this.maxDragsRadius = maxDragsRadius;
        //设置重新连接的半径长度
        setReConnectionRadius(maxDragsRadius*0.3f);
        invalidate();
    }

    public void setMaxDragsRadiusDP(float maxDragsRadius) {
        setMaxDragsRadius(dip2px(maxDragsRadius));
    }
    //设置被拖拽的View
    public void setDragView(View view){
        targetView = view;
        //获取被拖拽View的尺寸
        dragViewRect.set(view.getLeft(),view.getTop(),view.getRight(),view.getBottom());
        //获取被拖拽View的半径（最大内切圆）
        fixedRadius = Math.min(dragViewRect.width(),dragViewRect.height())*0.5f;
        //获得View再屏幕中的位置
        int[] loc = new int[2];
        view.getLocationInWindow(loc);
        fixedPoint.set(loc[0]+dragViewRect.width()*0.5f,loc[1]+dragViewRect.height()*0.5f);
        //获取被拖拽View的显示内容，用于拖拽显示
        view.setDrawingCacheEnabled(true);
        Bitmap bitmap = Bitmap.createBitmap(view.getDrawingCache());
        view.setDrawingCacheEnabled(false);
        ///设置为渲染对象
        BitmapShader bitmapShader = new BitmapShader(bitmap, Shader.TileMode.CLAMP,Shader.TileMode.CLAMP);
        dragPaint.setShader(bitmapShader);

        //如果设置为自动获取颜色，那么将否定前面的颜色设定，但是不会否定渲染内容
        if(autoColor){
            Palette.from(Bitmap.createBitmap(bitmap)).generate(this);
//            Palette palette = Palette.from(bitmap).generate();
//            onGenerated(palette);
        }

        view.setVisibility(INVISIBLE);
        invalidate();
    }

    public float getReConnectionRadius() {
        return reConnectionRadius;
    }

    public void setReConnectionRadius(float reConnectionRadius) {
        this.reConnectionRadius = reConnectionRadius;
        invalidate();
    }

    public void setReConnectionRadiusDP(float reConnectionRadius) {
        setReConnectionRadius(dip2px(reConnectionRadius));
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        //获取当前点位置
        dragPoint.set(event.getRawX(),event.getRawY());
        switch (event.getAction()){
            case MotionEvent.ACTION_DOWN:
                setBackground(null);
                isTouchUp = false;
                offsetSize.set(dragPoint.x-fixedPoint.x,dragPoint.y-fixedPoint.y);
                if(restoreAnimator!=null&&restoreAnimator.isRunning()){
                    restoreAnimator.cancel();
                }
                break;
            case MotionEvent.ACTION_MOVE:
                //计算得到当前拖拽半径
                float spacing = getPointSpacing(dragPoint,fixedPoint,offsetSize);
                if(isFracture&&spacing<reConnectionRadius){
                    //如果处于断开状态并且半径小于重连半径，那么就重新连接
                    isFracture = false;
                }else if(!isFracture&&spacing>maxDragsRadius){
                    //如果处于连接状态，并且拖拽半径大于最大拖拽半径，那么就断开
                    isFracture = true;
                }
                if(stateListener!=null){
                    if(isFracture){
                        stateListener.onOutOf(this,targetView,dragPoint.x-offsetSize.x,dragPoint.y-offsetSize.y);
                    }else{
                        stateListener.onAdhesion(this,targetView,dragPoint.x-offsetSize.x,dragPoint.y-offsetSize.y);
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                isTouchUp = true;
                lashDragPoint.set(dragPoint.x-fixedPoint.x,dragPoint.y-fixedPoint.y);

                if(isFracture){
                    if(stateListener!=null){
                        //如果达到了最大长度,那么松手就代表销毁效果
                        stateListener.onDismiss(this,targetView,dragPoint.x-offsetSize.x,dragPoint.y-offsetSize.y);
                    }
                    if(dismissDrawable!=null){
                        setBackground(dismissDrawable);
                        dismissDrawable.onLocationChange(this,targetView,dragPoint.x-offsetSize.x,dragPoint.y-offsetSize.y);
                        dismissDrawable.start();
                    }else{
                        if(stateListener!=null)
                            stateListener.onEnd(this,targetView,dragPoint.x-offsetSize.x,dragPoint.y-offsetSize.y);
                    }
                }else{
                    if(stateListener!=null){
                        //没有达到销毁状态，所以回归原位
                        stateListener.onHoming(this,targetView);
                        if(restoreAnimator.isRunning())
                            restoreAnimator.cancel();
                        restoreAnimator.start();
                    }
                }
                break;
        }
        invalidate();
        return true;
    }

    public float dip2px(float dpValue) {
        float scale = getContext().getResources().getDisplayMetrics().density;
        return dpValue * scale;
    }

    public boolean isDebug() {
        return isDebug;
    }

    public void setDebug(boolean debug) {
        isDebug = debug;
    }

    public float getTargetViewPadding() {
        return targetViewPadding;
    }

    public void setTargetViewPadding(float targetViewPadding) {
        this.targetViewPadding = targetViewPadding;
        invalidate();
    }

    public void setTargetViewPaddingDP(float targetViewPadding) {
        setTargetViewPadding(dip2px(targetViewPadding));
    }

    public void setShader(Shader shader){
        paint.setShader(shader);
        invalidate();
    }

    public void setColor(int color){
        paint.setColor(color);
        invalidate();
    }

    @Override
    public void onDismissAnimationEnd() {
        if(stateListener!=null){
            stateListener.onEnd(this,targetView,dragPoint.x-offsetSize.x,dragPoint.y-offsetSize.y);
        }
    }

    @Override
    public void onGenerated(Palette palette) {
        //获取主题色的方法
        setColor(palette.getVibrantColor(paint.getColor()));
        if(isDebug)
            Log.e("onGenerated","#"+Integer.toHexString(paint.getColor()));
    }

    @Override
    public void onAnimationStart(Animator animation) {

    }

    @Override
    public void onAnimationEnd(Animator animation) {
        if(stateListener!=null)
            stateListener.onRestore(this,targetView);
    }

    @Override
    public void onAnimationCancel(Animator animation) {
        if(stateListener!=null)
            stateListener.onRestore(this,targetView);
    }

    @Override
    public void onAnimationRepeat(Animator animation) {

    }

    @Override
    public void onAnimationUpdate(ValueAnimator animation) {
        //获取当前进度
        float pro = (float) animation.getAnimatedValue();
        //进度值为0-1，而我们需要的是收缩，所以需要反过来，所以1-pro
        pro = 1-pro;
        float x = (lashDragPoint.x-offsetSize.x)*pro+fixedPoint.x+offsetSize.x;
        float y = (lashDragPoint.y-offsetSize.y)*pro+fixedPoint.y+offsetSize.y;
        //将他的坐标差值*进度之后，加到相对标志物上，得到当前坐标
        dragPoint.set(x,y);
        if(isDebug)
            Log.e("onRestoreAnimUpdate","x:"+x+",y"+y+",pro:"+pro);
        //请求重绘
        invalidate();
    }

    /**
     * 拖拽的状态监听器
     * 用于监听拖拽的状态
     */
    public interface StateListener{
        /**
         * 当被手指拖住但是又处于粘连状态时的回调函数
         * 此状态松手时是处于归于状态
         * @param dragView 当前显示View
         * @param targetView 目标被操作的View
         * @param x 当前手指位置，此位置基于屏幕坐标
         * @param y 当前手指位置，此位置基于屏幕坐标
         */
        void onAdhesion(DragView dragView, View targetView, float x,float y);

        /**
         * 当用户放弃操作，在最大的拖拽范围内松手到View完全归位的过程
         * 当用户抬起手指时触发
         * 此时覆盖层并未被销毁，还会执行最后的复位动画效果
         * @param dragView  当前显示的View
         * @param targetView 被操作的View
         */
        void onHoming(DragView dragView, View targetView);

        /**
         * 当结束松手动画之后，View处于归位状态
         * 当用户松手后，在 {@link #onHoming(DragView,View)}后执行
         * @param dragView 当前显示的View，此时应当隐藏或销毁此View
         * @param targetView 被操作的View，此时，应当让此View显示
         */
        void onRestore(DragView dragView, View targetView);

        /**
         * 当手指拖拽中，超出了最大拖拽范围，处于脱离状态
         * 手指仍然在屏幕上，如果松手，将执行{@link #onDismiss(DragView,View,float,float)}
         * @param dragView 当前显示的View
         * @param targetView 被操作的View
         * @param x 最后的位置X
         * @param x 最后的位置Y
         */
        void onOutOf(DragView dragView, View targetView, float x,float y);

        /**
         * 当用户松手且拖拽距离超过了最大拖拽范围时，触发的方法
         * 返回值将用于判断是否执行销毁动画
         * @param dragView 当前显示的View
         * @param targetView 被炒作的View
         * @return 当为true时，表示允许销毁，则执行销毁动画效果，
         * 为false时，表示不执行销毁动画，但是此次效果仍然结束
         */
        boolean onDismiss(DragView dragView, View targetView,float x,float y);

        /**
         * 最后一个执行方法，当销毁动画执行后，将会执行此回调方法
         * @param dragView 显示的View
         * @param targetView 被操作的View
         * @param x 最后的位置
         * @param y 最后的位置
         */
        void onEnd(DragView dragView, View targetView,float x, float y);

    }

    public StateListener getStateListener() {
        return stateListener;
    }

    public void setStateListener(StateListener stateListener) {
        this.stateListener = stateListener;
    }

    public DismissDrawable getDismissDrawable() {
        return dismissDrawable;
    }

    public void setDismissDrawable(DismissDrawable dismissDrawable) {
        this.dismissDrawable = dismissDrawable;
        if(dismissDrawable!=null)
            this.dismissDrawable.addOnAnimationEndListener(this);
    }

    public boolean isAutoColor() {
        return autoColor;
    }

    public void setAutoColor(boolean autoColor) {
        this.autoColor = autoColor;
    }
}
