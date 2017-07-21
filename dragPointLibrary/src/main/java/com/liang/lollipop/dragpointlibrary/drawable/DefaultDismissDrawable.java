package com.liang.lollipop.dragpointlibrary.drawable;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.support.annotation.NonNull;
import android.view.View;

import com.liang.lollipop.dragpointlibrary.view.DragView;

import java.util.ArrayList;
import java.util.Random;

/**
 * Created by Lollipop on 2017/07/21.
 * 默认的销毁动画
 */
public class DefaultDismissDrawable extends TimerDismissDrawable {

    //动画最后发生位置
    private PointF location;
    //目标View的尺寸（用于包裹View做销毁动画）
    private RectF rectF;
    //泡泡的半径
    private float radius = 0;
    //泡泡的位置信息
    private ArrayList<PointF> cloudPontList;
    private ArrayList<PointF> cloudPontWaitList;
    //随机数对象，用于一定程度的动态调整泡泡的位置
    private Random random;
    //绘制的画笔
    private Paint paint;
    //最小泡泡数量
    private int minPopSize = 3;
    //最多泡泡数量
    private int maxPopSize = 6;

    public DefaultDismissDrawable() {
        super();
        location = new PointF();
        rectF = new RectF();
        cloudPontList = new ArrayList<>();
        cloudPontWaitList = new ArrayList<>();
        random = new Random();
        paint = new Paint();
        paint.setShadowLayer(5,5,5, Color.BLACK);
        paint.setColor(0xFFEEEEEE);
        paint.setAntiAlias(true);
        paint.setDither(true);
    }

    @Override
    public void onLocationChange(DragView dragView, View targetView, float x, float y) {
        //保存最后的位置数据
        location.set(x,y);
        //保存
        rectF.set(targetView.getLeft(),targetView.getTop(),targetView.getRight(),targetView.getBottom());

        //求对角线长度
        float width = rectF.width();
        float height = rectF.height();
        //半径长度仅仅为对角线的7/10，是因为5个泡泡仅仅在View的边缘，超过0.5即可完全包裹View了
        //为了避免泡泡过大，所以使用7/10
        radius = (float) Math.sqrt(width*width+height*height)*0.7f;
        //初始化泡泡的坐标点，泡泡的位置在View的边缘
        //坐标的值表示的是当前销毁View的中心点，所以需要做一点位置偏移
        //同时位置偏移时，并没有完全在View的边界，而是做了些许变化，保证不那么死板

        int popSize = random.nextInt(maxPopSize-minPopSize)+minPopSize;
        popSize = popSize<minPopSize?minPopSize:popSize;
        while(cloudPontList.size()<popSize){
            if(cloudPontWaitList.size()>0){
                cloudPontList.add(cloudPontWaitList.remove(0));
            }else{
                cloudPontList.add(new PointF());
            }
        }
        while(cloudPontList.size()>popSize){
            cloudPontWaitList.add(cloudPontList.remove(0));
        }
        for(PointF pointF:cloudPontList){
            pointF.set(location.x+getRandom(width),location.y+getRandom(height));
        }
    }

    private float getRandom(float size){
        return size*(random.nextFloat()-0.5f);
    }

    @Override
    public void draw(@NonNull Canvas canvas, float progress) {
        //简单模仿QQ的销毁动画（QQ使用的是Gif）
        progress = 1-progress;
        for(PointF pointF:cloudPontList){
            canvas.drawCircle(pointF.x,pointF.y,radius*progress,paint);
        }
    }

    public int getMinPopSize() {
        return minPopSize;
    }

    public void setMinPopSize(int minPopSize) {
        this.minPopSize = minPopSize;
    }

    public int getMaxPopSize() {
        return maxPopSize;
    }

    public void setMaxPopSize(int maxPopSize) {
        this.maxPopSize = maxPopSize;
    }
}
