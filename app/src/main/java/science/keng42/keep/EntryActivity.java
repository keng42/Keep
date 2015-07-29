package science.keng42.keep;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.facebook.android.crypto.keychain.SharedPrefsBackedKeyChain;
import com.facebook.crypto.Crypto;
import com.facebook.crypto.Entity;
import com.facebook.crypto.util.SystemNativeCryptoLibrary;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import science.keng42.keep.bean.Attachment;
import science.keng42.keep.bean.Entry;
import science.keng42.keep.bean.Folder;
import science.keng42.keep.bean.Location;
import science.keng42.keep.dao.AttachmentDao;
import science.keng42.keep.dao.EntryDao;
import science.keng42.keep.dao.FolderDao;
import science.keng42.keep.dao.LocationDao;
import science.keng42.keep.util.BitmapLoaderTask;
import science.keng42.keep.util.DisplayUtil;
import science.keng42.keep.util.SecureTool;

public class EntryActivity extends AppCompatActivity {

    // 常量
    private static final int REQUEST_CREATE_NEW_PLACE = 10013;
    private static final int NORMAL_BYTES_BUFFER_SIZE = 1024;
    private static final int REQUEST_RENAME_PLACE = 10014;
    private static final int REQUEST_SELECT_PHOTO = 100;
    private static final int REQUEST_TAKE_PHOTO = 102;
    private static final int REQUEST_VIEW_PHOTO = 103;
    private static final int MIN_HEIGHT_DIP = 56;
    private static final int ADD_ZERO_FLAG = 10;
    private static final String ENTRY_ID = "entryId";
    private static final String POSITION = "position";
    private static final String ACTION = "action";
    private static final String LOCATION_ID = "locationId";
    private static final String EMPTY_STRING = "";
    private static final String DATE_SEPARATOR = "/";
    private static final String DATE_ZERO = "0";

    // 全局变量
    private String mPassword;
    private String mNewPictureName;
    private Entry mEntry;
    private EntryDao mEntryDao;
    private int position;

    // 缩放动画
    private float mScaleX;
    private float mScaleY;
    private int mLeft;
    private int mTop;

    // View
    private RelativeLayout mRlEntry;
    private ImageView mIvTopLeft;
    private ImageView mIvTopRight;
    private ImageView mIvMiddleLeft;
    private ImageView mIvMiddleMiddle;
    private ImageView mIvMiddleRight;
    private TextView mEtTitle;
    private TextView mEtText;
    private LinearLayout mLlLabel;
    private TextView mTvDate;
    private TextView mTvTime;
    private TextView mTvPlace;
    private int mScreenWidth;
    private Bitmap mPlaceHolderBitmap;
    private int mScreenHeight;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entry);

        initView();
        initData();
        initToolbar();
        setListener();
        enterAnim();
    }

    /**
     * 初始化控件
     */
    private void initView() {
        mRlEntry = (RelativeLayout) findViewById(R.id.rl_entry);
        mIvTopLeft = (ImageView) findViewById(R.id.iv_top_left);
        mIvTopRight = (ImageView) findViewById(R.id.iv_top_right);
        mIvMiddleLeft = (ImageView) findViewById(R.id.iv_middle_left);
        mIvMiddleMiddle = (ImageView) findViewById(R.id.iv_middle_middle);
        mIvMiddleRight = (ImageView) findViewById(R.id.iv_middle_right);
        mEtTitle = (EditText) findViewById(R.id.et_title);
        mEtText = (EditText) findViewById(R.id.et_text);
        mLlLabel = (LinearLayout) findViewById(R.id.ll_label);
        mTvDate = (TextView) findViewById(R.id.tv_date);
        mTvTime = (TextView) findViewById(R.id.tv_time);
        mTvPlace = (TextView) findViewById(R.id.tv_place);
    }

    /**
     * 初始化全局变量
     */
    private void initData() {
        // 获取屏幕宽度
        DisplayMetrics dm = getResources().getDisplayMetrics();
        mScreenWidth = dm.widthPixels;
        if (getResources().getConfiguration().orientation ==
                Configuration.ORIENTATION_PORTRAIT) {
            mScreenHeight = 0;
        } else {
            mScreenHeight = dm.heightPixels - getStatusBarHeight() * 2;
        }

        mEntryDao = new EntryDao(this);
        Intent intent = getIntent();
        long id = intent.getLongExtra(ENTRY_ID, 0);
        position = intent.getIntExtra(POSITION, 0);

        // 获取数据加密密码
        if (position == -1) {
            // from widget
            mPassword = EMPTY_STRING;
        } else {
            MyApp myApp = (MyApp) getApplication();
            mPassword = myApp.getPassword();
        }

        if (id == 0) {
            // new entry
            mEntry = new Entry(System.currentTimeMillis(), EMPTY_STRING,
                    EMPTY_STRING, 1, 1, EMPTY_STRING, 0, 0);
            mEntryDao.insert(mEntry);
            updateDate();
            updateTime();
        } else {
            mEntry = mEntryDao.query(id);
            mEtTitle.setText(mEntry.getTitle());
            String cipherText = mEntry.getText();
            mEtText.setText(SecureTool.decryptStr(this, cipherText, mPassword));
            updatePlace();
            updateDate();
            updateTime();
            updateLabels();
            bindImageView();
            mEtText.clearFocus();
            mEtTitle.clearFocus();
        }
    }

    /**
     * 初始化 Toolbar 和背景颜色
     */
    private void initToolbar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        int folderId = (int) mEntry.getFolderId();
        if (folderId != 0) {
            int[] folderColors = {R.color.folder_1, R.color.folder_2,
                    R.color.folder_3, R.color.folder_4, R.color.folder_5,
                    R.color.folder_6, R.color.folder_7, R.color.folder_8};
            int[] toolbarColors = {R.color.toolbar_1, R.color.toolbar_2,
                    R.color.toolbar_3, R.color.toolbar_4, R.color.toolbar_5,
                    R.color.toolbar_6, R.color.toolbar_7, R.color.toolbar_8,
                    R.color.toolbar_0};
            int[] sysbarColors = {R.color.sysbar_1, R.color.sysbar_2,
                    R.color.sysbar_3, R.color.sysbar_4, R.color.sysbar_5,
                    R.color.sysbar_6, R.color.sysbar_7, R.color.sysbar_8,
                    R.color.sysbar_0};

            // 整体背景颜色
            int folderColor = getResources().getColor(folderColors[folderId - 1]);
            int toolbarColor, sysbarColor;

            AttachmentDao ad = new AttachmentDao(this);
            List<Attachment> attachments = ad.queryAllOfEntry(mEntry.getId());
            if (attachments.size() > 0) {
                // 条目包含图片时 Toolbar 透明
                toolbarColor = getResources().getColor(toolbarColors[8]);
                sysbarColor = getResources().getColor(sysbarColors[8]);
            } else {
                // 条目包含不图片时 Toolbar 和系统栏颜色根据背景颜色设定
                toolbarColor = getResources().getColor(toolbarColors[folderId - 1]);
                sysbarColor = getResources().getColor(sysbarColors[folderId - 1]);
            }

            toolbar.setBackgroundColor(toolbarColor);
            mRlEntry.setBackgroundColor(folderColor);
            // 系统版本大于5.0时才设置系统栏颜色
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Window window = getWindow();
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
                window.setStatusBarColor(sysbarColor);
            }
        }

        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // 向上按钮
            actionBar.setTitle(EMPTY_STRING);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    /**
     * 给控件设置监听器
     */
    private void setListener() {
        // 长按图片删除
        View.OnLongClickListener olcl = new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(final View v) {
                long id = (long) v.getTag();
                final int attachmentId = (int) id;
                // 确认删除对话框
                createDeleteDialog(attachmentId).show();
                return true;
            }
        };

        // 点击图片进入图片查看
        View.OnClickListener ocl = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getBaseContext(), PhotoActivity.class);
                intent.putExtra("entryId", mEntry.getId());
                startActivityForResult(intent, REQUEST_VIEW_PHOTO);
            }
        };

        mIvTopLeft.setOnClickListener(ocl);
        mIvTopRight.setOnClickListener(ocl);
        mIvMiddleLeft.setOnClickListener(ocl);
        mIvMiddleMiddle.setOnClickListener(ocl);
        mIvMiddleRight.setOnClickListener(ocl);

        mIvTopLeft.setOnLongClickListener(olcl);
        mIvTopRight.setOnLongClickListener(olcl);
        mIvMiddleLeft.setOnLongClickListener(olcl);
        mIvMiddleMiddle.setOnLongClickListener(olcl);
        mIvMiddleRight.setOnLongClickListener(olcl);

        mTvDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePickerDialog();
            }
        });

        mTvTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showTimePickerDialog();
            }
        });

        mTvPlace.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPlaceDialog();
            }
        });

        mLlLabel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLabelsDialog();
            }
        });
    }

    /**
     * Activity 进入动画
     * 从卡片放大
     */
    private void enterAnim() {
        // 获取从上一页传来的位置信息(左上角坐标和卡片尺寸)
        Intent intent = getIntent();
        final int top = intent.getIntExtra("top", 0);
        final int left = intent.getIntExtra("left", 0);
        final int width = intent.getIntExtra("width", 0);
        final int height = intent.getIntExtra("height", 0);

        // 整个页面的 view 对象(包括 Toolbar)
        View rootView = findViewById(android.R.id.content);
        // 获取屏幕的尺寸（不包括虚拟按键部分）
        DisplayMetrics dm = getResources().getDisplayMetrics();
        int w = dm.widthPixels;
        int h = dm.heightPixels; // StatusBar+rootView

        // 设置水平和竖直方向的偏移量
        rootView.setPivotX(0);
        rootView.setPivotY(0);

        // 计算并设置缩放比例
        mScaleX = (width * 1.0f) / w;
        mScaleY = (height * 1.0f) / (h - getStatusBarHeight());
        rootView.setScaleX(mScaleX);
        rootView.setScaleY(mScaleY);

        // 计算并设置水平和竖直方向的移动距离——左上角坐标
        mLeft = left;
        mTop = top - getStatusBarHeight();
        rootView.setTranslationX(mLeft);
        rootView.setTranslationY(mTop);

        rootView.animate().scaleX(1).scaleY(1)
                .translationX(0).translationY(0)
                .setDuration(getResources().getInteger(
                        android.R.integer.config_mediumAnimTime))
                .setInterpolator(new DecelerateInterpolator()).start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 从其他 Activity 返回时刷新界面
        updateEntry();
        updateLabels();
        updatePlace();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_entry, menu);
        if (mEntry.getArchived() == 1) {
            menu.findItem(R.id.action_archive).setIcon(
                    R.drawable.ic_material_unarchive_light).setTitle(R.string.unarchive);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case android.R.id.home:
                saveEntry();
                exitActivity();
                break;
            case R.id.action_add_picture:
                showPictureDialog();
                break;
            case R.id.action_change_labels:
                showLabelsDialog();
                break;
            case R.id.action_change_folder:
                showColorDialog();
                break;
            case R.id.action_change_place:
                showPlaceDialog();
                break;
            case R.id.action_archive:
                archiveEntry();
                break;
            case R.id.action_delete:
                deleteEntry();
                break;
            case R.id.action_hide:
                hideText();
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        // 按返回按钮时保存并退出
        saveEntry();
        exitActivity();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK) {
            return;
        }
        switch (requestCode) {
            case REQUEST_SELECT_PHOTO:
                addPictureOfEntry(data.getData());
                break;
            case REQUEST_TAKE_PHOTO:
                addNewPictureOfEntry();
                break;
            case REQUEST_VIEW_PHOTO:
                bindImageView();
                initToolbar();
                break;
            case REQUEST_CREATE_NEW_PLACE:
                mEntry.setLocationId(data.getLongExtra(LOCATION_ID, 0));
                mEntryDao.update(mEntry);
                updatePlace();
                break;
            case REQUEST_RENAME_PLACE:
                updatePlace();
                showPlaceDialog();
            default:
                break;
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    /**
     * 更新位置控件
     */
    private void updatePlace() {
        LocationDao ld = new LocationDao(this);
        String title = ld.query(mEntry.getLocationId()).getTitle();
        mTvPlace.setText(title);
    }

    /**
     * 更新日期控件
     */
    private void updateDate() {
        long mm = mEntry.getDate();
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(mm);

        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH);
        int day = cal.get(Calendar.DAY_OF_MONTH);

        String m = EMPTY_STRING + month, d = EMPTY_STRING + day;
        if (month < ADD_ZERO_FLAG) {
            m = DATE_ZERO + month;
        }
        if (day < ADD_ZERO_FLAG) {
            d = DATE_ZERO + day;
        }

        String date = m + DATE_SEPARATOR + d + DATE_SEPARATOR + year;

        mTvDate.setText(date);
    }

    /**
     * 更新时间空间
     */
    private void updateTime() {
        long mm = mEntry.getDate();
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(mm);
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        int minute = cal.get(Calendar.MINUTE);
        String h = EMPTY_STRING + hour, m = EMPTY_STRING + minute;
        if (hour < ADD_ZERO_FLAG) {
            h = DATE_ZERO + hour;
        }
        if (minute < ADD_ZERO_FLAG) {
            m = DATE_ZERO + minute;
        }
        String time = h + ":" + m;
        mTvTime.setText(time);
    }

    /**
     * 更新标签控件
     */
    private void updateLabels() {
        String[] labels = mEntry.getTags().split(",");
        if (mEntry.getTags().equals(EMPTY_STRING)) {
            mLlLabel.setVisibility(View.GONE);
            return;
        }
        mLlLabel.setVisibility(View.VISIBLE);
        mLlLabel.removeAllViews();
        TextView tv;
        for (String s : labels) {
            tv = new TextView(this);
            tv.setText(s);
            int p = DisplayUtil.dip2px(this, 1);
            tv.setPadding(2 * p, p, 2 * p, p);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(4 * p, 0, 0, 0);
            tv.setBackgroundColor(getResources().getColor(R.color.label_background));
            tv.setTextColor(getResources().getColor(R.color.black));
            mLlLabel.addView(tv, lp);
        }
    }

    /**
     * 绑定图片控件的数据
     * 动态设置尺寸（根据 mCommonHeight）和数量
     */
    private void bindImageView() {
        initToolbar();
        long entryId = mEntry.getId();
        mPlaceHolderBitmap = BitmapFactory.decodeResource(getResources(),
                R.drawable.ic_photo_placeholder_dark);

        // 初始化所有控件
        mIvTopLeft.setImageBitmap(null);
        mIvTopRight.setImageBitmap(null);
        mIvMiddleLeft.setImageBitmap(null);
        mIvMiddleMiddle.setImageBitmap(null);
        mIvMiddleRight.setImageBitmap(null);

        mIvTopLeft.setVisibility(View.VISIBLE);
        mIvTopRight.setVisibility(View.VISIBLE);
        mIvMiddleLeft.setVisibility(View.VISIBLE);
        mIvMiddleMiddle.setVisibility(View.VISIBLE);
        mIvMiddleRight.setVisibility(View.VISIBLE);

        AttachmentDao ad = new AttachmentDao(this);
        List<Attachment> list = ad.queryAllOfEntry(entryId);
        List<String> names = new ArrayList<>();
        List<Long> ids = new ArrayList<>();
        for (Attachment a : list) {
            names.add(a.getFilename());
            ids.add(a.getId());
        }

        ViewGroup.LayoutParams lpTop = mIvTopLeft.getLayoutParams();
        ViewGroup.LayoutParams lpMiddle = mIvMiddleLeft.getLayoutParams();

        // 设置最小高度（Toolbar 高度）
        ActionBar actionBar = getSupportActionBar();
        int toolbarHeight = MIN_HEIGHT_DIP;
        if (actionBar != null) {
            toolbarHeight = actionBar.getHeight();
        }
        lpTop.height = DisplayUtil.dip2px(getApplicationContext(), toolbarHeight);
        lpMiddle.height = 0;

        // 获取屏幕宽度
        switch (names.size()) {
            case 0:
                break;
            case 1:
                loadOneImage(ids, names);
                break;
            case 2:
                loadTwoImages(ids, names);
                break;
            case 3:
                loadThreeImages(ids, names);
                break;
            case 4:
                loadFourImages(ids, names);
                break;
            default:
                loadFiveImages(ids, names);
                break;
        }
    }

    /**
     * 获取系统栏的高度，单位 px
     *
     * @return 系统栏高度
     */
    public int getStatusBarHeight() {
        int result = 0;
        int resourceId = getResources().getIdentifier(
                "status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    /**
     * Activity 退出动画
     * 缩小到卡片，同时变为透明
     *
     * @param runnable 动画结束时运行，用于结束 Activity
     */
    private void exitAnim(Runnable runnable) {
        View rootView = findViewById(android.R.id.content);

        rootView.setPivotX(0);
        rootView.setPivotY(0);
        rootView.setScaleX(1);
        rootView.setPivotY(1);
        rootView.setAlpha(1.0f);
        rootView.setTranslationX(0);
        rootView.setTranslationY(getStatusBarHeight());

        int time = getResources().getInteger(
                android.R.integer.config_mediumAnimTime);

        if (getIntent().getIntExtra("width", 0) == 0) {
            time = 0;
        }

        rootView.animate().scaleX(mScaleX).scaleY(mScaleY)
                .translationX(mLeft).translationY(mTop)
                .alpha(0.0f)
                .withEndAction(runnable) // 在动画执行结束时才运行
                .setDuration(time)
                .setInterpolator(new DecelerateInterpolator()).start();
    }

    /**
     * 读取三张图片
     */
    private void loadThreeImages(List<Long> ids, List<String> names) {
        ViewGroup.LayoutParams lpTop = mIvTopLeft.getLayoutParams();
        ViewGroup.LayoutParams lpMiddle = mIvMiddleLeft.getLayoutParams();
        int w, h;

        w = mScreenWidth / 2;
        if (mScreenHeight == 0) {
            h = w;
        } else {
            h = mScreenHeight / 2;
        }
        lpMiddle.height = h;

        loadBitmap(mIvMiddleLeft, names.get(1), w, h);
        loadBitmap(mIvMiddleMiddle, names.get(2), w, h);

        mIvMiddleRight.setVisibility(View.GONE);
        mIvMiddleLeft.setTag(ids.get(1));
        mIvMiddleMiddle.setTag(ids.get(2));

        w = mScreenWidth;
        lpTop.height = h;
        mIvTopRight.setVisibility(View.GONE);

        loadBitmap(mIvTopLeft, names.get(0), w, h);

        mIvTopLeft.setTag(ids.get(0));
    }

    /**
     * 读取四张图片
     */
    private void loadFourImages(List<Long> ids, List<String> names) {
        ViewGroup.LayoutParams lpTop = mIvTopLeft.getLayoutParams();
        ViewGroup.LayoutParams lpMiddle = mIvMiddleLeft.getLayoutParams();
        int w, h;

        w = mScreenWidth / 2;
        if (mScreenHeight == 0) {
            h = w;
        } else {
            h = mScreenHeight / 2;
        }
        lpMiddle.height = h;

        loadBitmap(mIvMiddleLeft, names.get(2), w, h);
        loadBitmap(mIvMiddleMiddle, names.get(3), w, h);

        mIvMiddleRight.setVisibility(View.GONE);
        mIvMiddleLeft.setTag(ids.get(2));
        mIvMiddleMiddle.setTag(ids.get(3));

        lpTop.height = h;
        loadBitmap(mIvTopLeft, names.get(0), w, h);
        loadBitmap(mIvTopRight, names.get(1), w, h);

        mIvTopLeft.setTag(ids.get(0));
        mIvTopRight.setTag(ids.get(1));
    }

    /**
     * 读取五张图片
     */
    private void loadFiveImages(List<Long> ids, List<String> names) {
        ViewGroup.LayoutParams lpTop = mIvTopLeft.getLayoutParams();
        ViewGroup.LayoutParams lpMiddle = mIvMiddleLeft.getLayoutParams();
        int w, h;

        w = mScreenWidth / 3;
        if (mScreenHeight == 0) {
            h = mScreenWidth / 2;
        } else {
            h = mScreenHeight / 2;
        }
        lpMiddle.height = h;

        loadBitmap(mIvMiddleLeft, names.get(2), w, h);
        loadBitmap(mIvMiddleMiddle, names.get(3), w, h);
        loadBitmap(mIvMiddleRight, names.get(4), w, h);

        mIvMiddleLeft.setTag(ids.get(2));
        mIvMiddleMiddle.setTag(ids.get(3));
        mIvMiddleRight.setTag(ids.get(4));

        w = mScreenWidth / 2;
        lpTop.height = h;

        loadBitmap(mIvTopLeft, names.get(0), w, h);
        loadBitmap(mIvTopRight, names.get(1), w, h);

        mIvTopLeft.setTag(ids.get(0));
        mIvTopRight.setTag(ids.get(1));
    }

    /**
     * 读取两张图片
     */
    private void loadTwoImages(List<Long> ids, List<String> names) {
        ViewGroup.LayoutParams lpTop = mIvTopLeft.getLayoutParams();
        int w, h;
        w = mScreenWidth / 2;
        if (mScreenHeight == 0) {
            h = mScreenWidth;
        } else {
            h = mScreenHeight;
        }
        lpTop.height = h;
        loadBitmap(mIvTopLeft, names.get(0), w, h);
        loadBitmap(mIvTopRight, names.get(1), w, h);

        mIvTopLeft.setTag(ids.get(0));
        mIvTopRight.setTag(ids.get(1));
    }

    /**
     * 读取一张图片
     */
    private void loadOneImage(List<Long> ids, List<String> names) {
        ViewGroup.LayoutParams lpTop = mIvTopLeft.getLayoutParams();
        int w, h;
        w = mScreenWidth;
        if (mScreenHeight == 0) {
            h = w;
        } else {
            h = mScreenHeight;
        }
        lpTop.height = h;
        // 复用时不混乱
        mIvTopLeft.setTag(ids.get(0));
        loadBitmap(mIvTopLeft, names.get(0), w, h);
        mIvTopRight.setVisibility(View.GONE);
    }

    private void loadBitmap(ImageView iv, String name, int w, int h) {
        String path = getExternalFilesDir(Environment.DIRECTORY_PICTURES) + DATE_SEPARATOR + name;
        if (BitmapLoaderTask.cancelPotentialWork(path, iv)) {
            BitmapLoaderTask task = new BitmapLoaderTask(iv, this);
            BitmapLoaderTask.AsyncDrawable asyncDrawable = new BitmapLoaderTask
                    .AsyncDrawable(getResources(), mPlaceHolderBitmap, task);
            iv.setImageDrawable(asyncDrawable);
            task.execute(path, w, h);
        }
    }

    /**
     * 创建删除图片确认对话框
     *
     * @param attachmentId 图片 id
     * @return 确认对话框
     */
    private AlertDialog createDeleteDialog(final int attachmentId) {
        return new AlertDialog.Builder(EntryActivity.this)
                .setTitle(R.string.delete_photo)
                .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        deleteImage(attachmentId);
                        dialog.dismiss();
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).create();
    }

    /**
     * 从数据库和磁盘中删除图片
     *
     * @param attachmentId 图片 id
     */
    private void deleteImage(int attachmentId) {
        AttachmentDao ad = new AttachmentDao(EntryActivity.this);
        Attachment attachment = ad.query(attachmentId);
        ad.delete(attachmentId);
        File file = new File(getExternalFilesDir(
                Environment.DIRECTORY_PICTURES), attachment.getFilename());
        if (!file.delete()) {
            Log.e(MyApp.TAG, "图片文件删除失败");
        }
        bindImageView();
    }

    /**
     * 日期选择对话框
     */
    private void showDatePickerDialog() {
        long time = mEntry.getDate();
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(time);
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH);
        int dayOfMonth = cal.get(Calendar.DAY_OF_MONTH);

        new DatePickerDialog(EntryActivity.this, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                long time = mEntry.getDate();
                Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis(time);
                cal.set(year, monthOfYear, dayOfMonth);
                // 更新数据库
                mEntry.setDate(cal.getTimeInMillis());
                mEntryDao.update(mEntry);
                updateDate();
            }
        }, year, month, dayOfMonth).show();
    }

    /**
     * 时间选择对话框
     */
    private void showTimePickerDialog() {
        long time = mEntry.getDate();
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(time);
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        int minute = cal.get(Calendar.MINUTE);

        new TimePickerDialog(EntryActivity.this, new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                long time = mEntry.getDate();
                Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis(time);
                cal.set(Calendar.HOUR_OF_DAY, hourOfDay);
                cal.set(Calendar.MINUTE, minute);
                // 更新数据库
                mEntry.setDate(cal.getTimeInMillis());
                mEntryDao.update(mEntry);
                updateTime();
            }
        }, hour, minute, true).show();
    }

    /**
     * 修改颜色（文件夹）对话框
     */
    private void showColorDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        View view = LayoutInflater.from(this).inflate(R.layout.color_picker, null);
        Integer[] ids = {R.id.iv_color_1, R.id.iv_color_2,
                R.id.iv_color_3, R.id.iv_color_4, R.id.iv_color_5,
                R.id.iv_color_6, R.id.iv_color_7, R.id.iv_color_8};
        final List<Integer> idList = new ArrayList<>(Arrays.asList(ids));
        int folderId = (int) mEntry.getFolderId();
        ImageView iv = (ImageView) view.findViewById(ids[folderId - 1]);
        iv.setImageResource(R.drawable.ic_color_picker_swatch_selected);

        builder.setTitle(R.string.entry_color);

        final AlertDialog dialog = builder.setView(view).create();

        for (int id : ids) {
            view.findViewById(id).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    changeFolder(idList.indexOf(v.getId()) + 1);
                    dialog.dismiss();
                }
            });
            view.findViewById(id).setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    showRenameFolderDialog(idList.indexOf(v.getId()) + 1);
                    return true;
                }
            });
        }
        dialog.show();
    }

    /**
     * 标签选择和新建的 Activity
     */
    private void showLabelsDialog() {
        Intent intent = new Intent(this, LabelActivity.class);
        intent.putExtra("entryId", mEntry.getId());
        startActivity(intent);
    }

    /**
     * 位置选择、新建、修改、删除的对话框
     * TODO 唯一修改位置的地方？
     */
    private void showPlaceDialog() {
        LocationDao ld = new LocationDao(this);
        List<Location> locations = ld.queryAll();
        final CharSequence[] items = new String[locations.size() + 1];
        items[0] = getResources().getString(R.string.create_a_place);
        for (int i = 1; i < items.length; i++) {
            items[i] = locations.get(i - 1).getTitle();
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.pick_a_place)
                .setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) {
                            // create a new place
                            Intent intent = new Intent(getApplicationContext(), PlaceActivity.class);
                            startActivityForResult(intent, REQUEST_CREATE_NEW_PLACE);
                        } else {
                            changePlace(items[which]);
                        }
                        dialog.dismiss();
                    }
                });
        final AlertDialog ad = builder.create();
        ad.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                ListView lv = ad.getListView();
                lv.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                    @Override
                    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                        if (position <= 1) {
                            return true;
                        }
                        ad.dismiss();
                        showChangePlaceDialog(((TextView) view).getText().toString());
                        return true;
                    }
                });
            }
        });

        ad.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                updatePlace();
            }
        });

        ad.show();
    }

    /**
     * 修改和删除位置对话框
     */
    private void showChangePlaceDialog(final String placeName) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        String[] items = new String[]{getResources().getString(R.string.rename_place),
                getResources().getString(R.string.delete_place)};
        builder.setTitle(placeName)
                .setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) {
                            // rename place
                            renamePlace(placeName);
                        } else {
                            // delete place
                            showConfirmDeletePlaceDialog(placeName);
                        }
                    }
                }).create().show();
    }

    /**
     * 确认删除位置对话框
     */
    private void showConfirmDeletePlaceDialog(final String placeName) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        String[] item = new String[]{getResources().getString(R.string.delete_place_waring)};
        builder.setTitle(R.string.delete_place)
                .setItems(item, null)
                .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        deletePlace(placeName);
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        showPlaceDialog();
                    }
                }).create().show();
    }

    /**
     * 照片选择对话框（选择已有的还是拍照）
     */
    private void showPictureDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setItems(new String[]{getResources().getString(R.string.take_photo),
                        getResources().getString(R.string.choose_image)},
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) {
                            startTakePhotoIntent();
                        } else {
                            startSelectPhotoIntent();
                        }
                    }
                }).setTitle(R.string.action_add_picture).create().show();
    }

    /**
     * 修改该条目的颜色（文件夹）
     * 同时刷新页面
     */
    private void changeFolder(int folderId) {
        mEntry.setFolderId(folderId);
        mEntryDao.update(mEntry);
        initToolbar();
    }

    /**
     * 修改文件夹名对话框
     */
    private void showRenameFolderDialog(final int folderId) {
        FolderDao fd = new FolderDao(this);
        Folder f = fd.query((long) folderId);

        View view = getLayoutInflater().inflate(R.layout.rename_folder, null);
        final EditText et = (EditText) view.findViewById(R.id.et_rename_folder);
        et.setHint(f.getTitle());

        final AlertDialog d = new AlertDialog.Builder(this)
                .setView(view)
                .setTitle(R.string.rename_folder)
                .setPositiveButton(R.string.rename, null)
                .setNegativeButton(R.string.cancel, null)
                .create();

        d.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(final DialogInterface dialog) {
                Button p = d.getButton(AlertDialog.BUTTON_POSITIVE);
                Button n = d.getButton(AlertDialog.BUTTON_NEGATIVE);
                p.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (tryRenameFolder(et, folderId)) {
                            d.dismiss();
                        }
                    }
                });
                n.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.dismiss();
                    }
                });
            }
        });

        d.show();
    }

    /**
     * 尝试着去修改文件夹名
     *
     * @param et       文件夹名输入框
     * @param folderId 文件夹 id
     * @return 操作结果
     */
    private boolean tryRenameFolder(EditText et, int folderId) {
        String newFolder = et.getText().toString();
        if (newFolder.equals(EMPTY_STRING)) {
            et.setError(getResources().getString(R.string.enter_a_folder_name));
        } else if (renameFolder(folderId, newFolder)) {
            // 修改成功
            Toast.makeText(getApplicationContext(), getResources().
                            getString(R.string.folder_name_changed),
                    Toast.LENGTH_SHORT).show();
            return true;
        } else {
            et.setError(getResources().getString(R.string.folder_exists));
        }
        return false;
    }

    /**
     * 修改文件夹名
     */
    private boolean renameFolder(int folderId, String newFolder) {
        FolderDao fd = new FolderDao(this);
        long id = fd.getIdByTitle(newFolder);
        if (id != 0 && id != folderId) {
            return false;
        }
        Folder f = fd.query((long) folderId);
        f.setTitle(newFolder);
        fd.update(f);
        return true;
    }

    /**
     * 删除位置
     */
    private void deletePlace(String placeName) {
        LocationDao ld = new LocationDao(this);
        long id = ld.getIdByTitle(placeName);
        ld.delete(id);
        mEntryDao.deleteLocationOfEntries(id);
        mEntry = mEntryDao.query(mEntry.getId());
        showPlaceDialog();
    }

    /**
     * 修改位置详细信息
     */
    private void renamePlace(String placeName) {
        LocationDao ld = new LocationDao(this);
        long id = ld.getIdByTitle(placeName);
        Intent intent = new Intent(this, PlaceActivity.class);
        intent.putExtra("locationId", id);
        startActivityForResult(intent, REQUEST_RENAME_PLACE);
    }

    /**
     * 修改条目的位置
     */
    private void changePlace(CharSequence placeName) {
        LocationDao ld = new LocationDao(this);
        long id = ld.getIdByTitle(placeName.toString());
        mEntry.setLocationId(id);
        mEntryDao.update(mEntry);
        updatePlace();
    }

    /**
     * 调用选择图片的程序
     */
    private void startSelectPhotoIntent() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT, null);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_SELECT_PHOTO);
    }

    /**
     * 调用拍照程序
     */
    private void startTakePhotoIntent() {
        // 拍照并保存
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // 确保能有相机能拍照
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = createNewPictureFile();
            // 文件创建成功后继续
            if (photoFile != null) {
                mNewPictureName = photoFile.getName();
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
                        Uri.fromFile(photoFile));
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }

    /**
     * 接收返回的图片并加密保存
     * 更新数据库
     */
    private void addPictureOfEntry(Uri uri) {
        String filename = getFilename();
        saveCipherPicture(uri, filename, mPassword);

        AttachmentDao ad = new AttachmentDao(this);
        Attachment a = new Attachment(mEntry.getId(), filename);
        ad.insert(a);

        bindImageView();
    }

    /**
     * 加密图片并保存到到 sdcard/Android/data/...
     */
    private void saveCipherPicture(Uri uri, String filename, String password) {
        String targetPath = getExternalFilesDir(Environment.DIRECTORY_PICTURES) + DATE_SEPARATOR + filename;

        Crypto crypto = new Crypto(
                new SharedPrefsBackedKeyChain(this),
                new SystemNativeCryptoLibrary());
        if (!crypto.isAvailable()) {
            return;
        }

        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            OutputStream fileStream = new FileOutputStream(targetPath);
            OutputStream outputStream = crypto.getCipherOutputStream(fileStream, new Entity(password));
            byte[] buf = new byte[NORMAL_BYTES_BUFFER_SIZE];
            int len;
            while ((len = inputStream.read(buf)) > 0) {
                outputStream.write(buf, 0, len);
            }
            inputStream.close();
            outputStream.close();
        } catch (Exception e) {
            Log.e(MyApp.TAG, "", e);
        }
    }

    /**
     * 根据当前时间生成文件名
     */
    private String getFilename() {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss",
                Locale.CHINA).format(new Date());
        return "JPEG_" + timeStamp + ".jpg";
    }

    /**
     * 为拍照创建一个新文件
     */
    private File createNewPictureFile() {
        String filename = getFilename();
        File dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return new File(dir, filename);
    }

    /**
     * 加密新拍的照片
     */
    private void addNewPictureOfEntry() {
        String path = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                + DATE_SEPARATOR + mNewPictureName;
        SecureTool.encryptFile(this, path, mPassword);
        AttachmentDao ad = new AttachmentDao(this);
        Attachment a = new Attachment(mEntry.getId(), mNewPictureName);
        ad.insert(a);
        bindImageView();
    }

    /**
     * 更新条目
     */
    private void saveEntry() {
        mEntry.setText(SecureTool.encryptStr(this, mEtText.getText().toString(), mPassword));
        mEntry.setTitle(mEtTitle.getText().toString());
        mEntryDao.update(mEntry);
        if (mEtTitle.getText().toString().equals(EMPTY_STRING) &&
                mEntry.getTitle().equals(EMPTY_STRING) &&
                mEntry.getFolderId() == 0 &&
                mEntry.getLocationId() == 0) {
            // empty entry
            mEntryDao.delete(mEntry.getId());
        }
    }

    /**
     * 保存之后退出当前页面
     */
    private void exitActivity() {
        if (position == -1) {
            // from widget
            SecureTool.addIdToEncrypt(this, mEntry.getId() + EMPTY_STRING);
            finish();
        }
        Intent intent = new Intent();
        intent.putExtra(POSITION, position);
        intent.putExtra(ENTRY_ID, mEntry.getId());
        intent.putExtra(ACTION, 0);// normal
        setResult(RESULT_OK, intent);

        exitAnim(new Runnable() {
            @Override
            public void run() {
                finish();
                overridePendingTransition(0, 0);
            }
        });

//        overridePendingTransition(R.anim.nochange, R.anim.zoomout);
    }

    /**
     * 删除条目
     */
    private void deleteEntry() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        String[] items = new String[]{getResources().getString(R.string.delete_entry_waring)};
        builder.setTitle(R.string.delete_entry)
                .setItems(items, null)
                .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //mEntryDao.delete(mEntry.getId());
                        Intent intent = new Intent();
                        intent.putExtra(POSITION, position);
                        intent.putExtra(ENTRY_ID, mEntry.getId());
                        intent.putExtra(ACTION, 2);// delete
                        setResult(RESULT_OK, intent);
                        finish();
                    }
                })
                .setNegativeButton(R.string.cancel, null).create().show();
    }

    /**
     * 将文字颜色设为背景色
     */
    private void hideText() {
        final SeekBar seekBar = (SeekBar) findViewById(R.id.sb_text_alpha);
        RelativeLayout rl = (RelativeLayout) findViewById(R.id.rl_set_alpha);
        rl.setVisibility(View.VISIBLE);
        seekBar.setProgress((int) (mEtText.getAlpha() * 100));
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mEtText.setAlpha(progress / 100F);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        rl.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                v.setVisibility(View.GONE);
                return false;
            }
        });
    }

    /**
     * 存档或恢复条目
     */
    private void archiveEntry() {
        Intent intent = new Intent();
        intent.putExtra(POSITION, position);
        intent.putExtra(ENTRY_ID, mEntry.getId());
        if (mEntry.getArchived() == 0) {
            mEntry.setArchived(1);
            intent.putExtra(ACTION, 1);// archive
        } else {
            mEntry.setArchived(0);
            intent.putExtra(ACTION, 3);// unarchive
        }
        mEntryDao.update(mEntry);
        saveEntry();
        setResult(RESULT_OK, intent);
        finish();
    }

    /**
     * 刷新当前条目的信息
     */
    private void updateEntry() {

        mEntry = mEntryDao.query(mEntry.getId());
    }
}
