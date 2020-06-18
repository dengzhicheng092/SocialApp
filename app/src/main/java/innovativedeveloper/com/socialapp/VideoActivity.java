package innovativedeveloper.com.socialapp;

import android.app.Activity;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.VideoView;

public class VideoActivity extends AppCompatActivity implements MediaPlayer.OnPreparedListener {

    VideoView videoView;
    String videoUri;
    ProgressBar progressBar;

    public static void startActivity(Activity startingActivity, String videoUrl) {
        Intent intent = new Intent(startingActivity, VideoActivity.class);
        intent.putExtra("videoUrl", videoUrl);
        startingActivity.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);
        videoView = (VideoView) findViewById(R.id.videoView);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        MediaController vidControl = new MediaController(this);
        vidControl.setAnchorView(videoView);
        videoUri = getIntent().getStringExtra("videoUrl");
        videoView.setOnPreparedListener(this);
        videoView.setVideoURI(Uri.parse(videoUri));
        videoView.setMediaController(vidControl);
        videoView.start();
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        mp.setOnBufferingUpdateListener(new MediaPlayer.OnBufferingUpdateListener() {
            @Override
            public void onBufferingUpdate(MediaPlayer mp, int percent) {
                if (percent > 30) {
                    progressBar.setVisibility(View.GONE);
                }
            }
        });
    }
}
