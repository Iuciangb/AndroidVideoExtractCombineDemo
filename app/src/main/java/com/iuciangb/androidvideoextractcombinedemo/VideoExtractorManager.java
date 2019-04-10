package com.iuciangb.androidvideoextractcombinedemo;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import java.lang.ref.WeakReference;

/**
 * @author YY
 * @create 2019/4/9
 * @Describe
 **/
public class VideoExtractorManager {
    private final static String KEY_PATH = "key_path";
    private final static String KEY_DESTINATION_PATH = "key_destination_path";
    private final static String KEY_INPUT_FILE_NAME = "key_input_file_name";
    private final static String KEY_OUTPUT_MEDIA_NAME = "key_output_media_name";
    private final static String KEY_OUTPUT_AUDIO_NAME = "key_output_audio_name";
    private final static String KEY_DESTINATION_FILE_NAME = "key_destination_file_name";
    private final static String KEY_ONLY_MEDIA_FILE_NAME = "key_only_video_file_name";
    private final static String KEY_ONLY_AUDIO_FILE_NAME = "key_only_audio_file_name";
    private final static String KEY_START_MILLIS = "key_start_millis";
    private final static String KEY_END_MILLIS = "key_end_millis";
    private final static int TYPE_CUT_VIDEO = 1;
    private final static int TYPE_CUT_VIDEO2 = 22;
    private final static int TYPE_EXTRACT_AUDIO = 2;
    private final static int TYPE_EXTRACT_MEDIA_AND_AUDIO = 3;
    private final static int TYPE_MERGE_MEDIA_AND_AUDIO = 4;

    private static VideoExtractorManager sVideoExtractorManager;
    private ExtractHandler mExtractHandler;
    private VideoExtractor mVideoExtractor;

    public static VideoExtractorManager getInstance() {
        if (sVideoExtractorManager != null) {
            return sVideoExtractorManager;
        }
        synchronized (VideoExtractorManager.class) {
            if (sVideoExtractorManager != null) {
                return sVideoExtractorManager;
            }
            sVideoExtractorManager = new VideoExtractorManager();
        }
        return sVideoExtractorManager;
    }

    public VideoExtractorManager() {
        mExtractHandler = new ExtractHandler(this);
        mVideoExtractor = new VideoExtractor();
    }

    public void startCutVideo(final String path, final String inputFileName, final String outputMediaName) {
        Bundle bundle = new Bundle();
        bundle.putString(KEY_PATH, path);
        bundle.putString(KEY_INPUT_FILE_NAME, inputFileName);
        bundle.putString(KEY_OUTPUT_MEDIA_NAME, outputMediaName);

        Message message = mExtractHandler.obtainMessage();
        message.what = TYPE_CUT_VIDEO;
        message.setData(bundle);

        mExtractHandler.sendMessage(message);
    }

    public void startCutVideo2(final String path, String destinationPath, int startMillis, int endMillis) {
        Bundle bundle = new Bundle();
        bundle.putString(KEY_PATH, path);
        bundle.putString(KEY_DESTINATION_PATH, destinationPath);
        bundle.putInt(KEY_START_MILLIS, startMillis);
        bundle.putInt(KEY_END_MILLIS, endMillis);

        Message message = mExtractHandler.obtainMessage();
        message.what = TYPE_CUT_VIDEO2;
        message.setData(bundle);

        mExtractHandler.sendMessage(message);
    }

    public void startExtractAudio(final String path, final String inputFileName, final String outputAudioName) {
        Bundle bundle = new Bundle();
        bundle.putString(KEY_PATH, path);
        bundle.putString(KEY_INPUT_FILE_NAME, inputFileName);
        bundle.putString(KEY_OUTPUT_AUDIO_NAME, outputAudioName);

        Message message = mExtractHandler.obtainMessage();
        message.what = TYPE_EXTRACT_AUDIO;
        message.setData(bundle);

        mExtractHandler.sendMessage(message);
    }

    public void startExtractMediaAndAudio(final String path, final String inputFileName, final String outputMediaName, final String outputAudioName) {
        Bundle bundle = new Bundle();
        bundle.putString(KEY_PATH, path);
        bundle.putString(KEY_INPUT_FILE_NAME, inputFileName);
        bundle.putString(KEY_OUTPUT_MEDIA_NAME, outputMediaName);
        bundle.putString(KEY_OUTPUT_AUDIO_NAME, outputAudioName);

        Message message = mExtractHandler.obtainMessage();
        message.what = TYPE_EXTRACT_MEDIA_AND_AUDIO;
        message.setData(bundle);

        mExtractHandler.sendMessage(message);
    }

    public void startMergeMediaAndAudio(final String path, final String destinationFileName, final String onlyMediaFileName, final String onlyAudioFileName) {
        Bundle bundle = new Bundle();
        bundle.putString(KEY_PATH, path);
        bundle.putString(KEY_DESTINATION_FILE_NAME, destinationFileName);
        bundle.putString(KEY_ONLY_MEDIA_FILE_NAME, onlyMediaFileName);
        bundle.putString(KEY_ONLY_AUDIO_FILE_NAME, onlyAudioFileName);

        Message message = mExtractHandler.obtainMessage();
        message.what = TYPE_MERGE_MEDIA_AND_AUDIO;
        message.setData(bundle);

        mExtractHandler.sendMessage(message);
    }

    private class ExtractHandler extends Handler {
        private WeakReference<VideoExtractorManager> mVideoExtractorManagerWeakReference;

        public ExtractHandler(VideoExtractorManager videoExtractorManager) {
            mVideoExtractorManagerWeakReference = new WeakReference<>(videoExtractorManager);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            VideoExtractorManager videoExtractorManager = mVideoExtractorManagerWeakReference.get();
            if (videoExtractorManager == null) {
                return;
            }

            Bundle bundle = msg.getData();
            String path = bundle.getString(KEY_PATH);
            switch (msg.what) {
                case TYPE_CUT_VIDEO:
                    mVideoExtractor.cutVideo(path, bundle.getString(KEY_INPUT_FILE_NAME), bundle.getString(KEY_OUTPUT_MEDIA_NAME));
                    break;
                case TYPE_CUT_VIDEO2:
                    mVideoExtractor.cutVideo2(path, bundle.getString(KEY_DESTINATION_PATH), bundle.getInt(KEY_START_MILLIS), bundle.getInt(KEY_END_MILLIS), true, true);
                    break;
                case TYPE_EXTRACT_AUDIO:
                    mVideoExtractor.extractOnlyAudio(path, bundle.getString(KEY_INPUT_FILE_NAME), bundle.getString(KEY_OUTPUT_AUDIO_NAME));
                    break;
                case TYPE_EXTRACT_MEDIA_AND_AUDIO:
                    mVideoExtractor.muxerData(path, bundle.getString(KEY_INPUT_FILE_NAME), bundle.getString(KEY_OUTPUT_MEDIA_NAME), bundle.getString(KEY_OUTPUT_AUDIO_NAME));
                    break;
                case TYPE_MERGE_MEDIA_AND_AUDIO:
                    mVideoExtractor.mergeMediaAndAudioData(path, bundle.getString(KEY_DESTINATION_FILE_NAME), bundle.getString(KEY_ONLY_MEDIA_FILE_NAME), bundle.getString(KEY_ONLY_AUDIO_FILE_NAME));
                    break;
            }
        }
    }
}
