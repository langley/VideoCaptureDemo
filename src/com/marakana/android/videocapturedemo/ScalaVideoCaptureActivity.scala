
package com.marakana.android.videocapturedemo

import org.scaloid.common._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date

import android.app.Activity
import android.content.Intent
import android.hardware.Camera
import android.media.CamcorderProfile
import android.media.MediaRecorder
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.Toast

class ScalaVideoCaptureActivity extends SActivity {
    val TAG: String = "VideoCaptureActivity"

    var camera: Camera = null 

    var recordButton: ImageButton = null

    var stopButton: ImageButton = null

    var cameraPreviewFrame: FrameLayout = null

    var cameraPreview: CameraPreview = null 

    var mediaRecorder: android.media.MediaRecorder = null 

    var file: File = null


    override def onCreate(bundle: Bundle): Unit =  { 
        super.onCreate(bundle)
        super.setContentView(R.layout.video_capture)
        cameraPreviewFrame = findViewById(R.id.camera_preview).asInstanceOf[FrameLayout]
        recordButton = findViewById(R.id.recordButton).asInstanceOf[ImageButton]
        stopButton = findViewById(R.id.stopButton).asInstanceOf[ImageButton]
        toggleButtons(false)
        // we'll enable this button once the camera is ready
        recordButton.setEnabled(false)
    }

    def toggleButtons(recording: Boolean): Unit = {
        recordButton.setEnabled(!recording)
        recordButton.setVisibility(if (recording) View.GONE else View.VISIBLE);
        stopButton.setEnabled(recording);
        stopButton.setVisibility(if (recording) View.VISIBLE else View.GONE);
    }

    override def onResume(): Unit = {
        super.onResume();

        // initialize the camera in background, as this may take a while
        val f = Future {
        	  try {
        		  val camera = Camera.open();
        		  if (camera == null) Camera.open(0) else camera
        	  } catch { 
        	    case e: RuntimeException =>
        	    Log.wtf(TAG, "Failed to get camera", e);
        	    null
        	  }
         }
         f.onComplete { 
           case Success(camera) => 
        		  Toast.makeText(ScalaVideoCaptureActivity.this, R.string.cannot_record, Toast.LENGTH_SHORT)
           case Failure(e) => 
              initCamera(camera)
         }
    }

    def initCamera(camera: Camera): Unit = {
        // we now have the camera
        this.camera = camera
        // create a preview for our camera
        cameraPreview = new CameraPreview(ScalaVideoCaptureActivity.this, camera)
        // add the preview to our preview frame
        cameraPreviewFrame.addView(cameraPreview, 0)
        // enable just the record button
        recordButton.setEnabled(true)
    }

    def releaseCamera(): Unit = {
        if (camera != null) {
            camera.lock() // unnecessary in API >= 14
            camera.stopPreview()
            camera.release()
            camera = null
            cameraPreviewFrame.removeView(cameraPreview)
        }
    }

    def releaseMediaRecorder(): Unit = {
        if (this.mediaRecorder != null) {
            this.mediaRecorder.reset() // clear configuration (optional here)
            this.mediaRecorder.release()
            this.mediaRecorder = null
        }
    }

    def releaseResources(): Unit = {
        this.releaseMediaRecorder()
        this.releaseCamera()
    }

    
    override def onPause(): Unit = {
        super.onPause()
        this.releaseResources()
    }

    // gets called by the button press
    def startRecording(v: View): Unit = {
        Log.d(TAG, "startRecording()")
        // we need to unlock the camera so that mediaRecorder can use it
        camera.unlock() // unnecessary in API >= 14
        // now we can initialize the media recorder and set it up with our
        // camera
        mediaRecorder = new MediaRecorder
        mediaRecorder.setCamera(this.camera)

        mediaRecorder.setAudioSource(5) // MediaRecorder.AudioSource.CAMCORDER
        mediaRecorder.setVideoSource(1) // MediaRecorder.VideoSource.CAMERA
        mediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH))
        mediaRecorder.setOutputFile(this.initFile().getAbsolutePath())
        mediaRecorder.setPreviewDisplay(this.cameraPreview.getHolder().getSurface())
        try {
            mediaRecorder.prepare()
            // start the actual recording
            // throws IllegalStateException if not prepared
            mediaRecorder.start()
            Toast.makeText(this, R.string.recording, Toast.LENGTH_SHORT).show()
            // enable the stop button by indicating that we are recording
            this.toggleButtons(true)
        } catch { 
          case e: Exception => 
            Log.wtf(TAG, "Failed to prepare MediaRecorder", e)
            Toast.makeText(this, R.string.cannot_record, Toast.LENGTH_SHORT).show()
            this.releaseMediaRecorder()
        }
    }

    // gets called by the button press
    def stopRecording(v: View): Unit = {
        Log.d(TAG, "stopRecording()")
        // TODO JRL HACK assert this.mediaRecorder != null
        try {
            this.mediaRecorder.stop()
            Toast.makeText(this, R.string.saved, Toast.LENGTH_SHORT).show()
            // we are no longer recording
            this.toggleButtons(false)
        } catch { 
          
          case e: RuntimeException => 
            // the recording did not succeed
            Log.w(TAG, "Failed to record", e)
            if (this.file != null && this.file.exists() && this.file.delete()) {
                Log.d(TAG, "Deleted " + this.file.getAbsolutePath())
            }
            return
        } finally {
            this.releaseMediaRecorder()
        }
        if (this.file == null || !this.file.exists()) {
            Log.w(TAG, "File does not exist after stop: " + this.file.getAbsolutePath())
        } else {
            Log.d(TAG, "Going to display the video: " + this.file.getAbsolutePath())
            val intent = new Intent(this, classOf[com.marakana.android.videocapturedemo.ScalaVideoPlaybackActivity])
            intent.setData(Uri.fromFile(file))
            super.startActivity(intent)
        }
    }

    def initFile(): File = {
        val dir: File = new File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), this
                        .getClass().getPackage().getName())
        if (!dir.exists() && !dir.mkdirs()) {
            Log.wtf(TAG, "Failed to create storage directory: " + dir.getAbsolutePath())
            Toast.makeText(ScalaVideoCaptureActivity.this, R.string.cannot_record, Toast.LENGTH_SHORT)
            this.file = null
        } else {
            this.file = new File(dir.getAbsolutePath(), new SimpleDateFormat(
                    "'IMG_'yyyyMMddHHmmss'.m4v'").format(new Date()))
        }
        file
    }
}
