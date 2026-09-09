package com.yaonan.service;

import static com.yaonan.util.global.Global.TAG;

import android.app.Service;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;

import androidx.annotation.Nullable;

import com.yaonan.App;
import com.yaonan.util.MediaProjectionHelper;
import com.yaonan.util.NotificationHelper;
import com.yaonan.util.WindowHelper;
import com.yaonan.util.function.Tuple2;
import com.yaonan.util.function.Tuple5;
import com.yaonan.util.io.ImageUtil;
import com.yaonan.util.jna.UI;
import com.yaonan.util.lang.MyColor;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Calendar;
import java.util.Locale;

public class MediaProjectionService extends Service {

    @Nullable
    private static MediaProjection mMediaProjection;
    @Nullable
    private static ImageReader mImageReader;
    @Nullable
    private static VirtualDisplay mVirtualDisplayImageReader;
    public static int resultCode;
    public static Intent resultData;
    private static boolean mImageAvailable = false;
    public static boolean running = false;

    private static final MediaProjection.Callback MEDIA_PROJECTION_CALLBACK = new MediaProjection.Callback() {};

    @Override
    public void onCreate() {
        super.onCreate();
        NotificationHelper.startMediaProjectionForeground(this, "截图");
        mMediaProjection = MediaProjectionHelper.getManager().getMediaProjection(resultCode, resultData);
        createImageReaderVirtualDisplay();
        running = true;
    }

    private static void createImageReaderVirtualDisplay() {
        if (mMediaProjection != null) {
            DisplayMetrics dm = WindowHelper.getRealMetrics();
            mImageReader = ImageReader.newInstance(dm.widthPixels, dm.heightPixels, PixelFormat.RGBA_8888, 1);
            mImageReader.setOnImageAvailableListener(reader -> { mImageAvailable = true; }, null);
            mMediaProjection.registerCallback(MEDIA_PROJECTION_CALLBACK, null);
            mVirtualDisplayImageReader = mMediaProjection.createVirtualDisplay("ImageReader",
                    dm.widthPixels, dm.heightPixels, dm.densityDpi, Display.FLAG_ROUND, mImageReader.getSurface(), null, null);
        }
    }

    public static void screenshot() {
        if (!mImageAvailable) { UI.alert("截屏失败"); return; }
        if (mImageReader == null) { UI.alert("截屏失败"); return; }
        try {
            Image image = mImageReader.acquireLatestImage();
            int width = image.getWidth();
            int height = image.getHeight();
            final Image.Plane plane = image.getPlanes()[0];
            final ByteBuffer buffer = plane.getBuffer();
            int pixelStride = plane.getPixelStride();
            int rowStride = plane.getRowStride();
            int rowPadding = rowStride - pixelStride * width;
            int bitmapWidth = width + rowPadding / pixelStride;
            Bitmap bitmap = Bitmap.createBitmap(bitmapWidth, height, Bitmap.Config.ARGB_8888);
            bitmap.copyPixelsFromBuffer(buffer);
            image.close();
            Bitmap result = Bitmap.createBitmap(bitmap, 0, 0, width, height);
            bitmap.recycle();
            String fileName = createScreenshotFileName();
            File file = new File(App.getApp().getExternalFilesDir(null).getParent(), fileName);
            if (!file.exists()) file.createNewFile();
            FileOutputStream fos = new FileOutputStream(file);
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            result.compress(Bitmap.CompressFormat.PNG, 100, bos);
            bos.close();
            result.recycle();
            UI.alert("截图成功！" + fileName);
        } catch (IOException e) {
            e.printStackTrace();
            UI.alert("截图失败！");
        }
    }

    public static Tuple2<MyColor, byte[]> getColorAndScreenshot(int x, int y) throws IOException {
        if (!mImageAvailable || mImageReader == null) throw new IOException("截屏失败");
        Image image = mImageReader.acquireLatestImage();
        int width = image.getWidth();
        int height = image.getHeight();
        final Image.Plane plane = image.getPlanes()[0];
        final ByteBuffer buffer = plane.getBuffer();
        int pixelStride = plane.getPixelStride();
        int rowStride = plane.getRowStride();
        int rowPadding = rowStride - pixelStride * width;
        int bitmapWidth = width + rowPadding / pixelStride;
        Bitmap bitmap = Bitmap.createBitmap(bitmapWidth, height, Bitmap.Config.ARGB_8888);
        bitmap.copyPixelsFromBuffer(buffer);
        image.close();
        Bitmap result = Bitmap.createBitmap(bitmap, 0, 0, width, height);
        bitmap.recycle();
        MyColor myColor;
        if (x < 0 || y < 0) {
            myColor = new MyColor(0, 0, 0);
        } else {
            int color = result.getPixel(x, y);
            int r = ((color >> 16) & 0xff);
            int g = ((color >>  8) & 0xff);
            int b = ((color      ) & 0xff);
            myColor = new MyColor(r, g, b);
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        result.compress(Bitmap.CompressFormat.JPEG, 0, baos);
        result.recycle();
        return new Tuple2<>(myColor, baos.toByteArray());
    }

    public static Tuple5<Integer, Integer, Double, Double, Double> getScreenMatchImg(
            Resources res, int id, int rectX, int rectY, int rectW, int rectH, int scale) {
        if (!mImageAvailable || mImageReader == null) return null;
        try {
            Image image = mImageReader.acquireLatestImage();
            int width = image.getWidth();
            int height = image.getHeight();
            final Image.Plane plane = image.getPlanes()[0];
            final ByteBuffer buffer = plane.getBuffer();
            int pixelStride = plane.getPixelStride();
            int rowStride = plane.getRowStride();
            int rowPadding = rowStride - pixelStride * width;
            int bitmapWidth = width + rowPadding / pixelStride;
            Bitmap bitmap = Bitmap.createBitmap(bitmapWidth, height, Bitmap.Config.ARGB_8888);
            bitmap.copyPixelsFromBuffer(buffer);
            image.close();
            Bitmap result = Bitmap.createBitmap(bitmap, rectX, rectY, rectW, rectH);
            bitmap.recycle();
            Bitmap queryImage = ImageUtil.byteToImage(res.openRawResource(id));
            Tuple5<Integer, Integer, Double, Double, Double> t5 =
                    ImageUtil.cvMatchTemplate(result, queryImage, scale);
            t5._0 += rectX;
            t5._1 += rectY;
            return t5;
        } catch (Exception e) {
            UI.alert(e);
            return null;
        }
    }

    public static MyColor getColor(int x, int y) {
        if (!mImageAvailable || mImageReader == null) return null;
        try {
            Image image = mImageReader.acquireLatestImage();
            int width = image.getWidth();
            int height = image.getHeight();
            final Image.Plane plane = image.getPlanes()[0];
            final ByteBuffer buffer = plane.getBuffer();
            int pixelStride = plane.getPixelStride();
            int rowStride = plane.getRowStride();
            int rowPadding = rowStride - pixelStride * width;
            int bitmapWidth = width + rowPadding / pixelStride;
            Bitmap bitmap = Bitmap.createBitmap(bitmapWidth, height, Bitmap.Config.ARGB_8888);
            bitmap.copyPixelsFromBuffer(buffer);
            image.close();
            Bitmap result = Bitmap.createBitmap(bitmap, 0, 0, width, height);
            bitmap.recycle();
            int color = result.getPixel(x, y);
            result.recycle();
            int r = ((color >> 16) & 0xff);
            int g = ((color >>  8) & 0xff);
            int b = ((color      ) & 0xff);
            return new MyColor(r, g, b);
        } catch (Exception e) {
            UI.alert(e);
            return null;
        }
    }

    public static Bitmap getBitmap() {
        if (!mImageAvailable || mImageReader == null) return null;
        try {
            Image image = mImageReader.acquireLatestImage();
            int width = image.getWidth();
            int height = image.getHeight();
            final Image.Plane plane = image.getPlanes()[0];
            final ByteBuffer buffer = plane.getBuffer();
            int pixelStride = plane.getPixelStride();
            int rowStride = plane.getRowStride();
            int rowPadding = rowStride - pixelStride * width;
            int bitmapWidth = width + rowPadding / pixelStride;
            Bitmap bitmap = Bitmap.createBitmap(bitmapWidth, height, Bitmap.Config.ARGB_8888);
            bitmap.copyPixelsFromBuffer(buffer);
            image.close();
            Bitmap result = Bitmap.createBitmap(bitmap, 0, 0, width, height);
            bitmap.recycle();
            return result;
        } catch (Exception e) {
            return null;
        }
    }

    private static String createScreenshotFileName() {
        Calendar calendar = Calendar.getInstance(Locale.CHINA);
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH) + 1;
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        int second = calendar.get(Calendar.SECOND);
        String str = String.format("%d%02d%02d%02d%02d%02d", year, month, day, hour, minute, second);
        return "Screenshot-" + str + ".png";
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mVirtualDisplayImageReader != null) { mVirtualDisplayImageReader.release(); mVirtualDisplayImageReader = null; }
        if (mImageReader != null) { mImageReader.close(); mImageReader = null; }
        mImageAvailable = false;
        if (mMediaProjection != null) { mMediaProjection.unregisterCallback(MEDIA_PROJECTION_CALLBACK); mMediaProjection.stop(); mMediaProjection = null; }
        running = false;
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}
