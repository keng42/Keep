package science.keng42.keep;

import android.app.Application;

/**
 * Created by Keng on 2015/6/11
 */
public class MyApp extends Application {

    /**
     * 全局常量
     */
    public static final int NORMAL_BYTES_BUFFER_SIZE = 1024;
    public static final String TAG = "Keep";

    private String password;

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
