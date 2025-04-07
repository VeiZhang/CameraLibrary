package com.excellence.camera.sample

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.excellence.camera.library.BaseCameraPreviewActivity

class MainActivity : AppCompatActivity() {

    private val activityLauncher: ActivityResultLauncher<Intent> by lazy {
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val picture = result?.data?.getStringExtra(BaseCameraPreviewActivity.EXTRA_PIC)
            Toast.makeText(this, "拍照路径:$picture", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)
    }

    fun startCamera(v: View) {
        runOnUiThread {
            activityLauncher.launch(Intent(this, CameraPreviewActivity::class.java))
        }
    }

    fun startUsbCamera(v: View) {

    }
}
