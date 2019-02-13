package br.com.kazap.facedetectorlib

import androidx.lifecycle.LifecycleOwner
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetector
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions
import com.otaliastudios.cameraview.*

class FaceDetector private constructor(
    cameraView: CameraView,
    lifecycleOwner: LifecycleOwner,
    private val faceDetectorListener: FaceDetectorListener
) {

    companion object {
        private const val FRAME_COUNT = 30f
        private const val PERCENTAGE_THRESHOLD = 70f
    }

    private val arrayComparison: ArrayList<Boolean> = ArrayList()
    private var gotFirstFace = false

    init {
        cameraView.setLifecycleOwner(lifecycleOwner)
        cameraView.addFrameProcessor(getFrameProcessor())

        //test optional feature
        cameraView.addCameraListener(object : CameraListener() {

            override fun onCameraOpened(options: CameraOptions) {
                super.onCameraOpened(options)
                //re-initialize camera here
            }

            override fun onCameraClosed() {
                super.onCameraClosed()
                //stop/remove frame processor
            }
        })


    }

    private fun checkBounds(pair: Pair<Boolean, FirebaseVisionImageMetadata?>): Boolean {
        return pair.first
    }

    private fun getFaceDetector(): FirebaseVisionFaceDetector {
        return FirebaseVision.getInstance()
            .getVisionFaceDetector(
                FirebaseVisionFaceDetectorOptions.Builder()
                    .setContourMode(FirebaseVisionFaceDetectorOptions.FAST)
                    .setClassificationMode(FirebaseVisionFaceDetectorOptions.NO_CLASSIFICATIONS)
                    .setContourMode(FirebaseVisionFaceDetectorOptions.NO_CONTOURS)
                    .setLandmarkMode(FirebaseVisionFaceDetectorOptions.NO_LANDMARKS)
                    .setMinFaceSize(.15f)
                    .enableTracking()
                    .build()
            )
    }

    private fun getFrameProcessor(): FrameProcessor {

        return FrameProcessor { frame ->

            val pair = extractFrameMetadata(frame)
            if (checkBounds(pair)) {

                getFaceDetector().detectInImage(FirebaseVisionImage.fromByteArray(frame.data, pair.second!!))
                    .addOnSuccessListener { faceList ->

                        if (gotFirstFace && faceList.size == 0) {
                            //no face detected
                            faceDetectorListener.onFaceShowOff()
                        }

                        if (gotFirstFace && faceList.size > 1) {
                            //face(s) detected
                            faceDetectorListener.onFaceShowUp()
                        }

                        if (faceList.size > 0) {
                            arrayComparison.add(true)
                        } else {
                            arrayComparison.add(false)
                        }

                        if (arrayComparison.size >= FRAME_COUNT) {

                            val filtered = arrayComparison.filter { it }
                            val percentage: Float = (filtered.size / FRAME_COUNT) * 100

                            if (percentage > PERCENTAGE_THRESHOLD) {

                                if (!gotFirstFace){
                                    faceDetectorListener.onGotFirstFace()
                                }

                                gotFirstFace = true
                            }

                            arrayComparison.clear()
                        }


                    }.addOnFailureListener {
                        it.printStackTrace()
                    }
            }
        }
    }

    private fun extractFrameMetadata(frame: Frame): Pair<Boolean, FirebaseVisionImageMetadata?> {

        if (frame.size.width <= 0 || frame.size.height <= 0
            || frame.data.isEmpty()
        ) return Pair(false, null)
        else {

            frame.let {

                return Pair(
                    true, FirebaseVisionImageMetadata.Builder()
                        .setWidth(it.size.width)
                        .setHeight(it.size.height)
                        .setFormat(it.format)
                        .setRotation(it.rotation / 90)
                        .build()
                )
            }
        }

    }

    inner class Builder() {

        private lateinit var cameraView: CameraView
        private lateinit var lifecycleOwner: LifecycleOwner
        private lateinit var faceDetectorListener: FaceDetectorListener

        fun cameraInstance(cameraView: CameraView): Builder {
            this.cameraView = cameraView
            return this
        }

        fun lifecycleOwner(lifecycleOwner: LifecycleOwner): Builder {
            this.lifecycleOwner = lifecycleOwner
            return this
        }

        fun listener(faceDetectorListener: FaceDetectorListener): Builder {
            this.faceDetectorListener = faceDetectorListener
            return this
        }

        fun build(): FaceDetector {
            return FaceDetector(this.cameraView, this.lifecycleOwner, this.faceDetectorListener)
        }

    }

    interface FaceDetectorListener {
        fun onGotFirstFace()
        fun onFaceShowUp()
        fun onFaceShowOff()
    }
}