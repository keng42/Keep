package science.keng42.keep.adapter;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.facebook.android.crypto.keychain.SharedPrefsBackedKeyChain;
import com.facebook.crypto.Crypto;
import com.facebook.crypto.Entity;
import com.facebook.crypto.util.SystemNativeCryptoLibrary;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;

import science.keng42.keep.MyApp;
import science.keng42.keep.R;
import science.keng42.keep.bean.Attachment;
import science.keng42.keep.dao.AttachmentDao;
import science.keng42.keep.util.BitmapLoaderTask;

/**
 * Created by Keng on 2015/5/30
 */
public class PhotoFragment extends Fragment {

    private Attachment mAttachment;
    private Bitmap mPlaceHolderBitmap;

    /**
     * 新实例，新建时必须调用
     */
    public static PhotoFragment newInstance(long photoId) {
        PhotoFragment pf = new PhotoFragment();
        Bundle args = new Bundle();
        args.putLong("photoId", photoId);
        pf.setArguments(args);
        return pf;
    }

    public PhotoFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        long photoId = args.getLong("photoId", 1);

        AttachmentDao ad = new AttachmentDao(getActivity());

        mAttachment = ad.query(photoId);
        mPlaceHolderBitmap = BitmapFactory.decodeResource(getResources(),
                R.drawable.ic_photo_placeholder_dark);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_photo, container, false);
        // 获取屏幕尺寸
        DisplayMetrics dm = getResources().getDisplayMetrics();
        int screenWidth = dm.widthPixels;
        int screenHeight = dm.heightPixels;
        ImageView iv = (ImageView) view.findViewById(R.id.iv);
        loadBitmap(iv, mAttachment.getFilename(), screenWidth, screenHeight);
        iv.setTag(mAttachment.getFilename());

        setListener(iv);

        return view;
    }

    /**
     * 点击图片转到图库
     */
    private void setListener(ImageView iv) {
        iv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                transToGallery();
            }
        });
    }

    /**
     * 转到图库应用查看图片
     * 解密文件保存到缓存中再发送给图库
     */
    private void transToGallery() {
        String filename = mAttachment.getFilename();
        String path = getActivity().getExternalFilesDir(
                Environment.DIRECTORY_PICTURES) + File.separator;
        byte[] bytes = decryptFileToBytes(path + filename);
        if (bytes == null) {
            Log.e(MyApp.TAG, "图片文件解密失败");
            return;
        }
        File cacheFile = new File(getActivity().getExternalCacheDir(), "cache.jpg");
        try {
            FileOutputStream fos = new FileOutputStream(cacheFile);
            fos.write(bytes, 0, bytes.length);
            fos.flush();
            fos.close();
        } catch (Exception e) {
            Log.e(MyApp.TAG, "", e);
        }
        Intent it = new Intent(Intent.ACTION_VIEW);
        Uri uri = Uri.fromFile(cacheFile);
        it.setDataAndType(uri, "image/*");
        startActivity(it);
    }

    /**
     * 加载 Bitmap 到 ImageView
     */
    private void loadBitmap(ImageView iv, String name, int w, int h) {
        String path = getActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES) + "/" + name;
        if (BitmapLoaderTask.cancelPotentialWork(path, iv)) {
            BitmapLoaderTask task = new BitmapLoaderTask(iv, getActivity());
            BitmapLoaderTask.AsyncDrawable asyncDrawable = new BitmapLoaderTask
                    .AsyncDrawable(getResources(), mPlaceHolderBitmap, task);
            iv.setImageDrawable(asyncDrawable);
            task.execute(path, w, h);
        }
    }

    /**
     * 解密文件到字符数组用于加载到 Bitmap
     */
    private byte[] decryptFileToBytes(String path) {
        Crypto crypto = new Crypto(
                new SharedPrefsBackedKeyChain(getActivity()),
                new SystemNativeCryptoLibrary());

        if (!crypto.isAvailable()) {
            return null;
        }

        try {
            InputStream fileStream = new FileInputStream(path);
            MyApp myApp = (MyApp) getActivity().getApplication();
            String password = myApp.getPassword();
            InputStream inputStream = crypto.getCipherInputStream(fileStream, new Entity(password));
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            byte[] buf = new byte[MyApp.NORMAL_BYTES_BUFFER_SIZE];
            int len;
            while ((len = inputStream.read(buf)) > 0) {
                outputStream.write(buf, 0, len);
            }
            return outputStream.toByteArray();
        } catch (Exception e) {
            Log.e(MyApp.TAG, "", e);
        }
        return null;
    }

    /**
     * 删除该图片
     */
    public void delete() {
        AttachmentDao ad = new AttachmentDao(getActivity());
        ad.delete(mAttachment.getId());
        // TODO delete file
        File file = new File(getActivity().getExternalFilesDir(
                Environment.DIRECTORY_PICTURES), mAttachment.getFilename());
        if (!file.delete()) {
            Log.e(MyApp.TAG, "图片文件删除失败");
        }
    }
}
