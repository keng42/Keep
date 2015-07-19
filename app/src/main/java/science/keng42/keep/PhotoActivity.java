package science.keng42.keep;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;

import java.util.List;

import science.keng42.keep.adapter.PhotoAdapter;
import science.keng42.keep.adapter.PhotoFragment;
import science.keng42.keep.bean.Attachment;
import science.keng42.keep.dao.AttachmentDao;

public class PhotoActivity extends AppCompatActivity {

    private ViewPager mViewPager;
    private PhotoAdapter mPhotoAdapter;
    private List<Attachment> mPhotos;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo);

        initToolbar();
        initData();
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
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor(getResources().getColor(R.color.primary_dark));
        }
    }

    /**
     * 初始化数据
     */
    private void initData() {
        Intent intent = getIntent();
        long entryId = intent.getLongExtra("entryId", 1);

        AttachmentDao ad = new AttachmentDao(this);
        mPhotos = ad.queryAllOfEntry(entryId);

        mPhotoAdapter = new PhotoAdapter(getSupportFragmentManager(), mPhotos);
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mPhotoAdapter);
        mViewPager.setCurrentItem(0);

        updateToolbarTitle();

        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                if (state == 0) {
                    // 更新 ActionBar
                    invalidateOptionsMenu();
                    updateToolbarTitle();
                }
            }
        });
    }

    /**
     * 更新 ActionBar 标题
     */
    void updateToolbarTitle() {
        String mTitle = (mViewPager.getCurrentItem() + 1) + " of " + mPhotos.size();
        android.support.v7.app.ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(mTitle);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_photo, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_delete) {
            showDeletePhotoDialog();
            return true;
        }
        if (id == android.R.id.home) {
            exit();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * 退出当前界面
     */
    private void exit() {
        Intent intent = new Intent();
        setResult(RESULT_OK, intent);
        finish();
    }

    /**
     * 删除该图片
     */
    private void showDeletePhotoDialog() {
        // 确认删除对话框
        AlertDialog deleteConfirm = new AlertDialog.Builder(PhotoActivity.this)
                .setTitle(R.string.delete_photo)
                .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        deleteCurrentPhoto();
                        dialog.dismiss();
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).create();
        deleteConfirm.show();
    }

    /**
     * 删除当前图片
     */
    private void deleteCurrentPhoto() {
        int current = mViewPager.getCurrentItem();
        PhotoFragment pf = getCurrentPhotoFragment();
        pf.delete();
        mPhotos.remove(current);
        if (mPhotos.size() == 0) {
            // no more photo
            exit();
        }

        mPhotoAdapter = new PhotoAdapter(getSupportFragmentManager(), mPhotos);
        mViewPager.setAdapter(mPhotoAdapter);
        mViewPager.setCurrentItem(current);
        updateToolbarTitle();
    }

    /**
     * 获取当前页面 Fragment
     */
    private PhotoFragment getCurrentPhotoFragment() {
        return (PhotoFragment) mPhotoAdapter.instantiateItem(mViewPager,
                mViewPager.getCurrentItem());
    }
}
