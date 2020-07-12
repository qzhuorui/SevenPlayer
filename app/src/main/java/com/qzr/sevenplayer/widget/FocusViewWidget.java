package com.qzr.sevenplayer.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

import com.qzr.sevenplayer.R;
import com.qzr.sevenplayer.utils.HandlerProcess;

/**
 * @ProjectName: SevenPlayer
 * @Package: com.qzr.sevenplayer.widget
 * @ClassName: FocusViewWidget
 * @Description:
 * @Author: qzhuorui
 * @CreateDate: 2020/7/5 12:44
 */
public class FocusViewWidget extends View {

    private int mStrokeWidth;

    private int mPrepareColor;
    private int mFinishColor;
    private int mPaintColor;

    private Paint mPaint;
    private int mDuration;

    private boolean isFocusing;


    public FocusViewWidget(Context context) {
        super(context);
        initWidget(context, null);
    }

    public FocusViewWidget(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initWidget(context, attrs);
    }

    public FocusViewWidget(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initWidget(context, attrs);
    }

    private void initWidget(Context context, @Nullable AttributeSet attrs) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.camera_focus_view);
        this.mStrokeWidth = (int) typedArray.getDimension(R.styleable.camera_focus_view_stroke_width, 5);
        this.mPrepareColor = typedArray.getColor(R.styleable.camera_focus_view_prepare_color, Color.RED);
        this.mFinishColor = typedArray.getColor(R.styleable.camera_focus_view_finish_color, Color.YELLOW);
        this.mPaintColor = mPrepareColor;
        this.mPaint = new Paint();
        this.mDuration = 1000;
        this.setVisibility(GONE);
    }

    public void beginFocus(int centerX, int centerY) {
        this.mPaintColor = mPrepareColor;
        isFocusing = true;
        int x = centerX - getMeasuredWidth() / 2;
        int y = centerY - getMeasuredHeight() / 2;
        setX(x);
        setY(y);
        setVisibility(VISIBLE);
        invalidate();
    }

    public void endFocus(boolean isSuccess) {
        isFocusing = false;
        if (isSuccess) {
            this.mPaintColor = mFinishColor;
            HandlerProcess.getInstance().postDelayedOnMain(new Runnable() {
                @Override
                public void run() {
                    if (!isFocusing) {
                        setVisibility(GONE);
                    }
                }
            }, mDuration);
            invalidate();
        } else {
            setVisibility(GONE);
        }
    }

    public void cancelFocus() {
        isFocusing = false;
        setVisibility(GONE);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = getMeasuredWidth();
        int height = getMeasuredHeight();
        mPaint.setStrokeWidth(mStrokeWidth);
        mPaint.setAntiAlias(true);
        mPaint.setColor(mPaintColor);
        mPaint.setStyle(Paint.Style.FILL);
        canvas.drawLine(0, 0, width/3, 0, mPaint);
        canvas.drawLine(width*2/3, 0, width, 0, mPaint);
        canvas.drawLine(0, height, width/3, height, mPaint);
        canvas.drawLine(width*2/3, height, width, height, mPaint);

        canvas.drawLine(0, 0, 0, height/3, mPaint);
        canvas.drawLine(0, height*2/3, 0, height, mPaint);
        canvas.drawLine(width, 0, width, height/3, mPaint);
        canvas.drawLine(width, height*2/3, width, height, mPaint);
    }
}
