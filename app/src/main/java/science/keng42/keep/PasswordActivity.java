package science.keng42.keep;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import science.keng42.keep.util.SecureTool;

public class PasswordActivity extends AppCompatActivity {

    private EditText mEtCode;
    private EditText mEtKey;
    private Button mBtnCancel;
    private Button mBtnSave;
    private int mAction;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_password);

        initToolbar();
        initView();
        initData();
        setListener();
    }

    /**
     * 重置安全码时使用
     */
    private void initData() {
        Intent intent = getIntent();
        mAction = intent.getIntExtra("action", 0);
        if (mAction == 1) {
            // time to change security code
            MyApp myApp = (MyApp) getApplication();
            mEtKey.setText(myApp.getPassword());
        }
    }

    /**
     * 初始化 Toolbar
     */
    private void initToolbar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // 系统版本大于5.0时才设置系统栏颜色
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor(getResources().getColor(R.color.primary_dark));
        }
    }

    /**
     * 初始化控件
     */
    private void initView() {
        mEtCode = (EditText) findViewById(R.id.et_code);
        mEtKey = (EditText) findViewById(R.id.et_key);
        mBtnCancel = (Button) findViewById(R.id.btn_cancel);
        mBtnSave = (Button) findViewById(R.id.btn_save);
    }

    /**
     * 设置监听器
     */
    private void setListener() {
        mBtnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String codeHash = SecureTool.getSecureCode(getApplicationContext());
                if (codeHash != null) {
                    finish();
                } else {
                    Toast.makeText(getApplicationContext(),
                            getResources().getString(R.string.code_require),
                            Toast.LENGTH_SHORT).show();
                }
            }
        });

        mBtnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveCodeAndPassword();
            }
        });
    }

    /**
     * 加密并保存安全码和密码
     */
    private void saveCodeAndPassword() {
        String pin = mEtCode.getText().toString();
        SecureTool.saveSecureCodeHash(getApplicationContext(), pin);

        final MyApp myApp = (MyApp) getApplication();
        if (mAction == 1 && !myApp.getPassword().equals(mEtKey.getText().toString())) {
            // confirm if change password
            showConfirmChangePasswordDialog();
        } else {
            saveAndExit(0);
        }
    }

    private void showConfirmChangePasswordDialog() {
        String[] items = new String[]{getResources().getString(R.string.change_password_waring)};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.change_password)
                .setItems(items, null)
                .setPositiveButton(R.string.change, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        saveAndExit(0);
                        dialog.dismiss();
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        saveAndExit(1);
                        dialog.dismiss();
                    }
                }).create().show();
    }

    private void saveAndExit(int change) {
        MyApp myApp = (MyApp) getApplication();
        String pwd = mEtKey.getText().toString();
        if (change == 1) {
            pwd = myApp.getPassword();
        }
        String pin = mEtCode.getText().toString();
        SecureTool.savePasswordCipher(this, pwd, pin);
        if (mAction == 1) {
            Intent intent = new Intent();
            intent.putExtra("oldPassword", myApp.getPassword());
            if (myApp.getPassword() == null) {
                Log.i("1984", "pwd ac null");
            } else {
                Log.i("1984", "pwd " + myApp.getPassword());
            }
            setResult(RESULT_OK, intent);
        }
        finish();
    }
}
