package com.iuciangb.androidvideoextractcombinedemo;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.media.MediaMuxer;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;

/**
 * @author YY
 * @create 2019/4/9
 * @Describe
 **/
public class VideoExtractor {
    private final static String TYPE_VIDEO = "video/";
    private final static String TYPE_AUDIO = "audio/";

    MediaExtractor mMediaExtractor;
    MediaExtractor mAudioExtractor;
    MediaMuxer mMediaMuxer;

    /**
     * 裁剪影片
     * @param srcPath 來源檔案路徑含檔名
     * @param dstPath 目的地檔案路徑含檔名
     * @param startMs 從第幾秒開始，startMs之前的裁掉
     * @param endMs 裁剪到原影片的第幾秒
     * @param useAudio
     * @param useVideo
     */
    public void cutVideo2(String srcPath, String dstPath, int startMs, int endMs, boolean useAudio, boolean useVideo) {
        // Set up MediaExtractor to read from the source.
        try {
            mMediaExtractor = new MediaExtractor();
            mMediaExtractor.setDataSource(srcPath);
            // Set up MediaMuxer for the destination.
            mMediaMuxer = new MediaMuxer(dstPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        } catch (IOException e) {
            Log.d("/////", e.getMessage());
        }

        // Set up the tracks and retrieve the max buffer size for selected
        // tracks.
        int trackCount = mMediaExtractor.getTrackCount();
        HashMap<Integer, Integer> indexMap = new HashMap<>(trackCount);
        int bufferSize = -1;
        for (int i = 0; i < trackCount; i++) {
            MediaFormat format = mMediaExtractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);
            boolean selectCurrentTrack = false;
            if (mime.startsWith(TYPE_AUDIO) && useAudio) {
                selectCurrentTrack = true;
            } else if (mime.startsWith(TYPE_VIDEO) && useVideo) {
                selectCurrentTrack = true;
            }
            if (selectCurrentTrack) {
                mMediaExtractor.selectTrack(i);
                int dstIndex = mMediaMuxer.addTrack(format);
                indexMap.put(i, dstIndex);
                if (format.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
                    int newSize = format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE);
                    bufferSize = newSize > bufferSize ? newSize : bufferSize;
                }
            }
        }

        if (bufferSize < 0) {
            bufferSize = 1080 * 1920 * 30; // todo
        }

        // Set up the orientation and starting time for extractor.
        MediaMetadataRetriever retrieverSrc = new MediaMetadataRetriever();
        retrieverSrc.setDataSource(srcPath);
        String degreesString = retrieverSrc.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION);
        if (degreesString != null) {
            int degrees = Integer.parseInt(degreesString);
            if (degrees >= 0) {
                mMediaMuxer.setOrientationHint(degrees);
            }
        }
        if (startMs > 0) {
            mMediaExtractor.seekTo(startMs * 1000, MediaExtractor.SEEK_TO_NEXT_SYNC);
        }
        // Copy the samples from MediaExtractor to MediaMuxer. We will loop
        // for copying each sample and stop when we get to the end of the source
        // file or exceed the end time of the trimming.
        int offset = 0;
        int trackIndex = -1;
        ByteBuffer dstBuf = ByteBuffer.allocate(bufferSize);
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        try {
            mMediaMuxer.start();
            while (true) {
                bufferInfo.offset = offset;
                bufferInfo.size = mMediaExtractor.readSampleData(dstBuf, offset);
                if (bufferInfo.size < 0) {
                    Log.d("/////", "Saw input EOS.");
                    bufferInfo.size = 0;
                    break;
                } else {
                    bufferInfo.presentationTimeUs = mMediaExtractor.getSampleTime();
                    if (endMs > 0 && bufferInfo.presentationTimeUs > (endMs * 1000)) {
                        Log.d("/////", "The current sample is over the trim end time.");
                        break;
                    } else {
                        bufferInfo.flags = mMediaExtractor.getSampleFlags();
                        trackIndex = mMediaExtractor.getSampleTrackIndex();
                        mMediaMuxer.writeSampleData(indexMap.get(trackIndex), dstBuf, bufferInfo);
                        mMediaExtractor.advance();
                    }
                }
            }
        } catch (Exception e) {
            // Swallow the exception due to malformed source.
            Log.w("/////", "The source video file is malformed");
        } finally {
            finishAll();
        }
    }

    /**
     * 單純提取音頻(無影像只有聲音)
     */
    public void extractOnlyAudio(String path, String inputFileName, String outputAudioName) {
        mMediaExtractor = new MediaExtractor();
        int audioIndex = -1;
        try {
            mMediaExtractor.setDataSource(path + inputFileName);
            int trackCount = mMediaExtractor.getTrackCount();
            for (int i = 0; i < trackCount; i++) {
                MediaFormat trackFormat = mMediaExtractor.getTrackFormat(i);
                if (trackFormat.getString(MediaFormat.KEY_MIME).startsWith(TYPE_AUDIO)) {
                    audioIndex = i;
                }
            }
            mMediaExtractor.selectTrack(audioIndex);
            MediaFormat trackFormat = mMediaExtractor.getTrackFormat(audioIndex);
            mMediaMuxer = new MediaMuxer(path + outputAudioName, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            int writeAudioIndex = mMediaMuxer.addTrack(trackFormat);
            mMediaMuxer.start();
            ByteBuffer byteBuffer = ByteBuffer.allocate(500 * 1024);
            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();

            long stampTime = 0;
            //获取帧之间的间隔时间
            {
                mMediaExtractor.readSampleData(byteBuffer, 0);
                if (mMediaExtractor.getSampleFlags() == MediaExtractor.SAMPLE_FLAG_SYNC) {
                    mMediaExtractor.advance();
                }
                mMediaExtractor.readSampleData(byteBuffer, 0);
                long secondTime = mMediaExtractor.getSampleTime();
                mMediaExtractor.advance();
                mMediaExtractor.readSampleData(byteBuffer, 0);
                long thirdTime = mMediaExtractor.getSampleTime();
                stampTime = Math.abs(thirdTime - secondTime);
                Log.e("/////", stampTime + "");
            }

            mMediaExtractor.unselectTrack(audioIndex);
            mMediaExtractor.selectTrack(audioIndex);
            while (true) {
                int readSampleSize = mMediaExtractor.readSampleData(byteBuffer, 0);
                if (readSampleSize < 0) {
                    break;
                }
                mMediaExtractor.advance();

                bufferInfo.size = readSampleSize;
                bufferInfo.flags = mMediaExtractor.getSampleFlags();
                bufferInfo.offset = 0;
                bufferInfo.presentationTimeUs += stampTime;

                mMediaMuxer.writeSampleData(writeAudioIndex, byteBuffer, bufferInfo);
            }
            finishAll();
            Log.e("/////", "finish");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 把input.mp4分離成兩個檔案
     * 一個只有影像沒有聲音
     * 一個只有聲音沒有影像
     */
    public void muxerData(String path, String inputFileName, String outputVideoName, String outputAudioName) {
        String srcPath = path + inputFileName;

        String dirP = path + "demo2";
        String fPath1 = path + "demo2/" + outputVideoName;
        String fPath2 = path + "demo2/" + outputAudioName;
        File file = new File(dirP);
        if (!file.exists()) {
            file.mkdir();
        }

        File file1 = new File(fPath1);
        File file2 = new File(fPath2);
        try {
            if (file1.exists()) {
                file1.delete();

            }
            file1.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            if (file2.exists()) {
                file2.delete();
            }
            file2.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        int mVideoTrackIndex = 0;
        int mAudioTrackIndex = 0;
        long frameRate;

        try {
            mMediaExtractor = new MediaExtractor();//此类可分离视频文件的音轨和视频轨道
            mMediaExtractor.setDataSource(srcPath);//媒体文件的位置
            Log.d("/////", "getTrackCount()=" + mMediaExtractor.getTrackCount());
            for (int i = 0; i < mMediaExtractor.getTrackCount(); i++) {
                MediaFormat format = mMediaExtractor.getTrackFormat(i);
                String mime = format.getString(MediaFormat.KEY_MIME);
                if (mime.startsWith(TYPE_AUDIO)) {//获取音频轨道
                    ByteBuffer buffer = ByteBuffer.allocate(100 * 1024);
                    {
                        mMediaExtractor.selectTrack(i);//选择此音频轨道
                        mMediaExtractor.readSampleData(buffer, 0);
                        long first_sampletime = mMediaExtractor.getSampleTime();
                        mMediaExtractor.advance();
                        long second_sampletime = mMediaExtractor.getSampleTime();
                        frameRate = Math.abs(second_sampletime - first_sampletime);//时间戳
                        mMediaExtractor.unselectTrack(i);
                    }
                    mMediaExtractor.selectTrack(i);
                    Log.d("/////", "frameRate111=" + frameRate);
                    mMediaMuxer = new MediaMuxer(fPath2, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
                    mAudioTrackIndex = mMediaMuxer.addTrack(format);
                    mMediaMuxer.start();

                    MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
                    info.presentationTimeUs = 0;

                    int sampleSize = 0;
                    while ((sampleSize = mMediaExtractor.readSampleData(buffer, 0)) > 0) {
                        info.offset = 0;
                        info.size = sampleSize;
                        info.flags = mMediaExtractor.getSampleFlags();
                        info.presentationTimeUs += frameRate;
                        mMediaMuxer.writeSampleData(mAudioTrackIndex, buffer, info);
                        mMediaExtractor.advance();
                    }

                    mMediaMuxer.stop();
                    mMediaMuxer.release();

                }

                if (mime.startsWith(TYPE_VIDEO)) {
                    mMediaExtractor.selectTrack(i);//选择此视频轨道
                    frameRate = format.getInteger(MediaFormat.KEY_FRAME_RATE);
                    Log.d("/////", "frameRate222=" + 1000 * 1000 / frameRate);
                    mMediaMuxer = new MediaMuxer(fPath1, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
                    mVideoTrackIndex = mMediaMuxer.addTrack(format);
                    mMediaMuxer.start();

                    MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
                    info.presentationTimeUs = 0;
                    ByteBuffer buffer = ByteBuffer.allocate(100 * 1024);
                    int sampleSize = 0;
                    while ((sampleSize = mMediaExtractor.readSampleData(buffer, 0)) > 0) {
                        info.offset = 0;
                        info.size = sampleSize;
                        info.flags = mMediaExtractor.getSampleFlags();
                        info.presentationTimeUs += 1000 * 1000 / frameRate;
                        mMediaMuxer.writeSampleData(mVideoTrackIndex, buffer, info);
                        mMediaExtractor.advance();
                    }

                    mMediaMuxer.stop();
                    mMediaMuxer.release();

                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            mMediaExtractor.release();
            mMediaExtractor = null;
        }

    }

    /**
     * 把分離出來的單純聲音與單純影像檔案合成一個完整有聲音的影像
     */
    public void mergeMediaAndAudioData(String path, String destinationFileName, String onlyVideoFileName, String onlyAudioFileName) {
        String desPath = path + "demo2/" + destinationFileName;

        String dirP = path + "demo2";
        String fPath1 = path + "demo2/" + onlyVideoFileName;
        String fPath2 = path + "demo2/" + onlyAudioFileName;
        File file = new File(dirP);
        if (!file.exists()) {
            file.mkdir();
        }

        File files = new File(desPath);

        try {
            if (files.exists()) {
                files.delete();
            }
            files.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        MediaMuxer mMediaMuxer = null;

        try {
            mMediaMuxer = new MediaMuxer(desPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        } catch (IOException e) {
            e.printStackTrace();
        }
        int mVideoTrackIndex = 0;
        int mAudioTrackIndex = 0;
        long frameRate1 = 0;
        long frameRate2 = 0;

        MediaFormat format1;
        MediaFormat format2;
        try {
            mMediaExtractor = new MediaExtractor();//此类可分离视频文件的音轨和视频轨道
            mMediaExtractor.setDataSource(fPath1);//媒体文件的位置
            Log.d("/////", "getTrackCount()=" + mMediaExtractor.getTrackCount());
            for (int i = 0; i < mMediaExtractor.getTrackCount(); i++) {
                format1 = mMediaExtractor.getTrackFormat(i);
                String mime = format1.getString(MediaFormat.KEY_MIME);

                if (mime.startsWith(TYPE_VIDEO)) {
                    mMediaExtractor.selectTrack(i);//选择此视频轨道
                    frameRate1 = format1.getInteger(MediaFormat.KEY_FRAME_RATE);
                    Log.d("/////", "frameRate222=" + 1000 * 1000 / frameRate1);
                    mVideoTrackIndex = mMediaMuxer.addTrack(format1);

                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        MediaExtractor mediaExtractor2 = null;
        try {
            mediaExtractor2 = new MediaExtractor();//此类可分离视频文件的音轨和视频轨道
            mediaExtractor2.setDataSource(fPath2);//媒体文件的位置
            Log.d("/////", "getTrackCount()=" + mediaExtractor2.getTrackCount());
            for (int i = 0; i < mediaExtractor2.getTrackCount(); i++) {
                format2 = mediaExtractor2.getTrackFormat(i);
                String mime = format2.getString(MediaFormat.KEY_MIME);
                if (mime.startsWith(TYPE_AUDIO)) {//获取音频轨道
                    ByteBuffer buffer = ByteBuffer.allocate(100 * 1024);
                    {
                        mediaExtractor2.selectTrack(i);//选择此音频轨道
                        mediaExtractor2.readSampleData(buffer, 0);
                        long first_sampletime = mediaExtractor2.getSampleTime();
                        mediaExtractor2.advance();
                        long second_sampletime = mediaExtractor2.getSampleTime();
                        frameRate2 = Math.abs(second_sampletime - first_sampletime);//时间戳
                        mediaExtractor2.unselectTrack(i);
                    }
                    mediaExtractor2.selectTrack(i);
                    Log.d("/////", "frameRate111" + frameRate2);
                    mAudioTrackIndex = mMediaMuxer.addTrack(format2);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        mMediaMuxer.start();
        MediaCodec.BufferInfo info1 = new MediaCodec.BufferInfo();
        info1.presentationTimeUs = 0;
        ByteBuffer buffer = ByteBuffer.allocate(100 * 1024);
        int sampleSize1 = 0;
        while ((sampleSize1 = mMediaExtractor.readSampleData(buffer, 0)) > 0) {
            info1.offset = 0;
            info1.size = sampleSize1;
            info1.flags = mMediaExtractor.getSampleFlags();
            info1.presentationTimeUs += 1000 * 1000 / frameRate1;
            mMediaMuxer.writeSampleData(mVideoTrackIndex, buffer, info1);
            mMediaExtractor.advance();
        }

        MediaCodec.BufferInfo info2 = new MediaCodec.BufferInfo();
        info2.presentationTimeUs = 0;

        int sampleSize2 = 0;
        while ((sampleSize2 = mediaExtractor2.readSampleData(buffer, 0)) > 0) {
            info2.offset = 0;
            info2.size = sampleSize2;
            info2.flags = mediaExtractor2.getSampleFlags();
            info2.presentationTimeUs += frameRate2;
            mMediaMuxer.writeSampleData(mAudioTrackIndex, buffer, info2);
            mediaExtractor2.advance();
        }
    }

    /**
     * 裁剪影片(看不懂哪邊可以裁剪前的區間，但是較簡短，留待以後研究)
     */
    public void cutVideo(String path, String inputFileName, String outputVideoName) {
        mMediaExtractor = new MediaExtractor();
        int videoIndex = -1;
        try {
            mMediaExtractor.setDataSource(path + inputFileName);
            int trackCount = mMediaExtractor.getTrackCount();
            for (int i = 0; i < trackCount; i++) {
                MediaFormat trackFormat = mMediaExtractor.getTrackFormat(i);
                String mimeType = trackFormat.getString(MediaFormat.KEY_MIME);
                if (mimeType.startsWith(TYPE_VIDEO)) {
                    videoIndex = i;
                }
            }

            mMediaExtractor.selectTrack(videoIndex);
            MediaFormat trackFormat = mMediaExtractor.getTrackFormat(videoIndex);
            mMediaMuxer = new MediaMuxer(path + outputVideoName, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            int trackIndex = mMediaMuxer.addTrack(trackFormat);
            //分配緩衝
            ByteBuffer byteBuffer = ByteBuffer.allocate(1024 * 500);
            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
            mMediaMuxer.start();
            long videoSampleTime;
            //取得source video相鄰幀之間的時間間隔
            {
                mMediaExtractor.readSampleData(byteBuffer, 0);
                //skip first I frame
                if (mMediaExtractor.getSampleFlags() == MediaExtractor.SAMPLE_FLAG_SYNC) {
                    mMediaExtractor.advance();
                }
                mMediaExtractor.readSampleData(byteBuffer, 0);
                long firstVideoPTS = mMediaExtractor.getSampleTime();
                mMediaExtractor.advance();
                mMediaExtractor.readSampleData(byteBuffer, 0);
                long SecondVideoPTS = mMediaExtractor.getSampleTime();
                videoSampleTime = Math.abs(SecondVideoPTS - firstVideoPTS);
                Log.d("/////", "videoSampleTime is " + videoSampleTime);
            }

            mMediaExtractor.unselectTrack(videoIndex);
            mMediaExtractor.selectTrack(videoIndex);
            while (true) {
                int readSampleSize = mMediaExtractor.readSampleData(byteBuffer, 0);
                if (readSampleSize < 0) {
                    break;
                }
                mMediaExtractor.advance();
                bufferInfo.size = readSampleSize;
                bufferInfo.offset = 0;
                bufferInfo.flags = mMediaExtractor.getSampleFlags();
                bufferInfo.presentationTimeUs += videoSampleTime;

                mMediaMuxer.writeSampleData(trackIndex, byteBuffer, bufferInfo);
            }
            finishAll();

            Log.e("TAG", "finish");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void finishAll() {
        if (mMediaMuxer != null) {
            mMediaMuxer.stop();
            mMediaMuxer.release();
            mMediaMuxer = null;
        }
        if (mMediaExtractor != null) {
            mMediaExtractor.release();
            mMediaExtractor = null;
        }
        if (mAudioExtractor != null) {
            mAudioExtractor.release();
            mAudioExtractor = null;
        }
    }
}
