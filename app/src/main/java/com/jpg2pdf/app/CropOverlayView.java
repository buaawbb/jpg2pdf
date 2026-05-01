package com.jpg2pdf.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class CropOverlayView extends View {

    private static final int HANDLE_SIZE_DP = 24;
    private static final int STROKE_WIDTH_DP = 3;
    private static final int MIN_BOX_SIZE_DP = 50;

    private final Paint fillPaint;
    private final Paint cropBorderPaint;
    private final Paint whiteBorderPaint;
    private final Paint handlePaint;
    private final Paint dimPaint;
    private final float handleSize;
    private final float halfHandle;
    private final float minBoxSize;

    private RectF cropRect;
    private boolean hasCropRect = false;

    private RectF whiteRect;
    private boolean hasWhiteRect = false;

    private int activeMode = MODE_CROP;
    public static final int MODE_CROP = 0;
    public static final int MODE_WHITE_BOX = 1;

    private enum Edge {
        NONE, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT,
        TOP, BOTTOM, LEFT, RIGHT, CENTER
    }
    private Edge selectedEdge = Edge.NONE;

    private float lastX, lastY;
    private boolean isDragging = false;

    public CropOverlayView(Context context) {
        this(context, null);
    }

    public CropOverlayView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CropOverlayView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        float density = context.getResources().getDisplayMetrics().density;
        handleSize = HANDLE_SIZE_DP * density;
        halfHandle = handleSize / 2;
        minBoxSize = MIN_BOX_SIZE_DP * density;

        fillPaint = new Paint();
        fillPaint.setColor(0xFFFFFFFF);
        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setAntiAlias(true);

        cropBorderPaint = new Paint();
        cropBorderPaint.setColor(0xFFFF0000);
        cropBorderPaint.setStyle(Paint.Style.STROKE);
        cropBorderPaint.setStrokeWidth(STROKE_WIDTH_DP * density);
        cropBorderPaint.setAntiAlias(true);

        whiteBorderPaint = new Paint();
        whiteBorderPaint.setColor(0xFFFFFFFF);
        whiteBorderPaint.setStyle(Paint.Style.STROKE);
        whiteBorderPaint.setStrokeWidth(STROKE_WIDTH_DP * density);
        whiteBorderPaint.setAntiAlias(true);

        handlePaint = new Paint();
        handlePaint.setColor(0xFFFF0000);
        handlePaint.setStyle(Paint.Style.FILL);
        handlePaint.setAntiAlias(true);

        dimPaint = new Paint();
        dimPaint.setColor(0x80000000);
        dimPaint.setStyle(Paint.Style.FILL);
    }

    public void setActiveMode(int mode) {
        this.activeMode = mode;
        if (mode == MODE_WHITE_BOX) {
            handlePaint.setColor(0xFF000000);
        } else {
            handlePaint.setColor(0xFFFF0000);
        }
        invalidate();
    }

    public int getActiveMode() {
        return activeMode;
    }

    public void setCropRect(RectF rect) {
        this.cropRect = rect;
        this.hasCropRect = true;
        invalidate();
    }

    public RectF getCropRect() {
        return cropRect;
    }

    public boolean hasCropRect() {
        return hasCropRect;
    }

    public void clearCropRect() {
        hasCropRect = false;
        cropRect = null;
        invalidate();
    }

    public void setWhiteRect(RectF rect) {
        this.whiteRect = rect;
        this.hasWhiteRect = true;
        invalidate();
    }

    public RectF getWhiteRect() {
        return whiteRect;
    }

    public boolean hasWhiteRect() {
        return hasWhiteRect;
    }

    public void clearWhiteRect() {
        hasWhiteRect = false;
        whiteRect = null;
        invalidate();
    }

    public void initDefaultCropRect() {
        if (getWidth() > 0 && getHeight() > 0) {
            float topPadding = getHeight() / 10f;
            float bottomPadding = getHeight() / 10f;
            cropRect = new RectF(0, topPadding, getWidth(), getHeight() - bottomPadding);
            hasCropRect = true;
            activeMode = MODE_CROP;
            handlePaint.setColor(0xFFFF0000);
            invalidate();
        }
    }

    public void initDefaultWhiteRect() {
        if (getWidth() > 0 && getHeight() > 0) {
            float w = getWidth() / 3f;
            float h = getHeight() / 4f;
            float left = (getWidth() - w) / 2f;
            float top = (getHeight() - h) / 2f;
            whiteRect = new RectF(left, top, left + w, top + h);
            hasWhiteRect = true;
            activeMode = MODE_WHITE_BOX;
            handlePaint.setColor(0xFF000000);
            invalidate();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (hasCropRect && cropRect != null) {
            canvas.drawRect(0, 0, getWidth(), cropRect.top, dimPaint);
            canvas.drawRect(0, cropRect.bottom, getWidth(), getHeight(), dimPaint);
            canvas.drawRect(0, cropRect.top, cropRect.left, cropRect.bottom, dimPaint);
            canvas.drawRect(cropRect.right, cropRect.top, getWidth(), cropRect.bottom, dimPaint);
            canvas.drawRect(cropRect, cropBorderPaint);
        }

        if (hasWhiteRect && whiteRect != null) {
            canvas.drawRect(whiteRect, fillPaint);
            canvas.drawRect(whiteRect, whiteBorderPaint);
        }

        if (activeMode == MODE_CROP && hasCropRect && cropRect != null) {
            drawHandle(canvas, cropRect.left, cropRect.top);
            drawHandle(canvas, cropRect.right, cropRect.top);
            drawHandle(canvas, cropRect.left, cropRect.bottom);
            drawHandle(canvas, cropRect.right, cropRect.bottom);
            drawHandle(canvas, (cropRect.left + cropRect.right) / 2, cropRect.top);
            drawHandle(canvas, (cropRect.left + cropRect.right) / 2, cropRect.bottom);
            drawHandle(canvas, cropRect.left, (cropRect.top + cropRect.bottom) / 2);
            drawHandle(canvas, cropRect.right, (cropRect.top + cropRect.bottom) / 2);
        }

        if (activeMode == MODE_WHITE_BOX && hasWhiteRect && whiteRect != null) {
            drawHandle(canvas, whiteRect.left, whiteRect.top);
            drawHandle(canvas, whiteRect.right, whiteRect.top);
            drawHandle(canvas, whiteRect.left, whiteRect.bottom);
            drawHandle(canvas, whiteRect.right, whiteRect.bottom);
            drawHandle(canvas, (whiteRect.left + whiteRect.right) / 2, whiteRect.top);
            drawHandle(canvas, (whiteRect.left + whiteRect.right) / 2, whiteRect.bottom);
            drawHandle(canvas, whiteRect.left, (whiteRect.top + whiteRect.bottom) / 2);
            drawHandle(canvas, whiteRect.right, (whiteRect.top + whiteRect.bottom) / 2);
        }
    }

    private void drawHandle(Canvas canvas, float x, float y) {
        canvas.drawCircle(x, y, halfHandle, handlePaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                isDragging = false;
                selectedEdge = getEdge(x, y);
                lastX = x;
                lastY = y;
                if (selectedEdge != Edge.NONE) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                return true;

            case MotionEvent.ACTION_MOVE:
                if (selectedEdge != Edge.NONE) {
                    float dx = x - lastX;
                    float dy = y - lastY;

                    if (!isDragging && Math.abs(dx) < 10 && Math.abs(dy) < 10) {
                        return true;
                    }

                    isDragging = true;
                    moveEdge(selectedEdge, dx, dy);
                    lastX = x;
                    lastY = y;
                    invalidate();
                    return true;
                }
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                selectedEdge = Edge.NONE;
                isDragging = false;
                getParent().requestDisallowInterceptTouchEvent(false);
                return true;
        }
        return false;
    }

    private Edge getEdge(float x, float y) {
        RectF targetRect;
        if (activeMode == MODE_WHITE_BOX && hasWhiteRect && whiteRect != null) {
            targetRect = whiteRect;
        } else if (hasCropRect && cropRect != null) {
            targetRect = cropRect;
        } else {
            return Edge.NONE;
        }

        if (Math.abs(x - targetRect.left) < halfHandle && Math.abs(y - targetRect.top) < halfHandle)
            return Edge.TOP_LEFT;
        if (Math.abs(x - targetRect.right) < halfHandle && Math.abs(y - targetRect.top) < halfHandle)
            return Edge.TOP_RIGHT;
        if (Math.abs(x - targetRect.left) < halfHandle && Math.abs(y - targetRect.bottom) < halfHandle)
            return Edge.BOTTOM_LEFT;
        if (Math.abs(x - targetRect.right) < halfHandle && Math.abs(y - targetRect.bottom) < halfHandle)
            return Edge.BOTTOM_RIGHT;

        float centerX = (targetRect.left + targetRect.right) / 2;
        float centerY = (targetRect.top + targetRect.bottom) / 2;

        if (Math.abs(x - centerX) < halfHandle && Math.abs(y - targetRect.top) < halfHandle)
            return Edge.TOP;
        if (Math.abs(x - centerX) < halfHandle && Math.abs(y - targetRect.bottom) < halfHandle)
            return Edge.BOTTOM;
        if (Math.abs(x - targetRect.left) < halfHandle && Math.abs(y - centerY) < halfHandle)
            return Edge.LEFT;
        if (Math.abs(x - targetRect.right) < halfHandle && Math.abs(y - centerY) < halfHandle)
            return Edge.RIGHT;

        if (x >= targetRect.left && x <= targetRect.right && y >= targetRect.top && y <= targetRect.bottom)
            return Edge.CENTER;

        return Edge.NONE;
    }

    private void moveEdge(Edge edge, float dx, float dy) {
        RectF r;
        if (activeMode == MODE_WHITE_BOX && hasWhiteRect && whiteRect != null) {
            r = whiteRect;
        } else if (hasCropRect && cropRect != null) {
            r = cropRect;
        } else {
            return;
        }

        float viewWidth = getWidth();
        float viewHeight = getHeight();

        switch (edge) {
            case TOP_LEFT:
                r.left = Math.max(0, Math.min(r.right - minBoxSize, r.left + dx));
                r.top = Math.max(0, Math.min(r.bottom - minBoxSize, r.top + dy));
                break;
            case TOP_RIGHT:
                r.right = Math.min(viewWidth, Math.max(r.left + minBoxSize, r.right + dx));
                r.top = Math.max(0, Math.min(r.bottom - minBoxSize, r.top + dy));
                break;
            case BOTTOM_LEFT:
                r.left = Math.max(0, Math.min(r.right - minBoxSize, r.left + dx));
                r.bottom = Math.min(viewHeight, Math.max(r.top + minBoxSize, r.bottom + dy));
                break;
            case BOTTOM_RIGHT:
                r.right = Math.min(viewWidth, Math.max(r.left + minBoxSize, r.right + dx));
                r.bottom = Math.min(viewHeight, Math.max(r.top + minBoxSize, r.bottom + dy));
                break;
            case TOP:
                r.top = Math.max(0, Math.min(r.bottom - minBoxSize, r.top + dy));
                break;
            case BOTTOM:
                r.bottom = Math.min(viewHeight, Math.max(r.top + minBoxSize, r.bottom + dy));
                break;
            case LEFT:
                r.left = Math.max(0, Math.min(r.right - minBoxSize, r.left + dx));
                break;
            case RIGHT:
                r.right = Math.min(viewWidth, Math.max(r.left + minBoxSize, r.right + dx));
                break;
            case CENTER:
                float newLeft = r.left + dx;
                float newTop = r.top + dy;
                float newRight = r.right + dx;
                float newBottom = r.bottom + dy;

                if (newLeft < 0) {
                    newLeft = 0;
                    newRight = r.width();
                }
                if (newTop < 0) {
                    newTop = 0;
                    newBottom = r.height();
                }
                if (newRight > viewWidth) {
                    newRight = viewWidth;
                    newLeft = viewWidth - r.width();
                }
                if (newBottom > viewHeight) {
                    newBottom = viewHeight;
                    newTop = viewHeight - r.height();
                }

                r.left = newLeft;
                r.top = newTop;
                r.right = newRight;
                r.bottom = newBottom;
                break;
        }
    }

    public RectF getCropRectInImageCoords(int imageWidth, int imageHeight,
                                           int displayedWidth, int displayedHeight) {
        if (!hasCropRect || cropRect == null) return null;
        return convertToImageCoords(cropRect, imageWidth, imageHeight, displayedWidth, displayedHeight);
    }

    public RectF getWhiteRectInImageCoords(int imageWidth, int imageHeight,
                                            int displayedWidth, int displayedHeight) {
        if (!hasWhiteRect || whiteRect == null) return null;
        return convertToImageCoords(whiteRect, imageWidth, imageHeight, displayedWidth, displayedHeight);
    }

    private RectF convertToImageCoords(RectF rect, int imageWidth, int imageHeight,
                                        int displayedWidth, int displayedHeight) {
        float scale;
        float offsetX, offsetY;

        float imageAspect = (float) imageWidth / imageHeight;
        float viewAspect = (float) displayedWidth / displayedHeight;

        if (imageAspect > viewAspect) {
            scale = (float) displayedWidth / imageWidth;
            offsetX = 0;
            offsetY = (displayedHeight - imageHeight * scale) / 2;
        } else {
            scale = (float) displayedHeight / imageHeight;
            offsetX = (displayedWidth - imageWidth * scale) / 2;
            offsetY = 0;
        }

        float left = (rect.left - offsetX) / scale;
        float top = (rect.top - offsetY) / scale;
        float right = (rect.right - offsetX) / scale;
        float bottom = (rect.bottom - offsetY) / scale;

        left = Math.max(0, left);
        top = Math.max(0, top);
        right = Math.min(imageWidth, right);
        bottom = Math.min(imageHeight, bottom);

        return new RectF(left, top, right, bottom);
    }

    public RectF getCropRectInViewCoords(RectF imageRect, int imageWidth, int imageHeight,
                                          int viewWidth, int viewHeight) {
        float imageAspect = (float) imageWidth / imageHeight;
        float viewAspect = (float) viewWidth / viewHeight;

        float scale, offsetX, offsetY;
        if (imageAspect > viewAspect) {
            scale = (float) viewWidth / imageWidth;
            offsetX = 0;
            offsetY = (viewHeight - imageHeight * scale) / 2;
        } else {
            scale = (float) viewHeight / imageHeight;
            offsetX = (viewWidth - imageWidth * scale) / 2;
            offsetY = 0;
        }

        return new RectF(
                imageRect.left * scale + offsetX,
                imageRect.top * scale + offsetY,
                imageRect.right * scale + offsetX,
                imageRect.bottom * scale + offsetY
        );
    }
}
