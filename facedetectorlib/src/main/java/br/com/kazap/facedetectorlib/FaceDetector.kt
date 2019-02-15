package br.com.kazap.facedetectorlib

import androidx.lifecycle.LifecycleOwner
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata
import com.google.firebase.ml.vision.face.FirebaseVisionFace
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetector
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions
import com.otaliastudios.cameraview.*
import java.lang.Exception

class FaceDetector private constructor(
    cameraView: CameraView,
    lifecycleOwner: LifecycleOwner,
    private val faceDetectorListener: FaceDetectorListener
) {

    companion object {
        private const val FRAME_COUNT = 6f
        private const val PERCENTAGE_THRESHOLD = 60f
    }

    private var amountComparisonTrue = 0
    private var amountComparisonFalse = 0
    private var isFaceShowUp = false

    init {
        cameraView.setLifecycleOwner(lifecycleOwner)
        cameraView.addFrameProcessor(getFrameProcessor())
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
            extractFrameMetadata(frame)?.let { imageMetadata ->
                getFaceDetector()
                    .detectInImage(FirebaseVisionImage.fromByteArray(frame.data, imageMetadata))
                    .addOnSuccessListener { faceList -> detectInImageSuccess(faceList) }
                    .addOnFailureListener { exception -> detectInImageFailure(exception) }
            }
        }
    }

    private fun extractFrameMetadata(frame: Frame): FirebaseVisionImageMetadata? {
        //noinspection ConstantConditions
        return if (frame.data.isEmpty() || frame.size.width <= 0 || frame.size.height <= 0) {
            null
        } else {
            with(frame) {
                FirebaseVisionImageMetadata.Builder()
                    .setWidth(size.width)
                    .setHeight(size.height)
                    .setFormat(format)
                    .setRotation(rotation / 90)
                    .build()
            }
        }
    }

    private fun detectInImageSuccess(faceList: List<FirebaseVisionFace>) {
        if (faceList.isEmpty()) amountComparisonFalse++ else amountComparisonTrue++

        if (getTotalAmountComparison() >= FRAME_COUNT) {
            if (faceList.isEmpty() && isFaceShowUp) {
                faceDetectorListener.onFaceShowOff()
                isFaceShowUp = false
            } else if (faceList.isNotEmpty() && !isFaceShowUp && isPercentageGreaterThanThreshold()) {
                faceDetectorListener.onFaceShowUp()
                isFaceShowUp = true
            }

            amountComparisonTrue = 0
            amountComparisonFalse = 0
        }
    }

    private fun detectInImageFailure(exception: Exception) {
        exception.printStackTrace()
    }

    private fun getTotalAmountComparison() = amountComparisonTrue + amountComparisonFalse

    private fun isPercentageGreaterThanThreshold(): Boolean {
        val percentage: Float = (amountComparisonTrue / FRAME_COUNT) * 100
        return percentage > PERCENTAGE_THRESHOLD
    }

    class Builder(
        private val cameraView: CameraView,
        private val lifecycleOwner: LifecycleOwner,
        private val faceDetectorListener: FaceDetectorListener
    ) {
        fun build(): FaceDetector {
            return FaceDetector(cameraView, lifecycleOwner, faceDetectorListener)
        }

    }

    interface FaceDetectorListener {
        fun onFaceShowUp()
        fun onFaceShowOff()
    }
}