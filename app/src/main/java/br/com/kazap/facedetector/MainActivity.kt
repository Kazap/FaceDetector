package br.com.kazap.facedetector

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import br.com.kazap.facedetectorlib.FaceDetector
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), FaceDetector.FaceDetectorListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        FaceDetector.Builder().cameraInstance(cameraView)
            .lifecycleOwner(this)
            .listener(this)
            .build()

    }

    override fun onFaceShowUp() {
        Log.d("FACEDETECTION-LIB", "FACE SHOW UP")
    }

    override fun onFaceShowOff() {
        Log.d("FACEDETECTION-LIB", "FACE SHOW OFF")
    }
}
