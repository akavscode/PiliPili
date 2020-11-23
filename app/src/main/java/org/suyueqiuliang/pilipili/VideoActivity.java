package org.suyueqiuliang.pilipili;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.MediaSourceFactory;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;

import org.suyueqiuliang.pilipili.tool.QualityList;
import org.suyueqiuliang.pilipili.tool.ToolClass;
import org.suyueqiuliang.pilipili.tool.VideoInformation;

public class VideoActivity extends AppCompatActivity {
    static int av;
    private PlayerView playerView;
    static ToolClass toolClass;
    static SimpleExoPlayer player;
    static ImageView back;
    static EditText flame_editText;
    @SuppressLint("ClickableViewAccessibility")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.video_layout);
        hideSystemNavigationBar();
        toolClass = new ToolClass();
        back = findViewById(R.id.video_back);
        flame_editText = findViewById(R.id.flame_editText);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        Intent intent = getIntent();
        av = intent.getIntExtra("av",0);
        back.setOnClickListener(v -> {
            player.stop();
            player.release();
            finish();
        });
        flame_editText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus){
                    player.pause();
                }
            }
        });
        new Thread(() -> {
            VideoInformation videoInformation = toolClass.getVideoInformation(av);
            QualityList qualityList = toolClass.getVideoStreamQuality(av,videoInformation.pages.get(0).cid);
            String[] urls = toolClass.getVideoStream(av,videoInformation.pages.get(0).cid,qualityList.qn.get(0));
            //toolClass.playVideo(urls[0]);
            //urls[0] = urls[0].replace("http","https");
            runOnUiThread(()->{
                playerView = findViewById(R.id.video_view);
                Log.e("url",urls[0]);
                DefaultHttpDataSourceFactory dataSourceFactory = new DefaultHttpDataSourceFactory("545");
                dataSourceFactory.clearAllDefaultRequestProperties();
                dataSourceFactory.getDefaultRequestProperties().set("referer","http://m.bilibili.com/video/"+videoInformation.bvid);
                dataSourceFactory.getDefaultRequestProperties().set("Accept","*/*");
                dataSourceFactory.getDefaultRequestProperties().set("X-Requested-With","com.android.browser");
                dataSourceFactory.getDefaultRequestProperties().set("Origin","http://m.bilibili.com");
                dataSourceFactory.getDefaultRequestProperties().set("Connection","keep-alive");
                dataSourceFactory.getDefaultRequestProperties().set("Accept-Encoding","identity;q=1, *;q=0");
                dataSourceFactory.getDefaultRequestProperties().set("Accept-Language","zh-CN,zh;q=0.9,en-US;q=0.8,en;q=0.7");
                MediaSource videoSource = new ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(Uri.parse(urls[0]));
                //MediaSource audioSource = new ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(audioUri);
                //MergingMediaSource mergedSource = new MergingMediaSource(videoSource, audioSource);
                MediaSourceFactory mediaSourceFactory =
                        new DefaultMediaSourceFactory(dataSourceFactory)
                                .setAdViewProvider(playerView);
                player = new SimpleExoPlayer.Builder(this)
                        .setMediaSourceFactory(mediaSourceFactory)
                        .build();
                playerView.setPlayer(player);
                player.setMediaItem(MediaItem.fromUri(String.valueOf(urls[0])));
                player.prepare();
                player.play();
            });
        }).start();
    }
    private void hideSystemNavigationBar() {
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);
    }
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
            player.stop();
            player.release();
            finish();
            //不执行父类点击事件
            return true;
        }
        //继续执行父类其他点击事件
        return super.onKeyUp(keyCode, event);
    }


    @Override
    public void onPause() {
        super.onPause();
        player.pause();
    }
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            View v = getCurrentFocus();
            if (isShouldHideKeyboard(v, ev)) {
                hideKeyboard(v.getWindowToken());
            }
        }
        return super.dispatchTouchEvent(ev);
    }
    private void hideKeyboard(IBinder token) {
        if (token != null) {
            InputMethodManager im = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            im.hideSoftInputFromWindow(token, InputMethodManager.HIDE_NOT_ALWAYS);
            flame_editText.clearFocus();
        }
    }
    private boolean isShouldHideKeyboard(View v, MotionEvent event) {
        if ((v instanceof EditText)) {
            int[] l = {0, 0};
            v.getLocationInWindow(l);
            int left = l[0],
                    top = l[1],
                    bottom = top + v.getHeight(),
                    right = left + v.getWidth();
            // 点击EditText的事件，忽略它。
            return !(event.getX() > left) || !(event.getX() < right)
                    || !(event.getY() > top) || !(event.getY() < bottom);
        }
        // 如果焦点不是EditText则忽略，这个发生在视图刚绘制完，第一个焦点不在EditText上，和用户用轨迹球选择其他的焦点
        return false;
    }
}
