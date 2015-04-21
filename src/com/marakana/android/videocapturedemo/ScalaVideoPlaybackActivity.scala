
package com.marakana.android.videocapturedemo;

import org.scaloid.common._
import android.graphics.Color

import java.io.File;

import android.app.Activity;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;
import android.widget.VideoView;

class ScalaVideoPlaybackActivity extends SActivity with OnPreparedListener with OnCompletionListener {
    private val TAG = "VideoPlaybackActivity"
    Log.d(TAG, "initializer run!")

    private var videoView: VideoView = null // super.findViewById(R.id.video).asInstanceOf[VideoView]

    private var backButton: ImageButton =  null // super.findViewById(R.id.backButton).asInstanceOf[ImageButton]

    private var playButton: ImageButton = null // super.findViewById(R.id.playButton).asInstanceOf[ImageButton] 

    private var stopButton: ImageButton =  null // super.findViewById(R.id.stopButton).asInstanceOf[ImageButton]

    private var deleteButton: ImageButton = null // super.findViewById(R.id.deleteButton).asInstanceOf[ImageButton] 

    private var uri: Uri = super.getIntent().getData();

    onCreate {
     setContentView(R.layout.video_playback)
     videoView = findViewById(R.id.video).asInstanceOf[VideoView]
     backButton = findViewById(R.id.backButton).asInstanceOf[ImageButton]
     playButton = findViewById(R.id.playButton).asInstanceOf[ImageButton]
     stopButton = findViewById(R.id.stopButton).asInstanceOf[ImageButton]
     deleteButton = findViewById(R.id.deleteButton).asInstanceOf[ImageButton]
     uri = getIntent.getData
    }

    private def toggleButtons(playing: Boolean): Unit = {
        this.backButton.setEnabled(!playing);
        this.playButton.setVisibility(if (playing) View.GONE else View.VISIBLE)
        this.stopButton.setVisibility(if (playing) View.VISIBLE else View.GONE) 
        this.deleteButton.setEnabled(!playing);
    }

    
    override def onResume(): Unit = {
        super.onResume();
        this.videoView.setVideoURI(this.uri);
        this.videoView.setOnPreparedListener(this);
    }

    
    override def onPause(): Unit = {
        super.onPause();
        this.videoView.stopPlayback();
    }

    def onPrepared(mp: MediaPlayer): Unit = {
        Log.d(TAG, "Prepared. Subscribing for completion callback.");
        this.videoView.setOnCompletionListener(this);
        Log.d(TAG, "Starting plackback");
        this.videoView.start();
        Toast.makeText(this, R.string.playing, Toast.LENGTH_SHORT).show();
        this.toggleButtons(true);
    }

    def onCompletion(mp: MediaPlayer ): Unit = {
        Log.d(TAG, "Completed playback. Go to beginning.");
        this.videoView.seekTo(0);
        this.notifyUser(R.string.completed_playback);
        this.toggleButtons(false);
    }

    // gets called by the button press
    def back(v: View): Unit = {
        Log.d(TAG, "Going back");
        super.finish();
    }

    // gets called by the button press
    def play(v: View): Unit = {
        Log.d(TAG, "Playing");
        this.videoView.start();
        this.toggleButtons(true);
    }

    def stop(v: View): Unit =  {
        Log.d(TAG, "Stopping");
        this.videoView.pause();
        this.videoView.seekTo(0);
        this.toggleButtons(false);
    }

    // gets called by the button press
    def delete(v: View): Unit = {
        if (new File(this.uri.getPath()).delete()) {
            Log.d(TAG, "Deleted: " + this.uri);
            this.notifyUser(R.string.deleted);
        } else {
            Log.d(TAG, "Failed to delete: " + this.uri);
            this.notifyUser(R.string.cannot_delete);
        }
        Log.d(TAG, "Going back");
        super.finish();
    }

    private def notifyUser(messageResource: Int): Unit = {
        Toast.makeText(this, messageResource, Toast.LENGTH_SHORT).show();
    }
}
