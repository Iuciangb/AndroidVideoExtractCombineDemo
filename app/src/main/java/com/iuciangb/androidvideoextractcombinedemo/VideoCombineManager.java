package com.iuciangb.androidvideoextractcombinedemo;

import java.util.List;

/**
 * @author YY
 * @create 2019/4/9
 * @Describe
 **/
public class VideoCombineManager {
    private static final String TAG = "VideoCombineManage";

    private static VideoCombineManager mInstance;


    public static VideoCombineManager getInstance() {
        if (mInstance == null) {
            mInstance = new VideoCombineManager();
        }
        return mInstance;
    }

    /**
     * 初始化媒体合并器
     * @param videoPath
     * @param destPath
     */
    public void startVideoCombiner(final List<String> videoPath, final String destPath,
                                   final VideoCombiner.VideoCombineListener listener) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                VideoCombiner videoCombiner = new VideoCombiner(videoPath, destPath, listener);
                videoCombiner.combineVideo();
            }
        }).start();
    }
}