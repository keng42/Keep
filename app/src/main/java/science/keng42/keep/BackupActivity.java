package science.keng42.keep;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Xml;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import org.xmlpull.v1.XmlSerializer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import science.keng42.keep.adapter.SDCardFragmentPagerAdapter;
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
import science.keng42.keep.fragment.SDCardFragment;
import science.keng42.keep.util.Compress;

public class BackupActivity extends AppCompatActivity {

    private SDCardFragmentPagerAdapter mPagerAdapter;
    private ProgressDialog mPd;

    private Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    mPd.dismiss();
                    showToast(R.string.backup_finished);
                    refreshView(0);
                    break;
                case 2:
                    mPd.dismiss();
                    showToast(R.string.create_app_dir_failed);
                    break;
            }
            return false;
        }
    });

    public void refreshView(int page) {
        ((SDCardFragment) mPagerAdapter.getItem(page)).refreshView();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_backup);

        mPagerAdapter = new SDCardFragmentPagerAdapter(getSupportFragmentManager());
        ViewPager viewPager = (ViewPager) findViewById(R.id.view_pager);
        TabLayout tabLayout = (TabLayout) findViewById(R.id.tab_layout);

        viewPager.setAdapter(mPagerAdapter);
        // TabLayout 和 ViewPager 绑定
        tabLayout.setupWithViewPager(viewPager);
        tabLayout.setTabMode(TabLayout.MODE_FIXED);

        initToolbar();
    }

    /**
     * 初始化工具栏
     */
    private void initToolbar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // 向上按钮
            actionBar.setTitle(getString(R.string.action_backup_and_restore));
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        // 系统版本大于5.0时才设置系统栏颜色
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor(getResources().getColor(R.color.primary_dark));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_backup, menu);
        // 新建备份按钮设置为白色
        Drawable drawable = menu.findItem(R.id.action_backup).getIcon();
        if (drawable != null) {
            drawable.mutate();
            drawable.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_backup) {
            showBackupDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * 备份确认对话框
     */
    private void showBackupDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.action_backup)
                .setMessage("Backup?")
                .setPositiveButton(R.string.action_backup, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        createNewBackup();
                    }
                })
                .setNegativeButton(R.string.cancel, null);
        builder.create().show();
    }

    /**
     * 显示备份进度条并开新线程进行备份
     */
    private void createNewBackup() {
        mPd = new ProgressDialog(this);
        mPd.setTitle(getResources().getString(R.string.backuping));
        mPd.setMessage(getString(R.string.patience));
        mPd.show();

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    createBackup();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    handler.sendEmptyMessage(1);
                }
            }
        }).start();
    }

    /**
     * 备份数据库到文件中
     */
    private void createBackup() throws IOException {
        File appDir = new File(Environment.getExternalStorageDirectory(), "Keep");
        if (!appDir.exists()) {
            if (!appDir.mkdir()) {
                handler.sendEmptyMessage(2);
                return;
            }
        }
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.CHINA).format(new Date());
        File xmlFile = new File(appDir, "database.xml");

        List<Attachment> attachments = createXmlFile(xmlFile);
        if (attachments == null) {
            handler.sendEmptyMessage(2);
            return;
        }
        // zip all files
        String path = getExternalFilesDir(Environment.DIRECTORY_PICTURES) + File.separator;
        String[] _files = new String[attachments.size() + 1];
        _files[0] = xmlFile.getAbsolutePath();
        int index = 1;
        for (Attachment a : attachments) {
            _files[index++] = path + a.getFilename();
        }
        String _zipFile = appDir.getAbsolutePath() + "/Keep_" + timeStamp + ".zip";
        Compress compress = new Compress(_files, _zipFile);
        compress.zip();
        if (!xmlFile.delete()) {
            showToast(R.string.delete_file_failed);
        }
    }

    /**
     * 将数据库中的数据写入 xml 文件中
     */
    private List<Attachment> createXmlFile(File xmlFile) throws IOException {
        FolderDao fd = new FolderDao(this);
        TagDao td = new TagDao(this);
        LocationDao ld = new LocationDao(this);
        EntryDao ed = new EntryDao(this);
        AttachmentDao ad = new AttachmentDao(this);

        List<Folder> folders = fd.queryAll();
        List<Tag> tags = td.queryAll();
        List<Location> locations = ld.queryAll();
        List<Entry> entries = ed.queryAll();
        List<Attachment> attachments = ad.queryAll();

        // write to file
        XmlSerializer serializer = Xml.newSerializer();
        FileOutputStream fos = new FileOutputStream(xmlFile);

        serializer.setOutput(fos, "UTF-8");
        serializer.startDocument("UTF-8", true);
        serializer.startTag(null, "data");
        serializer.attribute(null, "version", "1");

        // folders
        serializer.startTag(null, "table");
        serializer.attribute(null, "name", "folders");
        for (Folder f : folders) {
            serializer.startTag(null, "r");
            serializer.startTag(null, "id");
            serializer.text("" + f.getId());
            serializer.endTag(null, "id");
            serializer.startTag(null, "title");
            serializer.text(f.getTitle());
            serializer.endTag(null, "title");
            serializer.startTag(null, "color");
            serializer.text(f.getColor());
            serializer.endTag(null, "color");
            serializer.endTag(null, "r");
        }
        serializer.endTag(null, "table");

        // tags
        serializer.startTag(null, "table");
        serializer.attribute(null, "name", "tags");
        for (Tag t : tags) {
            serializer.startTag(null, "r");
            serializer.startTag(null, "id");
            serializer.text("" + t.getId());
            serializer.endTag(null, "id");
            serializer.startTag(null, "title");
            serializer.text(t.getTitle());
            serializer.endTag(null, "title");
            serializer.endTag(null, "r");
        }
        serializer.endTag(null, "table");

        // locations
        serializer.startTag(null, "table");
        serializer.attribute(null, "name", "locations");
        for (Location l : locations) {
            serializer.startTag(null, "r");
            serializer.startTag(null, "id");
            serializer.text("" + l.getId());
            serializer.endTag(null, "id");
            serializer.startTag(null, "title");
            serializer.text(l.getTitle());
            serializer.endTag(null, "title");
            serializer.startTag(null, "address");
            serializer.text(l.getAddress());
            serializer.endTag(null, "address");
            serializer.startTag(null, "lat");
            serializer.text(l.getLat());
            serializer.endTag(null, "lat");
            serializer.startTag(null, "lon");
            serializer.text(l.getLon());
            serializer.endTag(null, "lon");
            serializer.startTag(null, "description");
            serializer.text(l.getDescription());
            serializer.endTag(null, "description");
            serializer.endTag(null, "r");
        }
        serializer.endTag(null, "table");

        // entries
        serializer.startTag(null, "table");
        serializer.attribute(null, "name", "entries");
        for (Entry e : entries) {
            serializer.startTag(null, "r");
            serializer.startTag(null, "id");
            serializer.text("" + e.getId());
            serializer.endTag(null, "id");
            serializer.startTag(null, "date");
            serializer.text("" + e.getDate());
            serializer.endTag(null, "date");
            serializer.startTag(null, "title");
            serializer.text(e.getTitle());
            serializer.endTag(null, "title");
            serializer.startTag(null, "text");
            serializer.text(e.getText());
            serializer.endTag(null, "text");
            serializer.startTag(null, "folder_id");
            serializer.text("" + e.getFolderId());
            serializer.endTag(null, "folder_id");
            serializer.startTag(null, "location_id");
            serializer.text("" + e.getLocationId());
            serializer.endTag(null, "location_id");
            serializer.startTag(null, "tags");
            serializer.text(e.getTags());
            serializer.endTag(null, "tags");
            serializer.startTag(null, "archived");
            serializer.text("" + e.getArchived());
            serializer.endTag(null, "archived");
            serializer.startTag(null, "encrypted");
            serializer.text("" + e.getEncrypted());
            serializer.endTag(null, "encrypted");
            serializer.endTag(null, "r");
        }
        serializer.endTag(null, "table");

        // attachments
        serializer.startTag(null, "table");
        serializer.attribute(null, "name", "attachments");
        for (Attachment a : attachments) {
            serializer.startTag(null, "r");
            serializer.startTag(null, "id");
            serializer.text("" + a.getId());
            serializer.endTag(null, "id");
            serializer.startTag(null, "entry_id");
            serializer.text("" + a.getEntryId());
            serializer.endTag(null, "entry_id");
            serializer.startTag(null, "filename");
            serializer.text(a.getFilename());
            serializer.endTag(null, "filename");
            serializer.endTag(null, "r");
        }
        serializer.endTag(null, "table");
        serializer.endTag(null, "data");
        serializer.endDocument();
        serializer.flush();
        fos.close();

        return attachments;
    }

    private void showToast(int id) {
        Toast.makeText(this, getString(id), Toast.LENGTH_SHORT).show();
    }
}
