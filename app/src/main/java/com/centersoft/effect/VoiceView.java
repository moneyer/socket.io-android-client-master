package com.centersoft.effect;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.AnimationDrawable;
import android.media.AudioFormat;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.blankj.utilcode.util.FileUtils;
import com.centersoft.ICallBack.VoiceRecordCompleteCallback;
import com.centersoft.chat.R;
import com.centersoft.util.AmrAudioRecorder;
import com.centersoft.util.UiUtils;

import java.io.File;
import java.util.UUID;

/**
 * Created by chenchao on 16/1/21.
 * 按下发语音模块
 */

public class VoiceView extends FrameLayout {

    private AppCompatActivity activity;

    private AnimationDrawable voiceRecrodAnimtion;

    private AmrAudioRecorder mAmrAudioRecorder;

    private boolean isRecoding;

    private String out = null;

    private float touchY;

    ViewGroup soundWaveLayout;

    ImageButton voiceRecordButton;

    SoundWaveView soundWaveLeft, soundWaveRight;

    TextView recordTime, tips_hold_to_talk;

    public VoiceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        activity = (AppCompatActivity) getContext();
    }

    @Override
    protected void onFinishInflate() {
        inflate(getContext(), R.layout.input_view_voice_view, this);
        init();
        super.onFinishInflate();
    }

    void init() {


        soundWaveLayout = (ViewGroup) findViewById(R.id.soundWaveLayout);
        voiceRecordButton = (ImageButton) findViewById(R.id.voiceRecordButton);

        voiceRecrodAnimtion = (AnimationDrawable) voiceRecordButton.getBackground();

        soundWaveLeft = (SoundWaveView) findViewById(R.id.soundWaveLeft);
        soundWaveRight = (SoundWaveView) findViewById(R.id.soundWaveRight);
        recordTime = (TextView) findViewById(R.id.recordTime);

        tips_hold_to_talk = (TextView) findViewById(R.id.tips_hold_to_talk);

        voiceRecordButton.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                    //long nowTime = System.currentTimeMillis();
                    float nowY = event.getRawY();
                    if (nowY - touchY <= -100) {
                        //上滑取消录音发送
                        cancelRecord();
                        showToast(R.string.record_has_canceled);
                    } else {
                        sendVoice();
                    }
                    return false;
                } else if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    touchY = event.getRawY();
                    return false;
                } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
                    if (out != null) {
                        if (!isTouchInside((int) event.getX(), (int) event.getY())) {
                            if (isRecoding) {
                                tips_hold_to_talk.setVisibility(View.VISIBLE);
                                tips_hold_to_talk.setText(R.string.loosen_to_cancel);
                                soundWaveLayout.setVisibility(View.GONE);
                                pause();
                            }
                        } else {
                            if (!isRecoding) {
                                touchY = event.getRawY();
                                tips_hold_to_talk.setVisibility(View.GONE);
                                tips_hold_to_talk.setText(R.string.hold_to_talk);
                                soundWaveLayout.setVisibility(View.VISIBLE);
                                reset();
                            }
                        }
                    }

                }
                return false;
            }
        });


        voiceRecordButton.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {

                //开始录音...
                startRecord();
                return true;
            }
        });


    }


    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Bundle data = msg.getData();
            long dur = (long) data.get("duration");
            double volume = (double) data.get("volume");
            float db = (float) volume * 1.618f;
            soundWaveLeft.setVolume(db);
            soundWaveRight.setVolume(db);
            if (dur >= 55000) {
                recordTime.setTextColor(0xFFFF3C30);
            } else {
                recordTime.setTextColor(0xFF50AEEA);
            }
            int t = (int) dur / 1000;
            int min = 0;
            int sec = 0;
            if (dur < 60) {
                sec = t;
            } else {
                min = t / 60;
                sec = t % 60;
            }
            String m = min < 10 ? "0" + min : "" + min;
            String s = sec < 10 ? "0" + sec : "" + sec;
            recordTime.setText(m + ":" + s);
            //录音时长在60.05s到60.5s的范围内时停止录音
            if (dur > 60100 && dur < 60500) {
                sendVoice();
            }
        }
    };

    private void sendVoice() {
        tips_hold_to_talk.setText(R.string.hold_to_talk);
        soundWaveLayout.setVisibility(View.GONE);
        long recordDuration = stopRecord();
        if (recordDuration > 0l && out != null) {
            Log.w("test", "recordDuration=" + recordDuration);
            if (recordDuration >= 1000l && new File(out).length() > 100) {
                //录音发送
                VoiceRecordCompleteCallback callback = (VoiceRecordCompleteCallback) activity;

                callback.recordFinished(recordDuration >= 60000 ? 60000 : recordDuration, out);
            } else {
                cancelRecord();
                showToast(R.string.voice_can_not_send_because_duration_too_short);
            }

        }
        out = null;
    }

    private void pause() {

        try {
            if (mAmrAudioRecorder != null) {
                isRecoding = false;
                mAmrAudioRecorder.pause();

            }
        } catch (Exception e) {
            if (out != null) {
                File f = new File(out);
                if (f.exists()) {
                    f.delete();
                }
                out = null;
            }

        }
    }

    private boolean isTouchInside(int x, int y) {
        int w = voiceRecordButton.getMeasuredWidth();
        int h = voiceRecordButton.getMeasuredHeight();
        if (x >= 0 && x <= w && y >= 0 && y <= h) {
            int centX = w / 2;
            int centY = h / 2;
            return Math.sqrt((x - centX) * (x - centX) + (y - centY) * (y - centY)) <= w / 2;
        }
        return false;
    }

    private void reset() {
        try {
            mAmrAudioRecorder.continueRecord();
            isRecoding = true;
        } catch (Exception e) {
            e.printStackTrace();
            mAmrAudioRecorder.stop();
        }
    }


    private void cancelRecord() {
        stopRecord();
        isRecoding = false;
        if (out != null) {
            File f = new File(out);
            f.delete();
            out = null;
        }
    }

    private long stopRecord() {
        isRecoding = false;
        //voiceRecrodAnimtion.stop();
        voiceRecrodAnimtion.selectDrawable(0);
        tips_hold_to_talk.setVisibility(View.VISIBLE);
        soundWaveLayout.setVisibility(View.GONE);
        recordTime.setTextColor(0xFF50AEEA);
        if (mAmrAudioRecorder == null) {
            return 0L;
        }
        mAmrAudioRecorder.stop();
        long dur = mAmrAudioRecorder.getDuration();
        mAmrAudioRecorder = null;
        return dur;
    }

    private void startRecord() {

        voiceRecrodAnimtion.selectDrawable(1);
        tips_hold_to_talk.setVisibility(View.GONE);
        soundWaveLayout.setVisibility(View.VISIBLE);
        recordTime.setText("00:00");
        soundWaveLeft.reSet();
        soundWaveRight.reSet();
        FileUtils.createOrExistsDir(Environment.getExternalStorageDirectory().getPath() + "/luanliao/");
        out = Environment.getExternalStorageDirectory().getPath() + "/luanliao/voice_" + UUID.randomUUID().toString() + ".amr";
        mAmrAudioRecorder = new AmrAudioRecorder(MediaRecorder.AudioSource.MIC, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT, out);
        mAmrAudioRecorder.setVoiceRecordingCallBack(mVoiceRecordingCallBack);
        mAmrAudioRecorder.prepare();
        mAmrAudioRecorder.start();
        if (AmrAudioRecorder.State.ERROR == mAmrAudioRecorder.getState()) {
            showToast(R.string.record_failed);
        } else {
            isRecoding = true;
        }
    }

    private AmrAudioRecorder.VoiceRecordingCallBack mVoiceRecordingCallBack = new AmrAudioRecorder.VoiceRecordingCallBack() {
        @Override
        public void onRecord(long duration, double volume) {
            Message m = Message.obtain();
            Bundle data = new Bundle();
            data.putDouble("volume", volume);
            data.putLong("duration", duration);
            m.setData(data);
            mHandler.sendMessage(m);
        }
    };


    private void showToast(int rid) {
        TextView tv = new TextView(activity);
        tv.setText(rid);
        tv.setTextSize(16);
        tv.setTextColor(Color.WHITE);
        tv.setBackgroundResource(R.drawable.tips_background);
        tv.setWidth((int) (getResources().getDisplayMetrics().widthPixels * 0.7));
        tv.setHeight(UiUtils.dip2px(48));
        tv.setGravity(Gravity.CENTER);
        Toast toast = new Toast(activity);
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, activity.getSupportActionBar().getHeight() + 30);
        toast.setView(tv);
        toast.show();
    }

}
