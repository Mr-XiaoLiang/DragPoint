package com.liang.lollipop.dragpoint;

import android.graphics.Color;
import android.graphics.PointF;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.liang.lollipop.dragpointlibrary.DragUtil;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        DragUtil.init(this).setDebug(true);

        TextView textView = (TextView) findViewById(R.id.text);
        DragUtil.dragWith(textView,null);

        TextView textView2 = (TextView) findViewById(R.id.text2);
        DragUtil.dragWith(textView2,null);

        TextView textView3 = (TextView) findViewById(R.id.text3);
        DragUtil.createNewBuilder(textView3)
                .setJellyColor(0xFF336E33)
                .build();

        TextView textView4 = (TextView) findViewById(R.id.text4);
        DragUtil.createNewBuilder(textView4)
                .setTargetViewPaddingDP(2)
//                .linearShader(Color.RED,Color.WHITE)
                .setAutoColor(true)
                .build();

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recyclerview);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(layoutManager);
        TestAdapter adapter = new TestAdapter(LayoutInflater.from(this),recyclerView);
        recyclerView.setItemAnimator(new DefaultItemAnimator());//设置列表item动画
        recyclerView.setAdapter(adapter);
    }

    private class TestAdapter extends RecyclerView.Adapter<TestHolder>{

        private LayoutInflater inflater;
        private RecyclerView recyclerView;

        public TestAdapter(LayoutInflater inflater,RecyclerView recyclerView) {
            this.inflater = inflater;
            this.recyclerView = recyclerView;
        }

        @Override
        public TestHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            TestHolder testHolder = new TestHolder(inflater.inflate(TestHolder.LAYOUT_ID,parent,false));
            testHolder.setRecyclerView(recyclerView);
            return testHolder;
        }

        @Override
        public void onBindViewHolder(TestHolder holder, int position) {
            if(holder!=null)
                holder.onBind(position+1+"");
            Log.e("onBindViewHolder",position+1+"");
        }

        @Override
        public int getItemCount() {
            return 100;
        }
    }

    private class TestHolder extends RecyclerView.ViewHolder{

        public static final int LAYOUT_ID = R.layout.item_test;

        private TextView pointView;
        private RecyclerView recyclerView;

        public TestHolder(View itemView) {
            super(itemView);
            pointView = itemView.findViewById(R.id.text);
            DragUtil.createNewBuilder(pointView).withRecyclerView(recyclerView).setAutoColor(true).build();
        }

        public RecyclerView getRecyclerView() {
            return recyclerView;
        }

        public void setRecyclerView(RecyclerView recyclerView) {
            this.recyclerView = recyclerView;
        }

        public void onBind(String size){
            pointView.setText(size);
        }
    }

}
