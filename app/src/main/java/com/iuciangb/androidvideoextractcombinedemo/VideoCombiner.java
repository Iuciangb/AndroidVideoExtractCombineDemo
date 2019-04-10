package com.iuciangb.androidvideoextractcombinedemo;

import android.annotation.SuppressLint;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.List;

/**
 * @author YY
 * @create 2019/4/9
 * @Describe
 **/
public class VideoCombiner {
    private static final String TAG = "/////";
    private static final boolean VERBOSE = true;

    // 最大緩衝區(1024 x 1024 = 1048576、1920 x 1080 = 2073600)
    // 由於沒有錄制1080P的视频，因此用1024的Buffer來緩存
    private static final int MAX_BUFFER_SIZE = 1048576;

    private List<String> mVideoList;
    private String mDestPath;

    private MediaMuxer mMuxer;
    private ByteBuffer mReadBuf;
    private int mOutAudioTrackIndex;
    private int mOutVideoTrackIndex;
    private MediaFormat mAudioFormat;
    private MediaFormat mVideoFormat;

    private VideoCombineListener mCombineListener;

    public interface VideoCombineListener {

        /**
         * 合併開始
         */
        void onCombineStart();

        /**
         * 合併過程
         *
         * @param current 當前合併的影片
         * @param sum     合併影片總數
         */
        void onCombineProcessing(int current, int sum);

        /**
         * 合併結束
         *
         * @param success 是否合併成功
         */
        void onCombineFinished(boolean success);
    }

    public VideoCombiner(List<String> videoList, String destPath, VideoCombineListener listener) {
        mVideoList = videoList;
        mDestPath = destPath;
        mCombineListener = listener;
        mReadBuf = ByteBuffer.allocate(MAX_BUFFER_SIZE);
    }

    /**
     * 合併影片
     *
     * @return
     */
    @SuppressLint("WrongConstant")
    public void combineVideo() {
        boolean hasAudioFormat = false;
        boolean hasVideoFormat = false;
        Iterator videoIterator = mVideoList.iterator();

        // 開始合併
        if (mCombineListener != null) {
            mCombineListener.onCombineStart();
        }

        // MediaExtractor拿到多媒體資訊，用於MediaMuxer創建文件
        while (videoIterator.hasNext()) {
            String videoPath = (String) videoIterator.next();
            MediaExtractor extractor = new MediaExtractor();
            try {
                extractor.setDataSource(videoPath);
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            int trackIndex;
            if (!hasVideoFormat) {
                trackIndex = selectTrack(extractor, "video/");
                if (trackIndex < 0) {
                    Log.e(TAG, "No video track found in " + videoPath);
                } else {
                    extractor.selectTrack(trackIndex);
                    mVideoFormat = extractor.getTrackFormat(trackIndex);
                    hasVideoFormat = true;
                }
            }

            if (!hasAudioFormat) {
                trackIndex = selectTrack(extractor, "audio/");
                if (trackIndex < 0) {
                    Log.e(TAG, "No audio track found in " + videoPath);
                } else {
                    extractor.selectTrack(trackIndex);
                    mAudioFormat = extractor.getTrackFormat(trackIndex);
                    hasAudioFormat = true;
                }
            }

            extractor.release();

            if (hasVideoFormat && hasAudioFormat) {
                break;
            }
        }

        try {
            mMuxer = new MediaMuxer(mDestPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (hasVideoFormat) {
            mOutVideoTrackIndex = mMuxer.addTrack(mVideoFormat);
        }

        if (hasAudioFormat) {
            mOutAudioTrackIndex = mMuxer.addTrack(mAudioFormat);
        }
        mMuxer.start();

        // MediaExtractor遍歷讀取幀，MediaMuxer寫入幀，並紀錄幀資訊
        long ptsOffset = 0L;
        Iterator trackIndex = mVideoList.iterator();
        int currentVideo = 0;
        boolean combineResult = true;
        while (trackIndex.hasNext()) {
            // 監聽當前合併第幾個影片
            currentVideo++;
            if (mCombineListener != null) {
                mCombineListener.onCombineProcessing(currentVideo, mVideoList.size());
            }

            String videoPath = (String) trackIndex.next();
            boolean hasVideo = true;
            boolean hasAudio = true;

            // 選擇Media軌道
            MediaExtractor videoExtractor = new MediaExtractor();
            try {
                videoExtractor.setDataSource(videoPath);
            } catch (Exception e) {
                e.printStackTrace();
            }
            int inVideoTrackIndex = selectTrack(videoExtractor, "video/");
            if (inVideoTrackIndex < 0) {
                hasVideo = false;
            }
            videoExtractor.selectTrack(inVideoTrackIndex);

            // 選擇Audio軌道
            MediaExtractor audioExtractor = new MediaExtractor();
            try {
                audioExtractor.setDataSource(videoPath);
            } catch (Exception e) {
                e.printStackTrace();
            }
            int inAudioTrackIndex = selectTrack(audioExtractor, "audio/");
            if (inAudioTrackIndex < 0) {
                hasAudio = false;
            }
            audioExtractor.selectTrack(inAudioTrackIndex);

            // 如果Media軌道和Audio軌道都不存在，則合併失敗，文件出錯
            if (!hasVideo && !hasAudio) {

                combineResult = false;

                videoExtractor.release();
                audioExtractor.release();

                break;
            }

            boolean bMediaDone = false;
            long presentationTimeUs = 0L;
            long audioPts = 0L;
            long videoPts = 0L;

            while (!bMediaDone) {
                // 判断是否存在音視頻
                if (!hasVideo && !hasAudio) {
                    break;
                }

                int outTrackIndex;
                MediaExtractor extractor;
                int currentTrackIndex;
                if ((!hasVideo || audioPts - videoPts <= 50000L) && hasAudio) {
                    currentTrackIndex = inAudioTrackIndex;
                    outTrackIndex = mOutAudioTrackIndex;
                    extractor = audioExtractor;
                } else {
                    currentTrackIndex = inVideoTrackIndex;
                    outTrackIndex = mOutVideoTrackIndex;
                    extractor = videoExtractor;
                }

                if (VERBOSE) {
                    Log.d(TAG, "currentTrackIndex： " + currentTrackIndex
                            + ", outTrackIndex: " + outTrackIndex);
                }

                mReadBuf.rewind();
                // 讀取幀資料
                int frameSize = extractor.readSampleData(mReadBuf, 0);
                if (frameSize < 0) {
                    if (currentTrackIndex == inVideoTrackIndex) {
                        hasVideo = false;
                    } else if (currentTrackIndex == inAudioTrackIndex) {
                        hasAudio = false;
                    }
                } else {
                    if (extractor.getSampleTrackIndex() != currentTrackIndex) {
                        Log.e(TAG, "got sample from track "
                                + extractor.getSampleTrackIndex()
                                + ", expected " + currentTrackIndex);
                    }

                    // 讀取幀的pts
                    presentationTimeUs = extractor.getSampleTime();
                    if (currentTrackIndex == inVideoTrackIndex) {
                        videoPts = presentationTimeUs;
                    } else {
                        audioPts = presentationTimeUs;
                    }

                    // 幀資訊
                    MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
                    info.offset = 0;
                    info.size = frameSize;
                    info.presentationTimeUs = ptsOffset + presentationTimeUs;

                    if ((extractor.getSampleFlags() & MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0) {
                        info.flags = MediaCodec.BUFFER_FLAG_KEY_FRAME;
                    }
                    mReadBuf.rewind();
                    if (VERBOSE) {
                        Log.d(TAG, String.format("write sample track %d, size %d, pts %d flag %d",
                                Integer.valueOf(outTrackIndex),
                                Integer.valueOf(info.size),
                                Long.valueOf(info.presentationTimeUs),
                                Integer.valueOf(info.flags))
                        );
                    }
                    // 將讀取到的資料寫入文件
                    mMuxer.writeSampleData(outTrackIndex, mReadBuf, info);
                    extractor.advance();
                }
            }

            // 當前文件最後一幀的PTS，用作下一個影片的pts
            ptsOffset += videoPts > audioPts ? videoPts : audioPts;
            // 當前文件最後一幀和下一幀的間隔差40ms，默認錄制25fps的影片，幀間隔時間就是40ms
            // 但由於使用MediaCodec錄制完以後，後面又寫入了一個OES的幀，導致前面解析的時候會有時間差
            // 這裡設置10ms效果比40ms的要好些。
            ptsOffset += 10000L;

            if (VERBOSE) {
                Log.d(TAG, "finish one file, ptsOffset " + ptsOffset);
            }

            // 釋放資源
            videoExtractor.release();
            audioExtractor.release();
        }

        // 釋放復用器
        if (mMuxer != null) {
            try {
                mMuxer.stop();
                mMuxer.release();
            } catch (Exception e) {
                Log.e(TAG, "Muxer close error. No data was written");
            } finally {
                mMuxer = null;
            }
        }

        if (VERBOSE) {
            Log.d(TAG, "video combine finished");
        }

        // 合併結束
        if (mCombineListener != null) {
            mCombineListener.onCombineFinished(combineResult);
        }
    }

    /**
     * 選擇軌道
     *
     * @param extractor  MediaExtractor
     * @param mimePrefix 音軌還是視軌
     * @return
     */
    private int selectTrack(MediaExtractor extractor, String mimePrefix) {
        // 獲得軌道總數
        int numTracks = extractor.getTrackCount();
        // 遍歷尋找包含mimePrefix的軌道
        for (int i = 0; i < numTracks; ++i) {
            MediaFormat format = extractor.getTrackFormat(i);
            String mime = format.getString("mime");
            if (mime.startsWith(mimePrefix)) {
                return i;
            }
        }

        return -1;
    }
}
