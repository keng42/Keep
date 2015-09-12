package science.keng42.keep.fragment;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Xml;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.Toast;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import science.keng42.keep.R;
import science.keng42.keep.adapter.SDCardRVAdapter;
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
import science.keng42.keep.util.Decompress;

public class SDCardFragment extends Fragment
        implements SDCardRVAdapter.SDCardRVAdapterListener {

    public static final String ARG_PAGE = "ARG_PAGE";
    private int mPage;
    private SDCardRVAdapter mAdapter;
    private List<String> mFileNames;
    private List<String> mInfos;
    private RecyclerView mRecyclerView;
    private ProgressDialog mPd;

    private Handler handle = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    mPd.dismiss();
                    makeToast(getString(R.string.restore_finished));
                    break;
                case 2:
                    mPd.dismiss();
                    makeToast(getString(R.string.restore_failed));
                    break;
            }
            return false;
        }
    });

    private void makeToast(String str) {
        Toast.makeText(getActivity(), str, Toast.LENGTH_SHORT).show();
    }

    public static SDCardFragment newInstance(int page) {
        SDCardFragment fragment = new SDCardFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_PAGE, page);
        fragment.setArguments(args);
        return fragment;
    }

    public SDCardFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mPage = getArguments().getInt(ARG_PAGE);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mFileNames = new ArrayList<>();
        mInfos = new ArrayList<>();
        freshData();
        View view = inflater.inflate(R.layout.fragment_sdcard, container, false);
        mRecyclerView = (RecyclerView) view.findViewById(R.id.rv);
        mAdapter = new SDCardRVAdapter(mFileNames, mInfos);
//        mListener = new SDCardRVAdapterListenerImpl();
        mAdapter.setListener(this);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mRecyclerView.setAdapter(mAdapter);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshView();
    }

    @Override
    public void onItemClick(final String filename) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.restore)
                .setMessage(R.string.restore_warning)
                .setPositiveButton(R.string.restore, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        restoreData(filename);
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .create().show();
    }

    @Override
    public void onMoreClick(final String filename, View view) {
        PopupMenu popupMenu = new PopupMenu(getActivity(), view);
        popupMenu.inflate(R.menu.menu_backup_more);
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.action_upload:
                        Toast.makeText(getActivity(), getString(R.string.not_available), Toast.LENGTH_SHORT).show();
                        break;
                    case R.id.action_delete:
                        showDeleteDialog(filename);
                        break;
                }
                return false;
            }
        });
        popupMenu.show();
    }

    /**
     * 从 SD card 中读取数据
     */
    private void freshData() {
        File appDir = new File(Environment.getExternalStorageDirectory(), "Keep");
        if (!appDir.exists() || !appDir.isDirectory()) {
            return;
        }
        String[] fileNames = appDir.list();
        mFileNames = Arrays.asList(fileNames);
        mInfos.clear();
        for (File f : appDir.listFiles()) {
            long time = f.lastModified();
            long length = f.length();
            mInfos.add(DateFormat.getDateTimeInstance()
                    .format(new Date(time)) + " | " + formatSize(length));
        }
    }

    /**
     * 返回 byte 的数据大小对应的文本
     */
    private String formatSize(long size) {
        int k = 1024;
        float kf = 1024f;
        DecimalFormat format = new DecimalFormat("####.00");
        if (size < k) {
            return size + " bytes";
        } else if (size < k * k) {
            return format.format(size / kf) + " KB";
        } else if (size / k < k * k) {
            return format.format(size / kf / kf) + " MB";
        } else if (size / k < k * k * k) {
            return format.format(size / kf / kf / kf) + " GB";
        } else {
            return "size: error";
        }
    }

    /**
     * 根据新数据刷新页面
     */
    private void refreshView() {
        freshData();
        mAdapter = new SDCardRVAdapter(mFileNames, mInfos);
        mAdapter.setListener(this);
        mRecyclerView.setAdapter(mAdapter);
    }

    /**
     * 从备份文件中重置数据库
     */
    private void restoreData(final String filename) {
        mPd = new ProgressDialog(getActivity());
        mPd.setTitle(R.string.restore);
        mPd.setMessage(getString(R.string.patience));
        mPd.show();

        new Thread(new Runnable() {
            @Override
            public void run() {
                decompressFile(filename);
                restoreDatabase();
                handle.sendEmptyMessage(1);
            }
        }).start();
    }

    /**
     * 重置数据库
     */
    private void restoreDatabase() {
        DB db = null;
        try {
            db = decodeXmlFile();
        } catch (IOException | XmlPullParserException e) {
            e.printStackTrace();
        }
        if (db == null) {
            return;
        }

        FolderDao fd = new FolderDao(getActivity());
        TagDao td = new TagDao(getActivity());
        LocationDao ld = new LocationDao(getActivity());
        EntryDao ed = new EntryDao(getActivity());
        AttachmentDao ad = new AttachmentDao(getActivity());

        // restore database
        getActivity().deleteDatabase("jki.db");
        for (Folder f : db.folders) {
            fd.restore(f);
        }
        for (Tag t : db.tags) {
            td.restore(t);
        }
        for (Location l : db.locations) {
            ld.restore(l);
        }
        for (Entry e : db.entries) {
            ed.restore(e);
        }
        for (Attachment a : db.attachments) {
            ad.restore(a);
        }
    }

    /**
     * 解析 xml 文件到简单数据库对象
     */
    private DB decodeXmlFile() throws IOException, XmlPullParserException {
        File xmlFile = new File(getActivity().getExternalFilesDir(
                Environment.DIRECTORY_PICTURES), "database.xml");
        FileInputStream fis = new FileInputStream(xmlFile);
        XmlPullParser parser = Xml.newPullParser();

        parser.setInput(fis, "utf-8");

        List<Folder> folders = new ArrayList<>();
        List<Tag> tags = new ArrayList<>();
        List<Location> locations = new ArrayList<>();
        List<Entry> entries = new ArrayList<>();
        List<Attachment> attachments = new ArrayList<>();

        Folder folder;
        Tag tag;
        Location location;
        Entry entry;
        Attachment attachment;

        String[] tmp = {"id", "title", "color", "date", "text", "folder_id",
                "location_id", "tags", "archived", "encrypted", "entry_id",
                "filename", "address", "lat", "lon", "description"};
        List<String> flag = Arrays.asList(tmp);
        String[] data = new String[flag.size()];

        int type = parser.getEventType();
        String attr = "";
        while (type != XmlPullParser.END_DOCUMENT) {
            if (type == XmlPullParser.START_TAG) {
                switch (parser.getName()) {
                    case "data":
                    case "r":
                        break;
                    case "table":
                        // 判断当前属于哪个类型的数据
                        attr = parser.getAttributeValue(0);
                        break;
                    default:
                        data[flag.indexOf(parser.getName())] = parser.nextText();
                        break;
                }
            } else if (type == XmlPullParser.END_TAG) {
                if (!"r".equals(parser.getName())) {
                    type = parser.next();
                    continue;
                }
                switch (attr) {
                    case "folders":
                        folder = new Folder(Long.parseLong(data[flag.indexOf("id")]),
                                data[flag.indexOf("title")], data[flag.indexOf("color")]);
                        folders.add(folder);
                        break;
                    case "tags":
                        tag = new Tag(Long.parseLong(data[flag.indexOf("id")]),
                                data[flag.indexOf("title")]);
                        tags.add(tag);
                        break;
                    case "locations":
                        location = new Location(Long.parseLong(data[flag.indexOf("id")]),
                                data[flag.indexOf("title")], data[flag.indexOf("address")],
                                data[flag.indexOf("description")], data[flag.indexOf("lat")],
                                data[flag.indexOf("lon")]);
                        locations.add(location);
                        break;
                    case "entries":
                        entry = new Entry(Long.parseLong(data[flag.indexOf("id")]),
                                Long.parseLong(data[flag.indexOf("date")]),
                                data[flag.indexOf("title")], data[flag.indexOf("text")],
                                Long.parseLong(data[flag.indexOf("folder_id")]),
                                Long.parseLong(data[flag.indexOf("location_id")]),
                                data[flag.indexOf("tags")],
                                Integer.parseInt(data[flag.indexOf("archived")]),
                                Integer.parseInt(data[flag.indexOf("encrypted")]));
                        entries.add(entry);
                        break;
                    case "attachments":
                        attachment = new Attachment(Long.parseLong(data[flag.indexOf("id")]),
                                Long.parseLong(data[flag.indexOf("entry_id")]),
                                data[flag.indexOf("filename")]);
                        attachments.add(attachment);
                        break;
                }
            }
            type = parser.next();
        }
        fis.close();
        xmlFile.delete();
        return new DB(folders, tags, locations, entries, attachments);
    }

    /**
     * 解压备份文件
     */
    private void decompressFile(String filename) {
        File appDir = new File(Environment.getExternalStorageDirectory(), "Keep");
        File file = new File(appDir, filename);
        String path = getActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES) + File.separator;
        File[] files = new File(path).listFiles();
        for (File f : files) {
            f.delete();
        }
        Decompress decompress = new Decompress(file.getAbsolutePath(), path);
        decompress.unzip();
    }

    /**
     * 确认删除文件对话框
     */
    private void showDeleteDialog(final String filename) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.delete_backup)
                .setMessage(getString(R.string.delete_file) + " \"" + filename + "\"? ")
                .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        if (deleteFile(filename)) {
                            refreshView();
                        } else {
                            Toast.makeText(getActivity(), getString(
                                    R.string.delete_file_failed), Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .create().show();
    }

    /**
     * 删除文件
     */
    private boolean deleteFile(String filename) {
        File appDir = new File(Environment.getExternalStorageDirectory(), "Keep");
        File file = new File(appDir, filename);
        return file.delete();
    }

    /**
     * 简单数据库对象
     */
    static class DB {
        List<Folder> folders;
        List<Tag> tags;
        List<Location> locations;
        List<Entry> entries;
        List<Attachment> attachments;

        public DB(List<Folder> folders, List<Tag> tags, List<Location> locations,
                  List<Entry> entries, List<Attachment> attachments) {
            this.folders = folders;
            this.tags = tags;
            this.locations = locations;
            this.entries = entries;
            this.attachments = attachments;
        }
    }
}
