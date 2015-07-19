package science.keng42.keep.util;

import android.content.Context;

/**
 * dp 和 px 转换的工具类
 * Created by Keng on 2015/6/2
 */
public final class DisplayUtil {

    public static final double HALF = 0.5D;

    private DisplayUtil() {
    }

    public static int dip2px(Context context, float dipValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dipValue * scale + HALF);
    }

    public static int px2dip(Context context, float pxValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (pxValue / scale + HALF);
    }
}
