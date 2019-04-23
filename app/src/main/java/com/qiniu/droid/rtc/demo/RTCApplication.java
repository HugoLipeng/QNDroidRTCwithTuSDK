package com.qiniu.droid.rtc.demo;

import android.app.Application;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.qiniu.droid.rtc.QNLogLevel;
import com.qiniu.droid.rtc.QNRTCEnv;

import org.lasque.tusdk.core.TuSdk;

public class RTCApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        QNRTCEnv.setLogLevel(QNLogLevel.INFO);
        /**
         * init must be called before any other func
         */
        QNRTCEnv.init(getApplicationContext());

        /**
         ************************* TuSDK 集成三部曲 *************************
         *
         * 1. 在官网注册开发者账户
         *
         * 2. 下载SDK和示例代码
         *
         * 3. 创建应用，获取appkey，导出资源包
         *
         ************************* TuSDK 集成三部曲 *************************
         *
         * 关于TuSDK体积 (约2M大小)
         *
         * Android 编译知识：
         * APK文件包含了Java代码，JNI库和资源文件；
         * JNI库包含arm64-v8a,armeabi等不同CPU的编译结果的集合，这些都会编译进 APK 文件；
         * 在安装应用时，系统会自动选择最合适的JNI版本，其他版本不会占用空间；
         * 参考TuSDK Demo的APK 大小，除去资源和JNI库，SDK本身的大小约2M；
         *
         * 开发文档:http://tusdk.com/doc
         */
        ImageLoader.getInstance().init(ImageLoaderConfiguration.createDefault(this));

        // 设置资源类，当 Application id 与 Package Name 不相同时，必须手动调用该方法, 且在 init 之前执行。
         TuSdk.setResourcePackageClazz(com.qiniu.droid.rtc.demo.R.class);

        // 自定义 .so 文件路径，在 init 之前调用
        // NativeLibraryHelper.shared().mapLibrary(NativeLibType.LIB_CORE, "libtusdk-library.so 文件路径");
        // NativeLibraryHelper.shared().mapLibrary(NativeLibType.LIB_IMAGE, "libtusdk-image.so 文件路径");

        // 设置输出状态，建议在接入阶段开启该选项，以便定位问题。
        TuSdk.enableDebugLog(true);

        /**
         *  初始化SDK，应用密钥是您的应用在 TuSDK 的唯一标识符。每个应用的包名(Bundle Identifier)、密钥、资源包(滤镜、贴纸等)三者需要匹配，否则将会报错。
         *
         *  @param appkey 应用秘钥 (请前往 http://tusdk.com 申请秘钥)
         */
        TuSdk.init(getApplicationContext(),"7096e3f39bab18c7-03-bshmr1");
    }
}
