package com.iwxyi.magnifydemo;

import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PointF;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.SizeF;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AbsoluteLayout;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.FileNotFoundException;

public class MainActivity extends AppCompatActivity implements View.OnTouchListener, View.OnClickListener {

    private ImageView img;
    private ImageView ten_top;
    private ImageView ten_left;
    private ImageView ten_right;
    private ImageView ten_bottom;

    private AbsoluteLayout.LayoutParams lp_main;
    private AbsoluteLayout.LayoutParams lp_top;
    private AbsoluteLayout.LayoutParams lp_left;
    private AbsoluteLayout.LayoutParams lp_right;
    private AbsoluteLayout.LayoutParams lp_bottom;
    private TextView mResultTv;

    /* 图片移动 */
    private static final int NONE = 0;
    private static final int DRAG = 1;
    private static final int ZOOM = 2;
    private int mode = NONE;

    private PointF down_point = new PointF(); // 按下的第一个点
    private PointF down_leftTop = new PointF(); // 按下的控件的左上角
    private View down_view;                   // 按下的 view，避免双指缩放两个控件
    private PointF down_mid = new PointF(); // 双指按下的中间点
    private float down_scale = 1f;           // 双指按下时的缩放倍数
    private float down_dis = 0f;           // 上次两个点的距离

    // 因为有缩放，所以这里所有的位置都是按照比例来的，而不是绝对的横纵坐标
    private SizeF ori_size; // 图片原来的大小
    private float scale = 1f; // 当前的缩放比例
    private float ten_top_x = 0.5f, ten_top_y = 0.1f; // ten_top 针对图片的位置比例
    private float ten_left_x = 0.1f, ten_left_y = 0.5f; // ten_left 针对图片的位置比例
    private float ten_right_x = 0.9f, ten_right_y = 0.5f; // ten_right 针对图片的位置比例
    private float ten_bottom_x = 0.5f, ten_bottom_y = 0.9f; // ten_top 针对图片的位置比例

    /* 常量 */
    private final int PHOTOZOOM = 1; // 选取照片

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
    }

    private void initView() {
        Resources resources = this.getResources();
        DisplayMetrics displayMetrics = resources.getDisplayMetrics();
        int screen_width = displayMetrics.widthPixels;
        int screen_height = displayMetrics.heightPixels;

        AbsoluteLayout ab_layout = (AbsoluteLayout) findViewById(R.id.ab_layout);

        img = new ImageView(this);
        img.setImageDrawable(getResources().getDrawable(R.drawable.tree));
        /*设置一开始的大小，记录为原图*/
        lp_main = new AbsoluteLayout.LayoutParams(
                (int) (screen_width * 0.8), (int) (screen_height * 0.8),
                (int) (screen_width * 0.1), (int) (screen_height * 0.01));
        ori_size = new SizeF(lp_main.width, lp_main.height);

        ten_top = new ImageView(this);
        ten_left = new ImageView(this);
        ten_right = new ImageView(this);
        ten_bottom = new ImageView(this);
        ten_top.setImageDrawable(getResources().getDrawable(R.drawable.ten));
        ten_left.setImageDrawable(getResources().getDrawable(R.drawable.ten));
        ten_right.setImageDrawable(getResources().getDrawable(R.drawable.ten));
        ten_bottom.setImageDrawable(getResources().getDrawable(R.drawable.ten));


        lp_top = new AbsoluteLayout.LayoutParams(64, 64, 0, 0);
        lp_left = new AbsoluteLayout.LayoutParams(64, 64, 0, 0);
        lp_right = new AbsoluteLayout.LayoutParams(64, 64, 0, 0);
        lp_bottom = new AbsoluteLayout.LayoutParams(64, 64, 0, 0);

        img.setOnTouchListener(this);
        ten_top.setOnTouchListener(this);
        ten_left.setOnTouchListener(this);
        ten_right.setOnTouchListener(this);
        ten_bottom.setOnTouchListener(this);

        ab_layout.addView(img, lp_main);
        ab_layout.addView(ten_top, lp_top);
        ab_layout.addView(ten_left, lp_left);
        ab_layout.addView(ten_right, lp_right);
        ab_layout.addView(ten_bottom, lp_bottom);

        adjustTens();

        mResultTv = (TextView) findViewById(R.id.tv_result);
        mResultTv.bringToFront();
    }

    private void adjustTens() {
        lp_top.x = lp_main.x + (int) (lp_main.width * ten_top_x) - lp_top.width / 2;
        lp_top.y = lp_main.y + (int) (lp_main.height * ten_top_y) - lp_top.height / 2;
        ten_top.setLayoutParams(lp_top);

        lp_left.x = lp_main.x + (int) (lp_main.width * ten_left_x) - lp_left.width / 2;
        lp_left.y = lp_main.y + (int) (lp_main.height * ten_left_y) - lp_left.height / 2;
        ten_left.setLayoutParams(lp_left);

        lp_right.x = lp_main.x + (int) (lp_main.width * ten_right_x) - lp_right.width / 2;
        lp_right.y = lp_main.y + (int) (lp_main.height * ten_right_y) - lp_right.height / 2;
        ten_right.setLayoutParams(lp_right);

        lp_bottom.x = lp_main.x + (int) (lp_main.width * ten_bottom_x) - lp_bottom.width / 2;
        lp_bottom.y = lp_main.y + (int) (lp_main.height * ten_bottom_y) - lp_bottom.height / 2;
        ten_bottom.setLayoutParams(lp_bottom);
    }


    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (down_view != null && v != down_view) // 避免双指缩放不同的控件
            return false;
        if (v == img) { // 如果是大图片
            switch (event.getAction() & MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_DOWN:         // 单指按下
                    // 初始化按下的数值
                    down_point.x = event.getX() + img.getLeft();
                    down_point.y = event.getY() + img.getTop();
                    down_leftTop.x = lp_main.x;
                    down_leftTop.y = lp_main.y;
                    down_view = v;
                    mode = DRAG;
                    break;
                case MotionEvent.ACTION_POINTER_DOWN: // 双指按下
                    down_mid = middle(event);
                    down_dis = distance(event);
                    down_scale = scale;
                    mode = ZOOM;
                    break;
                case MotionEvent.ACTION_MOVE:        // 单指移动(可能两个手指按下哦~)
                    if (mode == DRAG) { // 单指拖拽
                        lp_main.x = (int) (down_leftTop.x + img.getLeft() + event.getX() - down_point.x);
                        lp_main.y = (int) (down_leftTop.y + img.getTop() + event.getY() - down_point.y);
                        img.setLayoutParams(lp_main);
                    } else if (mode == ZOOM) {
                        float dis = distance(event); // 获取两个手指之间的距离
                        PointF mid = middle(event);   // 获取两个手指之间的中间点
                        float propertion = dis / down_dis;

                        if (propertion <= 0.1) return true; // 如果一下子放太小，图片可能会看不见？

                        scale = down_scale * propertion;              // 实际缩放倍数

                        lp_main.width = (int) (ori_size.getWidth() * scale);
                        lp_main.height = (int) (ori_size.getHeight() * scale);

                        // 坐标移动数值
                        float property_x = (down_mid.x - down_leftTop.x) / (ori_size.getWidth() * down_scale);
                        float property_y = (down_mid.y - down_leftTop.y) / (ori_size.getHeight() * down_scale);
                        lp_main.x = (int) (mid.x - property_x * lp_main.width);
                        lp_main.y = (int) (mid.y - property_y * lp_main.height);

                        img.setLayoutParams(lp_main);
                    }
                    adjustTens();
                    break;
                case MotionEvent.ACTION_UP:          // 单指放开
                    mode = NONE;
                    down_view = null;
                    break;
                case MotionEvent.ACTION_POINTER_UP:  // 手指放开
                    mode = NONE; // DRAG // 为了避免 bug，放开一根手指即算作全部放开
                    break;
            }
        } else if (v == ten_top) { // 如果是十字架，则移动
            switch (event.getAction() & MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_DOWN:        // 单指按下
                    // 初始化按下的数值
                    down_point.x = event.getX() + ten_top.getLeft();
                    down_point.y = event.getY() + ten_top.getTop();
                    down_leftTop.x = lp_top.x;
                    down_leftTop.y = lp_top.y;
                    down_view = v;
                    break;
                case MotionEvent.ACTION_MOVE:        // 单指移动
                    lp_top.x = (int) (down_leftTop.x + ten_top.getLeft() + event.getX() - down_point.x);
                    lp_top.y = (int) (down_leftTop.y + ten_top.getTop() + event.getY() - down_point.y);
                    ten_top.setLayoutParams(lp_top);

                    ten_top_x = (lp_top.x + lp_top.width / 2 - lp_main.x) / (float) lp_main.width;
                    ten_top_y = (lp_top.y + lp_top.height / 2 - lp_main.y) / (float) lp_main.height;

                    int pixel_x = (int) (ori_size.getWidth() * ten_top_x);
                    int pixel_y = (int) (ori_size.getHeight() * ten_top_y);
                    mResultTv.setText("x:" + pixel_x + "\ny:" + pixel_y);
                    break;
                case MotionEvent.ACTION_UP:          // 单指放开
                    down_view = null;
                    break;
            }
        } else if (v == ten_left) { // 如果是十字架，则移动
            switch (event.getAction() & MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_DOWN:        // 单指按下
                    // 初始化按下的数值
                    down_point.x = event.getX() + ten_left.getLeft();
                    down_point.y = event.getY() + ten_left.getTop();
                    down_leftTop.x = lp_left.x;
                    down_leftTop.y = lp_left.y;
                    down_view = v;
                    break;
                case MotionEvent.ACTION_MOVE:        // 单指移动
                    lp_left.x = (int) (down_leftTop.x + ten_left.getLeft() + event.getX() - down_point.x);
                    lp_left.y = (int) (down_leftTop.y + ten_left.getTop() + event.getY() - down_point.y);
                    ten_left.setLayoutParams(lp_left);

                    ten_left_x = (lp_left.x + lp_left.width / 2 - lp_main.x) / (float) lp_main.width;
                    ten_left_y = (lp_left.y + lp_left.height / 2 - lp_main.y) / (float) lp_main.height;

                    int pixel_x = (int) (ori_size.getWidth() * ten_left_x);
                    int pixel_y = (int) (ori_size.getHeight() * ten_left_y);
                    mResultTv.setText("x:" + pixel_x + "\ny:" + pixel_y);
                    break;
                case MotionEvent.ACTION_UP:          // 单指放开
                    down_view = null;
                    break;
            }
        } else if (v == ten_right) { // 如果是十字架，则移动
            switch (event.getAction() & MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_DOWN:        // 单指按下
                    // 初始化按下的数值
                    down_point.x = event.getX() + ten_right.getLeft();
                    down_point.y = event.getY() + ten_right.getTop();
                    down_leftTop.x = lp_right.x;
                    down_leftTop.y = lp_right.y;
                    down_view = v;
                    break;
                case MotionEvent.ACTION_MOVE:        // 单指移动
                    lp_right.x = (int) (down_leftTop.x + ten_right.getLeft() + event.getX() - down_point.x);
                    lp_right.y = (int) (down_leftTop.y + ten_right.getTop() + event.getY() - down_point.y);
                    ten_right.setLayoutParams(lp_right);

                    ten_right_x = (lp_right.x + lp_right.width / 2 - lp_main.x) / (float) lp_main.width;
                    ten_right_y = (lp_right.y + lp_right.height / 2 - lp_main.y) / (float) lp_main.height;

                    int pixel_x = (int) (ori_size.getWidth() * ten_right_x);
                    int pixel_y = (int) (ori_size.getHeight() * ten_right_y);
                    mResultTv.setText("x:" + pixel_x + "\ny:" + pixel_y);
                    break;
                case MotionEvent.ACTION_UP:          // 单指放开
                    down_view = null;
                    break;
            }
        } else if (v == ten_bottom) { // 如果是十字架，则移动
            switch (event.getAction() & MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_DOWN:        // 单指按下
                    // 初始化按下的数值
                    down_point.x = event.getX() + ten_bottom.getLeft();
                    down_point.y = event.getY() + ten_bottom.getTop();
                    down_leftTop.x = lp_bottom.x;
                    down_leftTop.y = lp_bottom.y;
                    down_view = v;
                    break;
                case MotionEvent.ACTION_MOVE:        // 单指移动
                    lp_bottom.x = (int) (down_leftTop.x + ten_bottom.getLeft() + event.getX() - down_point.x);
                    lp_bottom.y = (int) (down_leftTop.y + ten_bottom.getTop() + event.getY() - down_point.y);
                    ten_bottom.setLayoutParams(lp_bottom);

                    ten_bottom_x = (lp_bottom.x + lp_bottom.width / 2 - lp_main.x) / (float) lp_main.width;
                    ten_bottom_y = (lp_bottom.y + lp_bottom.height / 2 - lp_main.y) / (float) lp_main.height;

                    int pixel_x = (int) (ori_size.getWidth() * ten_bottom_x);
                    int pixel_y = (int) (ori_size.getHeight() * ten_bottom_y);
                    mResultTv.setText("x:" + pixel_x + "\ny:" + pixel_y);
                    break;
                case MotionEvent.ACTION_UP:          // 单指放开
                    down_view = null;
                    break;
            }
        }
        return true;
    }

    /* 计算两个触摸点之间的距离 */
    private float distance(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return Float.valueOf(String.valueOf(Math.sqrt(x * x + y * y)));
    }

    // 计算两个触摸点的中点(针对屏幕)
    private PointF middle(MotionEvent event) {
        float x = event.getX(0) + event.getX(1) + img.getLeft();
        float y = event.getY(0) + event.getY(1) + img.getTop();
        return new PointF(x / 2, y / 2);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tv_result:
                openAlbum();
                break;
        }
    }

    private void openAlbum() {
        Intent intent = new Intent(Intent.ACTION_PICK, null);
        intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
        startActivityForResult(intent, PHOTOZOOM);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (data == null) return ;
        if (requestCode == PHOTOZOOM) {
            Uri originalUrl = data.getData();
            String pathName;
            Bitmap bitmap = null;
            try {
                bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(data.getData()));
                img.setImageBitmap(bitmap);
                ori_size = new SizeF(bitmap.getWidth(), bitmap.getHeight());
                Resources resources = this.getResources();
                DisplayMetrics displayMetrics = resources.getDisplayMetrics();
                int screen_width = displayMetrics.widthPixels;
                int screen_height = displayMetrics.heightPixels;
                lp_main.width = bitmap.getWidth();
                lp_main.height = bitmap.getHeight();
                lp_main.x = (screen_width - lp_main.width) / 2;
                lp_main.y = (screen_height - lp_main.height) / 2;
                img.setLayoutParams(lp_main);
                adjustTens();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    }
}
