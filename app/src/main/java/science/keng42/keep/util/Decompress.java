package science.keng42.keep.util;

import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Created by Keng on 2015/9/11
 * http://jondev.net/articles/Unzipping_Files_with_Android_(Programmatically)
 */
public class Decompress {
    private String _zipFile;
    private String _location;

    public Decompress(String zipFile, String location) {
        _zipFile = zipFile;
        _location = location;

        _dirChecker("");
    }

    public void unzip() {
        try {
            FileInputStream fis = new FileInputStream(_zipFile);
            ZipInputStream zis = new ZipInputStream(fis);
            ZipEntry ze;
            byte[] buf = new byte[1024];
            while ((ze = zis.getNextEntry()) != null) {
                if (ze.isDirectory()) {
                    _dirChecker(ze.getName());
                } else {
                    FileOutputStream fos = new FileOutputStream(_location + ze.getName());
                    int len;
                    while ((len = zis.read(buf)) != -1) {
                        fos.write(buf, 0, len);
                    }
                    zis.closeEntry();
                    fos.close();
                }
            }
            zis.close();
        } catch (Exception e) {
            Log.e("Decompress", "unzip", e);
        }
    }

    private void _dirChecker(String dir) {
        File f = new File(_location + dir);
        if (!f.isDirectory()) {
            f.mkdirs();
        }
    }
}