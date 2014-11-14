package com.cpixelarts.pixelarts.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.View;

import com.cpixelarts.pixelarts.model.PixelArt;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Random;

/**
 * Created by vincent on 04/11/14.
 */
public class PixelArtView extends View {

    public interface OnSelectPixelsListener {
        public void onSelectPixels(Integer[] position);
    }

    private OnSelectPixelsListener mOnSelectPixelsListener = null;
    public void setOnSelectPixelsListener(OnSelectPixelsListener onSelectPixelsListener) {
        this.mOnSelectPixelsListener = onSelectPixelsListener;
    }

    private static final int INVALID_POINTER_ID = -1;
    private static final int NB_DISPLAYED_COLORS = 8;

    private Context context;
    private PixelArt mPixelArt;
    private int currentColor = -1;
    private int pixelSize = 0;
    private int colorPixelSize = 0;
    private boolean displayAllColors = false;
    private boolean addingPixels = false;
    private LinkedList<Integer> colors;

    private ArrayList<Integer> position = new ArrayList<Integer>();

    public PixelArtView(Context context) {
        super(context);
        this.context = context;
        mPixelArt = null;
        colors = new LinkedList<Integer>();
    }

    public PixelArtView(Context context, PixelArt pixelArt) {
        super(context);
        this.context = context;
        mPixelArt = pixelArt;
        colors = new LinkedList<Integer>();
    }

    public int getCurrentColor() {
        return currentColor;
    }

    public void setPixelArt(PixelArt pixelArt) {
        mPixelArt = pixelArt;
        colors = new LinkedList<Integer>();
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        final int action = ev.getAction();

        boolean wasAddingPixels = addingPixels;
        addingPixels = false;
        switch (action & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN: {
                final float x = ev.getX();
                final float y = ev.getY();

                position.clear();

                if (isOnPixelArt(x, y)) {
                    addPixel(x, y);
                } else if (isOnColor(x, y)) {
                    selectColor(x, y);
                } else if (isOnMoreColors(x, y)) {
                    displayAllColors = true;
                    invalidate();
                }

                break;
            }
            case MotionEvent.ACTION_MOVE: {
                final float x = ev.getX();
                final float y = ev.getY();

                if (wasAddingPixels && isOnPixelArt(x, y)) {
                    addPixel(x, y);
                }

                break;
            }
            case MotionEvent.ACTION_UP: {
                if (!wasAddingPixels || position.isEmpty()) {
                    return true;
                }

                final float x = ev.getX();
                final float y = ev.getY();
                if (isOnPixelArt(x, y)) {
                    if (mOnSelectPixelsListener != null) {
                        Integer[] positions = position.toArray(new Integer[position.size()]);
                        mOnSelectPixelsListener.onSelectPixels(positions);
                    }
                } else {
                    for (Integer pos : position) {
                        mPixelArt.pixels[pos] = -1;
                    }
                    invalidate();
                }
            }
            default: {
                break;
            }
        }

        return true;
    }

    private void addPixel(float x, float y) {
        addingPixels = true;

        int xx = (int)x / pixelSize;
        int yy = (int)y / pixelSize;
        int pos = xx + yy * mPixelArt.width;

        if (mPixelArt.locked == false && mPixelArt.pixels[pos] < 0) {
            mPixelArt.pixels[pos] = currentColor;
            invalidate();

            position.add(pos);
        }
    }

    private void selectColor(float x, float y) {
        int color = 0;
        if (displayAllColors) {
            int pixelAllColorSize = getWidth() / 16;
            int xx = (int) (x / pixelAllColorSize);
            int yy = (int) (y / pixelAllColorSize);

            if (yy < 8) {
                color = xx + (yy * 32);
            } else {
                color = xx + ((2 * (yy - 8) + 1) * 16);
            }
            displayAllColors = false;
        } else {
            int xx = (int)(x / colorPixelSize);
            if (colors.size() > xx) {
                color = colors.get(xx);
            }
        }

        currentColor = color;
        invalidate();
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (mPixelArt == null) {
            return;
        }

        Paint paint = new Paint();
        paint.setStyle(Paint.Style.FILL);

        if (getWidth() > getHeight()) {
            pixelSize = getHeight() / mPixelArt.width;
        } else {
            pixelSize = getWidth() / mPixelArt.width;
        }
        colorPixelSize = getWidth() / NB_DISPLAYED_COLORS;
        int height = pixelSize * mPixelArt.height;

        if (mPixelArt.pixels != null) {
            drawPixelArt(canvas);
        }

        if (mPixelArt.locked == false) {
            if (displayAllColors) {
                drawAllColors(canvas);
            } else {
                drawColors(canvas);
            }
        }

        drawGrid(canvas);
    }

    private void drawPixelArt(Canvas canvas) {
        for (int i = 0; i < mPixelArt.pixels.length; i++) {
            if (mPixelArt.pixels[i] >= 0) {
                drawPixel(canvas, i, getColor(mPixelArt.pixels[i]));
                if (colors.contains(mPixelArt.pixels[i])) {
                    colors.remove((Integer)mPixelArt.pixels[i]);
                }
                colors.addFirst(mPixelArt.pixels[i]);
            }
        }
    }

    private void drawPixel(Canvas canvas, int pos, int color) {
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);

        int x = (pos % mPixelArt.width) * pixelSize;
        int y = (pos / mPixelArt.width) * pixelSize;

        canvas.drawRect(x, y, x + pixelSize, y + pixelSize, paint);
    }

    private void drawGrid(Canvas canvas) {
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(getColor(currentColor));

        int width = pixelSize * mPixelArt.height;
        int height = pixelSize * mPixelArt.height;

        for (int x = 1; x < mPixelArt.width; x++) {
            int xx = x * pixelSize;
            canvas.drawLine(xx, 0, xx, height, paint);
        }

        for (int y = 1; y <= mPixelArt.height; y++) {
            int yy = y * pixelSize;
            canvas.drawLine(0, yy, width, yy, paint);
        }
    }

    private void drawColors(Canvas canvas) {
        int height = pixelSize * mPixelArt.height;

        Paint paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.BLACK);
        paint.setTextSize(colorPixelSize * 0.75f);
        canvas.drawText("Choose your color", 0, height + colorPixelSize * 0.75f, paint);

        Random rand = new Random();
        // add current color at first place
        if (currentColor == -1) {
            do currentColor = rand.nextInt() % 256; while (currentColor < 0 || colors.contains(currentColor));
        }
        if (colors.contains(currentColor)) {
            colors.remove((Integer)currentColor);
        }
        colors.addFirst(currentColor);

        int color = 0;
        int y = height + colorPixelSize;
        for (int pos = 0; pos < (NB_DISPLAYED_COLORS - 1); pos++) {
            if (pos < colors.size()) {
                color = colors.get(pos);
            } else {
                do color = rand.nextInt() % 256; while (color < 0 || colors.contains(color));
                colors.addLast(color);
            }
            int x = pos * colorPixelSize;

            paint.setColor(getColor(color));
            canvas.drawRect(x + 1, y + 1, x + colorPixelSize - 1, y + colorPixelSize - 1, paint);
        }

        paint.setTextSize(colorPixelSize);
        paint.setColor(Color.BLACK);

        int x = (NB_DISPLAYED_COLORS - 1) * colorPixelSize;
        canvas.drawText("...", x, y + (colorPixelSize / 2), paint);
    }

    private void drawAllColors(Canvas canvas) {

        Paint paint = new Paint();
        paint.setStyle(Paint.Style.FILL);

        int pixelAllColorSize = getWidth() / 16;
        for (int y = 0; y < 16; y++) {
            int yy;
            if (y % 2 == 0) {
                yy = (y / 2) * pixelAllColorSize;
            } else {
                yy = (8 + y / 2) * pixelAllColorSize;
            }
            for (int x = 0; x < 16; x++) {
                int xx = x * pixelAllColorSize;

                paint.setColor(getColor(x + y * 16));
                canvas.drawRect(xx, yy, xx + pixelAllColorSize, yy + pixelAllColorSize, paint);
            }
        }
    }

    private boolean isOnPixelArt(float x, float y) {
        return displayAllColors == false &&
                x >= 0 && x < mPixelArt.width * pixelSize &&
                y >= 0 && y < mPixelArt.height * pixelSize;
    }

    private boolean isOnColor(float x, float y) {
        if (displayAllColors) {
            return x >= 0 && x < getWidth() && y >= 0 && y < getWidth();
        }

        int height = pixelSize * mPixelArt.height;
        return mPixelArt.locked == false &&
                y >= height + colorPixelSize && y < height + 2 * colorPixelSize &&
                x >= 0 && x < (NB_DISPLAYED_COLORS - 1) * colorPixelSize;
    }

    private boolean isOnMoreColors(float x, float y) {
        int height = pixelSize * mPixelArt.height;

        return mPixelArt.locked == false &&
                y >= height + colorPixelSize && y < height + 2 * colorPixelSize &&
                x >= (NB_DISPLAYED_COLORS - 1) * colorPixelSize && x < NB_DISPLAYED_COLORS * colorPixelSize;
    }

    private int getColor(int color) {
        int r = color >> 5;
        int rr = ((r << 5) + (r << 2) + (r >> 1)) % 256;

        int g = (color & 0x1f) >> 2;
        int gg = ((g << 5) + (g << 2) + (g >> 1)) % 256;

        int b = color & 0x03;
        int bb = (b << 6) + (b << 4) + (b << 2) + b;

        return Color.rgb(rr, gg, bb);
    }
}
