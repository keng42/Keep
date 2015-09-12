package science.keng42.keep;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PorterDuff;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.util.LruCache;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import science.keng42.keep.adapter.RVAdapter;
import science.keng42.keep.bean.Attachment;
import science.keng42.keep.bean.Entry;
import science.keng42.keep.bean.Folder;
import science.keng42.keep.bean.Location;
import science.keng42.keep.bean.Tag;
import science.keng42.keep.dao.AttachmentDao;
import science.keng42.keep.dao.EntryDao;
import science.keng42.keep.dao.FolderDao;
import science.keng42.keep.dao.LocationDao;
import science.keng42.keep.dao.TagDao;
import science.keng42.keep.model.EntryCard;
import science.keng42.keep.util.DiskLruCache;
import science.keng42.keep.util.MyDefaultItemAnim;
import science.keng42.keep.util.SecureTool;
import science.keng42.keep.util.SwipeableRVTL;

public class HomeActivity extends AppCompatActivity {

    // 常量
    private static final int REQUEST_ENTRY_DETAIL = 10010;
    private static final int REQUEST_NEW_ENTRY = 10011;
    private static final int REQUEST_RESET_CODE = 10012;
    private static final int EXIT_WAITING_TIME = 2000;
    private static final int NORMAL_BYTES_BUFFER_SIZE = 1024;
    private static final int DISK_CACHE_SIZE = 1024 * 1024 * 10;
    public static int mSpanCount;
    // 全局变量
    private RVAdapter mAdapter;
    private StaggeredGridLayoutManager mManager;
    private MenuItem mItemNoFolder;
    private List<EntryCard> mDataSet;
    private List<String> mCheckedLocation;
    private List<String> mCheckedTag;
    private LruCache<String, Bitmap> mMemoryCache;
    // View
    private RecyclerView mRecyclerView;
    private FloatingActionButton fab;
    private DrawerLayout mDrawerLayout;
    private NavigationView mNavigationView;
    // Disk cache
    private DiskLruCache mDiskLruCache;
    private boolean mDiskCacheStarting = true;
    private final Object mDiskCacheLock = new Object();
    // 设置按两次返回键才退出
    private long mKeyTime = 0;
    private boolean isHome = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        initView();
        initData();
        initToolbar();
        initDrawer();
        bindRecyclerView();
        setListener();
    }

    /**
     * 初始化控件
     */
    private void initView() {
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mNavigationView = (NavigationView) findViewById(R.id.nav_view);
        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        fab = (FloatingActionButton) findViewById(R.id.fab);
    }

    /**
     * 初始化数据，包括磁盘缓存和内存缓存
     */
    private void initData() {
        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / NORMAL_BYTES_BUFFER_SIZE);
        final int cacheSize = maxMemory / 8;

        // 保存内存缓存的引用，在屏幕切换时也可保留内存缓存不需要重新加载
        RetainFragment fragment = RetainFragment.findOrCreateRetainFragment(
                getSupportFragmentManager());
        mMemoryCache = fragment.mRetainedCache;
        if (mMemoryCache == null) {
            mMemoryCache = new LruCache<String, Bitmap>(cacheSize) {
                @Override
                protected int sizeOf(String key, Bitmap value) {
                    return value.getByteCount() / NORMAL_BYTES_BUFFER_SIZE;
                }
            };
            fragment.mRetainedCache = mMemoryCache;
        }

        // init Disk cache
        File cacheDir = new File(getCacheDir(), "thumbnails");
        new InitDiskCacheTask().execute(cacheDir);
    }

    /**
     * 初始化 Toolbar
     */
    private void initToolbar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // 向上按钮
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(R.drawable.ic_menu_light);
        }

        updateTitle(getResources().getString(R.string.entries));
    }

    /**
     * 更新 Toolbar 标题
     */
    private void updateTitle(String title) {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(title);
        }
    }

    /**
     * 初始化抽屉数据
     */
    private void initDrawer() {
        mCheckedLocation = new ArrayList<>();
        mCheckedTag = new ArrayList<>();

        Menu menu = mNavigationView.getMenu();

        // TODO 为了使文件夹列表能够全部取消选中
        mItemNoFolder = menu.add(R.id.group_folder, Menu.NONE, Menu.NONE,
                getResources().getString(R.string.no_folder)).setVisible(false);
        addFolderItem(menu);

        TagDao td = new TagDao(this);
        LocationDao ld = new LocationDao(this);
        List<Tag> tags = td.queryAll();
        List<Location> locations = ld.queryAll();

        for (Tag t : tags) {
            addTagItem(menu, t.getTitle());
        }

        for (Location l : locations) {
            addLocationItem(menu, l.getTitle());
        }

        menu.setGroupCheckable(R.id.group_folder, true, true);
        menu.setGroupCheckable(R.id.group_tag, true, false);
        menu.setGroupCheckable(R.id.group_location, true, false);
    }

    /**
     * 添加抽屉文件夹项
     */
    private void addFolderItem(Menu menu) {
        FolderDao fd = new FolderDao(this);
        List<Folder> folders = fd.queryAll();
        int[] folderColors = {R.color.folder_1, R.color.folder_2,
                R.color.folder_3, R.color.folder_4, R.color.folder_5,
                R.color.folder_6, R.color.folder_7, R.color.folder_8};

        for (int i = 0; i < folders.size(); i++) {
            MenuItem item = menu.add(R.id.group_folder, Menu.NONE, Menu.NONE,
                    folders.get(i).getTitle())
                    .setCheckable(true).setIcon(R.drawable.ic_folder_white_24dp);
            item.setIcon(item.getIcon().mutate());
            item.getIcon().setColorFilter(getResources().getColor(folderColors[i]), PorterDuff.Mode.MULTIPLY);
        }
    }

    /**
     * 添加抽屉标签项
     */
    private void addTagItem(Menu menu, String title) {
        menu.add(R.id.group_tag, Menu.NONE, Menu.NONE, title)
                .setCheckable(true).setIcon(R.drawable.ic_local_offer_white_24dp);
    }

    /**
     * 添加抽屉位置项
     */
    private void addLocationItem(Menu menu, String title) {
        menu.add(R.id.group_location, Menu.NONE, Menu.NONE, title)
                .setCheckable(true).setIcon(R.drawable.ic_place_white_24dp);
    }

    /**
     * 绑定 RecyclerView
     */
    private void bindRecyclerView() {
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setItemAnimator(new MyDefaultItemAnim());

        if (getResources().getConfiguration().orientation ==
                Configuration.ORIENTATION_PORTRAIT) {
            mSpanCount = 2;
        } else {
            mSpanCount = 3;
        }

        mManager = new StaggeredGridLayoutManager(mSpanCount, StaggeredGridLayoutManager.VERTICAL);
        mRecyclerView.setLayoutManager(mManager);

        getAllEntries();
        mAdapter = new RVAdapter(this, mDataSet);
        mRecyclerView.setAdapter(mAdapter);
    }

    /**
     * 获得所有条目
     */
    private void getAllEntries() {
        mDataSet = new ArrayList<>();
        EntryDao ed = new EntryDao(this);
        List<Entry> list = ed.queryAll();
        getCardsByEntries(list);
    }

    /**
     * Entry 转换为 EntryCard 并更新数据集合
     */
    private void getCardsByEntries(List<Entry> list) {
        for (Entry e : list) {
            mDataSet.add(entryToCard(e));
        }
    }

    /**
     * Entry 转换为 EntryCard
     */
    private EntryCard entryToCard(Entry e) {
        int[] folderColors = {R.color.folder_1, R.color.folder_2,
                R.color.folder_3, R.color.folder_4, R.color.folder_5,
                R.color.folder_6, R.color.folder_7, R.color.folder_8};

        int folderId = (int) e.getFolderId();
        int color = getResources().getColor(folderColors[folderId - 1]);

        AttachmentDao ad = new AttachmentDao(this);
        List<Attachment> attachments = ad.queryAllOfEntry(e.getId());
        List<String> names = new ArrayList<>();
        for (Attachment a : attachments) {
            names.add(a.getFilename());
        }
        String[] ss = e.getTags().split(",");
        List<String> labels = new ArrayList<>();
        labels.addAll(Arrays.asList(ss));
        MyApp myApp = (MyApp) getApplication();
        String password = myApp.getPassword();
        return new EntryCard(e.getId(), e.getTitle(),
                SecureTool.decryptStr(this, e.getText(), password),
                color, names, labels);
    }

    /**
     * 设置监听器
     */
    private void setListener() {
        // 卡片左右滑动监听
        SwipeableRVTL swipeTouchListener = new
                SwipeableRVTL(mRecyclerView, new CardSwipeListener());
        mRecyclerView.addOnItemTouchListener(swipeTouchListener);

        // 抽屉项点击事件监听
        mNavigationView.setNavigationItemSelectedListener(new NavigationView.
                OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem menuItem) {
                int groupId = menuItem.getGroupId();
                switch (groupId) {
                    case R.id.group_folder:
                        updateRecyclerViewByFolder(menuItem);
                        break;
                    case R.id.group_location:
                        updateRecyclerViewByPlace(menuItem);
                        break;
                    case R.id.group_tag:
                        updateRecyclerViewByLabel(menuItem);
                        break;
                    case R.id.group:
                        updateRecyclerViewByGroup(menuItem);
                        break;
                    default:
                        break;
                }
                return false;
            }
        });

        // 新建条目
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int[] location = new int[2];
                Intent intent = new Intent(HomeActivity.this, EntryActivity.class);
                view.getLocationOnScreen(location);
                intent.putExtra("top", location[1]);
                intent.putExtra("left", location[0]);
                intent.putExtra("width", 0);
                intent.putExtra("height", 0);
                startActivityForResult(intent, REQUEST_NEW_ENTRY);
                overridePendingTransition(0, 0);
            }
        });

        mAdapter.setOnItemClickListener(new RVAdapter.OnItemClickListener() {
            @Override
            public void onClick(long entryId, int position, View view) {
                startEntryActivity(entryId, position, view);
            }

            @Override
            public void onLongClick(long entryId, int position) {
                showDeleteDialog(entryId, position);
            }
        });
    }

    /**
     * 进入 Entry 编辑界面
     */
    private void startEntryActivity(long entryId, int position, View view) {
        int[] location = new int[2];
        view.getLocationOnScreen(location);
        Intent intent = new Intent(this, EntryActivity.class);
        intent.putExtra("entryId", entryId);
        intent.putExtra("position", position);
        intent.putExtra("top", location[1]);
        intent.putExtra("left", location[0]);
        intent.putExtra("width", view.getWidth());
        intent.putExtra("height", view.getHeight());
        startActivityForResult(intent, REQUEST_ENTRY_DETAIL);
        overridePendingTransition(0, 0);
    }

    /**
     * 根据选中的文件夹来列出相关条目
     * 单选
     */
    private void updateRecyclerViewByFolder(MenuItem menuItem) {
        isHome = false;
        mDataSet.clear();
        // DONE 去掉此行解决占位问题！！
        // mRecyclerView.removeAllViews();
        boolean flag = menuItem.isChecked();
        updateTitle(menuItem.getTitle().toString());

        // 取消选中其他所有的项
        Menu menu = mNavigationView.getMenu();
        for (int i = 3; i < menu.size(); i++) {
            menu.getItem(i).setChecked(false);
        }
        menu.findItem(R.id.item_all).setChecked(true);

        if (flag) {
            backHome();
        } else {
            menuItem.setChecked(true);
            getEntriesByFolder(menuItem.getTitle().toString());
            updateRecyclerView();
            mDrawerLayout.closeDrawers();
        }
    }

    /**
     * 取消分类选择回到主页时调用
     */
    private void backHome() {
        // 取消选中，进入所有条目模式
        isHome = true;
        updateTitle(getResources().getString(R.string.entries));
        mItemNoFolder.setChecked(true);
        EntryDao ed = new EntryDao(getApplicationContext());
        List<Entry> list = ed.queryAll();
        getCardsByEntries(list);
        updateRecyclerView();
        mDrawerLayout.closeDrawers();
    }

    /**
     * 通知 RecyclerView 刷新页面
     */
    private void updateRecyclerView() {
        mAdapter.notifyDataSetChanged();
    }

    /**
     * 根据文件夹标题获取相应的所有条目
     */
    private void getEntriesByFolder(String title) {
        FolderDao folderDao = new FolderDao(this);
        long id = folderDao.getIdByTitle(title);
        EntryDao entryDao = new EntryDao(this);
        List<Entry> list = entryDao.getEntriesByFolder(id);

        getCardsByEntries(list);
    }

    /**
     * 根据选中的位置来列出相关条目
     * 多选、交集
     */
    private void updateRecyclerViewByPlace(MenuItem menuItem) {
        isHome = false;
        mDataSet.clear();
//        mRecyclerView.removeAllViews();
        mCheckedTag.clear();

        mNavigationView.getMenu().findItem(R.id.item_all).setChecked(true);
        mItemNoFolder.setChecked(true);

        if (menuItem.isChecked()) {
            mCheckedLocation.remove(menuItem.getTitle().toString());
        } else {
            mCheckedLocation.add(menuItem.getTitle().toString());
        }

        menuItem.setChecked(!menuItem.isChecked());
        if (mCheckedLocation.isEmpty()) {
            backHome();
            return;
        }

        // 更新 Toolbar 标题
        StringBuilder sb = new StringBuilder();
        for (String s : mCheckedLocation) {
            sb.append(s).append(" ");
        }
        updateTitle(sb.toString());

        getEntriesByPlaces();
        updateRecyclerView();
    }

    /**
     * 根据选中的位置来列出相关条目
     * 多选、并集
     */
    private void updateRecyclerViewByLabel(MenuItem menuItem) {
        isHome = false;
        mCheckedLocation.clear();
        mDataSet.clear();

        mNavigationView.getMenu().findItem(R.id.item_all).setChecked(true);
        mItemNoFolder.setChecked(true);

        if (menuItem.isChecked()) {
            mCheckedTag.remove(menuItem.getTitle().toString());
        } else {
            mCheckedTag.add(menuItem.getTitle().toString());
        }

        if (mCheckedTag.contains("")) {
            mCheckedTag.remove("");
        }

        menuItem.setChecked(!menuItem.isChecked());
        if (mCheckedTag.isEmpty()) {
            backHome();
            return;
        }

        StringBuilder sb = new StringBuilder();
        for (String s : mCheckedTag) {
            sb.append(s).append(" ");
        }
        updateTitle(sb.toString());

        getEntriesByLabels();
        updateRecyclerView();
    }

    /**
     * 根据标签标题获取相应的所有条目
     */
    private void getEntriesByLabels() {
        EntryDao entryDao = new EntryDao(this);
        String[] ss = new String[mCheckedTag.size()];
        for (int i = 0; i < ss.length; i++) {
            ss[i] = mCheckedTag.get(i);
        }
        List<Entry> list = entryDao.getEntriesByTags(ss);

        getCardsByEntries(list);
    }

    /**
     * 普通条目 0
     * 存档条目 1
     */
    private void updateRecyclerViewByGroup(MenuItem menuItem) {
        mDataSet.clear();
        updateTitle(menuItem.getTitle().toString());

        // 取消选中其他所有的项
        Menu menu = mNavigationView.getMenu();
        for (int i = 2; i < menu.size(); i++) {
            menu.getItem(i).setChecked(false);
        }
        mItemNoFolder.setChecked(true);

        mCheckedTag.clear();
        mCheckedLocation.clear();

        // 取消选中，进入所有条目模式
        EntryDao ed = new EntryDao(getApplicationContext());

        List<Entry> list;
        if (menuItem.getItemId() == R.id.item_all) {
            list = ed.queryAll();
            isHome = true;
        } else {
            list = ed.queryArchive();
            isHome = false;
        }

        getCardsByEntries(list);

        menuItem.setChecked(true);
        updateRecyclerView();
        mDrawerLayout.closeDrawers();
    }

    /**
     * 删除 Entry 确认对话框
     */
    private void showDeleteDialog(final long entryId, final int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        String[] items = new String[]{getResources().getString(R.string.delete_entry_waring)};
        builder.setTitle(R.string.delete_entry)
                .setItems(items, null)
                .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // delete
                        deleteEntry(entryId, position);
                    }
                })
                .setNegativeButton(R.string.cancel, null).create().show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_home, menu);
//        initSearchView(menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case android.R.id.home:
                mDrawerLayout.openDrawer(GravityCompat.START);
                break;
            case R.id.action_set_lang:
                setLang();
                break;
            case R.id.action_reset_code:
                showResetCodeDialog();
                break;
            case R.id.action_backup_and_restore:
                goBackupAndRestore();
                break;
            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK) {
            return;
        }
        if (requestCode == REQUEST_ENTRY_DETAIL) {
            // 更新某个条目
            final int index = data.getIntExtra("position", 0);
            long id = data.getLongExtra("entryId", 1);
            int action = data.getIntExtra("action", 0);
            EntryDao ed = new EntryDao(this);
            switch (action) {
                case 0:
                    // update
                    mDataSet.set(index, entryToCard(ed.query(id)));
                    break;
                case 1:
                    // archive
                    mDataSet.remove(index);
                    break;
                case 2:
                    // delete
                    deleteEntry(id, index);
                    break;
                case 3:
                    // unarchive
                    updateRecyclerViewByGroup(mNavigationView.getMenu().findItem(R.id.item_all));
                    break;
                default:
                    break;
            }
            // runOnUiThread 不应该出现在这里
            mAdapter.notifyDataSetChanged();
        } else if (requestCode == REQUEST_NEW_ENTRY) {
            // 新建条目
            long id = data.getLongExtra("entryId", 1);
            EntryDao ed = new EntryDao(this);
            mAdapter.notifyItemInserted(0);
            mDataSet.add(0, entryToCard(ed.query(id)));
            mAdapter.notifyItemRangeChanged(0, mAdapter.getItemCount());
            mRecyclerView.scrollToPosition(0);
        } else if (requestCode == REQUEST_RESET_CODE) {
            finish();
            String oldPassword = data.getStringExtra("oldPassword");
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra("oldPassword", oldPassword);
            intent.putExtra("action", 1);
            startActivity(intent);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mNavigationView.getMenu().clear();
        mNavigationView.inflateMenu(R.menu.drawer);
        initDrawer();
        synchronized (mDiskCacheLock) {
            if (mDiskLruCache != null) {
                try {
                    mDiskLruCache.flush();
                } catch (IOException e) {

                    Log.e(MyApp.TAG, "", e);
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            mDiskLruCache.close();
        } catch (IOException e) {

            Log.e(MyApp.TAG, "", e);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if ((System.currentTimeMillis() - mKeyTime) > EXIT_WAITING_TIME) {
                mKeyTime = System.currentTimeMillis();
                Toast.makeText(this, getString(R.string.press_once_again), Toast.LENGTH_SHORT).show();
            } else {
                finish();
                // TODO how to exit correctly
                System.exit(0);
            }
            return false;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            mSpanCount = 2;
        } else {
            mSpanCount = 3;
        }
        mManager = new StaggeredGridLayoutManager(mSpanCount,
                StaggeredGridLayoutManager.VERTICAL);
        mRecyclerView.setLayoutManager(mManager);
    }

    public void addBitmapToMemCache(String key, Bitmap bitmap) {
        if (getBitmapFromMemCache(key) == null) {
            // TODO 图片文件找不到的情况
            if (bitmap == null) {
                return;
            }
            mMemoryCache.put(key, bitmap);
        }
        addBitmapToDiskCache(key, bitmap);
    }

    public void addBitmapToDiskCache(String key, Bitmap bitmap) {
        // Also add to disk cache
        if (bitmap == null) {
            return;
        }
        synchronized (mDiskCacheLock) {
            try {
                if (mDiskLruCache != null && mDiskLruCache.get(key) == null) {
                    String keyHash = hashKeyForDisk(key);
                    DiskLruCache.Editor editor = mDiskLruCache.edit(keyHash);
                    OutputStream os = editor.newOutputStream(0);
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, os);
//                    os.flush();
                    editor.commit();
                }
            } catch (IOException e) {
                Log.e(MyApp.TAG, "", e);
            }
        }
    }

    public String hashKeyForDisk(String key) {
        String cacheKey;
        try {
            final MessageDigest mDigest = MessageDigest.getInstance("MD5");
            mDigest.update(key.getBytes());
            cacheKey = bytesToHexString(mDigest.digest());
        } catch (NoSuchAlgorithmException e) {
            cacheKey = String.valueOf(key.hashCode());
        }
        return cacheKey;
    }

    private String bytesToHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte aByte : bytes) {
            String hex = Integer.toHexString(0xFF & aByte);
            if (hex.length() == 1) {
                sb.append('0');
            }
            sb.append(hex);
        }
        return sb.toString();
    }

    public Bitmap getBitmapFromMemCache(String key) {
        return mMemoryCache.get(key);
    }

    public Bitmap getBitmapFromDisk(String key) {
        synchronized (mDiskCacheLock) {
            // Wait while disk cache is started from background thread
            while (mDiskCacheStarting) {
                try {
                    mDiskCacheLock.wait();
                } catch (InterruptedException e) {
                    Log.e(MyApp.TAG, "", e);
                }
            }
            if (mDiskLruCache != null) {
                try {
                    String keyHash = hashKeyForDisk(key);
                    DiskLruCache.Snapshot snapShot = mDiskLruCache.get(keyHash);
                    if (snapShot != null) {
                        InputStream is = snapShot.getInputStream(0);
                        return BitmapFactory.decodeStream(is);
                    }
                } catch (IOException e) {
                    Log.e(MyApp.TAG, "", e);
                }
            }
        }
        return null;
    }

    /**
     * 删除 Entry 和相关图片
     */
    private void deleteEntry(long entryId, int position) {
        EntryDao ed = new EntryDao(this);
        AttachmentDao ad = new AttachmentDao(this);
        ed.delete(entryId);
        // delete all attachments
        List<Attachment> attachments = ad.queryAllOfEntry(entryId);
        for (Attachment a : attachments) {
            deletePicture(a.getFilename());
            ad.delete(a.getId());
        }
        mDataSet.remove(position);
        mAdapter.notifyItemRemoved(position);
    }

    /**
     * 删除图片
     */
    private void deletePicture(String filename) {
        File file = new File(getExternalFilesDir(
                Environment.DIRECTORY_PICTURES), filename);
        if (!file.delete()) {
            Log.i(MyApp.TAG, "图片文件删除失败");
        }
    }

    /**
     * 根据位置标题获取相应的所有条目
     */
    private void getEntriesByPlaces() {
        String[] ids = new String[mCheckedLocation.size()];
        LocationDao ld = new LocationDao(this);
        for (int i = 0; i < ids.length; i++) {
            long id = ld.getIdByTitle(mCheckedLocation.get(i));
            ids[i] = "" + id;
        }

        EntryDao entryDao = new EntryDao(this);
        List<Entry> list = entryDao.getEntriesByLocations(ids);

        getCardsByEntries(list);
    }

    /**
     * 切换语言
     */
    private void setLang() {
        String lang = getResources().getString(R.string.action_set_lang);
        Configuration config = getResources().getConfiguration();
        DisplayMetrics dm = getResources().getDisplayMetrics();
        if (lang.equals("中文")) {
            config.locale = Locale.SIMPLIFIED_CHINESE;
        } else {
            config.locale = Locale.ENGLISH;
        }
        getResources().updateConfiguration(config, dm);
        startActivity(new Intent(this, HomeActivity.class));
        finish();
    }

    /**
     * 重置安全码对话框
     */
    private void showResetCodeDialog() {
        View view = getLayoutInflater().inflate(R.layout.reset_code, null);
        final EditText et = (EditText) view.findViewById(R.id.et_rename_label);

        final AlertDialog d = new AlertDialog.Builder(this)
                .setView(view)
                .setTitle(R.string.confirm_password)
                .setPositiveButton(R.string.reset, null)
                .setNegativeButton(R.string.cancel, null)
                .create();

        MyApp myApp = (MyApp) getApplication();
        final String password = myApp.getPassword();

        d.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(final DialogInterface dialog) {
                Button p = d.getButton(AlertDialog.BUTTON_POSITIVE);
                Button n = d.getButton(AlertDialog.BUTTON_NEGATIVE);
                p.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (et.getText().toString().equals(password)) {
                            resetCode();
                            d.dismiss();
                        } else {
                            et.setError(getResources().getString(R.string.wrong_password));
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

    private void goBackupAndRestore() {
        startActivity(new Intent(this, BackupActivity.class));
    }

    /**
     * 重置安全码
     */
    private void resetCode() {
        Intent intent = new Intent(this, PasswordActivity.class);
        intent.putExtra("action", 1);
        startActivityForResult(intent, REQUEST_RESET_CODE);
    }

    /**
     * 卡片滑动监听类
     */
    private class CardSwipeListener implements SwipeableRVTL.SwipeListener {

        private EntryCard card;
        private int pos;
        private View view;

        @Override
        public boolean canSwipe(int position) {
            return isHome;
        }

        @Override
        public void onDismissedBySwipeLeft(RecyclerView rv, int[] rsp) {
            // reverseSortedPositions rsp
            for (int position : rsp) {
                card = mDataSet.get(position);
                pos = position;
                view = mManager.findViewByPosition(pos);
                view.setVisibility(View.INVISIBLE);
                mDataSet.remove(position);
                mAdapter.notifyItemRemoved(position);
                archiveEntry(card.getId());
            }

            Snackbar.make(fab, R.string.archived, Snackbar.LENGTH_SHORT)
                    .setAction(R.string.undo, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            undoArchive();
                        }
                    }).show();
        }

        @Override
        public void onDismissedBySwipeRight(RecyclerView rv, int[] rsp) {
            onDismissedBySwipeLeft(rv, rsp);
        }

        /**
         * 将条目存档
         *
         * @param id 条目 id
         */
        private void archiveEntry(long id) {
            EntryDao ed = new EntryDao(getBaseContext());
            Entry e = ed.query(id);
            e.setArchived(1);
            ed.update(e);
        }

        /**
         * 将条目从存档中复原
         *
         * @param id 条目 id
         */
        private void unArchiveEntry(long id) {
            EntryDao ed = new EntryDao(getBaseContext());
            Entry e = ed.query(id);
            e.setArchived(0);
            ed.update(e);
        }

        /**
         * 撤销存档
         */
        private void undoArchive() {
            mDataSet.add(pos, card);
            mAdapter.notifyItemInserted(pos);
            mAdapter.notifyItemRangeChanged(pos + 1, mAdapter.getItemCount() - 1 - pos);
            unArchiveEntry(card.getId());
            view.setVisibility(View.VISIBLE);
            if (pos == 0) {
                mRecyclerView.scrollToPosition(0);
            }
        }
    }

    /**
     * 解决屏幕切换时需要重新处理图片的问题
     * 新建一个 Fragment 保存缓存
     */
    public static class RetainFragment extends Fragment {
        private static final String TAG = "RetainFragment";
        private LruCache<String, Bitmap> mRetainedCache;

        public static RetainFragment findOrCreateRetainFragment(FragmentManager fm) {
            RetainFragment fragment = (RetainFragment) fm.findFragmentByTag(TAG);
            if (fragment == null) {
                fragment = new RetainFragment();
                fm.beginTransaction().add(fragment, TAG).commit();
            }
            return fragment;
        }

        public RetainFragment() {
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setRetainInstance(true);
        }
    }

    class InitDiskCacheTask extends AsyncTask<File, Void, Void> {
        @Override
        protected Void doInBackground(File... params) {
            synchronized (mDiskCacheLock) {
                File cacheDir = params[0];
                try {
                    mDiskLruCache = DiskLruCache.open(cacheDir, 1, 1, DISK_CACHE_SIZE);
                    mDiskCacheStarting = false;
                    mDiskCacheLock.notifyAll();
                } catch (IOException e) {
                    Log.e(MyApp.TAG, "", e);
                }
            }
            return null;
        }
    }
}
