package android.masterfzu.millionheros.touch;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.masterfzu.millionheros.R;
import android.masterfzu.millionheros.TheApp;
import android.masterfzu.millionheros.hint.BaiduSearch;
import android.masterfzu.millionheros.util.Counter;
import android.masterfzu.millionheros.util.StringUtil;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;

/**
 * 主要业务流程
 */
public class TouchService extends Service {

    private static final String TAG = "TouchService";
    public static final int TOUCHER_WID = 200;
    public static final int TOUCHER_HEI = 200;
    public static int mYUPLINE = 266;
    public static int mYDONWLINE = 1159;

    ConstraintLayout toucherLayout;
    LinearLayout upLine, downLine;
    WindowManager.LayoutParams params, upLineP, downLineP;
    WindowManager windowManager;
    LinearLayout hintlayout;

    TextView hintView;
    WebView hintWeb;

    private static int mResultCode = 0;
    private static Intent mResultData = null;

    private SimpleDateFormat dateFormat = null;
    private String strDate = null;
    private String pathImage = null;
    private String nameImage = null;

    private MediaProjection mMediaProjection = null;
    private VirtualDisplay mVirtualDisplay = null;
    private ImageReader mImageReader = null;
    private DisplayMetrics metrics = null;
    public static MediaProjectionManager mMediaProjectionManager = null;

    private int windowWidth = 0;
    private int windowHeight = 0;
    private int mScreenDensity = 0;

    //状态栏高度.
    int statusBarHeight = -1;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            String t = msg.getData().getString("toast");
            if (!StringUtil.isEmpty(t)) {
                Toast toast = Toast.makeText(TouchService.this, t, Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
            }

            String path = msg.getData().getString("path");
            if (!StringUtil.isEmpty(path)) {
                hintWeb.loadUrl(path);
                return;
            }

            String result = msg.getData().getString("result");
//            hintView.setText(result);
            hintWeb.loadDataWithBaseURL(null, result,"text/html; charset=UTF-8", "utf-8", null);
        }
    };


    //不与Activity进行绑定.
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "TouchService Created");

        setUpMediaProjection();
        createVirtualEnvironment();
        addUpHintLine();
        addDownHintLine();
        addHintLayout();
        addToucher();
    }

    public void setUpMediaProjection(){
        mMediaProjectionManager = (MediaProjectionManager) getApplication().getSystemService(Context.MEDIA_PROJECTION_SERVICE);

        mResultData = ((TheApp)getApplication()).getIntent();
        mResultCode = ((TheApp)getApplication()).getResult();
        mMediaProjection = mMediaProjectionManager.getMediaProjection(mResultCode, mResultData);
        Log.i(TAG, "mMediaProjection defined");
    }

    private void createVirtualEnvironment() {
        dateFormat = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");
        strDate = dateFormat.format(new java.util.Date());
        pathImage = ensurePath();


        windowManager = (WindowManager) getApplication().getSystemService(Context.WINDOW_SERVICE);
        windowWidth = windowManager.getDefaultDisplay().getWidth();
        windowHeight = windowManager.getDefaultDisplay().getHeight();
        metrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(metrics);
        mScreenDensity = metrics.densityDpi;
        mImageReader = ImageReader.newInstance(windowWidth, windowHeight, 0x1, 2); //ImageFormat.RGB_565

        Log.i(TAG, "prepared the virtual environment");

        virtualDisplay();
    }

    @NonNull
    private String ensurePath() {
        File file = new File(Environment.getExternalStorageDirectory().getPath() + "/MillionHeros");
        if (!file.isDirectory())
            file.mkdir();

        return file.getAbsolutePath();
    }

    private void virtualDisplay() {
        mVirtualDisplay = mMediaProjection.createVirtualDisplay("screen-mirror",
                windowWidth, windowHeight, mScreenDensity, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mImageReader.getSurface(), null, null);
        Log.i(TAG, "virtual displayed");
    }

    private void addToucher() {
        //赋值WindowManager&LayoutParam.
        params = new WindowManager.LayoutParams();
        //设置type.系统提示型窗口，一般都在应用程序窗口之上.
        params.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        //设置效果为背景透明.
        params.format = PixelFormat.RGBA_8888;
        //设置flags.不可聚焦及不可使用按钮对悬浮窗进行操控.
        params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;

        //设置窗口初始停靠位置.
        params.gravity = Gravity.LEFT | Gravity.TOP;
        params.x = 0;
        params.y = 0;

        //设置悬浮窗口长宽数据.
        params.width = TOUCHER_WID;
        params.height = TOUCHER_HEI;

        LayoutInflater inflater = LayoutInflater.from(getApplication());
        //获取浮动窗口视图所在布局.
        toucherLayout = (ConstraintLayout) inflater.inflate(R.layout.toucherlayout, null);
        //添加toucherlayout
        windowManager.addView(toucherLayout, params);

        //主动计算出当前View的宽高信息.
        toucherLayout.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);

        //用于检测状态栏高度.
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            statusBarHeight = getResources().getDimensionPixelSize(resourceId);
        }
        Log.i(TAG, "状态栏高度为:" + statusBarHeight);

        toucherLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showMe();
            }

        });

        toucherLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_MOVE) {
                    params.x = (int) event.getRawX();
                    params.y = (int) event.getRawY() - statusBarHeight;
                    windowManager.updateViewLayout(toucherLayout, params);
                }
                return false;
            }
        });
    }

    private void showMe() {
        final byte [] img = getCapture();

        if (img != null || img.length > 0) {
            hintView.setText("截图成功");
//            hintView.loadData("截图成功", "text/html", "UTF-8");
            BaiduSearch.search(img, mHandler);
        } else
            hintView.setText("!!!!!!截图失败，马上重试!!!!!!!");
//        hintView.loadData("!!!!!!截图失败，马上重试!!!!!!!", "text/html", "UTF-8");
    }

    private void addHintLayout() {
        //赋值WindowManager&LayoutParam.
        WindowManager.LayoutParams params = new WindowManager.LayoutParams();
        //设置type.系统提示型窗口，一般都在应用程序窗口之上.
        params.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        //设置效果为背景透明.
        params.format = PixelFormat.RGBA_8888;
        //设置flags.不可聚焦及不可使用按钮对悬浮窗进行操控.
        params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;

        //设置窗口初始停靠位置.
        params.gravity = Gravity.BOTTOM;
//        params.x = 0;
//        params.y = 0;

        //设置悬浮窗口长宽数据.
        params.width = windowWidth;
        params.height = this.getResources().getDimensionPixelSize(R.dimen.hint_layout_height);

        LayoutInflater inflater = LayoutInflater.from(getApplication());
        //获取浮动窗口视图所在布局.
        hintlayout = (LinearLayout) inflater.inflate(R.layout.hintlayout, null);
        //添加toucherlayout
        windowManager.addView(hintlayout, params);

        hintView = hintlayout.findViewById(R.id.hintText);

        hintWeb = hintlayout.findViewById(R.id.hintWeb);
        hintWeb.getSettings().setDefaultTextEncodingName("UTF-8");
        hintWeb.getSettings().setJavaScriptEnabled(true);
//        hintWeb.getSettings().setUseWideViewPort(true);

        hintlayout.findViewById(R.id.add).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onAddLine();
            }
        });
        hintlayout.findViewById(R.id.remove).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onQuit();
            }
        });

        hintWeb.loadDataWithBaseURL(null,"### 长按此处退出 ~ 点小猪进行提示 ### \n\n 移动标线确保上下两条线内只有问题与答案 \n 截屏前最好关闭标线", "text/html", "UTF-8", null);
//        hintView.setText("### 长按此处退出 ~ 点小猪进行提示 ### \n\n 移动标线确保上下两条线内只有问题与答案 \n 截屏前最好关闭标线  \n\nPS：不可能/不属于/不是的问题选无匹配的答案~~");
    }

    private void addUpHintLine() {
        upLineP = new WindowManager.LayoutParams();
        upLineP.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        upLineP.format = PixelFormat.RGBA_8888;
        upLineP.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;

        upLineP.gravity = Gravity.LEFT | Gravity.TOP;
        upLineP.x = 0;
        upLineP.y = mYUPLINE + statusBarHeight;

        //设置悬浮窗口长宽数据.
        upLineP.width = windowWidth;
        upLineP.height = this.getResources().getDimensionPixelSize(R.dimen.hint_line_height);

        LayoutInflater inflater = LayoutInflater.from(getApplication());
        //获取浮动窗口视图所在布局.
        upLine = (LinearLayout) inflater.inflate(R.layout.hintline, null);
        final TextView hintLineText = upLine.findViewById(R.id.hintLineText);
        hintLineText.setText("------------------ " + mYUPLINE + " ------------------");

        //添加toucherlayout
        windowManager.addView(upLine, upLineP);

        upLine.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_MOVE) {
//                    upLineP.x = (int) event.getRawX();
                    upLineP.y = (int) event.getRawY();
                    mYUPLINE = upLineP.y - statusBarHeight;
                    hintLineText.setText("------------------ " + mYUPLINE + " ------------------");
                    windowManager.updateViewLayout(upLine, upLineP);
                }
                return false;
            }
        });
    }

    private void onAddLine() {
        if (upLine == null) {
            addUpHintLine();
            addDownHintLine();
        } else {
            removeLine();
        }
    }

    private void onQuit() {
        stopSelf();
    }

    private void addDownHintLine() {
        downLineP = new WindowManager.LayoutParams();
        downLineP.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        downLineP.format = PixelFormat.RGBA_8888;
        downLineP.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;

        downLineP.gravity = Gravity.LEFT | Gravity.TOP;
        downLineP.x = 0;
        downLineP.y = mYDONWLINE + statusBarHeight;

        downLineP.width = windowWidth;
        downLineP.height = this.getResources().getDimensionPixelSize(R.dimen.hint_line_height);

        LayoutInflater inflater = LayoutInflater.from(getApplication());
        downLine = (LinearLayout) inflater.inflate(R.layout.hintline, null);
        final TextView hintLineText = downLine.findViewById(R.id.hintLineText);
        hintLineText.setText("------------------ " + mYDONWLINE + " ------------------");

        windowManager.addView(downLine, downLineP);

        downLine.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_MOVE) {
//                    downLineP.x = (int) event.getRawX();
                    downLineP.y = (int) event.getRawY();
                    mYDONWLINE = downLineP.y - statusBarHeight;
                    hintLineText.setText("------------------ " + mYDONWLINE + " ------------------");
                    windowManager.updateViewLayout(downLine, downLineP);
                }
                return false;
            }
        });
    }


    private byte[] getCapture() {
        Counter.letsgo("startCapture");
//        removeLine();

        strDate = dateFormat.format(new java.util.Date());
        nameImage = pathImage + File.separator + strDate + ".png";

        Image image = mImageReader.acquireLatestImage();
        int width = image.getWidth();
        int height = image.getHeight();
        final Image.Plane[] planes = image.getPlanes();
        final ByteBuffer buffer = planes[0].getBuffer();

//        byte [] b = new byte[buffer.remaining()];
//        buffer.get(b, 0, b.length);
//        BaiduOCR.doOCR(b, mHandler);

        int pixelStride = planes[0].getPixelStride();
        int rowStride = planes[0].getRowStride();
        int rowPadding = rowStride - pixelStride * width;
        int hintLineHeight = this.getResources().getDimensionPixelSize(R.dimen.hint_line_height);
        Bitmap bitmap = Bitmap.createBitmap(width + rowPadding / pixelStride, height, Bitmap.Config.ARGB_8888);
//        buffer.position(0);
        bitmap.copyPixelsFromBuffer(buffer);
        bitmap = Bitmap.createBitmap(bitmap, 100, mYUPLINE + statusBarHeight + hintLineHeight, 900, mYDONWLINE - mYUPLINE - hintLineHeight);
        image.close();
        Log.i(TAG, "image data captured");
//        saveToSD(bitmap);
        return convertToByte(bitmap);
//        makeToast("截屏完成耗时" + Counter.spendS("startCapture") + "s，保存路径：" + fileImage.getAbsolutePath());
    }

    private byte[] convertToByte(Bitmap bitmap) {
        if (bitmap == null)
            return new byte[]{};

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        return baos.toByteArray();
    }

    private void saveToSD(Bitmap bitmap) {
        if (bitmap != null) {
            try {
                File fileImage = new File(nameImage);
                if (!fileImage.exists()) {
                    fileImage.createNewFile();
                    Log.i(TAG, "image file created");
                }
                FileOutputStream out = new FileOutputStream(fileImage);
                if (out != null) {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                    out.flush();
                    out.close();
                    Intent media = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                    Uri contentUri = Uri.fromFile(fileImage);
                    media.setData(contentUri);
                    this.sendBroadcast(media);

                    Log.i(TAG, "screen image saved");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onDestroy() {
        if (toucherLayout != null) {
            windowManager.removeView(toucherLayout);
        }
        if (hintlayout != null) {
            windowManager.removeView(hintlayout);
        }

        removeLine();

        stopVirtual();
        tearDownMediaProjection();

        super.onDestroy();
    }

    private void removeLine() {
        if (upLine != null) {
            windowManager.removeView(upLine);
            upLine = null;
        }
        if (downLine != null) {
            windowManager.removeView(downLine);
            downLine = null;
        }
    }

    private void tearDownMediaProjection() {
        if (mMediaProjection != null) {
            mMediaProjection.stop();
            mMediaProjection = null;
        }
        Log.i(TAG,"mMediaProjection undefined");
    }

    private void stopVirtual() {
        if (mVirtualDisplay == null) {
            return;
        }
        mVirtualDisplay.release();
        mVirtualDisplay = null;
        Log.i(TAG,"virtual display stopped");
    }
}
