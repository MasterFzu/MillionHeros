package masterfzu.millionheros.touch;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import butterknife.OnClick;
import masterfzu.millionheros.R;
import masterfzu.millionheros.TheApp;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * 获取截屏与悬浮窗权限
 */
public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private final int REQUEST_MEDIA_PROJECTION = 1;

    @BindView(R.id.overhint)
    TextView overhint;
    @BindView(R.id.btnover)
    Button btnover;
    @BindView(R.id.caphint)
    TextView caphint;
    @BindView(R.id.btncap)
    Button btncap;
    @BindView(R.id.go)
    Button go;

    Toast mToast;

    private MediaProjectionManager mMediaProjectionManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mMediaProjectionManager = (MediaProjectionManager) this.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        mToast = Toast.makeText(this, "", Toast.LENGTH_LONG);

        setContentView(R.layout.mainlayout);

        ButterKnife.bind(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        //当AndroidSDK>=23及Android版本6.0及以上时，需要获取OVERLAY_PERMISSION.
        //使用canDrawOverlays用于检查，下面为其源码。其中也提醒了需要在manifest文件中添加权限.
        /**
         * Checks if the specified context can draw on top of other apps. As of API
         * level 23, an app cannot draw on top of other apps unless it declares the
         * {@link Manifest.permission#SYSTEM_ALERT_WINDOW} permission in its
         * manifest, <em>and</em> the user specifically grants the app this
         * capability. To prompt the user to grant this approval, the app must send an
         * intent with the action
         * {@link Settings#ACTION_MANAGE_OVERLAY_PERMISSION}, which
         * causes the system to display a permission management screen.
         *
         */
        if (Settings.canDrawOverlays(MainActivity.this)) {
            overhint.setText("已允许启动悬浮窗权限！");
            btnover.setEnabled(false);
            if (((TheApp) getApplication()).getIntent() == null)
                startActivityForResult(mMediaProjectionManager.createScreenCaptureIntent(), REQUEST_MEDIA_PROJECTION);
            else {
                caphint.setText(R.string.capok);
                go.setText(R.string.go);
                btncap.setEnabled(false);
            }

        } else {
            //若没有权限，提示获取.
//            starToOverlayPermission();
            overhint.setText("开启悬浮窗权限");
        }
    }

    @OnClick(R.id.btncap)
    public void onRequestCap(View v) {
        startActivityForResult(mMediaProjectionManager.createScreenCaptureIntent(), REQUEST_MEDIA_PROJECTION);
    }

    @OnClick(R.id.btnover)
    public void onRequestOver(View v) {
        starToOverlayPermission();
    }

    @OnClick(R.id.go)
    public void letsgo(View v) {
        if (!Settings.canDrawOverlays(MainActivity.this)) {
            mToast.setText("请先开启悬浮窗权限！");
            mToast.show();
            return;
        }
        if (((TheApp) getApplication()).getIntent() == null) {
            mToast.setText("请先获取截屏权限！");
            mToast.show();
            return;
        }

        Intent intent = new Intent(MainActivity.this, TouchService.class);
        startService(intent);
        finish();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_MEDIA_PROJECTION) {
            if (resultCode != Activity.RESULT_OK) {
//                caphint.setText("需要取得权限以截屏");
//                Toast.makeText(MainActivity.this, "需要取得权限以截屏", Toast.LENGTH_SHORT).show();
                mToast.setText("需要取得权限以截屏");
                mToast.show();
                return;
            } else if (data != null && resultCode != 0) {
                caphint.setText(R.string.capok);
                btncap.setEnabled(false);
                setCapIntent(resultCode, data);
                if (Settings.canDrawOverlays(this)) {
                    go.setText(R.string.go);
                }
            }
        }
    }


    private void setCapIntent(int resultCode, Intent data) {
        ((TheApp) getApplication()).setResult(resultCode);
        ((TheApp) getApplication()).setIntent(data);
    }

    private void starToOverlayPermission() {
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
        mToast.setText("请开启#" + this.getString(R.string.app_name) + "#悬浮窗权限");
        mToast.show();
        startActivity(intent);
    }


}
