package com.excellence.camera.library

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.util.Size
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCapture.Metadata
import androidx.camera.core.ImageCapture.OnImageSavedCallback
import androidx.camera.core.ImageCapture.OutputFileOptions
import androidx.camera.core.ImageCapture.OutputFileResults
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date


/**
 * <pre>
 *     author : VeiZhang
 *     blog   : http://tiimor.cn
 *     time   : 2024/10/25
 *     desc   :
 * </pre>
 */
abstract class BaseCameraPreviewActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "CameraPreview"

        const val EXTRA_PIC = "camera_picture"
        private val FORMAT_DATE = SimpleDateFormat("yyyyMMdd")
        private val FORMAT_TIME = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss")

        fun getDefaultCameraPictureStorageDir(): File {
//            val dir = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//                // DCIM/camera/
//                "${Environment.DIRECTORY_DCIM}/camera/"
//            } else {
//                // /storage/emulated/0/Android/data/com.lyy.site.cashier.sit/files/DCIM/camera/
//                "${context.getExternalFilesDir(Environment.DIRECTORY_DCIM)}/camera/"
//                // Environment.getExternalStorageDirectory()报错
//                "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)}/camera/"
//            }

            val dir =
                "${Environment.getExternalStorageDirectory()}/${Environment.DIRECTORY_DCIM}/camera/"
            return File(dir)
        }

        fun getDefaultCameraPictureStorageFile(): File {
            val dir = getDefaultCameraPictureStorageDir()

            val yyyyMMdd = FORMAT_DATE.format(Date())
            val cameraDir = File(dir, yyyyMMdd)
            val pictureDir = File(cameraDir, "pictures")
            val ret = (if (pictureDir.exists()) pictureDir.isDirectory() else pictureDir.mkdirs())
            val fileName = "${FORMAT_TIME.format(Date())}.jpg"
            return File(pictureDir, fileName)
        }

    }

    private var imageCapture: ImageCapture? = null

    // 拍照防抖
    private var isPicturing = false

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        // 屏幕常亮
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        // 强制竖屏
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        setContentView(getLayoutId())

        init()
        requestCamera()
    }

    protected fun takePicture() {
        Log.w(TAG, "takePicture: $imageCapture, $isPicturing")

        if (imageCapture == null) {
            return
        }
        if (isPicturing) {
            return
        }

        isPicturing = true
        val pictureFile = getCameraPictureStorageFile()
        val localImageCapture: ImageCapture = imageCapture!!

        val metadata = Metadata()
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        // Options fot the output image file
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//            val contentValues = ContentValues().apply {
//                put(MediaStore.MediaColumns.DISPLAY_NAME, pictureFile.name)
//                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
//                put(MediaStore.MediaColumns.RELATIVE_PATH, pictureFile.parent)
//            }
//
//            // Create the output uri
//            val contentUri =
//                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
//
//            OutputFileOptions.Builder(contentResolver, contentUri, contentValues)
//        }
        val outputOptions = OutputFileOptions.Builder(pictureFile).setMetadata(metadata).build()

        localImageCapture.takePicture(
            outputOptions, // the options needed for the final image
            ContextCompat.getMainExecutor(this), // the executor, on which the task will run
            object : OnImageSavedCallback { // the callback, about the result of capture process
                override fun onImageSaved(outputFileResults: OutputFileResults) {
                    // This function is called if capture is successfully completed
                    outputFileResults.savedUri?.let { uri ->
                        Log.d(TAG, "Photo saved in $uri, ${pictureFile.path}")

                        isPicturing = false
                        responseImage(pictureFile.path)
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    // This function is called if there is an errors during capture process
                    val msg = "拍照失败: ${exception.message}"
                    Toast.makeText(this@BaseCameraPreviewActivity, msg, Toast.LENGTH_SHORT).show()
                    Log.e(TAG, msg)
                    exception.printStackTrace()
                    isPicturing = false
                }
            }
        )
    }

    protected fun responseImage(path: String?) {
        val intent = Intent().apply {
            putExtra(EXTRA_PIC, path ?: "")
        }
        setResult(Activity.RESULT_OK, intent)
        finish()
    }

    protected fun previewCamera(previewView: PreviewView, cameraSelector: CameraSelector) {
        imageCapture = null
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            // 摄像头个数
            val cameraInfoList = cameraProvider.availableCameraInfos
            notifyCameraInfo(cameraInfoList)

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            imageCapture = buildImageCapture()

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }, ContextCompat.getMainExecutor(this))
    }

    override fun onStart() {
        super.onStart()
    }

    override fun onResume() {
        super.onResume()

        resumeCameraView()
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onStop() {
        super.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onBackPressed() {
        setResult(RESULT_CANCELED)
        super.onBackPressed()
    }

    /**
     * 拍照存储路径
     */
    protected open fun getCameraPictureStorageFile(): File {
        return getDefaultCameraPictureStorageFile()
    }

    protected open fun resumeCameraView() {
        // 防止首次安装，没有打开摄像头，因为PermissionX首次安装没有回调
        if (imageCapture == null) {
            requestCamera()
        }

    }

    /**
     * 读取摄像头几组：前置 和 后置
     */
    protected open fun notifyCameraInfo(cameraInfoList: List<CameraInfo>) {

    }

    protected open fun buildImageCapture(): ImageCapture {
        return ImageCapture.Builder()
            // 优化捕获速度，可能降低图片质量
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            // 设置分辨率宽高，减小图片大小
            .setTargetResolution(Size(720, 1280))
            .build()
    }

    /**
     * 申请摄像头权限
     */
    protected abstract fun requestCamera()

    protected open fun init() {

    }

    /**
     * 拍照防抖
     */
    protected fun isPicturing() = isPicturing

    protected abstract fun getLayoutId(): Int
}