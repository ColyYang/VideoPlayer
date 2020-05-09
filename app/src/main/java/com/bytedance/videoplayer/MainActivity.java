package com.bytedance.videoplayer;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import java.lang.ref.WeakReference;

import tv.danmaku.ijk.media.player.IjkMediaPlayer;

public class MainActivity extends AppCompatActivity {
    private VideoPlayerIJK ijkPlayer;

    private Button playbtn;
    private Button changebtn;
    private SeekBar seekBar;

    private TextView playtime;
    private Handler handler;
    private Thread thread;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setTitle("ijkPlayer");

        ijkPlayer =findViewById(R.id.Player);


        //加载native库
        try{
            IjkMediaPlayer.loadLibrariesOnce(null);
            IjkMediaPlayer.native_profileBegin("ijkpayer.so");
        }catch (Exception e){
            this.finish();
        }
        ijkPlayer.setListener(new VideoPlayerListener());
        ijkPlayer.setVideoResource(R.raw.bytedance);


        playbtn = findViewById(R.id.buttonPlay);
        changebtn = findViewById(R.id.buttonChange);//由于纵向排列，全屏后change view按钮会无法点击。故，改为横向排列
        seekBar = findViewById(R.id.seekB);

        //play、pause按钮合并
        playbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(playbtn.getText().equals("Play")){
                    ijkPlayer.start();
                    playbtn.setText("Pause");
                }
                else if(playbtn.getText().equals("Pause")){
                    ijkPlayer.pause();
                    playbtn.setText("Play");
                }
            }
        });



        handler = new FHandler(this);
        playtime = findViewById(R.id.playTime);
        playbtn.setText("Pause");


        //实现横竖屏切换
        changebtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ChangeScreen();
            }
        });


        //视频进度条
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int i = seekBar.getProgress();
                long totalTime = ijkPlayer.getDuration();
                float seekTime = ((float)i)/100;
                ijkPlayer.seekTo((long)(totalTime * seekTime));
            }
        });



        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                //视频计时器每秒更新一次
                while (true) {
                    handler.sendEmptyMessage(1);
                    delay(1000);
                }
            }
        });
        thread.start();

    }

    //实现横屏/竖屏切换
    private void ChangeScreen() {
        if (getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {// 切换为横屏/全屏
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
    }

    //实现时间更新
    public void updateTime() {
        if(playbtn.getText().equals("Pause")){
            long currentTime = ijkPlayer.getCurrentPosition();
            long totalTime = ijkPlayer.getDuration();
            if(totalTime != 0){ //时间更新
                int progress = (int)(currentTime*100/totalTime);
                seekBar.setProgress(progress);
                Integer currentTime_s = (int)(currentTime/1000);
                playtime.setText(currentTime_s.toString()+"s");
            }
        }
    }

    private static class FHandler extends Handler {
        private WeakReference<MainActivity> reference;
        public FHandler(MainActivity activity) {
            reference = new WeakReference<MainActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    if(reference.get()!=null)
                        reference.get().updateTime();
                    break;
            }
            super.handleMessage(msg);
        }
    };

    private void delay(int ms){
        try {
            Thread.currentThread();
            Thread.sleep(ms);
        } catch (InterruptedException interruptexception) {
            interruptexception.printStackTrace();
        }
    }


    //防止内存泄漏
    @Override
    protected void onDestroy(){
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
        thread.interrupt();
    }



}
