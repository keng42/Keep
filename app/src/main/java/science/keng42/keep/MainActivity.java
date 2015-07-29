package science.keng42.keep;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Set;

import science.keng42.keep.bean.Attachment;
import science.keng42.keep.bean.Entry;
import science.keng42.keep.bean.Folder;
import science.keng42.keep.bean.Location;
import science.keng42.keep.dao.AttachmentDao;
import science.keng42.keep.dao.EntryDao;
import science.keng42.keep.dao.FolderDao;
import science.keng42.keep.dao.LocationDao;
import science.keng42.keep.util.SecureTool;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    public static final int MSG_FRESH_PERCENT = 22;
    public static final int NORMAL_BYTES_BUFFER_SIZE = 1024;
    public static final int HUNDRED = 100;
    public static final int MSG_ENCRYPT_NORMAL_DATA = 3;
    public static final int MSG_RE_ENCRYPT_NORMAL_DATA = 4;
    private EditText mEtCode;
    private ImageView mIvDone;
    private ImageView mIvDelete;
    private ProgressDialog mPd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        checkIfFirstLaunch();
    }

    /**
     * 检查是否第一次启动应用
     */
    private void checkIfFirstLaunch() {
        SharedPreferences sp = getSharedPreferences("Secure", Context.MODE_PRIVATE);
        boolean firstLaunch = sp.getBoolean("FirstLaunch", true);
        if (firstLaunch) {
            SharedPreferences.Editor editor = sp.edit();
            editor.putBoolean("FirstLaunch", false);
            editor.apply();
            initDataBase();
        }
    }

    /**
     * 检查安全码是否已设置
     */
    private void checkCode() {
        if (SecureTool.getSecureCode(this) == null) {
            // 安全码未设置，跳转到设置界面
            startActivity(new Intent(this, PasswordActivity.class));
            Toast.makeText(getApplicationContext(),
                    getResources().getString(R.string.code_require),
                    Toast.LENGTH_SHORT).show();
        } else if (validateCode("")) {
            // 如果安全码设置为空，直接进入主界面
            initView();
            encryptEntry();
        } else {
            // 安全码输入界面
            initView();
        }
    }

    /**
     * 初始化 view
     */
    private void initView() {
        mEtCode = (EditText) findViewById(R.id.et_code);
        mIvDone = (ImageView) findViewById(R.id.iv_done);
        mIvDelete = (ImageView) findViewById(R.id.iv_backspace);
        changeButton();

        mIvDone.setOnClickListener(this);
        int[] ids = {R.id.tv_0, R.id.tv_1, R.id.tv_2, R.id.tv_3, R.id.tv_4,
                R.id.tv_5, R.id.tv_6, R.id.tv_7, R.id.tv_8, R.id.tv_9};
        for (int i : ids) {
            findViewById(i).setOnClickListener(this);
        }

        mIvDelete.setOnClickListener(this);

        mIvDelete.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                etDeleteAll();
                return false;
            }
        });

        // 系统版本大于5.0时才设置系统栏颜色
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor(getResources().getColor(R.color.primary_dark));
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();

        switch (id) {
            case R.id.iv_backspace:
                etDeleteOne();
                break;
            case R.id.iv_done:
                validateCode();
                break;
            default:
                addOne(id);
                break;
        }
    }

    /**
     * 验证输入的安全码是否正确
     */
    private void validateCode() {
        if (validateCode(mEtCode.getText().toString())) {
            encryptEntry();
            return;
        }
        etDeleteAll();
    }

    private Handler handler = new ReEncryptHandler(this);

    private void encryptEntry() {
        mPd = new ProgressDialog(this);
        mPd.setTitle(R.string.tv_encrypt);
        mPd.setMessage(getString(R.string.tv_encrypting_clear_data));
        mPd.show();
        new Thread() {
            @Override
            public void run() {
                encryptEntryTrue();
                handler.sendEmptyMessage(1);
            }
        }.start();
    }


    /**
     * 重新加密使用默认密码加密的条目
     */
    private void encryptEntryTrue() {
        // 重新加密所有使用默认密码加密的数据
        MyApp myApp = (MyApp) getApplication();
        String password = myApp.getPassword();
        EntryDao ed = new EntryDao(this);
        AttachmentDao ad = new AttachmentDao(this);
        String path = getExternalFilesDir(Environment.DIRECTORY_PICTURES) + "/";
        Entry entry;
        Set<String> set = SecureTool.getIdsToEncrypt(this);

        if (set != null) {
            int count = set.size();
            int i = 0;
            // re encrypting normal data
            Message msg = new Message();
            handler.sendEmptyMessage(2);

            msg.what = MSG_FRESH_PERCENT;
            msg.obj = i / (count * 1.0f);
            handler.sendMessage(msg);

            for (String s : set) {
                long id = Long.parseLong(s);
                entry = ed.query(id);
                String text = SecureTool.decryptStr(this, entry.getText(), "");
                String cipher = SecureTool.encryptStr(this, text, password);
                entry.setText(cipher);
                ed.update(entry);

                List<Attachment> as = ad.queryAllOfEntry(id);

                for (Attachment a : as) {
                    SecureTool.decryptFile(this, path + a.getFilename(), "");
                    SecureTool.encryptFile(this, path + a.getFilename(), password);
                }
                msg = new Message();
                msg.what = MSG_FRESH_PERCENT;
                msg.obj = (++i / (count * 1.0f)) * HUNDRED;
                handler.sendMessage(msg);
            }
        }
        SecureTool.resetIdsToEncrypt(this);

        // 检查是否需要重新加密所有数据
        int action = getIntent().getIntExtra("action", 0);
        if (action == 1) {
            // re encrypt all data
            reEncryptAllData();
        }
    }

    private void reEncryptAllData() {
        MyApp myApp = (MyApp) getApplication();
        String nowPassword = myApp.getPassword();
        String oldPassword = getIntent().getStringExtra("oldPassword");
        if (nowPassword.equals(oldPassword)) {
            return;
        }
        EntryDao ed = new EntryDao(this);
        AttachmentDao ad = new AttachmentDao(this);
        String path = getExternalFilesDir(Environment.DIRECTORY_PICTURES) + "/";

        List<Entry> entryList = ed.queryAll();
        int count = entryList.size();
        if (count == 0) {
            return;
        }
        int i = 0;
        // re encrypting normal data
        Message msg = new Message();
        handler.sendEmptyMessage(MSG_ENCRYPT_NORMAL_DATA);

        msg.what = MSG_FRESH_PERCENT;
        msg.obj = i / (count * 1.0f);
        handler.sendMessage(msg);

        for (Entry entry : entryList) {
            String text = SecureTool.decryptStr(this, entry.getText(), oldPassword);
            String cipher = SecureTool.encryptStr(this, text, nowPassword);
            entry.setText(cipher);
            ed.update(entry);

            msg = new Message();
            msg.what = MSG_FRESH_PERCENT;
            msg.obj = (++i / (count * 1.0f)) * HUNDRED;
            handler.sendMessage(msg);
        }

        List<Attachment> attachmentList = ad.queryAll();

        count = entryList.size();
        if (count == 0) {
            return;
        }
        i = 0;
        // re encrypting normal data
        msg = new Message();
        handler.sendEmptyMessage(MSG_RE_ENCRYPT_NORMAL_DATA);

        msg.what = MSG_FRESH_PERCENT;
        msg.obj = (++i / (count * 1.0f)) * HUNDRED;
        handler.sendMessage(msg);

        for (Attachment a : attachmentList) {
            SecureTool.decryptFile(this, path + a.getFilename(), oldPassword);
            SecureTool.encryptFile(this, path + a.getFilename(), nowPassword);

            msg = new Message();
            msg.what = MSG_FRESH_PERCENT;
            msg.obj = (++i / (count * 1.0f)) * HUNDRED;
            handler.sendMessage(msg);
        }
    }

    private void copyAssets() {
        AssetManager assetManager = getAssets();
        String[] files = null;
        try {
            files = assetManager.list("");
        } catch (IOException e) {
            Log.e(MyApp.TAG, "获取资源文件列表失败", e);
        }
        if (files == null) {
            return;
        }
        for (String filename : files) {
            InputStream in = null;
            OutputStream out = null;
            try {
                in = assetManager.open(filename);
                File outFile = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), filename);
                out = new FileOutputStream(outFile);
                copyFile(in, out);
            } catch (IOException e) {
                Log.e(MyApp.TAG, "复制资源文件失败：" + filename, e);
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {
                        Log.e(MyApp.TAG, "", e);
                    }
                }
                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException e) {
                        Log.e(MyApp.TAG, "", e);
                    }
                }
            }
        }
    }

    private void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[NORMAL_BYTES_BUFFER_SIZE];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
    }

    /**
     * 验证安全码是否正确
     */
    private boolean validateCode(String code) {
        boolean result = SecureTool.validateSecureCode(this, code);
        if (result) {
            MyApp myApp = (MyApp) getApplication();
            myApp.setPassword(SecureTool.getPassword(this, code));
        }
        return result;
    }

    /**
     * 数字按钮点击
     */
    private void addOne(int id) {
        int[] ids = {R.id.tv_0, R.id.tv_1, R.id.tv_2, R.id.tv_3, R.id.tv_4,
                R.id.tv_5, R.id.tv_6, R.id.tv_7, R.id.tv_8, R.id.tv_9};
        for (int i = 0; i < ids.length; i++) {
            if (id == ids[i]) {
                etAddOne(i);
                return;
            }
        }
    }

    /**
     * 安全码输入框相关操作
     */
    private void etAddOne(int i) {
        mEtCode.append("" + i);
        changeButton();
    }

    private void etDeleteOne() {
        String s = mEtCode.getText().toString();
        if (!s.equals("")) {
            mEtCode.setText(s.substring(0, s.length() - 1));
        }
        changeButton();
    }

    private void etDeleteAll() {
        mEtCode.setText("");
        changeButton();
    }

    /**
     * 隐藏或显示按钮
     */
    private void changeButton() {
        if (mEtCode.getText().toString().equals("")) {
            mIvDone.setVisibility(View.GONE);
            mIvDelete.setImageResource(R.drawable.ic_backspace_white_disabled_24dp);
            mIvDelete.setClickable(false);
        } else {
            mIvDone.setVisibility(View.VISIBLE);
            mIvDelete.setImageResource(R.drawable.ic_backspace_white_24dp);
            mIvDelete.setClickable(true);
        }
    }

    /**
     * 第一次启动程序时初始化数据库
     * 添加默认数据
     */
    private void initDataBase() {
        deleteDatabase("jki.db");

        File photos = new File(getFilesDir(), "photos");
        if (!photos.exists() && !photos.mkdir()) {
            Log.e(MyApp.TAG, "图片文件夹创建失败");
        }

        FolderDao folderDao = new FolderDao(this);
        LocationDao locationDao = new LocationDao(this);

        int[] folderColorNameIds = {R.string.folder_color_name_1,
                R.string.folder_color_name_2, R.string.folder_color_name_3,
                R.string.folder_color_name_4, R.string.folder_color_name_5,
                R.string.folder_color_name_6, R.string.folder_color_name_7,
                R.string.folder_color_name_8};
        // 8 个文件夹，只有名字可以自定义，其他的不可修改
        // 默认文件夹 id == 1
        int entryCount = 8;
        for (int i = 0; i < entryCount; i++) {
            Folder folder = new Folder(getResources().getString(folderColorNameIds[i]), "");
            folderDao.insert(folder);
        }

        // 默认位置，不可删除或修改，在且仅在没有其他标签的情况下才能包含
        Location location = new Location(getResources().getString(
                R.string.no_place), "", "", "", "");
        locationDao.insert(location);

        // example entries
        String password = "";
        EntryDao ed = new EntryDao(this);
        Entry entry;
        String[] titles = new String[]{
                "1984",
                "我是标题",
                "",
                "标题什么的随便写写就好",
                "呵呵",
                "",
                "",
                ""
        };
        String[] texts = new String[]{
                "斯奎拉，人类英雄！！",
                "Don't panic!",
                "不要回答！不要回答！！不要回答！！！\n\n--因为很重要，所以要说三遍。因为很重要，所以要说三遍。因为很重要，所以要说三遍。",
                "中午的饭真恶心。。。",
                "I am your father.",
                "没饭吃，你会去吃屎吗？！\n\n\n\n\n\n呵呵",
                "新四大文明古国：古巴、伊朗、朝鮮和中國。妥妥的。",
                "那个人好像一条狗啊~"
        };

        for (int i = 0; i < entryCount; i++) {
            entry = new Entry(System.currentTimeMillis(), titles[i],
                    SecureTool.encryptStr(this, texts[i], password), i + 1, 1, "", 0, 1);
            ed.insert(entry);
        }

        // pictures
        copyAssets();
        String path = getExternalFilesDir(Environment.DIRECTORY_PICTURES) + "/";
        String[] filenames = new String[]{"xxy1.jpg", "xxy2.jpg", "xxy3.jpg", "xxy4.jpg", "xxy5.jpg"};
        for (String fn : filenames) {
            SecureTool.encryptFile(this, path + fn, password);
        }

        Attachment attachment;
        AttachmentDao ad = new AttachmentDao(this);
        attachment = new Attachment(entryCount, "xxy1.jpg");
        ad.insert(attachment);
        attachment = new Attachment(entryCount, "xxy2.jpg");
        ad.insert(attachment);
        attachment = new Attachment(entryCount, "xxy3.jpg");
        ad.insert(attachment);
        attachment = new Attachment(entryCount, "xxy4.jpg");
        ad.insert(attachment);
        attachment = new Attachment(entryCount, "xxy5.jpg");
        ad.insert(attachment);

        for (int i = 1; i <= entryCount; i++) {
            SecureTool.addIdToEncrypt(this, i + "");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkCode();
    }

    private static class ReEncryptHandler extends Handler {

        private WeakReference<MainActivity> mActivity;

        public ReEncryptHandler(MainActivity context) {
            mActivity = new WeakReference<>(context);
        }

        @Override
        public void handleMessage(Message msg) {
            MainActivity activity = mActivity.get();
            switch (msg.what) {
                case 1:
                    // 数据加密完成，进入主页面
                    activity.mPd.cancel();
                    activity.finish();
                    Intent intent = new Intent(activity, HomeActivity.class);
                    activity.startActivity(intent);
                    break;
                case 2:
                    activity.mPd.setMessage(activity.getString(R.string.tv_encrypting_clear_data));
                    break;
                case MSG_ENCRYPT_NORMAL_DATA:
                    activity.mPd.setMessage(activity.getString(R.string.tv_encrypt_ciphertext));
                    break;
                case MSG_RE_ENCRYPT_NORMAL_DATA:
                    activity.mPd.setMessage(activity.getString(R.string.tv_encrypting_photos));
                    break;
                case MSG_FRESH_PERCENT:
                    break;
                default:
                    break;
            }
        }
    }
}
