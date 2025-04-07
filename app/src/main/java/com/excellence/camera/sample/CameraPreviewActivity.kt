package com.excellence.camera.sample

import android.Manifest
import android.content.Intent
import android.os.Build
import android.util.Log
import android.view.View
import android.widget.ImageButton
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.view.PreviewView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.excellence.camera.library.BaseCameraPreviewActivity
import com.excellence.camera.sample.utils.toggleButton
import com.permissionx.guolindev.PermissionX

/**
 * <pre>
 *     author : VeiZhang
 *     blog   : http://tiimor.cn
 *     time   : 2025/4/7
 *     desc   :
 * </pre>
 */
class CameraPreviewActivity : BaseCameraPreviewActivity() {

    companion object {
        private const val TAG = "CameraPreview"
    }

    private lateinit var previewView: PreviewView
    private lateinit var fabPicture: ImageButton
    private lateinit var devicePicture: ImageButton
    private lateinit var galleryPicture: ImageButton

    // 默认后置摄像头
    private var cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

    override fun getLayoutId(): Int = R.layout.libs_ability_activity_camera_preview

    protected override fun init() {
        previewView = findViewById(R.id.preview_view)
        fabPicture = findViewById(R.id.fabPicture)
        devicePicture = findViewById(R.id.devicePicture)
        galleryPicture = findViewById(R.id.galleryPicture)

        fabPicture.setOnClickListener {
            takePicture()
        }
        devicePicture.setOnClickListener {
            if (isPicturing()) {
                Log.w(TAG, "拍照中不允许切换摄像头")
                return@setOnClickListener
            }

            devicePicture.toggleButton(
                flag = cameraSelector == CameraSelector.DEFAULT_FRONT_CAMERA,
                rotationAngle = 180f,
                firstIcon = R.drawable.ic_outline_camera_rear,
                secondIcon = R.drawable.ic_outline_camera_front,
            ) {
                cameraSelector = if (it) {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                } else {
                    CameraSelector.DEFAULT_BACK_CAMERA
                }
                requestCamera()

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

    override fun requestCamera() {
        PermissionX.init(this)
            .permissions(
                Manifest.permission.CAMERA,
                // Build.VERSION_CODES.TIRAMISU
                if (Build.VERSION.SDK_INT >= 33) {
                    // Manifest.permission.READ_MEDIA_IMAGES
                    "android.permission.READ_MEDIA_IMAGES"
                } else {
                    Manifest.permission.READ_EXTERNAL_STORAGE
                }
            )
            .request { allGranted, _, _ ->
                Log.w(TAG, "permission allGranted = ${allGranted}")
                if (allGranted) {
                    previewCamera(previewView, cameraSelector)
                }
            }
    }

    override fun resumeCameraView() {
        super.resumeCameraView()

        val mediaList = GalleryActivity.getMedia(getDefaultCameraPictureStorageDir())
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

    override fun notifyCameraInfo(cameraInfoList: List<CameraInfo>) {
        super.notifyCameraInfo(cameraInfoList)
        if (cameraInfoList.size > 1) {
            devicePicture.visibility = View.VISIBLE
        }

    }
}