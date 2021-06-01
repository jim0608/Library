package com.mysafe.lib_identification.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class MsArcFaceRect extends View {
    private CopyOnWriteArrayList<DrawInfo> drawInfoList = new CopyOnWriteArrayList<>();

    // 画笔，复用
    private Paint paint;

    // 默认人脸框厚度
    private static final int DEFAULT_FACE_RECT_THICKNESS = 6;

    public MsArcFaceRect(Context context) {
        this(context, null);
    }

    public MsArcFaceRect(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        paint = new Paint();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (drawInfoList != null && drawInfoList.size() > 0) {
            for (int i = 0; i < drawInfoList.size(); i++) {
                drawFaceRect(canvas, drawInfoList.get(i), DEFAULT_FACE_RECT_THICKNESS, paint);
            }
        }
    }

    public void clearFaceInfo() {
        drawInfoList.clear();
        postInvalidate();
    }

    public void AddFaceInfo(DrawInfo faceInfo) {
        drawInfoList.add(faceInfo);
        postInvalidate();
    }

    public void AddFaceInfo(List<DrawInfo> faceInfoList) {
        drawInfoList.addAll(faceInfoList);
        postInvalidate();
    }

    public void DrawRealtimeFaceInfo(List<DrawInfo> drawInfoList) {
        clearFaceInfo();
        if (drawInfoList == null || drawInfoList.size() == 0) {
            return;
        }
        AddFaceInfo(drawInfoList);
    }

    public static class DrawInfo {
        private Rect rect;
        private int sex;
        private int age;
        private int liveness;
        private int color;
        private String name = null;

        public DrawInfo(Rect rect, int sex, int age, int liveness, int color, String name) {
            this.rect = rect;
            this.sex = sex;
            this.age = age;
            this.liveness = liveness;
            this.color = color;
            this.name = name;
        }

        public DrawInfo(DrawInfo drawInfo) {
            if (drawInfo == null) {
                return;
            }
            this.rect = drawInfo.rect;
            this.sex = drawInfo.sex;
            this.age = drawInfo.age;
            this.liveness = drawInfo.liveness;
            this.color = drawInfo.color;
            this.name = drawInfo.name;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Rect getRect() {
            return rect;
        }

        public void setRect(Rect rect) {
            this.rect = rect;
        }

        public int getSex() {
            return sex;
        }

        public void setSex(int sex) {
            this.sex = sex;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }

        public int getLiveness() {
            return liveness;
        }

        public void setLiveness(int liveness) {
            this.liveness = liveness;
        }

        public int getColor() {
            return color;
        }

        public void setColor(int color) {
            this.color = color;
        }
    }


    /**
     * 绘制数据信息到view上，若 {@link com.mysafe.lib_identification.view.MsArcFaceRect.DrawInfo#getName()} 不为null则绘制 {@link com.mysafe.lib_identification.view.MsArcFaceRect.DrawInfo#getName()}
     *
     * @param canvas            需要被绘制的view的canvas
     * @param drawInfo          绘制信息
     * @param faceRectThickness 人脸框厚度
     * @param paint             画笔
     */
    private static void drawFaceRect(Canvas canvas, MsArcFaceRect.DrawInfo drawInfo, int faceRectThickness, Paint paint) {
        if (canvas == null || drawInfo == null) {
            return;
        }
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(faceRectThickness);
        paint.setColor(drawInfo.getColor());
        paint.setAntiAlias(true);

        Path mPath = new Path();
        // 左上
        Rect rect = drawInfo.getRect();
        mPath.moveTo(rect.left, rect.top + rect.height() / 4);
        mPath.lineTo(rect.left, rect.top);
        mPath.lineTo(rect.left + rect.width() / 4, rect.top);
        // 右上
        mPath.moveTo(rect.right - rect.width() / 4, rect.top);
        mPath.lineTo(rect.right, rect.top);
        mPath.lineTo(rect.right, rect.top + rect.height() / 4);
        // 右下
        mPath.moveTo(rect.right, rect.bottom - rect.height() / 4);
        mPath.lineTo(rect.right, rect.bottom);
        mPath.lineTo(rect.right - rect.width() / 4, rect.bottom);
        // 左下
        mPath.moveTo(rect.left + rect.width() / 4, rect.bottom);
        mPath.lineTo(rect.left, rect.bottom);
        mPath.lineTo(rect.left, rect.bottom - rect.height() / 4);
        canvas.drawPath(mPath, paint);
    }

}
