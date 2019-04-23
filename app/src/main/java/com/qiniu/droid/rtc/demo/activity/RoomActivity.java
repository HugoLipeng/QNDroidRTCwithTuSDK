package com.qiniu.droid.rtc.demo.activity;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.opengl.EGL14;
import android.opengl.EGLContext;
import android.opengl.GLES20;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPropertyAnimatorListener;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.qiniu.droid.rtc.QNBeautySetting;
import com.qiniu.droid.rtc.QNCameraSwitchResultCallback;
import com.qiniu.droid.rtc.QNLocalVideoCallback;
import com.qiniu.droid.rtc.QNRTCManager;
import com.qiniu.droid.rtc.QNRTCSetting;
import com.qiniu.droid.rtc.QNRemoteAudioCallback;
import com.qiniu.droid.rtc.QNRemoteSurfaceView;
import com.qiniu.droid.rtc.QNRoomEventListener;
import com.qiniu.droid.rtc.QNRoomState;
import com.qiniu.droid.rtc.QNStatisticsReport;
import com.qiniu.droid.rtc.QNVideoFormat;
import com.qiniu.droid.rtc.demo.R;
import com.qiniu.droid.rtc.demo.fragment.ControlFragment;
import com.qiniu.droid.rtc.demo.tusdk.ConfigViewSeekBar;
import com.qiniu.droid.rtc.demo.tusdk.FilterCellView;
import com.qiniu.droid.rtc.demo.tusdk.FilterConfigSeekbar;
import com.qiniu.droid.rtc.demo.tusdk.FilterConfigView;
import com.qiniu.droid.rtc.demo.tusdk.FilterListView;
import com.qiniu.droid.rtc.demo.tusdk.StickerListAdapter;
import com.qiniu.droid.rtc.demo.ui.LocalVideoView;
import com.qiniu.droid.rtc.demo.ui.RTCVideoView;
import com.qiniu.droid.rtc.demo.utils.Config;
import com.qiniu.droid.rtc.demo.utils.QNAppServer;
import com.qiniu.droid.rtc.demo.utils.ToastUtils;

import org.json.JSONArray;
import org.json.JSONObject;
import org.lasque.tusdk.api.video.preproc.filter.TuSDKFilterEngine;
import org.lasque.tusdk.core.TuSdkContext;
import org.lasque.tusdk.core.gl.SelesWindowsSurface;
import org.lasque.tusdk.core.seles.SelesParameters;
import org.lasque.tusdk.core.seles.tusdk.FilterWrap;
import org.lasque.tusdk.core.struct.TuSdkSize;
import org.lasque.tusdk.core.utils.TLog;
import org.lasque.tusdk.core.utils.ThreadHelper;
import org.lasque.tusdk.core.utils.hardware.CameraConfigs;
import org.lasque.tusdk.core.utils.image.ImageOrientation;
import org.lasque.tusdk.core.utils.json.JsonHelper;
import org.lasque.tusdk.core.view.recyclerview.TuSdkTableView;
import org.lasque.tusdk.core.view.widget.button.TuSdkTextButton;
import org.lasque.tusdk.impl.view.widget.TuSeekBar;
import org.lasque.tusdk.modules.view.widget.sticker.StickerGroup;
import org.lasque.tusdk.modules.view.widget.sticker.StickerLocalPackage;
import org.webrtc.VideoFrame;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static com.qiniu.droid.rtc.QNErrorCode.ERROR_KICKED_OUT_OF_ROOM;

public class RoomActivity extends Activity implements QNRoomEventListener, ControlFragment.OnCallEvents {

    private static final String TAG = "RoomActivity";

    public static final String EXTRA_ROOM_ID = "ROOM_ID";
    public static final String EXTRA_ROOM_TOKEN = "ROOM_TOKEN";
    public static final String EXTRA_USER_ID = "USER_ID";
    public static final String EXTRA_VIDEO_WIDTH = "VIDEO_WIDTH";
    public static final String EXTRA_VIDEO_HEIGHT = "VIDEO_HEIGHT";
    public static final String EXTRA_HW_CODEC = "HW_CODEC";

    private static final String[] MANDATORY_PERMISSIONS = {
            "android.permission.MODIFY_AUDIO_SETTINGS",
            "android.permission.RECORD_AUDIO",
            "android.permission.INTERNET"
    };

    private List<String> mHWBlackList = new ArrayList<>();
    private List<RTCVideoView> mUsedWindowList;
    private List<RTCVideoView> mUnusedWindowList;
    private ConcurrentHashMap<String, RTCVideoView> mUserWindowMap;
    private String[] mMergeStreamPosition;

    private RTCVideoView mRemoteWindowA;
    private RTCVideoView mRemoteWindowB;
    private RTCVideoView mRemoteWindowC;
    private RTCVideoView mRemoteWindowD;
    private RTCVideoView mRemoteWindowE;
    private RTCVideoView mRemoteWindowF;
    private RTCVideoView mRemoteWindowG;
    private RTCVideoView mRemoteWindowH;
    private LocalVideoView mLocalWindow;

    private Toast mLogToast;
    private QNRTCManager mRTCManager;

    private boolean mIsError;
    private boolean mCallControlFragmentVisible = true;
    private long mCallStartedTimeMs = 0;
    private boolean mMicEnabled = true;
    private boolean mBeautyEnabled = false;
    private boolean mVideoEnabled = true;
    private boolean mSpeakerEnabled = true;
    private boolean mIsJoinedRoom = false;
    private String mRoomId;
    private String mRoomToken;
    private String mUserId;
    private String mLocalLogText;
    private ControlFragment mControlFragment;

    private int mScreenWidth = 0;
    private int mScreenHeight = 0;
    private int mVideoWidth = 0;
    private int mVideoHeight = 0;
    private float mDensity = 0;
    private boolean mIsAdmin = false;

    private AlertDialog mKickoutDialog;

    /// ========================= TuSDK 相关 ========================= ///
    // 滤镜 code 列表, 每个 code 代表一种滤镜效果, 具体 code 可在 lsq_tusdk_configs.json 查看 (例如:lsq_filter_SkinNature02 滤镜的 code 为 SkinNature02)
    private static final String[] VIDEOFILTERS = new String[]{"none","nature01", "pink01", "jelly01", "ruddy01", "sugar01",
            "honey01", "clear01","timber01","whitening01","porcelain01","Skinwhitening_1"};

    /** 参数调节视图 */
    protected FilterConfigView mConfigView;
    /** 滤镜栏视图 */
    protected FilterListView mFilterListView;
    /** 滤镜底部栏 */
    private View mFilterBottomView;

    // 记录是否是首次进入录制页面
    private boolean mIsFirstEntry = true;
    // 记录当前滤镜
    private FilterWrap mSelesOutInput;
    // 记录当前贴纸
    private StickerGroup mShowStickerGroup;
    // 滤镜Tab
    private TuSdkTextButton mFilterTab;
    // 美颜布局
    private RelativeLayout mFilterLayout;
    // 美颜Tab
    private TuSdkTextButton mBeautyTab;
    // 美颜布局
    private LinearLayout mBeautyLayout;
    // 磨皮调节栏
    private ConfigViewSeekBar mSmoothingBarLayout;
    // 大眼调节栏
    private ConfigViewSeekBar mEyeSizeBarLayout;
    // 瘦脸调节栏
    private ConfigViewSeekBar mChinSizeBarLayout;
    // 用于记录当前调节栏效果系数
    private float mMixiedProgress = -1.0f;
    // 用于记录当前调节栏磨皮系数
    private float mSmoothingProgress = -1.0f;
    // 用于记录当前调节栏大眼系数
    private float mEyeSizeProgress = -1.0f;
    // 用于记录当前调节栏瘦脸系数
    private float mChinSizeProgress = -1.0f;

    // 用于记录焦点位置
    private int mFocusPostion = 2;

    //贴纸底部栏
    private RecyclerView mStickerBottomView;
    private StickerListAdapter stickerListAdapter;

    // TuSDK 滤镜引擎
    private TuSDKFilterEngine mFilterEngine;

    private String filterCode = Arrays.asList(VIDEOFILTERS).get(mFocusPostion);

    // 记录摄像头
    private boolean isFront = true;

    private HandlerThread mGLThread;
    private Handler mGLHandler;
    private int newTextureId;
    private SelesWindowsSurface mSelesWindowsSurface = null;

    /// ========================= TuSDK 相关 ========================= ///

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(LayoutParams.FLAG_FULLSCREEN | LayoutParams.FLAG_KEEP_SCREEN_ON
                | LayoutParams.FLAG_DISMISS_KEYGUARD | LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | LayoutParams.FLAG_TURN_SCREEN_ON);
        getWindow().getDecorView().setSystemUiVisibility(getSystemUiVisibility());

        WindowManager windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics outMetrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getRealMetrics(outMetrics);

        mScreenWidth = outMetrics.widthPixels;
        mScreenHeight = outMetrics.heightPixels;
        mDensity = outMetrics.density;

        setContentView(R.layout.activity_room);

        Intent intent = getIntent();
        mRoomId = intent.getStringExtra(EXTRA_ROOM_ID);
        mRoomToken = intent.getStringExtra(EXTRA_ROOM_TOKEN);
        mUserId = intent.getStringExtra(EXTRA_USER_ID);

        mLocalWindow = (LocalVideoView) findViewById(R.id.local_video_view);
        mLocalWindow.setUserId(mUserId);

        mRemoteWindowA = (RTCVideoView) findViewById(R.id.remote_video_view_a);
        mRemoteWindowB = (RTCVideoView) findViewById(R.id.remote_video_view_b);
        mRemoteWindowC = (RTCVideoView) findViewById(R.id.remote_video_view_c);
        mRemoteWindowD = (RTCVideoView) findViewById(R.id.remote_video_view_d);
        mRemoteWindowE = (RTCVideoView) findViewById(R.id.remote_video_view_e);
        mRemoteWindowF = (RTCVideoView) findViewById(R.id.remote_video_view_f);
        mRemoteWindowG = (RTCVideoView) findViewById(R.id.remote_video_view_g);
        mRemoteWindowH = (RTCVideoView) findViewById(R.id.remote_video_view_h);

        mUsedWindowList = Collections.synchronizedList(new LinkedList<RTCVideoView>());
        mUsedWindowList.add(mLocalWindow);
        mUnusedWindowList = Collections.synchronizedList(new LinkedList<RTCVideoView>());
        mUnusedWindowList.add(mRemoteWindowA);
        mUnusedWindowList.add(mRemoteWindowB);
        mUnusedWindowList.add(mRemoteWindowC);
        mUnusedWindowList.add(mRemoteWindowD);
        mUnusedWindowList.add(mRemoteWindowE);
        mUnusedWindowList.add(mRemoteWindowF);
        mUnusedWindowList.add(mRemoteWindowG);
        mUnusedWindowList.add(mRemoteWindowH);

        for (RTCVideoView rtcVideoView : mUnusedWindowList) {
            rtcVideoView.setOnLongClickListener(mOnLongClickListener);
        }

        mUserWindowMap = new ConcurrentHashMap<>();

        mControlFragment = new ControlFragment();

        for (String permission : MANDATORY_PERMISSIONS) {
            if (checkCallingOrSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                logAndToast("Permission " + permission + " is not granted");
                setResult(RESULT_CANCELED);
                finish();
                return;
            }
        }

        SharedPreferences preferences = getSharedPreferences(getString(R.string.app_name), Context.MODE_PRIVATE);
        mVideoWidth = preferences.getInt(Config.WIDTH, QNRTCSetting.DEFAULT_WIDTH);
        mVideoHeight = preferences.getInt(Config.HEIGHT, QNRTCSetting.DEFAULT_HEIGHT);
        boolean isHwCodec = preferences.getInt(Config.CODEC_MODE, Config.SW) == Config.HW;
        boolean isScreenCaptureEnabled = preferences.getInt(Config.CAPTURE_MODE, Config.CAMERA_CAPTURE) == Config.SCREEN_CAPTURE;

        if (isScreenCaptureEnabled) {
            mLocalWindow.setAudioViewVisible(0);
        }

        // get the items in hw black list, and set isHwCodec false forcibly
        String[] hwBlackList = getResources().getStringArray(R.array.hw_black_list);
        mHWBlackList.addAll(Arrays.asList(hwBlackList));
        if (mHWBlackList.contains(Build.MODEL)) {
            isHwCodec = false;
        }

        QNRTCSetting setting = new QNRTCSetting();
        setting.setCameraID(QNRTCSetting.CAMERA_FACING_ID.FRONT)
                .setHWCodecEnabled(isHwCodec)
                .setScreenCaptureEnabled(isScreenCaptureEnabled)
                .setVideoPreviewFormat(new QNVideoFormat(mVideoWidth, mVideoHeight, QNRTCSetting.DEFAULT_FPS))
                .setVideoEncodeFormat(new QNVideoFormat(mVideoWidth, mVideoHeight, QNRTCSetting.DEFAULT_FPS));

        int audioBitrate = 100 * 1000;
        int videoBitrate = preferences.getInt(Config.BITRATE, 600 * 1000);
        setting.setAudioBitrate(audioBitrate);
        setting.setVideoBitrate(videoBitrate);
        //当设置的最低码率，远高于弱网下的常规传输码率值时，会严重影响连麦的画面流畅度
        setting.setBitrateRange(0, videoBitrate + audioBitrate);

        mControlFragment.setArguments(intent.getExtras());
        mControlFragment.setScreenCaptureEnabled(isScreenCaptureEnabled);
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.add(R.id.control_fragment_container, mControlFragment);
        ft.commitAllowingStateLoss();

        mRTCManager = new QNRTCManager();
        mRTCManager.setRoomEventListener(this);
        mRTCManager.addRemoteWindow(mRemoteWindowA.getRemoteSurfaceView());
        mRTCManager.addRemoteWindow(mRemoteWindowB.getRemoteSurfaceView());
        mRTCManager.addRemoteWindow(mRemoteWindowC.getRemoteSurfaceView());
        mRTCManager.addRemoteWindow(mRemoteWindowD.getRemoteSurfaceView());
        mRTCManager.addRemoteWindow(mRemoteWindowE.getRemoteSurfaceView());
        mRTCManager.addRemoteWindow(mRemoteWindowF.getRemoteSurfaceView());
        mRTCManager.addRemoteWindow(mRemoteWindowG.getRemoteSurfaceView());
        mRTCManager.addRemoteWindow(mRemoteWindowH.getRemoteSurfaceView());
        mRTCManager.initialize(this, setting, mLocalWindow.getLocalSurfaceView());

        // TuSDK初始化
        initTuSDK();
    }
    /// ========================= TuSDK 代码起始 ========================= ///

    /**
     * 初始化TuSDK
     */
    private void initTuSDK(){
        mGLThread = new HandlerThread(TAG);
        mGLThread.start();
        mGLHandler = new Handler(mGLThread.getLooper());

        initFilterListView();
        initStickerListView();
        //动态加载版本初始化
        initBottomView();

        mLocalWindow.setLocalVideoCallback(new QNLocalVideoCallback() {
            @Override
            public synchronized int onRenderingFrame(final int textureId,final int width,final int height, VideoFrame.TextureBuffer.Type type,final long timestampNs) {

                newTextureId = textureId;
                final CountDownLatch count = new CountDownLatch(1);

                if (mGLHandler == null || mFilterEngine == null) {
                    createFilterEngine();
                    return textureId;
                }

                mGLHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (mSelesWindowsSurface != null)
                            mSelesWindowsSurface.makeCurrent();

                        if (mFilterEngine != null)
                            newTextureId = mFilterEngine.processFrame(textureId, width, height, timestampNs);

                        mSelesWindowsSurface.swapBuffers();
                        count.countDown();
                    }
                });

                try {
                    count.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                return newTextureId;
            }

            @Override
            public void onSurfaceCreated() {
                createFilterEngine();
            }

            @Override
            public void onSurfaceChanged(final int width,final int height) {
                mGLHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if(mFilterEngine != null) mFilterEngine.onSurfaceChanged(width,height);
                    }
                });
            }

            @Override
            public void onSurfaceDestroyed() {
                destroyFilterEngine();
            }
        });
    }

    /** 重新设置效果 **/
    private void replayMediaEffect() {
        if(mFilterEngine == null) return;

        mFilterEngine.runOnDrawEnd(new Runnable() {
            @Override
            public void run() {

                if(mShowStickerGroup != null)
                {
                    mFilterEngine.showGroupSticker(mShowStickerGroup);
                }

                if(!TextUtils.isEmpty(filterCode))
                {
                    mFilterEngine.switchFilter(filterCode);
                }

            }
        });
    }

    /**
     * 创建
     */
    private void createFilterEngine(){
        final EGLContext eglContext = EGL14.eglGetCurrentContext();
        mGLHandler.post(new Runnable() {
            @Override
            public void run() {
                if(mSelesWindowsSurface == null)
                    mSelesWindowsSurface = new SelesWindowsSurface(eglContext, 0);

                if(mFilterEngine == null) initEngine();

                mFilterEngine.setCameraFacing(isFront ? CameraConfigs.CameraFacing.Front : CameraConfigs.CameraFacing.Back);
                mFilterEngine.setOutputImageOrientation(isFront ? ImageOrientation.LeftMirrored : ImageOrientation.Left);
            }
        });
    }

    /**
     * 销毁
     */
    private void destroyFilterEngine(){
        mGLHandler.post(new Runnable() {
            @Override
            public void run() {
                if(mFilterEngine != null){
                    mFilterEngine.onSurfaceDestroy();
                    mFilterEngine.destroy();
                    mFilterEngine = null;
                }
                if(mSelesWindowsSurface != null) {
                    mSelesWindowsSurface.release();
                    mSelesWindowsSurface = null;
                }
            }
        });
    }

    /**
     * 初始化滤镜处理类
     */
    private void initEngine(){
        mFilterEngine = new TuSDKFilterEngine(this,false);
        mFilterEngine.setEnableLiveSticker(true);
        // 前置 后置
//        mFilterEngine.setCameraFacing(CameraConfigs.CameraFacing.Front);
        // 默认 true 不修正方向  false 修正输出方向
        mFilterEngine.setOutputOriginalImageOrientation(true);
        // 设置输入方向
//        mFilterEngine.setInputImageOrientation(ImageOrientation.LeftMirrored);
        // 设置输出方向
//        mFilterEngine.setOutputImageOrientation(ImageOrientation.LeftMirrored);
        mFilterEngine.onSurfaceCreated();

        mFilterEngine.setDelegate(mFilterDelegate);
        // 添加滤镜
        replayMediaEffect();
    }

    /**
     * 初始化滤镜
     */
    private void initFilterListView(){
        getFilterListView();

        this.mFilterListView.setModeList(Arrays.asList(VIDEOFILTERS));
    }
    /**
     * 滤镜栏视图
     *
     * @return
     */
    public FilterListView getFilterListView()
    {
        if (mFilterListView == null)
        {
            mFilterListView = (FilterListView) findViewById(R.id.lsq_filter_list_view);
            mFilterListView.loadView();
            mFilterListView.setCellLayoutId(R.layout.filter_list_cell_view);
            mFilterListView.setCellWidth(TuSdkContext.dip2px(62));
            mFilterListView.setItemClickDelegate(mFilterTableItemClickDelegate);
            mFilterListView.reloadData();
            mFilterListView.selectPosition(mFocusPostion);
        }
        return mFilterListView;
    }

    /**
     * 初始化贴纸列表
     */
    private void initStickerListView(){
        mStickerBottomView = (RecyclerView) findViewById(R.id.lsq_sticker_list_view);
        mStickerBottomView.setVisibility(View.GONE);

        stickerListAdapter = new StickerListAdapter();
        GridLayoutManager manager = new GridLayoutManager(this,5);
        mStickerBottomView.setLayoutManager(manager);
        mStickerBottomView.setAdapter(stickerListAdapter);

        List<StickerGroup> stickerGroups;
        // 获取打包贴纸资源
//        stickerGroups = StickerLocalPackage.shared().getSmartStickerGroups();
        // 获取在线贴纸配置
        stickerGroups = getRawStickGroupList();

        stickerGroups.add(0,new StickerGroup());
        stickerListAdapter.setStickerList(stickerGroups);

        stickerListAdapter.setOnItemClickListener(new StickerListAdapter.OnItemClickListener() {

            @Override
            public void onClickItem(StickerGroup itemData, StickerListAdapter.StickerHolder
                    stickerHolder, int position) {
                onStickerGroupSelected(itemData,stickerHolder,position);
            }
        });
    }

    /**
     * 初始化底部栏
     */
    private void initBottomView(){
        mBeautyTab = (TuSdkTextButton) findViewById(R.id.lsq_beauty_btn);
        mBeautyLayout = (LinearLayout) findViewById(R.id.lsq_beauty_content);

        mFilterTab = (TuSdkTextButton) findViewById(R.id.lsq_filter_btn);
        mFilterLayout = (RelativeLayout) findViewById(R.id.lsq_filter_content);

        //美颜
        mSmoothingBarLayout = (ConfigViewSeekBar) mBeautyLayout.findViewById(R.id.lsq_dermabrasion_bar);
        mSmoothingBarLayout.getTitleView().setText(R.string.lsq_dermabrasion);
        mSmoothingBarLayout.getSeekbar().setDelegate(mTuSeekBarDelegate);

        mEyeSizeBarLayout = (ConfigViewSeekBar) mBeautyLayout.findViewById(R.id.lsq_big_eyes_bar);
        mEyeSizeBarLayout.getTitleView().setText(R.string.lsq_big_eyes);
        mEyeSizeBarLayout.getSeekbar().setDelegate(mTuSeekBarDelegate);

        mChinSizeBarLayout = (ConfigViewSeekBar) mBeautyLayout.findViewById(R.id.lsq_thin_face_bar);
        mChinSizeBarLayout.getTitleView().setText(R.string.lsq_thin_face);
        mChinSizeBarLayout.getSeekbar().setDelegate(mTuSeekBarDelegate);

        mFilterBottomView = findViewById(R.id.lsq_filter_group_bottom_view);
    }

    /** 拖动条监听事件 */
    private TuSeekBar.TuSeekBarDelegate mTuSeekBarDelegate = new TuSeekBar.TuSeekBarDelegate()
    {
        @Override
        public void onTuSeekBarChanged(TuSeekBar seekBar, float progress)
        {
            if (seekBar == mSmoothingBarLayout.getSeekbar())
            {
                mSmoothingProgress = progress;
                applyFilter(mSmoothingBarLayout,"smoothing",progress);
            }
            else if (seekBar == mEyeSizeBarLayout.getSeekbar())
            {
                mEyeSizeProgress = progress;
                applyFilter(mEyeSizeBarLayout,"eyeSize",progress);
            }
            else if (seekBar == mChinSizeBarLayout.getSeekbar())
            {
                mChinSizeProgress = progress;
                applyFilter(mChinSizeBarLayout,"chinSize",progress);
            }
        }
    };

    /**
     * 应用滤镜
     * @param viewSeekBar
     * @param key
     * @param progress
     */
    private void applyFilter(ConfigViewSeekBar viewSeekBar, String key, float progress)
    {
        if (viewSeekBar == null || mSelesOutInput == null) return;

        viewSeekBar.getConfigValueView().setText((int)(progress*100) + "%");
        SelesParameters params = mSelesOutInput.getFilterParameter();
        params.setFilterArg(key, progress);
        mSelesOutInput.submitFilterParameter();
    }

    // TuSDKFilterEngine 事件回调
    private TuSDKFilterEngine.TuSDKFilterEngineDelegate mFilterDelegate = new TuSDKFilterEngine.TuSDKFilterEngineDelegate()
    {
        /**
         * 滤镜更改事件，每次调用 switchFilter 切换滤镜后即触发该事件
         *
         * @param filterWrap
         *            新的滤镜对象
         */
        @Override
        public void onFilterChanged(FilterWrap filterWrap) {
            // 获取滤镜参数列表. 如果开发者希望自定义滤镜栏,可通过 ilter.getParameter().getArgs() 对象获取支持的参数列表。
            if (filterWrap == null) return;

            // 默认滤镜参数调节
            SelesParameters params = filterWrap.getFilterParameter();
            List<SelesParameters.FilterArg> list = params.getArgs();
            for (SelesParameters.FilterArg arg : list)
            {
                if (arg.equalsKey("smoothing") && mSmoothingProgress != -1.0f)
                    arg.setPrecentValue(mSmoothingProgress);
                else if (arg.equalsKey("smoothing") && mSmoothingProgress == -1.0f)
                    mSmoothingProgress = arg.getPrecentValue();
                else if (arg.equalsKey("mixied") && mMixiedProgress !=  -1.0f)
                    arg.setPrecentValue(mMixiedProgress);
                else if (arg.equalsKey("mixied") && mMixiedProgress == -1.0f)
                    mMixiedProgress = arg.getPrecentValue();
                else if (arg.equalsKey("eyeSize")&& mEyeSizeProgress != -1.0f)
                    arg.setPrecentValue(mEyeSizeProgress);
                else if (arg.equalsKey("chinSize")&& mChinSizeProgress != -1.0f)
                    arg.setPrecentValue(mChinSizeProgress);
                else if (arg.equalsKey("eyeSize") && mEyeSizeProgress == -1.0f)
                    mEyeSizeProgress = arg.getPrecentValue();
                else if (arg.equalsKey("chinSize") && mChinSizeProgress == -1.0f)
                    mChinSizeProgress = arg.getPrecentValue();
            }
            filterWrap.setFilterParameter(params);

            mSelesOutInput = filterWrap;

            if (getFilterConfigView() != null)
                getFilterConfigView().setSelesFilter(mSelesOutInput.getFilter());

            if (mIsFirstEntry || (mBeautyLayout!=null && mBeautyLayout.getVisibility() == View.VISIBLE))
            {
                mIsFirstEntry = false;
                showBeautySeekBar();
            }
        }

        @Override
        public void onPictureDataCompleted(IntBuffer intBuffer, TuSdkSize tuSdkSize) {

        }

        @Override
        public void onPreviewScreenShot(Bitmap bitmap) {

        }
    };

    /**
     * 滤镜配置视图
     *
     * @return
     */
    private FilterConfigView getFilterConfigView()
    {
        if (mConfigView == null)
        {
            mConfigView = (FilterConfigView) findViewById(R.id.lsq_filter_config_view);
        }

        return mConfigView;
    }

    /**
     * 获取本地贴纸列表
     * @return
     */
    public List<StickerGroup> getRawStickGroupList()
    {
        List<StickerGroup> list = new ArrayList<StickerGroup>();
        try {
            InputStream stream = getResources().openRawResource(R.raw.square_sticker);
//            if (!isSquareSticker)
//                stream = getResources().openRawResource(R.raw.full_screen_sticker);

            if (stream == null) return null;

            byte buffer[] = new byte[stream.available()];
            stream.read(buffer);
            String json = new String(buffer, "UTF-8");

            JSONObject jsonObject = JsonHelper.json(json);
            JSONArray jsonArray = jsonObject.getJSONArray("stickerGroups");

            for(int i = 0; i < jsonArray.length();i++)
            {
                JSONObject item = jsonArray.getJSONObject(i);
                StickerGroup group = new StickerGroup();
                group.groupId = item.optLong("id");
                group.previewName = item.optString("previewImage");
                group.name = item.optString("name");
                list.add(group);
            }
            return list;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 贴纸组选择事件
     *
     * @param itemData
     * @param stickerHolder
     * @param position
     */
    protected void onStickerGroupSelected(StickerGroup itemData, StickerListAdapter.StickerHolder
            stickerHolder, int position)
    {
        // 设置点击贴纸时呈现或是隐藏贴纸
        if (position == 0)
        {
            mFilterEngine.removeAllLiveSticker();
            stickerListAdapter.setSelectedPosition(position);
            mShowStickerGroup = null;
            return;
        }

        // 如果贴纸已被下载到本地
        if (stickerListAdapter.isDownloaded(itemData))
        {
            stickerListAdapter.setSelectedPosition(position);
            // 必须重新获取StickerGroup,否则itemData.stickers为null
            itemData = StickerLocalPackage.shared().getStickerGroup(itemData.groupId);
            mFilterEngine.showGroupSticker(itemData);
            mShowStickerGroup = itemData;
        }else
        {
            stickerListAdapter.downloadStickerGroup(itemData,stickerHolder);
        }

    }

    /**
     * 更新贴纸栏相关视图的显示状态
     *
     * @param isShow
     */
    private void updateStickerViewStaff(boolean isShow)
    {
        mStickerBottomView.setVisibility(isShow? View.VISIBLE: View.GONE);
    }

    /**
     * 隐藏贴纸栏
     */
    public void hideStickerStaff()
    {
        if(mStickerBottomView.getVisibility() == View.GONE) return;

        updateStickerViewStaff(false);

        // 滤镜栏向下动画并隐藏
        ViewCompat.animate(mStickerBottomView)
                .translationY(mStickerBottomView.getHeight()).setDuration(200);
    }

    /**
     * 显示贴纸底部栏
     */
    public void showStickerLayout()
    {
        updateStickerViewStaff(true);

        // 滤镜栏向上动画并显示
        ViewCompat.setTranslationY(mStickerBottomView,
                mStickerBottomView.getHeight());
        ViewCompat.animate(mStickerBottomView).translationY(0).setDuration(200).setListener(mViewPropertyAnimatorListener);
    }

    /** 属性动画监听事件 */
    private ViewPropertyAnimatorListener mViewPropertyAnimatorListener = new ViewPropertyAnimatorListener()
    {

        @Override
        public void onAnimationCancel(View view)
        {

        }

        @Override
        public void onAnimationEnd(View view)
        {
            ViewCompat.animate(mStickerBottomView).setListener(null);
            ViewCompat.animate(mFilterBottomView).setListener(null);
            mStickerBottomView.clearAnimation();
            mFilterBottomView.clearAnimation();
        }

        @Override
        public void onAnimationStart(View view)
        {

        }
    };

    /**
     * TuSDK点击事件
     * @param view
     */
    public void onTuSDKClick(View view){
        switch (view.getId()){
            case R.id.lsq_beauty_btn:
                showBeautySeekBar();
                break;
            case R.id.lsq_filter_btn:
                showFilterLayout();
                break;
            case R.id.lsq_smart_beauty_btn:
                if(mFilterBottomView.getVisibility() == View.VISIBLE){
                    hideFilterStaff();
                    findViewById(R.id.bottom_button_layout).setVisibility(View.VISIBLE);
                }else {
                    hideStickerStaff();
                    showSmartBeautyLayout();
                    findViewById(R.id.bottom_button_layout).setVisibility(View.INVISIBLE);
                }
                break;
            case R.id.sticker_list_btn:
                if(mStickerBottomView.getVisibility()==View.VISIBLE){
                    hideStickerStaff();
                    findViewById(R.id.bottom_button_layout).setVisibility(View.VISIBLE);
                }else {
                    hideFilterStaff();
                    showStickerLayout();
                    findViewById(R.id.bottom_button_layout).setVisibility(View.INVISIBLE);
                }
                break;
        }
    }

    /**
     * 显示智能美颜底部栏
     */
    public void showSmartBeautyLayout()
    {
        updateFilterViewStaff(true);

        // 滤镜栏向上动画并显示
        ViewCompat.setTranslationY(mFilterBottomView,
                mFilterBottomView.getHeight());
        ViewCompat.animate(mFilterBottomView).translationY(0).setDuration(200).setListener(mViewPropertyAnimatorListener);
        showBeautySeekBar();
    }

    /**
     * 隐藏滤镜栏
     */
    public void hideFilterStaff()
    {
        if(mFilterBottomView.getVisibility() == View.GONE) return;

        updateFilterViewStaff(false);

        // 滤镜栏向下动画并隐藏
        ViewCompat.animate(mFilterBottomView)
                .translationY(mFilterBottomView.getHeight()).setDuration(200);
    }

    /**
     * 更新滤镜栏相关视图的显示状态
     *
     * @param isShow
     */
    private void updateFilterViewStaff(boolean isShow)
    {
        mFilterBottomView.setVisibility(isShow? View.VISIBLE: View.GONE);
    }

    /** 显示滤镜列表 */
    private void showFilterLayout()
    {
        if (mBeautyLayout == null || mFilterLayout == null)
            return;

        mFilterLayout.setVisibility(View.VISIBLE);
        mBeautyLayout.setVisibility(View.GONE);
        updateSmartBeautyTab(mBeautyTab,false);
        updateSmartBeautyTab(mFilterTab,true);

        if (mFocusPostion>0 && getFilterConfigView() != null && mSelesOutInput != null)
        {
            getFilterConfigView().post(new Runnable()
            {

                @Override
                public void run() {
                    getFilterConfigView().setSelesFilter(mSelesOutInput.getFilter());
                    getFilterConfigView().setVisibility(View.VISIBLE);
                }});

            getFilterConfigView().setSeekBarDelegate(mConfigSeekBarDelegate);
            getFilterConfigView().invalidate();
        }
    }

    /** 滤镜拖动条监听事件 */
    private FilterConfigView.FilterConfigViewSeekBarDelegate mConfigSeekBarDelegate = new FilterConfigView.FilterConfigViewSeekBarDelegate()
    {

        @Override
        public void onSeekbarDataChanged(FilterConfigSeekbar seekbar, SelesParameters.FilterArg arg)
        {
            if (arg == null) return;

            if (arg.equalsKey("smoothing"))
                mSmoothingProgress = arg.getPrecentValue();
            else if (arg.equalsKey("eyeSize"))
                mEyeSizeProgress = arg.getPrecentValue();
            else if (arg.equalsKey("chinSize"))
                mChinSizeProgress = arg.getPrecentValue();
            else if (arg.equalsKey("mixied"))
                mMixiedProgress = arg.getPrecentValue();
        }

    };

    /** 显示美颜调节栏 */
    private void showBeautySeekBar()
    {
        if (mIsFirstEntry)
        {
            changeVideoFilterCode(Arrays.asList(VIDEOFILTERS).get(mFocusPostion));
        }

        if (mBeautyLayout == null || mFilterLayout == null)
            return;

        mBeautyLayout.setVisibility(View.VISIBLE);
        mFilterLayout.setVisibility(View.GONE);
        updateSmartBeautyTab(mBeautyTab,true);
        updateSmartBeautyTab(mFilterTab,false);

        if (mSelesOutInput == null)
        {
            setEnableAllSeekBar(false);
            return;
        }

        // 滤镜参数
        SelesParameters params = mSelesOutInput.getFilterParameter();
        if (params == null)
        {
            setEnableAllSeekBar(false);
            return;
        }

        List<SelesParameters.FilterArg> list = params.getArgs();
        if (list == null || list.size() == 0)
        {
            setEnableAllSeekBar(false);
            return;
        }

        for(SelesParameters.FilterArg arg : list)
        {
            if (arg.equalsKey("smoothing"))
            {
                setEnableSeekBar(mSmoothingBarLayout,true,arg.getPrecentValue(),
                        R.drawable.tusdk_view_widget_seekbar_drag);
            }
            else if (arg.equalsKey("eyeSize"))
            {
                setEnableSeekBar(mEyeSizeBarLayout,true,arg.getPrecentValue(),
                        R.drawable.tusdk_view_widget_seekbar_drag);
            }
            else if (arg.equalsKey("chinSize"))
            {
                setEnableSeekBar(mChinSizeBarLayout,true,arg.getPrecentValue(),
                        R.drawable.tusdk_view_widget_seekbar_drag);
            }
        }
    }

    /**
     * 更新美颜滤镜Tab
     * @param button
     * @param clickable
     */
    private void updateSmartBeautyTab(TuSdkTextButton button, boolean clickable)
    {
        int imgId = 0, colorId = 0;

        switch (button.getId())
        {
            case R.id.lsq_filter_btn:
                imgId = clickable? R.drawable.lsq_style_default_btn_filter_selected
                        : R.drawable.lsq_style_default_btn_filter_unselected;
                colorId = clickable? R.color.lsq_filter_title_color : R.color.lsq_filter_title_default_color;
                break;
            case R.id.lsq_beauty_btn:
                imgId = clickable? R.drawable.lsq_style_default_btn_beauty_selected
                        : R.drawable.lsq_style_default_btn_beauty_unselected;
                colorId = clickable? R.color.lsq_filter_title_color : R.color.lsq_filter_title_default_color;
                break;
        }

        button.setCompoundDrawables(null, TuSdkContext.getDrawable(imgId), null, null);
        button.setTextColor(TuSdkContext.getColor(colorId));
    }

    private void setEnableAllSeekBar(boolean enable)
    {
        setEnableSeekBar(mSmoothingBarLayout,enable,0, R.drawable.tusdk_view_widget_seekbar_none_drag);
        setEnableSeekBar(mEyeSizeBarLayout,enable,0, R.drawable.tusdk_view_widget_seekbar_none_drag);
        setEnableSeekBar(mChinSizeBarLayout,enable,0, R.drawable.tusdk_view_widget_seekbar_none_drag);
    }

    /** 设置调节栏是否有效 */
    private void setEnableSeekBar(ConfigViewSeekBar viewSeekBar, boolean enable, float progress, int id)
    {
        if (viewSeekBar == null) return;

        viewSeekBar.setProgress(progress);
        viewSeekBar.getSeekbar().setEnabled(enable);
        viewSeekBar.getSeekbar().getDragView().setBackgroundResource(id);
    }

    /**
     * 切换滤镜
     * @param code
     */
    protected void changeVideoFilterCode(final String code)
    {
        if (mFilterEngine == null) return;

        filterCode = code;
        // 切换滤镜效果 code 为滤镜代号可在 lsq_tusdk_configs.json 查看
        mFilterEngine.switchFilter(filterCode);
    }


    /** 滤镜组列表点击事件 */
    private TuSdkTableView.TuSdkTableViewItemClickDelegate<String, FilterCellView> mFilterTableItemClickDelegate = new TuSdkTableView.TuSdkTableViewItemClickDelegate<String, FilterCellView>()
    {
        @Override
        public void onTableViewItemClick(String itemData,
                                         FilterCellView itemView, int position)
        {
            onFilterGroupSelected(itemData, itemView, position);
        }
    };

    /**
     * 滤镜组选择事件
     *
     * @param itemData
     * @param itemView
     * @param position
     */
    protected void onFilterGroupSelected(String itemData,
                                         FilterCellView itemView, int position)
    {
        FilterCellView prevCellView = (FilterCellView) mFilterListView.findViewWithTag(mFocusPostion);
        mFocusPostion = position;
        changeVideoFilterCode(itemData);
        mFilterListView.selectPosition(mFocusPostion);
        deSelectLastFilter(prevCellView);
        selectFilter(itemView, position);
        getFilterConfigView().setVisibility((position == 0)?View.GONE:View.VISIBLE);
    }

    /**
     * 取消上一个滤镜的选中状态
     *
     * @param lastFilter
     */
    private void deSelectLastFilter(FilterCellView lastFilter)
    {
        if (lastFilter == null) return;

        updateFilterBorderView(lastFilter,true);
        lastFilter.getTitleView().setBackground(TuSdkContext.getDrawable(R.drawable.tusdk_view_filter_unselected_text_roundcorner));
        lastFilter.getImageView().invalidate();
    }

    /**
     * 设置滤镜单元边框是否可见
     * @param lastFilter
     * @param isHidden
     */
    private void updateFilterBorderView(FilterCellView lastFilter, boolean isHidden)
    {
        View filterBorderView = lastFilter.getBorderView();
        filterBorderView.setVisibility(isHidden ? View.GONE : View.VISIBLE);
    }

    /**
     * 滤镜选中状态
     *
     * @param itemView
     * @param position
     */
    private void selectFilter(FilterCellView itemView, int position)
    {
        updateFilterBorderView(itemView, false);
        itemView.setFlag(position);
        TextView titleView = itemView.getTitleView();
        titleView.setBackground(TuSdkContext.getDrawable(R.drawable.tusdk_view_filter_selected_text_roundcorner));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        destroyFilterEngine();
        mGLHandler.post(new Runnable() {
            @Override
            public void run() {
                mGLThread.quitSafely();
                mGLThread = null;
                mGLHandler = null;
            }
        });
    }

    /// ========================= TuSDK 代码结束========================= ///
    /************      TuSDK 结束 **********************/

    public void onClickScreen(View v) {
        if (mUsedWindowList.size() < 3) {
            toggleControlFragmentVisibility();
        }
    }

    private void toggleControlFragmentVisibility() {
        if (!mControlFragment.isAdded()) {
            return;
        }

        mCallControlFragmentVisible = !mCallControlFragmentVisible;
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        if (mCallControlFragmentVisible) {
            ft.show(mControlFragment);
        } else {
            ft.hide(mControlFragment);
        }
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        ft.commitAllowingStateLoss();
    }

    private void startCall() {
        if (mRTCManager == null || mIsJoinedRoom) {
            return;
        }
        mCallStartedTimeMs = System.currentTimeMillis();

        logAndToast(getString(R.string.connecting_to, mRoomId));
        mRTCManager.joinRoom(mRoomToken);
        mIsJoinedRoom = true;
    }

    private void onConnectedInternal() {
        final long delay = System.currentTimeMillis() - mCallStartedTimeMs;
        Log.i(TAG, "Call connected: delay=" + delay + "ms");
        logAndToast(getString(R.string.connected_to_room));
    }

    private void subscribeAllRemoteStreams() {
        ArrayList<String> publishingUsers = mRTCManager.getPublishingUserList();
        if (publishingUsers != null && !publishingUsers.isEmpty()) {
            for (String userId : publishingUsers) {
                mRTCManager.subscribe(userId);
                mRTCManager.addRemoteAudioCallback(userId, new QNRemoteAudioCallback() {
                    @Override
                    public void onRemoteAudioAvailable(String userId, ByteBuffer audioData, int size, int bitsPerSample, int sampleRate, int numberOfChannels) {
                    }
                });
            }
        }
    }

    private void clearAllRemoteStreams() {
        if (mUsedWindowList.size() > 2) {
            setTargetWindowParams(1, 0, mLocalWindow);
        }
        mUsedWindowList.clear();
        mUsedWindowList.add(mLocalWindow);

        for (RTCVideoView rtcVideoView : mUserWindowMap.values()) {
            rtcVideoView.setVisible(false);
        }
        mUserWindowMap.clear();

        mUnusedWindowList.clear();
        mUnusedWindowList.add(mRemoteWindowA);
        mUnusedWindowList.add(mRemoteWindowB);
        mUnusedWindowList.add(mRemoteWindowC);
        mUnusedWindowList.add(mRemoteWindowD);
        mUnusedWindowList.add(mRemoteWindowE);
        mUnusedWindowList.add(mRemoteWindowF);
        mUnusedWindowList.add(mRemoteWindowG);
        mUnusedWindowList.add(mRemoteWindowH);
    }

    private void disconnect() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mControlFragment.stopTimer();
            }
        });
        if (mLogToast != null) {
            mLogToast.cancel();
        }
        if (mRTCManager != null) {
            if (mIsAdmin) {
                mRTCManager.stopMergeStream();
            }
            mRTCManager.destroy();
            mRTCManager = null;
        }
        mLocalWindow = null;
        mRemoteWindowA = null;
        mRemoteWindowB = null;
        mRemoteWindowC = null;
        mRemoteWindowD = null;
        mRemoteWindowE = null;
        mRemoteWindowF = null;
        mRemoteWindowG = null;
        mRemoteWindowH = null;

        mIsJoinedRoom = false;
    }

    private void disconnectWithErrorMessage(final String errorMessage) {
        new AlertDialog.Builder(this)
                .setTitle(getText(R.string.channel_error_title))
                .setMessage(errorMessage)
                .setCancelable(false)
                .setNeutralButton(R.string.ok,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        })
                .create()
                .show();
    }

    private void logAndToast(final String msg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, msg);
                if (mLogToast != null) {
                    mLogToast.cancel();
                }
                mLogToast = Toast.makeText(RoomActivity.this, msg, Toast.LENGTH_SHORT);
                mLogToast.show();
            }
        });
    }

    private void reportError(final String description) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (!mIsError) {
                    mIsError = true;
                    disconnectWithErrorMessage(description);
                }
            }
        });
    }

    private RTCVideoView getWindowByUserId(String userId) {
        return mUserWindowMap.containsKey(userId) ? mUserWindowMap.get(userId) : null;
    }

    private void toggleToMultiUsersUI(final int userCount, List<RTCVideoView> windowList) {
        for (int i = 0; i < userCount; i++) {
            setTargetWindowParams(userCount, i, windowList.get(i));
        }
    }

    public synchronized void setTargetWindowParams(final int userCount, final int targetPos, final RTCVideoView targetWindow) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                switch (userCount) {
                    case 1:
                        updateLayoutParams(targetWindow, 0, mScreenWidth, mScreenHeight, 0, 0, -1);
                    case 2:
                        if (targetPos == 0) {
                            updateLayoutParams(targetWindow, targetPos, mScreenWidth, mScreenHeight, 0, 0, -1);
                        } else if (targetPos == 1) {
                            updateLayoutParams(targetWindow, targetPos, (int) (120 * mDensity + 0.5f), (int) (160 * mDensity + 0.5f), 0, 0, Gravity.TOP | Gravity.END);
                        }
                        break;
                    case 3:
                        if (targetPos == 0) {
                            updateLayoutParams(targetWindow, targetPos, mScreenWidth / 2, mScreenWidth / 2, 0, 0, -1);
                        } else if (targetPos == 1) {
                            updateLayoutParams(targetWindow, targetPos, mScreenWidth / 2, mScreenWidth / 2, mScreenWidth / 2, 0, -1);
                        } else {
                            updateLayoutParams(targetWindow, targetPos, mScreenWidth / 2, mScreenWidth / 2, 0, mScreenWidth / 2, Gravity.CENTER_HORIZONTAL);
                        }
                        break;
                    case 4:
                        if (targetPos == 0) {
                            updateLayoutParams(targetWindow, targetPos, mScreenWidth / 2, mScreenWidth / 2, 0, 0, -1);
                        } else if (targetPos == 1) {
                            updateLayoutParams(targetWindow, targetPos, mScreenWidth / 2, mScreenWidth / 2, mScreenWidth / 2, 0, -1);
                        } else if (targetPos == 2) {
                            updateLayoutParams(targetWindow, targetPos, mScreenWidth / 2, mScreenWidth / 2, 0, mScreenWidth / 2, Gravity.START);
                        } else {
                            updateLayoutParams(targetWindow, targetPos, mScreenWidth / 2, mScreenWidth / 2, mScreenWidth / 2, mScreenWidth / 2, -1);
                        }
                        break;
                    case 5:
                    case 6:
                    case 7:
                    case 8:
                    case 9:
                        if (targetPos == 0) {
                            updateLayoutParams(targetWindow, targetPos, mScreenWidth / 3, mScreenWidth / 3, 0, 0, -1);
                        } else if (targetPos == 1) {
                            updateLayoutParams(targetWindow, targetPos, mScreenWidth / 3, mScreenWidth / 3, mScreenWidth / 3, 0, -1);
                        } else if (targetPos == 2) {
                            updateLayoutParams(targetWindow, targetPos, mScreenWidth / 3, mScreenWidth / 3, mScreenWidth * 2 / 3, 0, Gravity.END);
                        } else if (targetPos == 3) {
                            updateLayoutParams(targetWindow, targetPos, mScreenWidth / 3, mScreenWidth / 3, 0, mScreenWidth / 3, -1);
                        } else if (targetPos == 4) {
                            updateLayoutParams(targetWindow, targetPos, mScreenWidth / 3, mScreenWidth / 3, mScreenWidth / 3, mScreenWidth / 3, -1);
                        } else if (targetPos == 5) {
                            updateLayoutParams(targetWindow, targetPos, mScreenWidth / 3, mScreenWidth / 3, mScreenWidth * 2 / 3, mScreenWidth / 3, -1);
                        } else if (targetPos == 6) {
                            updateLayoutParams(targetWindow, targetPos, mScreenWidth / 3, mScreenWidth / 3, 0, mScreenWidth * 2 / 3, -1);
                        } else if (targetPos == 7) {
                            updateLayoutParams(targetWindow, targetPos, mScreenWidth / 3, mScreenWidth / 3, mScreenWidth / 3, mScreenWidth * 2 / 3, -1);
                        } else if (targetPos == 8) {
                            updateLayoutParams(targetWindow, targetPos, mScreenWidth / 3, mScreenWidth / 3, mScreenWidth * 2 / 3, mScreenWidth * 2 / 3, -1);
                        }
                        break;
                }
            }
        });
    }

    private void updateLayoutParams(RTCVideoView targetView, int targetPos, int width, int height, int marginStart, int marginTop, int gravity) {
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) targetView.getLayoutParams();
        lp.width = width;
        lp.height = height;
        lp.topMargin = marginTop;
        lp.gravity = gravity;
        lp.setMarginStart(marginStart);
        targetView.setLayoutParams(lp);
        targetView.setMicrophoneStateVisibility(
                (width == mScreenWidth && height == mScreenHeight) ? View.INVISIBLE : View.VISIBLE);
        if (targetView.getAudioViewVisibility() == View.VISIBLE) {
            targetView.updateAudioView(targetPos);
        }
    }

    private void updateRemoteLogText(final String logText) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mControlFragment.updateRemoteLogText(logText);
            }
        });
    }

    private boolean isAdmin() {
        return mUserId.toLowerCase().indexOf(QNAppServer.ADMIN_USER) != -1;
    }

    private synchronized void clearMergeStreamPos(String userId) {
        int pos = -1;
        if (mMergeStreamPosition != null && !TextUtils.isEmpty(userId)) {
            for (int i = 0; i < mMergeStreamPosition.length; i++) {
                if (userId.equals(mMergeStreamPosition[i])) {
                    pos = i;
                    break;
                }
            }
        }
        if (pos >= 0 && pos < mMergeStreamPosition.length) {
            mMergeStreamPosition[pos] = null;
        }
    }

    private int getMergeStreamIdlePos() {
        int pos = -1;
        for (int i = 0; i < mMergeStreamPosition.length; i++) {
            if (TextUtils.isEmpty(mMergeStreamPosition[i])) {
                pos = i;
                break;
            }
        }
        return pos;
    }

    private synchronized void setMergeRemoteStreamLayout(String userId) {
        if (mIsAdmin) {
            int pos = getMergeStreamIdlePos();
            if (pos == -1) {
                Log.e(TAG, "No idle position for merge streaming, so discard.");
                return;
            }
            int x = QNAppServer.MERGE_STREAM_POS[pos][0];
            int y = QNAppServer.MERGE_STREAM_POS[pos][1];
            mRTCManager.setMergeStreamLayout(userId, x, y, 1, QNAppServer.MERGE_STREAM_WIDTH, QNAppServer.MERGE_STREAM_HEIGHT);
            mMergeStreamPosition[pos] = userId;
        }
    }

    private void showKickoutDialog(final String userId) {
        if (mKickoutDialog == null) {
            mKickoutDialog = new AlertDialog.Builder(this)
                    .setNegativeButton(R.string.negative_dialog_tips, null)
                    .create();
        }
        mKickoutDialog.setMessage(getString(R.string.kickout_tips, userId));
        mKickoutDialog.setButton(DialogInterface.BUTTON_POSITIVE, getResources().getString(R.string.positive_dialog_tips),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mRTCManager.kickOutUser(userId);
                    }
                });
        mKickoutDialog.show();
    }

    @TargetApi(19)
    private static int getSystemUiVisibility() {
        int flags = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            flags |= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        }
        return flags;
    }

    @Override
    protected void onResume() {
        super.onResume();
        startCall();
    }

    @Override
    public void onBackPressed() {
        disconnect();
        super.onBackPressed();
    }

    @Override
    public void onCallHangUp() {
        disconnect();
        finish();
    }

    @Override
    public void onCameraSwitch() {
        if (mRTCManager != null) {
            mRTCManager.switchCamera(new QNCameraSwitchResultCallback() {
                @Override
                public void onCameraSwitchDone(boolean isFrontCamera) {

                    isFront= isFrontCamera;

                    // 切换摄像头必须销毁当前FilterEngine
                    destroyFilterEngine();
                }

                @Override
                public void onCameraSwitchError(String errorMessage) {
                }
            });
        }
    }

    @Override
    public boolean onToggleMic() {
        if (mRTCManager != null) {
            mMicEnabled = !mMicEnabled;
            mRTCManager.muteLocalAudio(!mMicEnabled);
            mLocalWindow.updateMicrophoneStateView(!mMicEnabled);
        }
        return mMicEnabled;
    }

    @Override
    public boolean onToggleVideo() {
        if (mRTCManager != null) {
            mVideoEnabled = !mVideoEnabled;
            mRTCManager.muteLocalVideo(!mVideoEnabled);
            if (!mVideoEnabled) {
                mUsedWindowList.get(0).setAudioViewVisible(0);
            } else {
                mUsedWindowList.get(0).setAudioViewInvisible();
            }
            mRTCManager.setPreviewEnabled(mVideoEnabled);
        }
        return mVideoEnabled;
    }

    @Override
    public boolean onToggleSpeaker() {
        if (mRTCManager != null) {
            mSpeakerEnabled = !mSpeakerEnabled;
            mRTCManager.muteRemoteAudio(!mSpeakerEnabled);
        }
        return mSpeakerEnabled;
    }

    @Override
    public boolean onToggleBeauty() {
        if (mRTCManager != null) {
            mBeautyEnabled = !mBeautyEnabled;
            QNBeautySetting beautySetting = new QNBeautySetting(0.5f, 0.5f, 0.5f);
            beautySetting.setEnable(mBeautyEnabled);
            mRTCManager.setBeauty(beautySetting);
        }
        return mBeautyEnabled;
    }

    @Override
    public void onJoinedRoom() {
        mIsAdmin = isAdmin();
        if (mIsAdmin) {
            mMergeStreamPosition = new String[9];
        }
        onConnectedInternal();
        mRTCManager.publish();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mControlFragment.startTimer();
            }
        });
    }

    @Override
    public void onLocalPublished() {
        if (mIsAdmin) {
            mRTCManager.setMergeStreamLayout(mUserId, 0, 0, 0, QNAppServer.STREAMING_WIDTH, QNAppServer.STREAMING_HEIGHT);
        }
        subscribeAllRemoteStreams();
        mRTCManager.setStatisticsInfoEnabled(mUserId, true, 3000);
    }

    @Override
    public void onSubscribed(String userId) {
        Log.i(TAG, "onSubscribed: userId: " + userId);
        updateRemoteLogText("onSubscribed : " + userId);
        setMergeRemoteStreamLayout(userId);
        mRTCManager.setStatisticsInfoEnabled(userId, true, 3000);
    }

    @Override
    public void onRemotePublished(String userId, boolean hasAudio, boolean hasVideo) {
        Log.i(TAG, "onRemotePublished: userId: " + userId);
        updateRemoteLogText("onRemotePublished : " + userId + " hasAudio : " + hasAudio + " hasVideo : " + hasVideo);
        mRTCManager.subscribe(userId);
        mRTCManager.addRemoteAudioCallback(userId, new QNRemoteAudioCallback() {
            @Override
            public void onRemoteAudioAvailable(String userId, ByteBuffer audioData, int size, int bitsPerSample, int sampleRate, int numberOfChannels) {
            }
        });
    }

    @Override
    public QNRemoteSurfaceView onRemoteStreamAdded(final String userId, final boolean isAudioEnabled, final boolean isVideoEnabled,
                                                   final boolean isAudioMuted, final boolean isVideoMuted) {
        Log.i(TAG, "onRemoteStreamAdded: user = " + userId + ", hasAudio = " + isAudioEnabled + ", hasVideo = " + isVideoEnabled
                + ", isAudioMuted = " + isAudioMuted + ", isVideoMuted = " + isVideoMuted);
        updateRemoteLogText("onRemoteStreamAdded : " + userId);

        final RTCVideoView remoteWindow = mUnusedWindowList.remove(0);
        remoteWindow.setUserId(userId);
        mUserWindowMap.put(userId, remoteWindow);
        mUsedWindowList.add(remoteWindow);
        final int userCount = mUsedWindowList.size();

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                remoteWindow.setVisible(true);
                remoteWindow.updateMicrophoneStateView(isAudioMuted);
                if (isVideoMuted || !isVideoEnabled) {
                    remoteWindow.setAudioViewVisible(mUsedWindowList.indexOf(remoteWindow));
                }

                if (userCount <= 5) {
                    toggleToMultiUsersUI(userCount, mUsedWindowList);
                } else {
                    setTargetWindowParams(userCount, userCount - 1, remoteWindow);
                }
                if (userCount >= 3) {
                    mCallControlFragmentVisible = true;
                    FragmentTransaction ft = getFragmentManager().beginTransaction();
                    ft.show(mControlFragment);
                    ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
                    ft.commitAllowingStateLoss();
                }
            }
        });
        return remoteWindow.getRemoteSurfaceView();
    }

    @Override
    public void onRemoteStreamRemoved(final String userId) {
        Log.i(TAG, "onRemoteStreamRemoved: " + userId);
        updateRemoteLogText("onRemoteStreamRemoved : " + userId);
        clearMergeStreamPos(userId);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mUserWindowMap.containsKey(userId)) {
                    RTCVideoView remoteVideoView = mUserWindowMap.remove(userId);
                    remoteVideoView.setVisible(false);
                    mUsedWindowList.remove(remoteVideoView);
                    mUnusedWindowList.add(remoteVideoView);
                }
                toggleToMultiUsersUI(mUsedWindowList.size(), mUsedWindowList);
            }
        });
    }

    @Override
    public void onRemoteUserLeaved(String userId) {
        Log.i(TAG, "onUserOut: " + userId);
        updateRemoteLogText("onRemoteUserLeaved : " + userId);
    }

    @Override
    public void onRemoteUserJoined(String userId) {
        Log.i(TAG, "onUserIn: " + userId);
        updateRemoteLogText("onRemoteUserJoined : " + userId);
    }

    @Override
    public void onRemoteUnpublished(String userId) {
        Log.i(TAG, "onRemoteUnpublish: " + userId);
        updateRemoteLogText("onRemoteUnpublished : " + userId);
    }

    @Override
    public void onRemoteMute(final String userId, final boolean isAudioMuted, final boolean isVideoMuted) {
        Log.i(TAG, "onRemoteMute: user = " + userId + ", audio = " + isAudioMuted + ", video = " + isVideoMuted);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                RTCVideoView remoteWindow = getWindowByUserId(userId);
                if (remoteWindow != null) {
                    if (isVideoMuted && remoteWindow.getAudioViewVisibility() != View.VISIBLE) {
                        remoteWindow.setAudioViewVisible(mUsedWindowList.indexOf(remoteWindow));
                    } else if (!isVideoMuted && remoteWindow.getAudioViewVisibility() != View.INVISIBLE) {
                        remoteWindow.setAudioViewInvisible();
                    }
                    remoteWindow.updateMicrophoneStateView(isAudioMuted);
                }
            }
        });
    }

    @Override
    public void onStateChanged(QNRoomState state) {
        Log.i(TAG, "onStateChanged: " + state);
        updateRemoteLogText("onStateChanged : " + state.name());
        switch (state) {
            case RECONNECTING:
                mCallStartedTimeMs = System.currentTimeMillis();
                logAndToast(getString(R.string.reconnecting_to_room));
                break;
            case CONNECTED:
                break;
        }
    }

    @Override
    public void onError(final int errorCode, String description) {
        Log.i(TAG, "onError: " + errorCode + " " + description);
        updateRemoteLogText("onError : " + errorCode + " " + description);
        switch (errorCode) {
            case ERROR_KICKED_OUT_OF_ROOM:
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ToastUtils.s(RoomActivity.this, getString(R.string.kicked_by_admin));
                    }
                });
                onCallHangUp();
                break;
            default:
                reportError("errorCode: " + errorCode + "\ndescription: \n" + description);
                break;
        }
    }

    @Override
    public void onStatisticsUpdated(QNStatisticsReport report) {
        Log.d(TAG, "onStatisticsUpdated: " + report.toString());
        if (!mUserId.equals(report.userId)) {
            return;
        }
        mLocalLogText = String.format(getString(R.string.log_text), report.userId, report.frameRate, report.videoBitrate / 1000, report.audioBitrate / 1000, report.videoPacketLostRate, report.audioPacketLostRate, report.width, report.height);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mControlFragment.updateLocalLogText(mLocalLogText);
            }
        });
    }

    @Override
    public void onUserKickedOut(String userId) {
        Log.i(TAG, "kicked out user: " + userId);
        updateRemoteLogText("onUserKickedOut : " + userId);
    }

    private RTCVideoView.OnLongClickListener mOnLongClickListener = new RTCVideoView.OnLongClickListener() {
        @Override
        public void onLongClick(String userId) {
            if (!mIsAdmin) {
                Log.i(TAG, "Only admin user can kick a player!");
                return;
            }
            showKickoutDialog(userId);
        }
    };
}