package com.excellence.camera.sample

import android.Manifest
import android.content.Intent
import android.hardware.usb.UsbDevice
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.excellence.camera.library.BaseCameraPreviewActivity
import com.excellence.camera.library.BaseUvcActivity
import com.herohan.uvcapp.CameraException
import com.permissionx.guolindev.PermissionX
import com.serenegiant.widget.AspectRatioTextureView

/**
 * <pre>
 *     author : VeiZhang
 *     blog   : http://tiimor.cn
 *     time   : 2025/4/7
 *     desc   :
 * </pre>
 */
class UvcActivity : BaseUvcActivity() {

    companion object {
        private const val TAG = "UvcActivity"

        private const val REASON_STORAGE = "请授予应用读写外部存储文件权限"
        private const val REASON_CAMERA = "请授予摄像头权限"
    }

    private lateinit var aspectRatioTextureView: AspectRatioTextureView
    private lateinit var tvConnectUSBCameraTip: TextView
    private lateinit var devicePicture: ImageButton
    private lateinit var galleryPicture: ImageButton
    private lateinit var fabPicture: ImageButton

    private val mDeviceListDialog by lazy {
        val dialog = DeviceListDialogFragment()

        dialog.setOnDeviceItemSelectListener(object :
            DeviceListDialogFragment.OnDeviceItemSelectListener {

            override fun onItemSelect(usbDevice: UsbDevice?) {
                selectDevice(usbDevice)
            }
        })

        dialog
    }

    override fun getLayoutId(): Int = R.layout.uvc_activity

    override fun getAspectRatioTextureView(): AspectRatioTextureView = aspectRatioTextureView

    override fun init() {
        super.init()

        aspectRatioTextureView = findViewById(R.id.viewMainPreview)
        tvConnectUSBCameraTip = findViewById(R.id.tvConnectUSBCameraTip)
        devicePicture = findViewById(R.id.devicePicture)
        galleryPicture = findViewById(R.id.galleryPicture)
        fabPicture = findViewById(R.id.fabPicture)

        devicePicture.setOnClickListener { v ->
            showDeviceSelector()
        }
        fabPicture.setOnClickListener { v ->
            requestPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, REASON_STORAGE) {
                takePicture { success, message, e ->
                    if (!success) {
                        if (message != null) {
                            Toast.makeText(this, "拍照失败:$message", Toast.LENGTH_SHORT).show()
                        } else if (e != null) {
                            Toast.makeText(
                                this,
                                "拍照异常:${Log.getStackTraceString(e)}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
        }

        val activityLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            val picture = result?.data?.getStringExtra(EXTRA_PIC)
            Log.d(TAG, "getPhoto from gallery ${picture}")
            if (!picture.isNullOrEmpty()) {
                responseImage(picture)
            }
        }

        galleryPicture.setOnClickListener {
            activityLauncher.launch(Intent(this, GalleryActivity::class.java))
        }
    }

    private fun showDeviceSelector() {
        mDeviceListDialog.show(
            supportFragmentManager,
            getDeviceList(),
            if (isCameraConnected()) getUsbDevice() else null
        )
    }

    override fun updateUIControls() {
        runOnUiThread {
            if (isCameraConnected()) {
                aspectRatioTextureView.visibility = View.VISIBLE
                tvConnectUSBCameraTip.visibility = View.GONE
                fabPicture.visibility = View.VISIBLE
            } else {
                aspectRatioTextureView.visibility = View.GONE
                tvConnectUSBCameraTip.visibility = View.VISIBLE
                fabPicture.visibility = View.GONE
            }

            val deviceList = getDeviceList()
            if (deviceList == null || deviceList.size <= 1) {
                // 多个才可以切换
                devicePicture.visibility = View.GONE
            } else {
                devicePicture.visibility = View.VISIBLE
            }

            mDeviceListDialog.notifySelectDevice(getDeviceList(), getUsbDevice())
        }
    }

    override fun onResume() {
        super.onResume()
        val mediaList =
            GalleryActivity.getMedia(BaseCameraPreviewActivity.getDefaultCameraPictureStorageDir())
        if (mediaList.isNotEmpty()) {
            Glide.with(this).load(mediaList[0].path)
                .apply(RequestOptions().circleCrop())
                .placeholder(R.drawable.ic_no_picture)
                .error(R.drawable.ic_no_picture)
                .into(galleryPicture)
        } else {
            galleryPicture.setImageResource(R.drawable.ic_no_picture)
        }
    }

    override fun requestCameraPermission(
        permission: String,
        result: (allGranted: Boolean) -> Unit
    ) {
        requestPermission(permission, REASON_CAMERA, result)
    }

    private fun requestPermission(
        permission: String,
        reason: String,
        result: (allGranted: Boolean) -> Unit
    ) {
        PermissionX.init(this)
            .permissions(permission)
            .explainReasonBeforeRequest()
            .onExplainRequestReason { scope, deniedList ->
                scope.showRequestReasonDialog(
                    deniedList,
                    reason,
                    "同意",
                    "拒绝"
                )
            }
            .onForwardToSettings { scope, deniedList ->
                if (deniedList.isNotEmpty()) {
                    scope.showForwardToSettingsDialog(
                        deniedList,
                        reason,
                        "去设置",
                        "再想想"
                    )
                }
            }
            .request { allGranted, _, _ ->
                result(allGranted)
            }
    }

    override fun onCameraError(e: CameraException?) {
        super.onCameraError(e)
        e?.let {
            val tip =
                if (it.code == CameraException.CAMERA_OPEN_ERROR_BUSY) {
                    "设备被占用。请重新插拔一下设备！"
                } else {
                    "发生未知错误。请重新插拔一下设备！"
                }
            Toast.makeText(this, tip, Toast.LENGTH_SHORT).show()
        }
    }
}