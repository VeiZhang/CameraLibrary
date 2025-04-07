package com.excellence.camera.library

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Point
import android.graphics.SurfaceTexture
import android.hardware.usb.UsbDevice
import android.os.Bundle
import android.util.Log
import android.view.TextureView.SurfaceTextureListener
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.herohan.uvcapp.CameraException
import com.herohan.uvcapp.CameraHelper
import com.herohan.uvcapp.ICameraHelper
import com.herohan.uvcapp.ImageCapture
import com.herohan.uvcapp.ImageCapture.OnImageCaptureCallback
import com.serenegiant.widget.AspectRatioTextureView
import java.io.File

/**
 * <pre>
 *     author : VeiZhang
 *     blog   : http://tiimor.cn
 *     time   : 2024/10/16
 *     desc   :
 * </pre>
 */
abstract class BaseUvcActivity : AppCompatActivity() {

    companion object {

        private const val TAG = "UvcActivity"

        const val EXTRA_PIC = "camera_picture"

    }

    private var mCameraHelper: ICameraHelper? = null

    private var mPreviewWidth: Int = 0
    private var mPreviewHeight: Int = 0

    private var mUsbDevice: UsbDevice? = null
    private var isCameraConnected = false
    private var isPicturing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(getLayoutId())

        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val point = Point()
        wm.defaultDisplay.getSize(point)
        // 1280 x 720
        mPreviewWidth = point.x
        mPreviewHeight = point.y

        init()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        // AndroidManifest里面需要注册监听设备
//        if (intent.action == UsbManager.ACTION_USB_DEVICE_ATTACHED) {
//            if (!isCameraConnected) {
//                mUsbDevice = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
//                LogService.d(TAG, "onNewIntent: $mUsbDevice")
//                selectDevice(mUsbDevice)
//            }
//        }
    }

    protected fun takePicture(result: (success: Boolean, message: String?, e: Throwable?) -> Unit) {
        try {
            Log.i(TAG, "拍照，是否进行中 = $isPicturing")
            if (isPicturing) {
                return
            }

            isPicturing = true
            val file = getCameraPictureStorageFile()
            Log.d(TAG, "takePicture = ${file.path}")

            val options = ImageCapture.OutputFileOptions.Builder(file).build()
            mCameraHelper?.takePicture(options, object : OnImageCaptureCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    Log.d(TAG, "拍照成功:${outputFileResults.savedUri}")
                    // toast("拍照成功:${outputFileResults.savedUri}")
                    isPicturing = false

                    responseImage(file.path)
                }

                override fun onError(imageCaptureError: Int, message: String, cause: Throwable?) {
                    Log.d(
                        TAG,
                        "拍照失败:error = $imageCaptureError, message = $message, cause = ${
                            Log.getStackTraceString(cause)
                        }"
                    )
                    isPicturing = false
                    result(false, message, null)
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "拍照异常:${Log.getStackTraceString(e)}")
            isPicturing = false
            result(false, null, e)
        }
    }

    /**
     * 拍照存储路径
     */
    protected open fun getCameraPictureStorageFile(): File {
        return BaseCameraPreviewActivity.getDefaultCameraPictureStorageFile()
    }

    protected fun responseImage(path: String?) {
        val intent = Intent().apply {
            putExtra(EXTRA_PIC, path ?: "")
        }
        setResult(Activity.RESULT_OK, intent)
        finish()
    }

    protected open fun initCameraHelper(): ICameraHelper {
        Log.i(TAG, "initCameraHelper")

        val helper = CameraHelper()
        val cameraHelperCallback = CameraHelperCallback()
        helper.setStateCallback(cameraHelperCallback)

        // 设置截屏画质，画质越高，延时越久
        helper.imageCaptureConfig = helper.imageCaptureConfig.setJpegCompressionQuality(95)
        // 视频录制配置
        helper.videoCaptureConfig = helper.videoCaptureConfig
//            .setAudioCaptureEnable(false)
            .setBitRate((1024 * 1024 * 25 * 0.25).toInt())
            .setVideoFrameRate(25)
            .setIFrameInterval(1)
        return helper
    }

    private fun initPreviewView() {
        val textureView: AspectRatioTextureView = getAspectRatioTextureView()
        setAspectRatio(mPreviewWidth, mPreviewHeight)
        textureView.surfaceTextureListener = object : SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(
                surface: SurfaceTexture,
                width: Int,
                height: Int
            ) {
                mCameraHelper?.addSurface(surface, false)
            }

            override fun onSurfaceTextureSizeChanged(
                surface: SurfaceTexture,
                width: Int,
                height: Int
            ) {
            }

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                mCameraHelper?.removeSurface(surface)
                return false
            }

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
        }
    }

    private fun setAspectRatio(width: Int, height: Int) {
        mPreviewWidth = width
        mPreviewHeight = height
        getAspectRatioTextureView().setAspectRatio(mPreviewWidth, mPreviewHeight)
    }

    protected fun selectDevice(usbDevice: UsbDevice?) {
        Log.i(TAG, "select device = $usbDevice")
        // 关闭上一个
        mCameraHelper?.closeCamera()
        mUsbDevice = usbDevice

        usbDevice?.let {
            requestCameraPermission(Manifest.permission.CAMERA) { allGranted ->
                if (allGranted) {
                    isCameraConnected = false
                    updateUIControls()

                    // 通过UsbDevice对象，尝试获取设备权限
                    mCameraHelper?.selectDevice(it)
                } else {
                    selectDevice(it)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        startPreview()
    }

    protected open fun startPreview() {
        initPreviewView()
    }

    override fun onStop() {
        super.onStop()
    }

    override fun onDestroy() {
        releaseCameraHelper()
        super.onDestroy()
    }

    private fun releaseCameraHelper() {
        Log.w(TAG, "releaseCameraHelper")
        mCameraHelper?.release()
        mCameraHelper = null
    }

    private inner class CameraHelperCallback : ICameraHelper.StateCallback {

        override fun onAttach(device: UsbDevice?) {
            Log.d(TAG, "onAttach $device")

            selectDevice(device)
        }

        override fun onDeviceOpen(device: UsbDevice?, isFirstOpen: Boolean) {
            Log.d(TAG, "onDeviceOpen $device, isFirstOpen = $isFirstOpen")

            mCameraHelper?.let {
                it.openCamera()
                it.setButtonCallback { button, state ->
                    Log.d(TAG, "setButtonCallback button = $button, state = $state")
                }
            }
        }

        override fun onCameraOpen(device: UsbDevice?) {
            Log.d(TAG, "onCameraOpen $device")

            mCameraHelper?.let {
                it.startPreview()

                val size = it.previewSize
                size?.let {
                    // 设置原始大小，不拉伸
                    setAspectRatio(size.width, size.height)
                }

                if (getAspectRatioTextureView().surfaceTexture != null) {
                    it.addSurface(getAspectRatioTextureView().surfaceTexture, false)
                }

                isCameraConnected = true
                updateUIControls()
            }
        }

        override fun onCameraClose(device: UsbDevice?) {
            Log.d(TAG, "onCameraClose $device")

            if (mCameraHelper != null && getAspectRatioTextureView().surfaceTexture != null) {
                mCameraHelper?.removeSurface(getAspectRatioTextureView().surfaceTexture)
            }

            isCameraConnected = false
            updateUIControls()
        }

        override fun onDeviceClose(device: UsbDevice?) {
            Log.d(TAG, "onDeviceClose $device")
        }

        override fun onDetach(device: UsbDevice?) {
            Log.d(TAG, "onDetach $device")

            if (device == mUsbDevice) {
                mUsbDevice = null
                updateUIControls()
            }
        }

        override fun onCancel(device: UsbDevice?) {
            Log.d(TAG, "onCancel $device")

            if (device == mUsbDevice) {
                mUsbDevice = null
            }
        }

        override fun onError(device: UsbDevice?, e: CameraException?) {
            Log.e(TAG, "onError $device, ${Log.getStackTraceString(e)}")

            onCameraError(e)
        }

    }

    protected open fun onCameraError(e: CameraException?) {

    }

    protected open fun init() {
        releaseCameraHelper()
        mCameraHelper = initCameraHelper()
    }

    protected fun isCameraConnected() = isCameraConnected

    protected fun getDeviceList(): List<UsbDevice> =
        mCameraHelper?.deviceList?.toList() ?: ArrayList()

    protected fun getUsbDevice() = mUsbDevice

    abstract fun getLayoutId(): Int

    abstract fun getAspectRatioTextureView(): AspectRatioTextureView

    abstract fun requestCameraPermission(permission: String, result: (allGranted: Boolean) -> Unit)

    abstract fun updateUIControls()
}