package science.keng42.keep.util;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ImageView;

import com.facebook.android.crypto.keychain.SharedPrefsBackedKeyChain;
import com.facebook.crypto.Crypto;
import com.facebook.crypto.Entity;
import com.facebook.crypto.util.SystemNativeCryptoLibrary;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.ref.WeakReference;

import science.keng42.keep.HomeActivity;
import science.keng42.keep.MyApp;
import science.keng42.keep.PhotoActivity;

/**
 * 从本地读取图片到 ImageView
 * Created by Keng on 2015/7/6
 */
public class BitmapLoaderTask extends AsyncTask<Object, Void, Object> {

    public static final int NORMAL_BYTES_BUFFER_SIZE = 1024;
    private final WeakReference imageViewReference;
    private String path;
    private Context context;
    private String mPassword;
    // 用于标志图片展示形式
    private boolean isNormalSize;

    public BitmapLoaderTask(ImageView imageView, Context c) {
        imageViewReference = new WeakReference<>(imageView);
        context = c;
        MyApp myApp = (MyApp) context.getApplicationContext();
        mPassword = myApp.getPassword();
    }

    @Override
    protected final Object doInBackground(Object... params) {
        path = (String) params[0];
        int reqWidth = (int) params[1];
        int reqHeight = (int) params[2];

        File file = new File(path);
        if (!file.exists()) {
            // file not found
            isNormalSize = true;
            return BitmapFactory.decodeResource(context.getResources(),
                    android.R.drawable.ic_menu_report_image);
        }
        isNormalSize = false;
        byte[] bytes = decryptFileToBytes(path);
        Bitmap bitmap = BitmapTool.getSampleBitmapFromBytes(bytes, reqWidth, reqHeight);

        if (context instanceof HomeActivity) {
            HomeActivity activity = (HomeActivity) context;
            activity.addBitmapToMemCache(path, bitmap);
        } else if (context instanceof PhotoActivity) {
            isNormalSize = true;
        }

        return bitmap;
    }

    /**
     * 解密文件到字符数组用于加载到 Bitmap
     */
    private byte[] decryptFileToBytes(String path) {

        Crypto crypto = new Crypto(
                new SharedPrefsBackedKeyChain(context),
                new SystemNativeCryptoLibrary());

        if (!crypto.isAvailable()) {
            return null;
        }

        try {
            InputStream fileStream = new FileInputStream(path);
            InputStream inputStream = crypto.getCipherInputStream(fileStream, new Entity(mPassword));
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            byte[] buf = new byte[NORMAL_BYTES_BUFFER_SIZE];
            int len;
            while ((len = inputStream.read(buf)) > 0) {
                outputStream.write(buf, 0, len);
            }
            return outputStream.toByteArray();
        } catch (Exception e) {
            Log.e(MyApp.TAG, "", e);
        }
        return null;
    }

    @Override
    protected final void onPostExecute(Object o) {
        Bitmap bitmap = (Bitmap) o;
        if (isCancelled()) {
            bitmap = null;
        }
        if (imageViewReference != null && bitmap != null) {
            final ImageView imageView = (ImageView) imageViewReference.get();
            final BitmapLoaderTask task = getBitmapLoaderTask(imageView);
            if (this == task && imageView != null) {
                imageView.setImageBitmap(bitmap);
                if (!isNormalSize) {
                    imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                }
            }
        }
    }

    /**
     * 辅助方法：获取 ImageView 关联的任务
     *
     * @param imageView 目标 ImageView
     * @return 关联任务
     */
    private static BitmapLoaderTask getBitmapLoaderTask(ImageView imageView) {
        if (imageView != null) {
            Drawable drawable = imageView.getDrawable();
            if (drawable instanceof AsyncDrawable) {
                AsyncDrawable asyncDrawable = (AsyncDrawable) drawable;
                return asyncDrawable.getBitmapLoaderTask();
            }
        }
        return null;
    }

    /**
     * 取消潜在的任务
     * 检查是否有另外一个任务与该 ImageView 关联，若存在一个任务已关联：
     * 任务数据相吻合则 不需要进行下一步动作了
     * 任务数据不吻合则 取消该任务然后才新建任务
     *
     * @param data      任务数据
     * @param imageView 目标 ImageView
     * @return 是否需要新建任务
     */
    public static boolean cancelPotentialWork(String data, ImageView imageView) {
        // 查找与该 ImageView 关联的任务
        final BitmapLoaderTask bitmapLoaderTask = getBitmapLoaderTask(imageView);

        if (bitmapLoaderTask != null) {
            final String bitmapData = bitmapLoaderTask.path;
            if ("".equals(bitmapData) || !data.equals(bitmapData)) {
                // 任务数据不吻合，取消该任务
                bitmapLoaderTask.cancel(true);
            } else {
                // 一个相同的任务已存在，不需要重复建任务
                return false;
            }
        }
        // 不存在关联的任务或该任务已取消，于是应该新建任务
        return true;
    }

    /**
     * 解决并发问题：
     * ImageView 保存最近使用的 AsyncTask 的引用
     * 这个引用可以在任务完成的时候再次读取检查
     * 此类用于存储任务的引用
     * 在任务执行中一个占位图会显示在 ImageView 中
     */
    public static class AsyncDrawable extends BitmapDrawable {
        // 任务的弱引用
        private final WeakReference bitmapWorkerTaskReference;

        /**
         * @param res              Resources
         * @param bitmap           占位图
         * @param bitmapWorkerTask 任务
         */
        public AsyncDrawable(Resources res, Bitmap bitmap,
                             BitmapLoaderTask bitmapWorkerTask) {
            super(res, bitmap);
            bitmapWorkerTaskReference =
                    new WeakReference<>(bitmapWorkerTask);
        }

        public final BitmapLoaderTask getBitmapLoaderTask() {
            return (BitmapLoaderTask) bitmapWorkerTaskReference.get();
        }
    }


}