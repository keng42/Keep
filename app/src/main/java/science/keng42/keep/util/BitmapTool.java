package science.keng42.keep.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

/**
 * 加载 Bitmap 的工具类
 * Created by Keng on 2015/5/9
 */
public final class BitmapTool {

    /**
     *  Utility classes should not have a public or default constructor.
     */
    private BitmapTool() {
    }

    /**
     * 根据图片文件路径加载指定宽高的 Bitmap
     *
     * @param path      图片文件路径
     * @param reqWidth  目标宽度
     * @param reqHeight 目标高度
     * @return Bitmap 对象
     */
    public static Bitmap getSampleBitmap(String path, int reqWidth, int reqHeight) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        // 仅获取图片尺寸用于计算缩放比例
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        // 按缩放比例加载图片
        options.inJustDecodeBounds = false;
        // 避免多余的本地变量——会消耗资源
        // Bitmap bitmap = BitmapFactory.decodeFile(path, options);
        // return bitmap;
        return BitmapFactory.decodeFile(path, options);
    }

    /**
     * 根据图片文件读取的字节数组加载指定宽高的 Bitmap
     *
     * @param bytes     图片文件读取的字节数组
     * @param reqWidth  目标宽度
     * @param reqHeight 目标高度
     * @return Bitmap 对象
     */
    public static Bitmap getSampleBitmapFromBytes(byte[] bytes, int reqWidth,
                                                  int reqHeight) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        // 仅获取图片尺寸用于计算缩放比例
        options.inJustDecodeBounds = true;
        if (bytes == null) {
            // 图片不存在
            return null;
        }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        // 按缩放比例加载图片
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
    }

    /**
     * 计算缩放比例
     *
     * @param options   待加载图片的信息
     * @param reqWidth  目标宽度
     * @param reqHeight 目标高度
     * @return 缩放比例
     */
    public static int calculateInSampleSize(BitmapFactory.Options options,
                                            int reqWidth, int reqHeight) {
        // 获取图片的真实宽高
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;
            // 确保缩放后图片的宽高都不小于目标宽高
            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }
}

