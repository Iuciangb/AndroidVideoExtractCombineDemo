package com.iuciangb.androidvideoextractcombinedemo;

import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import java.util.ArrayList;
import java.util.List;

/**
 * Reference
 * http://www.manongjc.com/article/45212.html
 * https://blog.csdn.net/u010126792/article/details/86497571
 */
public class MainActivity extends AppCompatActivity {
    private final static String SYMBOL_SLASH = "/";
    private final static String SDCARD_PATH = Environment.getExternalStorageDirectory().getPath() + SYMBOL_SLASH + "download" + SYMBOL_SLASH;
    private final static String INPUT_FILE_NAME = "netflix.mp4";
    private final static String MERGED_FILE_NAME = "video.mp4";
    private final static String OUTPUT_VIDEO_NAME = "output_video.mp4";
    private final static String OUTPUT_AUDIO_NAME = "output_audio.mp4";
    private final static String COMBINED_FIRST_VIDEO_PATH = SDCARD_PATH + SYMBOL_SLASH + "demo2" + SYMBOL_SLASH + "video.mp4";
    private final static String COMBINED_SECOND_VIDEO_PATH = SDCARD_PATH + SYMBOL_SLASH + "demo2" + SYMBOL_SLASH + "video2.mp4";
    private final static String COMBINED_DESTINATION_PATH = SDCARD_PATH + SYMBOL_SLASH + "demo2" + SYMBOL_SLASH + "combined.mp4";

    Button mExtractorButton;
    Button mMuxerMediaButton;
    Button mMuxerAudioButton;
    Button mMergeDataButton;
    Button mCombineVideoButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        PermissionHelper.requestPermission(this);

        mExtractorButton = findViewById(R.id.exactor);
        mMuxerMediaButton = findViewById(R.id.muxer);
        mMuxerAudioButton = findViewById(R.id.muxer_audio);
        mMergeDataButton = findViewById(R.id.merge_data);
        mCombineVideoButton = findViewById(R.id.combine_video);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mExtractorButton.setOnClickListener(mOnClickListener);
        mMuxerMediaButton.setOnClickListener(mOnClickListener);
        mMuxerAudioButton.setOnClickListener(mOnClickListener);
        mMergeDataButton.setOnClickListener(mOnClickListener);
        mCombineVideoButton.setOnClickListener(mOnClickListener);
    }

    /**
     * 將兩個影片合成一個
     */
    private void combineVideo() {

        List<String> videoPathList = new ArrayList<>();
        videoPathList.add(COMBINED_FIRST_VIDEO_PATH);
        videoPathList.add(COMBINED_SECOND_VIDEO_PATH);

        VideoCombineManager.getInstance().startVideoCombiner(videoPathList, COMBINED_DESTINATION_PATH, new VideoCombiner.VideoCombineListener() {
            @Override
            public void onCombineStart() {

            }

            @Override
            public void onCombineProcessing(int current, int sum) {

            }

            @Override
            public void onCombineFinished(boolean success) {

            }
        });
    }

    private View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (v == mExtractorButton) {
                VideoExtractorManager.getInstance().startExtractMediaAndAudio(SDCARD_PATH, INPUT_FILE_NAME, OUTPUT_VIDEO_NAME, OUTPUT_AUDIO_NAME);
            } else if (v == mMuxerMediaButton) {
                VideoExtractorManager.getInstance().startCutVideo2(SDCARD_PATH + INPUT_FILE_NAME, SDCARD_PATH + "demo2" + OUTPUT_VIDEO_NAME,10000, 15000);
//                VideoExtractorManager.getInstance().startCutVideo(SDCARD_PATH, INPUT_FILE_NAME, OUTPUT_VIDEO_NAME);
            } else if (v == mMuxerAudioButton) {
                VideoExtractorManager.getInstance().startExtractAudio(SDCARD_PATH, INPUT_FILE_NAME, OUTPUT_AUDIO_NAME);
            } else if (v == mMergeDataButton) {
                VideoExtractorManager.getInstance().startMergeMediaAndAudio(SDCARD_PATH, MERGED_FILE_NAME, OUTPUT_VIDEO_NAME, OUTPUT_AUDIO_NAME);
            } else if (v == mCombineVideoButton) {
                combineVideo();
            }
        }
    };
}