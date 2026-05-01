package com.jpg2pdf.app;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * PDF生成器 - 无损图片转PDF
 * 使用Android原生PdfDocument，不压缩、不缩放图片
 */
public class PdfGenerator {

    public interface ProgressListener {
        void onProgress(int current, int total);
    }

    /**
     * 生成PDF文件
     *
     * @param context          上下文
     * @param imageUris        图片URI列表
     * @param cropRects        每张图片对应的裁剪区域（可为null表示不裁剪）
     * @param whiteBoxRects    每张图片对应的白框区域（可为null表示无白框）
     * @param outputFile       输出PDF文件
     * @param listener         进度监听器
     * @return 是否成功
     */
    public static boolean generatePdf(Context context, java.util.List<Uri> imageUris,
                                      java.util.List<RectF> cropRects,
                                      java.util.List<RectF> whiteBoxRects,
                                      File outputFile, ProgressListener listener) {
        PdfDocument document = null;
        try {
            document = new PdfDocument();
            int total = imageUris.size();

            for (int i = 0; i < total; i++) {
                Uri uri = imageUris.get(i);

                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                decodeStream(context, uri, options);

                int originalWidth = options.outWidth;
                int originalHeight = options.outHeight;

                RectF cropRect = (cropRects != null && i < cropRects.size()) ? cropRects.get(i) : null;
                RectF whiteBoxRect = (whiteBoxRects != null && i < whiteBoxRects.size()) ? whiteBoxRects.get(i) : null;

                int pageWidth, pageHeight;
                if (cropRect != null) {
                    pageWidth = Math.round(cropRect.width());
                    pageHeight = Math.round(cropRect.height());
                } else {
                    pageWidth = originalWidth;
                    pageHeight = originalHeight;
                }

                if (pageWidth <= 0 || pageHeight <= 0) {
                    pageWidth = originalWidth;
                    pageHeight = originalHeight;
                }

                PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(
                        pageWidth, pageHeight, i + 1).create();
                PdfDocument.Page page = document.startPage(pageInfo);

                Bitmap bitmap = decodeBitmap(context, uri, originalWidth, originalHeight);

                if (bitmap != null) {
                    Canvas canvas = page.getCanvas();

                    if (cropRect != null) {
                        android.graphics.Rect srcRect = new android.graphics.Rect(
                                Math.round(cropRect.left),
                                Math.round(cropRect.top),
                                Math.round(cropRect.right),
                                Math.round(cropRect.bottom)
                        );
                        android.graphics.Rect dstRect = new android.graphics.Rect(
                                0, 0, pageWidth, pageHeight
                        );
                        canvas.drawBitmap(bitmap, srcRect, dstRect, null);

                        if (whiteBoxRect != null) {
                            float scaleX = (float) pageWidth / cropRect.width();
                            float scaleY = (float) pageHeight / cropRect.height();

                            float whiteLeft = (whiteBoxRect.left - cropRect.left) * scaleX;
                            float whiteTop = (whiteBoxRect.top - cropRect.top) * scaleY;
                            float whiteRight = (whiteBoxRect.right - cropRect.left) * scaleX;
                            float whiteBottom = (whiteBoxRect.bottom - cropRect.top) * scaleY;

                            Paint whitePaint = new Paint();
                            whitePaint.setColor(Color.WHITE);
                            whitePaint.setStyle(Paint.Style.FILL);
                            canvas.drawRect(whiteLeft, whiteTop, whiteRight, whiteBottom, whitePaint);
                        }
                    } else {
                        canvas.drawBitmap(bitmap, 0, 0, null);

                        if (whiteBoxRect != null) {
                            float scaleX = (float) pageWidth / originalWidth;
                            float scaleY = (float) pageHeight / originalHeight;

                            float whiteLeft = whiteBoxRect.left * scaleX;
                            float whiteTop = whiteBoxRect.top * scaleY;
                            float whiteRight = whiteBoxRect.right * scaleX;
                            float whiteBottom = whiteBoxRect.bottom * scaleY;

                            Paint whitePaint = new Paint();
                            whitePaint.setColor(Color.WHITE);
                            whitePaint.setStyle(Paint.Style.FILL);
                            canvas.drawRect(whiteLeft, whiteTop, whiteRight, whiteBottom, whitePaint);
                        }
                    }

                    if (!bitmap.isRecycled()) {
                        bitmap.recycle();
                    }
                }

                document.finishPage(page);

                if (listener != null) {
                    listener.onProgress(i + 1, total);
                }
            }

            try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                document.writeTo(fos);
            }

            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            if (document != null) {
                document.close();
            }
        }
    }

    private static void decodeStream(Context context, Uri uri, BitmapFactory.Options options)
            throws IOException {
        try (InputStream is = context.getContentResolver().openInputStream(uri)) {
            if (is != null) {
                BitmapFactory.decodeStream(is, null, options);
            }
        }
    }

    private static Bitmap decodeBitmap(Context context, Uri uri, int width, int height) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        options.inScaled = false;
        options.inDensity = 0;
        options.inTargetDensity = 0;
        options.inScreenDensity = 0;

        try (InputStream is = context.getContentResolver().openInputStream(uri)) {
            if (is != null) {
                return BitmapFactory.decodeStream(is, null, options);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        try (ParcelFileDescriptor pfd = context.getContentResolver().openFileDescriptor(uri, "r")) {
            if (pfd != null) {
                try (InputStream is = new java.io.FileInputStream(pfd.getFileDescriptor())) {
                    return BitmapFactory.decodeStream(is, null, options);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }
}
