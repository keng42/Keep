package science.keng42.keep;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import science.keng42.keep.bean.Entry;
import science.keng42.keep.bean.Tag;
import science.keng42.keep.dao.EntryDao;
import science.keng42.keep.dao.TagDao;

public class LabelActivity extends AppCompatActivity {

    private ListView mListView;
    private LabelAdapter mAdapter;
    private List<LabelPack> mDataSet;
    private Entry mEntry;
    private EntryDao mEntryDao;
    private LinearLayout mLlAddLabel;
    private TextView mTvCreate;
    private SearchView mSearchView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_label);

        initData();
        initView();
        initToolbar();
        setListener();
    }

    /**
     * 初始化控件
     */
    private void initView() {
        mListView = (ListView) findViewById(R.id.lv_labels);
        mAdapter = new LabelAdapter(this, mDataSet);
        mListView.setAdapter(mAdapter);
        mListView.setDivider(null);
        mLlAddLabel = (LinearLayout) findViewById(R.id.ll_add_label);
        mTvCreate = (TextView) findViewById(R.id.tv_new_label);

        // 5.0 以后修改系统栏颜色
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor(getResources().getColor(R.color.sysbar_1));
        }
    }

    /**
     * 设置监听器
     */
    private void setListener() {
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                CheckBox cb = (CheckBox) view.findViewById(R.id.cb);
                TextView tv = (TextView) view.findViewById(R.id.tv);
                cb.setChecked(!cb.isChecked());
                updateTagOfEntry(tv.getText().toString());
                mDataSet.get(position).setChecked(cb.isChecked());
            }
        });

        mListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                TextView tv = (TextView) view.findViewById(R.id.tv);
                showChangeLabelDialog(tv.getText().toString());
                return true;
            }
        });

        mLlAddLabel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String title = (String) mTvCreate.getTag();
                createNewTag(title);
            }
        });

        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                updateListView(newText);
                return false;
            }
        });
    }

    private void showChangeLabelDialog(final String label) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setItems(new String[]{getResources().getString(R.string.rename_label),
                        getResources().getString(R.string.delete_label)},
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) {
                            showRenameLabelDialog(label);
                        } else {
                            showDeleteLabelDialog(label);
                        }
                    }
                }).setTitle(label).create().show();
    }

    private void showRenameLabelDialog(final String label) {
        View view = getLayoutInflater().inflate(R.layout.rename_label, null);
        final EditText et = (EditText) view.findViewById(R.id.et_rename_label);
        et.setHint(label);

        final AlertDialog d = new AlertDialog.Builder(this)
                .setView(view)
                .setTitle(R.string.rename_label)
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
                        if (tryRenameLabel(et, label)) {
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
     * 尝试着修改标签名
     */
    private boolean tryRenameLabel(EditText et, String label) {
        String newLabel = et.getText().toString();
        if (newLabel.equals("")) {
            et.setError(getResources().getString(R.string.enter_a_label_name));
        } else if (renameLabel(label, newLabel)) {
            // 修改成功
            return true;
        } else {
            et.setError(getResources().getString(R.string.label_exists));
        }
        return false;
    }

    /**
     * 修改标签名
     */
    private boolean renameLabel(String oldLabel, String newLabel) {
        TagDao td = new TagDao(this);
        long id = td.getIdByTitle(newLabel);
        if (id != 0) {
            return false;
        }
        id = td.getIdByTitle(oldLabel);
        td.update(new Tag(id, newLabel));
        EntryDao ed = new EntryDao(this);
        ed.changeTitle(oldLabel, newLabel);
        mEntry = mEntryDao.query(mEntry.getId());
        updateListView("");
        return true;
    }

    private void showDeleteLabelDialog(final String label) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        String[] item = new String[]{getResources().getString(R.string.delete_label_waring)};
        builder.setTitle(R.string.if_delete_label)
                .setItems(item, null)
                .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //delete
                        deleteLabel(label);
                    }
                })
                .setNegativeButton(R.string.cancel, null).create().show();

    }

    private void deleteLabel(String label) {
        TagDao td = new TagDao(this);
        td.delete(td.getIdByTitle(label));
        mEntryDao.deleteLabel(label);
        updateListView("");
    }

    private void updateListView(String newText) {
        TagDao td = new TagDao(getApplicationContext());
        List<Tag> tags = td.queryAllByKeyword(newText);
        String[] labels = mEntry.getTags().split(",");
        List<String> tagList = Arrays.asList(labels);
        TextView tvCreate = (TextView) findViewById(R.id.tv_new_label);
        LinearLayout llAddLabel = (LinearLayout) findViewById(R.id.ll_add_label);
        boolean flag;
        mDataSet.clear();
        boolean c;
        LabelPack lp;
        for (Tag t : tags) {
            c = tagList.contains(t.getTitle());
            lp = new LabelPack(t.getTitle(), c);
            mDataSet.add(lp);
        }
        long id = td.getIdByTitle(newText);
        flag = id == 0;
        if (newText.equals("")) {
            flag = false;
        }
        if (flag) {
            llAddLabel.setVisibility(View.VISIBLE);
            tvCreate.setText(getResources().getString(R.string.create) + " \"" + newText + "\"");
            tvCreate.setTag(newText);
        } else {
            llAddLabel.setVisibility(View.GONE);
        }
        mAdapter.notifyDataSetChanged();
    }

    /**
     * 新建一个标签
     */
    private void createNewTag(String title) {
        TagDao td = new TagDao(this);
        Tag t = new Tag(title);
        td.insert(t);
        updateTagOfEntry(title);

        mSearchView.setQuery("", true);

        // 把最近添加的移动到第一位
        LabelPack lp = mDataSet.get(mDataSet.size() - 1);
        mDataSet.remove(mDataSet.size() - 1);
        mDataSet.add(0, lp);
        mAdapter.notifyDataSetChanged();
    }

    /**
     * 更新一个标签
     */
    private void updateTagOfEntry(String tagTitle) {
        String[] tagStr = mEntry.getTags().split(",");
        List<String> checkedTags = new ArrayList<>(Arrays.asList(tagStr));

        if (checkedTags.contains(tagTitle)) {
            // 已存在，删除之
            checkedTags.remove(tagTitle);
        } else {
            checkedTags.add(tagTitle);
        }

        if (checkedTags.contains("")) {
            // 已存在，删除之
            checkedTags.remove("");
        }

//        // 添加或移除默认标签
//        TagDao td = new TagDao(this);
//        String defaultTag = td.query(1L).getTitle();
//        if (checkedTags.contains(defaultTag)) {
//            // 已存在，删除之
//            if (checkedTags.size() != 1)
//                checkedTags.remove(defaultTag);
//        } else {
//            if (checkedTags.size() == 0)
//                checkedTags.add(defaultTag);
//        }

        StringBuilder sb = new StringBuilder();
        Collections.sort(checkedTags);
        for (String s : checkedTags) {
            sb.append(s).append(",");
        }

        if (sb.length() > 0) {
            sb.deleteCharAt(sb.length() - 1);
        }
        mEntry.setTags(sb.toString());
        mEntryDao.update(mEntry);
    }

    /**
     * 更新数据集合
     */
    private void updateDataSet(List<Tag> allTags) {
        String[] tagStr = mEntry.getTags().split(",");
        List<String> checkedTags = Arrays.asList(tagStr);

        mDataSet.clear();
        boolean c;
        LabelPack lp;
        for (Tag t : allTags) {
            c = checkedTags.contains(t.getTitle());
            lp = new LabelPack(t.getTitle(), c);
            mDataSet.add(lp);
        }
    }

    /**
     * 初始化数据
     */
    private void initData() {
        Intent intent = getIntent();
        long id = intent.getLongExtra("entryId", 0);

        mEntryDao = new EntryDao(this);
        mEntry = mEntryDao.query(id);
        mDataSet = new ArrayList<>();

        TagDao td = new TagDao(this);
        List<Tag> allTags = td.queryAll();
        updateDataSet(allTags);
    }

    /**
     * 初始化 Toolbar
     */
    private void initToolbar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mSearchView = (SearchView) toolbar.findViewById(R.id.sv);
        mSearchView.setIconifiedByDefault(false);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // 向上按钮
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_label, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private static class LabelPack {
        private String title;
        private boolean checked;

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public boolean isChecked() {
            return checked;
        }

        public void setChecked(boolean checked) {
            this.checked = checked;
        }

        public LabelPack(String title, boolean checked) {
            this.title = title;
            this.checked = checked;
        }
    }

    private static class LabelAdapter extends BaseAdapter {

        private List<LabelPack> mDataSet;
        private Context mContext;

        public LabelAdapter(Context mContext, List<LabelPack> mDataSet) {
            this.mContext = mContext;
            this.mDataSet = mDataSet;
        }

        @Override
        public int getCount() {
            return mDataSet.size();
        }

        @Override
        public Object getItem(int position) {
            return mDataSet.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (convertView == null) {
                view = LayoutInflater.from(mContext).inflate(R.layout.item_label, null);
            }
            TextView tv = (TextView) view.findViewById(R.id.tv);
            CheckBox cb = (CheckBox) view.findViewById(R.id.cb);
            LabelPack pack = mDataSet.get(position);
            tv.setText(pack.getTitle());
            cb.setChecked(pack.isChecked());

            return view;
        }
    }
}
