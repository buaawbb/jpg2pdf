package com.jpg2pdf.app;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.core.content.FileProvider;

import java.io.File;

/**
 * PDF分享助手 - 调用Android原生分享功能
 */
public class PdfShareHelper {

    /**
     * 分享PDF文件
     *
     * @param context    上下文
     * @param pdfFile    PDF文件
     * @param authority  FileProvider的authority
     */
    public static void sharePdf(Context context, File pdfFile, String authority) {
        Uri fileUri = FileProvider.getUriForFile(context, authority, pdfFile);

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("application/pdf");
        shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        context.startActivity(Intent.createChooser(shareIntent, "分享PDF"));
    }
}
