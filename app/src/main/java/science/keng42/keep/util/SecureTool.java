package science.keng42.keep.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import android.util.Log;

import com.facebook.android.crypto.keychain.SharedPrefsBackedKeyChain;
import com.facebook.crypto.Crypto;
import com.facebook.crypto.Entity;
import com.facebook.crypto.util.SystemNativeCryptoLibrary;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.Set;

import science.keng42.keep.MyApp;

/**
 * 有关安全的工具类
 * 包括计算哈希值
 */
public final class SecureTool {

    private static final String SP_NAME = "SP_NAME";
    private static final String SECURE_CODE_KEY = "SECURE_CODE_KEY";
    private static final String TO_ENCRYPT = "ToEncrypt";
    private static final String PASSWORD_KEY = "Password";
    private static final String CRYPTO_IS_NOT_AVAILABLE = "Crypto 不可用，检查 Conceal 是否正确加载。";
    private static final String SALT = "1SimpleSalt.com";
    private static final String KEEP_CHARSET = "utf-8";

    private SecureTool() {
    }

    /**
     * 计算一个字符串+盐的哈希值，并以16进制字符串存储
     */
    public static String makeSHA256Hash(String input, String salt) {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA256");
            md.reset();
            byte[] buffer = (input + salt).getBytes();
            md.update(buffer);
            byte[] digest = md.digest();
            return bytesToHexStr(digest);
        } catch (NoSuchAlgorithmException e) {
            Log.e(MyApp.TAG, "", e);
        }
        return null;
    }

    /**
     * 字节数组转换为16进制字符串
     * 占用内存会加倍，但便于使用
     */
    public static String bytesToHexStr(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(Integer.toString((b & 0xff) + 0x100, 16).substring(1));
        }
        return sb.toString();
    }

    /**
     * 保存安全码的哈希值到 SharePreferences
     */
    public static void saveSecureCodeHash(Context context, String code) {
        SharedPreferences sp = context.getSharedPreferences(SP_NAME,
                Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString(SECURE_CODE_KEY, SecureTool.makeSHA256Hash(code, SALT));
        editor.apply();
    }

    /**
     * 使用默认密码加密的条目列表
     */
    public static void addIdToEncrypt(Context context, String id) {
        Set<String> set = getIdsToEncrypt(context);
        if (set == null) {
            set = new HashSet<>();
        }
        set.add(id);

        SharedPreferences sp = context.getSharedPreferences(SP_NAME,
                Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putStringSet(TO_ENCRYPT, set);
        editor.apply();
    }

    /**
     * 获取待重新加密的条目列表
     */
    public static Set<String> getIdsToEncrypt(Context context) {
        SharedPreferences sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        return sp.getStringSet(TO_ENCRYPT, null);
    }

    /**
     * 重置待重新加密的条目列表
     */
    public static void resetIdsToEncrypt(Context context) {
        SharedPreferences sp = context.getSharedPreferences(SP_NAME,
                Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putStringSet(TO_ENCRYPT, null);
        editor.apply();
    }

    /**
     * 获取安全码的哈希值
     */
    public static String getSecureCode(Context context) {
        SharedPreferences sp = context.getSharedPreferences(SP_NAME,
                Context.MODE_PRIVATE);
        return sp.getString(SECURE_CODE_KEY, null);
    }

    /**
     * 验证安全码是否正确
     */
    public static boolean validateSecureCode(Context context, String code) {
        SharedPreferences sp = context.getSharedPreferences(SP_NAME,
                Context.MODE_PRIVATE);
        String codeHash = sp.getString(SECURE_CODE_KEY, null);
        return codeHash != null && codeHash.equals(makeSHA256Hash(code, SALT));
    }

    /**
     * 加密并保存密码
     * 使用 PIN+SALT 作为密码加密数据加密密码得到的密文保存在 SharedPreferences
     */
    public static void savePasswordCipher(Context context, String password, String pin) {
        String cipher = encryptStr(context, password, pin + SALT);
        SharedPreferences sp = context.getSharedPreferences(SP_NAME,
                Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString(PASSWORD_KEY, cipher);
        editor.apply();
    }

    /**
     * 获取数据解密密码
     */
    public static String getPassword(Context context, String pin) {
        SharedPreferences sp = context.getSharedPreferences(SP_NAME,
                Context.MODE_PRIVATE);
        String cipher = sp.getString(PASSWORD_KEY, null);
        // TODO what if pin wrong
        return decryptStr(context, cipher, pin + SALT);
    }

    /**
     * Conceal 相关方法
     */
    /**
     * 加密字符数组
     *
     * @param plainByte 明文数组
     * @param password  密码
     * @return 密文数组
     */
    public static byte[] encryptByte(Context context, byte[] plainByte, String password) {
        Crypto crypto = new Crypto(new SharedPrefsBackedKeyChain(context),
                new SystemNativeCryptoLibrary());

        if (!crypto.isAvailable()) {
            Log.e(MyApp.TAG, CRYPTO_IS_NOT_AVAILABLE);
            return null;
        }

        try {
            return crypto.encrypt(plainByte, new Entity(password));
        } catch (Exception e) {
            Log.e(MyApp.TAG, "", e);
        }
        return null;
    }

    /**
     * 解密字符数组
     *
     * @param cipherByte 密文数组
     * @param password   密码
     * @return 明文数组
     */
    public static byte[] decryptByte(Context context, byte[] cipherByte, String password) {
        Crypto crypto = new Crypto(new SharedPrefsBackedKeyChain(context),
                new SystemNativeCryptoLibrary());

        if (!crypto.isAvailable()) {
            Log.e(MyApp.TAG, CRYPTO_IS_NOT_AVAILABLE);
            return null;
        }

        try {
            return crypto.decrypt(cipherByte, new Entity(password));
        } catch (Exception e) {
            Log.e(MyApp.TAG, "", e);
        }
        return null;
    }

    /**
     * 加密字符串
     *
     * @param plainStr 明文字符串
     * @param password 密码
     * @return 密文字符串(16进制)
     */
    public static String encryptStr(Context context, String plainStr, String password) {
        byte[] bytes = null;
        try {
            bytes = encryptByte(context, plainStr.getBytes(KEEP_CHARSET), password);
        } catch (UnsupportedEncodingException e) {
            Log.e(MyApp.TAG, "", e);
        }
        if (bytes == null) {
            return null;
        }
        return Base64.encodeToString(bytes, Base64.DEFAULT);
    }

    /**
     * 解密字符串
     *
     * @param cipherStr 密文字符串(16进制)
     * @param password  密码
     * @return 明文字符串
     */
    public static String decryptStr(Context context, String cipherStr, String password) {
        byte[] bytes = decryptByte(context, Base64.decode(
                cipherStr, Base64.DEFAULT), password);
        if (bytes == null) {
            return null;
        }
        try {
            return new String(bytes, KEEP_CHARSET);
        } catch (UnsupportedEncodingException e) {
            Log.e(MyApp.TAG, "", e);
        }
        return null;
    }

    /**
     * 加密文件（替换）
     *
     * @param path     原文件路径
     * @param password 密码
     */
    public static void encryptFile(Context context, String path, String password) {
        Crypto crypto = new Crypto(new SharedPrefsBackedKeyChain(context),
                new SystemNativeCryptoLibrary());

        if (!crypto.isAvailable()) {
            Log.e(MyApp.TAG, CRYPTO_IS_NOT_AVAILABLE);
            return;
        }

        try {
            FileInputStream fis = new FileInputStream(path);
            OutputStream fileStream = new BufferedOutputStream(new FileOutputStream(path + ".encrypted"));
            OutputStream outputStream = crypto.getCipherOutputStream(fileStream, new Entity(password));

            int read;
            byte[] buffer = new byte[MyApp.NORMAL_BYTES_BUFFER_SIZE];

            while ((read = fis.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }

            fis.close();
            outputStream.close();

            File file1 = new File(path);
            File file2 = new File(path + ".encrypted");
            if (file1.delete()) {
                if (!file2.renameTo(file1)) {
                    Log.e(MyApp.TAG, "加密失败：加密文件重命名失败");
                }
            } else {
                Log.e(MyApp.TAG, "加密失败：原文件删除失败");
            }
        } catch (Exception e) {
            Log.e(MyApp.TAG, "", e);
        }
    }

    /**
     * 解密文件（替换）
     *
     * @param path     已加密文件路径
     * @param password 密码
     */
    public static void decryptFile(Context context, String path, String password) {
        Crypto crypto = new Crypto(
                new SharedPrefsBackedKeyChain(context),
                new SystemNativeCryptoLibrary());

        if (!crypto.isAvailable()) {
            Log.e(MyApp.TAG, CRYPTO_IS_NOT_AVAILABLE);
            return;
        }

        try {
            FileOutputStream out = new FileOutputStream(path + ".decrypted");
            FileInputStream fileStream = new FileInputStream(path);
            InputStream inputStream = crypto.getCipherInputStream(fileStream, new Entity(password));

            int read;
            byte[] buffer = new byte[MyApp.NORMAL_BYTES_BUFFER_SIZE];

            while ((read = inputStream.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            inputStream.close();
            out.close();

            File file1 = new File(path);
            File file2 = new File(path + ".decrypted");
            if (file1.delete()) {
                if (!file2.renameTo(file1)) {
                    Log.e(MyApp.TAG, "解密失败：解密文件重命名失败");
                }
            } else {
                Log.e(MyApp.TAG, "解密失败：原文件删除失败");
            }
        } catch (Exception e) {
            Log.e(MyApp.TAG, "", e);
        }
    }
}
