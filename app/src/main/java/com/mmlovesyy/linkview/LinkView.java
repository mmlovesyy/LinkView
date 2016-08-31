package com.mmlovesyy.linkview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

/**
 * TODO: document your custom view class.
 */
public class LinkView extends View {

    private static final String TAG = "LinkLayout";

    private boolean mMeasureFinished;
    private float mWidth = 0;
    private float mHeight = 0;

    private int mPaddingLeft;
    private int mPaddingTop;
    private int mPaddingRight;
    private int mPaddingBottom;

    // attributes
    private int mMaxTextLines = 1;
    private int mLineCount = 1;
    private float mLineSpacing = 1.3f;

    // text
    private String mText;
    private int mTextColor = 0xFF333333;
    private float mTextSize = 0;
    private float mTextDesiredWidthInSingleLine;
    private float mTextHeightInSingleLine;
    private float mTextWidth;
    private float mTextHeight;
    private float mTextLeftMargin;
    private List<String> mTextLines = new ArrayList<>(2);

    private TextPaint mTextPaint;

    // drawable
    private Drawable mDrawable;
    private float mDrawableWidth = 0;
    private float mDrawableHeight = 0;

    public LinkView(Context context) {
        super(context);
        init(null, 0);
    }

    public LinkView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public LinkView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    private void init(AttributeSet attrs, int defStyle) {

        // Load attributes
        final TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.LinkView, defStyle, 0);

        mText = a.getString(R.styleable.LinkView_text);
        mTextColor = a.getColor(R.styleable.LinkView_textColor, mTextColor);

        // Use getDimensionPixelSize or getDimensionPixelOffset when dealing with
        // values that should fall on pixel boundaries.
        mTextSize = a.getDimension(R.styleable.LinkView_textSize, mTextSize);
        mTextLeftMargin = a.getDimension(R.styleable.LinkView_textLeftMargin, 0);
        mMaxTextLines = a.getInt(R.styleable.LinkView_maxLines, mMaxTextLines);
        mLineSpacing = a.getFloat(R.styleable.LinkView_textLineSpacing, 1.3f);

        if (a.hasValue(R.styleable.LinkView_srcDrawable)) {
            mDrawable = a.getDrawable(R.styleable.LinkView_srcDrawable);
            mDrawable.setCallback(this);

            BitmapDrawable bd = (BitmapDrawable) mDrawable;
            mDrawableWidth = a.getDimension(R.styleable.LinkView_drawableWidth, bd.getBitmap().getWidth());
            mDrawableHeight = a.getDimension(R.styleable.LinkView_drawableHeight, bd.getBitmap().getHeight());

            mTextLeftMargin = mDrawableHeight == 0 || mDrawableWidth == 0 ? 0 : mTextLeftMargin;
        }

        a.recycle();

        mPaddingLeft = getPaddingLeft();
        mPaddingTop = getPaddingTop();
        mPaddingRight = getPaddingRight();
        mPaddingBottom = getPaddingBottom();

        // Set up a default TextPaint object
        mTextPaint = new TextPaint();
        mTextPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        mTextPaint.setTextAlign(Paint.Align.LEFT);

        // Update TextPaint and text measurements from attributes
        invalidateTextPaintAndMeasurements();
    }

    private void invalidateTextPaintAndMeasurements() {

        mMeasureFinished = false;

        mTextPaint.setTextSize(mTextSize);
        mTextPaint.setColor(mTextColor);
        mTextDesiredWidthInSingleLine = mTextPaint.measureText(mText);

        Rect bounds = new Rect();
        mTextPaint.getTextBounds(mText, 0, mText.length(), bounds);
        mTextHeightInSingleLine = bounds.height();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        if (mMeasureFinished) {
            setMeasuredDimension((int) mWidth, (int) mHeight);
            return;
        }

        Log.d(TAG, "onMeasure()");

        // width
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);

        switch (widthMode) {

            case MeasureSpec.EXACTLY:

                mWidth = widthSize;

                break;

            case MeasureSpec.AT_MOST:

                float desiredWidth = mPaddingLeft + mPaddingRight + mDrawableWidth + mTextLeftMargin + mTextDesiredWidthInSingleLine;
                mWidth = widthSize > desiredWidth ? desiredWidth : widthSize;

                break;

            case MeasureSpec.UNSPECIFIED:

                mWidth = widthSize;

                break;
        }

        // height
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        switch (heightMode) {

            case MeasureSpec.EXACTLY:

                mHeight = heightSize;

                break;

            case MeasureSpec.AT_MOST:

                mHeight = mPaddingTop + mPaddingBottom + (mDrawableHeight > mTextHeightInSingleLine ? mDrawableHeight : mTextHeightInSingleLine);
                mHeight = mHeight > heightSize ? heightSize : mHeight;

                break;

            case MeasureSpec.UNSPECIFIED:

                mHeight = heightSize;

                break;
        }

        setMeasuredDimension((int) mWidth, (int) mHeight);
        mMeasureFinished = true;

        Log.d(TAG, "mWidth: " + mWidth + ", mHeight: " + mHeight);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        Log.d(TAG, "onDraw()");

        handleBreakLines();

        // Draw drawable
        if (mDrawable != null) {
            mDrawable.setBounds(mPaddingLeft, mPaddingTop, (int) (mPaddingLeft + mDrawableWidth), (int) (mPaddingTop + mDrawableHeight));
            mDrawable.draw(canvas);
        }

        Log.d(TAG, toString() + ", mHeight=" + mHeight + ", mPaddingTop=" + mPaddingTop);

        // Draw the text
        int size = Math.min(mTextLines.size(), mMaxTextLines);
        float offsetX = mPaddingLeft + mDrawableWidth + mTextLeftMargin;
        float added = (mLineSpacing - 1) * mTextHeightInSingleLine;
        float baseY = mHeight / 2 - (size * mTextHeightInSingleLine) / 2 - added * (size - 1);

        for (int i = 0; i < size; i++) {
            String t = mTextLines.get(i);
            canvas.drawText(t, offsetX, baseY + mTextHeightInSingleLine * (i + 1) + i * added, mTextPaint);
        }

        mTextLines.clear();
    }

    private void handleBreakLines() {

        float textWidthAtMost = mWidth - mDrawableWidth - mPaddingRight - mPaddingLeft - mTextLeftMargin;

        if (textWidthAtMost <= 0) {
            throw new IllegalStateException("too large drawable: width=" + mDrawableWidth);
        }

//        String tmp = mText;
//        boolean truncate;
//
//        while (true) {
//
//            int cha = mTextPaint.breakText(tmp, true, textWidthAtMost, null);
//            String line = tmp.substring(0, cha);
//
//            if ("".equals(line)) {
//                truncate = mTextLines.size() > mMaxTextLines;
//                break;
//            }
//
//            mTextLines.add(line);
//            tmp = tmp.substring(cha);
//        }

        int pre = 0;
        int index = 0;
        int len = mText.length();
        while (index < len - 1) {
            index += mTextPaint.breakText(mText, index, len, true, textWidthAtMost, null);
            String line = mText.substring(pre, index);
            mTextLines.add(line);
            pre = index;
        }

        if (mTextLines.size() > mMaxTextLines) {

            int truncateAt = mMaxTextLines - 1;
            String lastLine = mTextLines.get(truncateAt);
            int l = lastLine.length();
            lastLine = l > 1 ? lastLine.substring(0, l - 1) : lastLine;
            lastLine += "...";

            mTextLines.set(truncateAt, lastLine);
        }

        Log.d(TAG, "textWidthAtMost: " + textWidthAtMost);
        Log.d(TAG, "truncated text: " + mText);
    }

    /**
     * Gets the example string attribute value.
     *
     * @return The example string attribute value.
     */
    public String getExampleString() {
        return mText;
    }

    /**
     * Sets the view's example string attribute value. In the example view, this string
     * is the text to draw.
     *
     * @param exampleString The example string attribute value to use.
     */
    public void setExampleString(String exampleString) {
        mText = exampleString;
        invalidateTextPaintAndMeasurements();
    }

    /**
     * Gets the example color attribute value.
     *
     * @return The example color attribute value.
     */
    public int getExampleColor() {
        return mTextColor;
    }

    /**
     * Sets the view's example color attribute value. In the example view, this color
     * is the font color.
     *
     * @param exampleColor The example color attribute value to use.
     */
    public void setExampleColor(int exampleColor) {
        mTextColor = exampleColor;
        invalidateTextPaintAndMeasurements();
    }

    /**
     * Gets the example dimension attribute value.
     *
     * @return The example dimension attribute value.
     */
    public float getExampleDimension() {
        return mTextSize;
    }

    /**
     * Sets the view's example dimension attribute value. In the example view, this dimension
     * is the font size.
     *
     * @param exampleDimension The example dimension attribute value to use.
     */
    public void setExampleDimension(float exampleDimension) {
        mTextSize = exampleDimension;
        invalidateTextPaintAndMeasurements();
    }

    /**
     * Gets the example drawable attribute value.
     *
     * @return The example drawable attribute value.
     */
    public Drawable getExampleDrawable() {
        return mDrawable;
    }

    /**
     * Sets the view's example drawable attribute value. In the example view, this drawable is
     * drawn above the text.
     *
     * @param exampleDrawable The example drawable attribute value to use.
     */
    public void setExampleDrawable(Drawable exampleDrawable) {
        mDrawable = exampleDrawable;
    }

    private static int dip2px(Context context, float dipValue) {
        if (context == null) {
            return (int) dipValue;
        }
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dipValue * scale + 0.5f);
    }

    private static int getHorizontalMarginSum(View v) {
        ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
        return mlp.leftMargin + mlp.rightMargin;
    }

    @Override
    public String toString() {
        return "mDrawableWidth=" + mDrawableWidth + ", mTextDesiredWidthInSingleLine=" + mTextDesiredWidthInSingleLine + ", mTextHeightInSingleLine=" + mTextHeightInSingleLine;
    }
}
