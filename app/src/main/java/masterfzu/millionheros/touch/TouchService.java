package masterfzu.millionheros.touch;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import masterfzu.millionheros.R;
import masterfzu.millionheros.TheApp;
import masterfzu.millionheros.baiduocr.BaiduOCR;
import masterfzu.millionheros.hint.BaiduSearch;
import masterfzu.millionheros.hint.QandA;
import masterfzu.millionheros.util.Counter;
import masterfzu.millionheros.util.StringUtil;
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
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

/**
 * 主要业务流程
 */
public class TouchService extends Service {

    private static final String TAG = "TouchService";
    public static int mYUPLINE = 243;
    public static int mYDONWLINE = 1159;

    LinearLayout upLine, downLine, topLayout;
    WindowManager.LayoutParams upLineP, downLineP, hintLayoutParams;
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
    private String findWd = "";

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
//            String t = msg.getData().getString("toast");
//            if (!StringUtil.isEmpty(t)) {
//                Toast toast = Toast.makeText(TouchService.this, t, Toast.LENGTH_SHORT);
//                toast.setGravity(Gravity.CENTER, 0, 0);
//                toast.show();
//            }

            String path = msg.getData().getString("path");
            if (!StringUtil.isEmpty(path)) {
                hintWeb.loadUrl(path);
                return;
            }

            String result = msg.getData().getString("result");
            hintView.setText(result);
            findWd = msg.getData().getString("ans");
//            hintWeb.loadDataWithBaseURL(null, result,"text/html; charset=UTF-8", "utf-8", null);
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

        addHintLayout();
        addUpHintLine();
        addDownHintLine();
        addToplayout();
    }

    private void addToplayout() {
        WindowManager.LayoutParams params = new WindowManager.LayoutParams();
        params.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        params.format = PixelFormat.RGBA_8888;
        params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;

        params.gravity = Gravity.TOP;
        params.x = 0;
        params.y = 0;

        //设置悬浮窗口长宽数据.
        params.width = windowWidth;
        params.height = this.getResources().getDimensionPixelSize(R.dimen.top_layout_height);

        LayoutInflater inflater = LayoutInflater.from(getApplication());
        //获取浮动窗口视图所在布局.
        topLayout = (LinearLayout) inflater.inflate(R.layout.toplayout, null);

        windowManager.addView(topLayout, params);
        topLayout.findViewById(R.id.add).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onAddLine();
            }
        });
        topLayout.findViewById(R.id.cap).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                showMe();
                rxAction();
            }
        });
        topLayout.findViewById(R.id.remove).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopSelf();
            }
        });
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

    private void rxAction() {
        final String c = "rxAction";
        Counter.letsgo(c);
        Observable.just(getCapture()) // 截图
                .map(new Function<byte[], String>() {
                    @Override
                    public String apply(byte[] bytes) throws Exception {
                        Log.e("RXACTION", "map-ocr, " + Thread.currentThread().getName());
                        String result =  BaiduOCR.doOCR(bytes); // 百度识图
                        return result;
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext(new Consumer<String>() {
                    @Override
                    public void accept(String s) throws Exception {
                        Log.e("RXACTION", "do-ocr, " + Thread.currentThread().getName());
                        hintView.setText("识图，" + Counter.spendS(c));
                    }
                })
                .map(new Function<String, QandA>() {
                    @Override
                    public QandA apply(String s) throws Exception {
                        Log.e("RXACTION", "map-qanda, " + Thread.currentThread().getName());
                        return QandA.format(s); // 分析结果
                    }
                })
                .doOnNext(new Consumer<QandA>() {
                    @Override
                    public void accept(QandA qandA) throws Exception {
                        Log.e("RXACTION", "do-qanda, " + Thread.currentThread().getName());
                        hintView.setText("找问题，" + Counter.spendS(c));
                    }
                })
                .observeOn(Schedulers.io())
                .map(new Function<QandA, BaiduSearch.ResultSum>() {
                    @Override
                    public BaiduSearch.ResultSum apply(QandA qandA) throws Exception {
                        Log.e("RXACTION", "map-search, " + Thread.currentThread().getName());
                        return BaiduSearch.searchResult(qandA); // 百度搜索
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<BaiduSearch.ResultSum>() {
                    @Override
                    public void accept(BaiduSearch.ResultSum resultSum) throws Exception {
                        Log.e("RXACTION", "sub-accept, " + Thread.currentThread().getName());
                        hintWeb.loadUrl(resultSum.path);
                        hintView.setText("完成！" + Counter.spendS(c));
                        hintView.setText(BaiduSearch.getHintIfCatch(resultSum));
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        Log.e("RXACTION", "sub-error, " + Thread.currentThread().getName());
                        hintView.setText("失败！请重试！" + Counter.spendS(c));
                        Log.e("RXJAVA", throwable.getMessage());
                    }
                });
    }

    private void addHintLayout() {
        //赋值WindowManager&LayoutParam.
        hintLayoutParams = new WindowManager.LayoutParams();
        //设置type.系统提示型窗口，一般都在应用程序窗口之上.
        hintLayoutParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        //设置效果为背景透明.
        hintLayoutParams.format = PixelFormat.RGBA_8888;
        //设置flags.不可聚焦及不可使用按钮对悬浮窗进行操控.
        hintLayoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;

        //设置窗口初始停靠位置.
        hintLayoutParams.gravity = Gravity.BOTTOM;
//        params.x = 0;
//        params.y = 0;

        //设置悬浮窗口长宽数据.
        hintLayoutParams.width = windowWidth;
        hintLayoutParams.height = windowHeight - mYDONWLINE - statusBarHeight - this.getResources().getDimensionPixelSize(R.dimen.hint_line_height);

        LayoutInflater inflater = LayoutInflater.from(getApplication());
        //获取浮动窗口视图所在布局.
        hintlayout = (LinearLayout) inflater.inflate(R.layout.hintlayout, null);
        //添加toucherlayout
        windowManager.addView(hintlayout, hintLayoutParams);

        hintView = hintlayout.findViewById(R.id.hintBottomText);

        hintWeb = hintlayout.findViewById(R.id.hintWeb);
        hintWeb.getSettings().setDefaultTextEncodingName("UTF-8");
        hintWeb.getSettings().setJavaScriptEnabled(true);
        hintWeb.getSettings().setDomStorageEnabled(true);
        hintWeb.getSettings().setTextZoom(75);
//        hintWeb.getSettings().setBlockNetworkImage(true);
        hintWeb.setWebViewClient(new WebViewClient() {
            //覆盖shouldOverrideUrlLoading 方法
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return false;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (!StringUtil.isEmpty(findWd)) {
                    view.clearMatches();
                    view.findAllAsync(findWd);
                }
            }
        });
        hintWeb.loadUrl("http://m.baidu.com");
//        hintWeb.loadDataWithBaseURL(null,"### 长按此处退出 ~ 点小猪进行提示 ### \n\n 移动标线确保上下两条线内只有问题与答案 \n 截屏前最好关闭标线", "text/html", "UTF-8", null);
        hintView.setText("# 移动标线确保上下两条线内只有问题与答案 #");
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
        hintLineText.setText("--------------- 镖姬线 " + mYUPLINE + " ------------------");

        //添加toucherlayout
        windowManager.addView(upLine, upLineP);

        upLine.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_MOVE) {
//                    upLineP.x = (int) event.getRawX();
                    upLineP.y = (int) event.getRawY();
                    mYUPLINE = upLineP.y - statusBarHeight;
                    hintLineText.setText("--------------- 镖姬线 " + mYUPLINE + " ------------------");
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
        hintLineText.setText("--------------- 镖姬线 " + mYDONWLINE + " ------------------");

        windowManager.addView(downLine, downLineP);

        downLine.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_MOVE) {
//                    downLineP.x = (int) event.getRawX();
                    downLineP.y = (int) event.getRawY();
                    mYDONWLINE = downLineP.y - statusBarHeight;
                    hintLineText.setText("--------------- 镖姬线 " + mYDONWLINE + " ------------------");
                    windowManager.updateViewLayout(downLine, downLineP);
                    hintLayoutParams.height = windowHeight - mYDONWLINE - statusBarHeight - downLineP.height;
                    windowManager.updateViewLayout(hintlayout, hintLayoutParams);
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
        if (hintlayout != null) {
            windowManager.removeView(hintlayout);
        }
        if (topLayout != null) {
            windowManager.removeView(topLayout);
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
