package ru.sawim.widget.chat;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import ru.sawim.General;
import ru.sawim.Scheme;
import ru.sawim.widget.Util;

/**
 * Created with IntelliJ IDEA.
 * User: admin
 * Date: 14.11.13
 * Time: 17:58
 * To change this template use File | Settings | File Templates.
 */
public class MessageTitleItemView extends View {

    private String msgTimeText;
    private String nickText;
    private int nickColor;
    private int msgTimeColor;
    private Typeface nickTypeface;
    private Typeface msgTimeTypeface;
    private int nickSize;
    private int msgTimeSize;
    private BitmapDrawable msgImage;
    private int msgIconY;

    private float msgTimeX;
    private int textY;
    private static TextPaint textPaint;
    private boolean isTimeStampVisible;

    public MessageTitleItemView(Context context) {
        super(context);
        if (textPaint == null) {
            textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
            textPaint.setAntiAlias(true);
            textPaint.setTextSize(General.getFontSize());
            textPaint.setColor(Scheme.getColor(Scheme.THEME_TEXT));
        }
    }

    private void setTextSize(int size) {
        textPaint.setTextSize(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP, size, General.getResources(getContext()).getDisplayMetrics()));
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = measureHeight(heightMeasureSpec);
        setMeasuredDimension(width, height);
    }

    private int measureHeight(int measureSpec) {
        int result = 0;
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);
        int ascent = (int) textPaint.ascent();
        int descent = (int) textPaint.descent();
        if (specMode == MeasureSpec.EXACTLY) {
            result = specSize;
        } else {
            int textHeight = (-ascent + descent) + getPaddingTop() + getPaddingBottom();
            int iconHeight = msgImage == null ? 0 : msgImage.getBitmap().getHeight();
            result = Math.max(textHeight, iconHeight);
            if (specMode == MeasureSpec.AT_MOST) {
                result = Math.min(result, specSize);
            }
        }
        return result;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right,
                            int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        computeCoordinates(right - left, bottom - top);
    }

    private void computeCoordinates(int viewWidth, int viewHeight) {
        int y = viewHeight >> 1;
        if (msgImage != null)
            msgIconY = y - (msgImage.getBitmap().getHeight() >> 1);
        msgTimeX = viewWidth - getPaddingRight();
        textY = getPaddingTop() - (int) textPaint.ascent();
    }

    public void setNick(int nickColor, int nickSize, Typeface nickTypeface, String nickText) {
        this.nickColor = nickColor;
        this.nickSize = nickSize;
        this.nickTypeface = nickTypeface;
        this.nickText = nickText;
    }

    public void setMsgTime(int msgTimeColor, int msgTimeSize, Typeface msgTimeTypeface, String msgTimeText) {
        this.msgTimeColor = msgTimeColor;
        this.msgTimeSize = msgTimeSize;
        this.msgTimeTypeface = msgTimeTypeface;
        this.msgTimeText = msgTimeText;
    }

    public void setMsgImage(BitmapDrawable msgImage) {
        this.msgImage = msgImage;
    }

    public void repaint() {
        requestLayout();
        invalidate();
    }

    @Override
    public void requestLayout() {
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (nickText != null) {
            textPaint.setColor(nickColor);
            textPaint.setTextAlign(Paint.Align.LEFT);
            setTextSize(nickSize);
            textPaint.setTypeface(nickTypeface);
            canvas.drawText(nickText, getPaddingLeft(), textY, textPaint);
        }
        if (msgTimeText != null && isTimeStampVisible) {
            textPaint.setColor(msgTimeColor);
            textPaint.setTextAlign(Paint.Align.RIGHT);
            setTextSize(msgTimeSize);
            textPaint.setTypeface(msgTimeTypeface);
            canvas.drawText(msgTimeText, msgTimeX, textY, textPaint);
        }
        if (msgImage != null && !isTimeStampVisible)
            canvas.drawBitmap(msgImage.getBitmap(), msgTimeX - msgImage.getBitmap().getWidth(), msgIconY, null);
    }

    public void setTimeStampVisible(boolean timeStampVisible) {
        isTimeStampVisible = timeStampVisible;
        invalidate();
    }
}