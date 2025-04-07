package com.excellence.camera.sample

import android.app.Activity
import android.content.Intent
import android.graphics.Point
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.GridView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.excellence.basetoolslibrary.baseadapter.CommonAdapter
import com.excellence.basetoolslibrary.baseadapter.ViewHolder
import com.excellence.camera.library.BaseCameraPreviewActivity
import com.excellence.camera.library.BaseCameraPreviewActivity.Companion.getDefaultCameraPictureStorageDir
import java.io.File
import java.util.Collections

/**
 * <pre>
 *     author : VeiZhang
 *     blog   : http://tiimor.cn
 *     time   : 2024/11/2
 *     desc   :
 * </pre>
 */
class GalleryActivity : AppCompatActivity() {

    companion object {

        fun getMedia(dir: File): List<Media> {
            val items = mutableListOf<Media>()

            dir.listFiles()?.forEach {
                if (it.isDirectory) {
                    items.addAll(getMedia(it))
                } else {
                    if (isImage(it.path)) {
                        items.add(Media(it.path, it.lastModified()))
                    }
                }
            }

            Collections.sort(items, object : Comparator<Media> {
                override fun compare(o1: Media, o2: Media): Int {
                    if (o1.date > o2.date) {
                        return -1
                    } else {
                        return 1
                    }
                }
            })

            return items
        }

        fun isImage(path: String): Boolean {
            return path.endsWith(".jpg")
                    || path.endsWith(".jpeg")
                    || path.endsWith(".png")
                    || path.endsWith(".bmp")
//                || path.endsWith(".gif")
        }
    }

    private lateinit var tvTip: TextView
    private lateinit var topLayout: RelativeLayout
    private lateinit var gridView: GridView
    private lateinit var ivDelete: ImageView
    private lateinit var deleteLayout: LinearLayout
    private lateinit var ivConfirm: ImageView
    private lateinit var ivCancel: ImageView

    private var deleteMode = false
    private lateinit var adapter: CommonAdapter<Media?>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.libs_ability_activity_gallery)

        tvTip = findViewById(R.id.tv_tip)
        topLayout = findViewById(R.id.top_layout)
        gridView = findViewById(R.id.grid_view)
        ivDelete = findViewById(R.id.iv_delete)
        deleteLayout = findViewById(R.id.delete_layout)
        ivConfirm = findViewById(R.id.iv_confirm)
        ivCancel = findViewById(R.id.iv_cancel)

        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val point = Point()
        wm.defaultDisplay.getSize(point)

        val mediaList = getMedia(getDefaultCameraPictureStorageDir())

        if (mediaList.isNullOrEmpty()) {
            tvTip.visibility = View.VISIBLE
            topLayout.visibility = View.GONE
        } else {
            tvTip.visibility = View.GONE
            topLayout.visibility = View.VISIBLE
        }

        adapter = object : CommonAdapter<Media?>(mediaList, R.layout.libs_gallery_item) {

            override fun convert(viewHolder: ViewHolder, item: Media?, position: Int) {
                val imageView = viewHolder.getView<ImageView>(R.id.iv_gallery)
                imageView?.let {
                    val params = imageView.layoutParams
                    if (point.x > 0) {
                        val size = point.x / 3
                        params.width = size
                        params.height = size
                        imageView.layoutParams = params
                    }

                    if (item == null) {
                        it.setImageResource(R.drawable.ic_no_picture)
                    } else {
                        Glide.with(viewHolder.getConvertView()).load(item.path).into(it)
                    }
                }

                viewHolder.setVisible(R.id.iv_select, item?.selected ?: false)
            }

        }
        gridView.adapter = adapter
        gridView.setOnItemClickListener { parent, view, position, id ->
            if (position < 0 || position >= adapter.count) {
                return@setOnItemClickListener
            }

            val item = adapter.getItem(position) ?: return@setOnItemClickListener

            if (deleteMode) {
                item.selected = !item.selected
                adapter.notifyDataSetChanged()
            } else {
                val intent = Intent().apply {
                    putExtra(BaseCameraPreviewActivity.EXTRA_PIC, item?.path)
                }
                setResult(Activity.RESULT_OK, intent)
                finish()
            }
        }

        ivDelete.setOnClickListener {
            toggleDeleteMode()
        }
        ivCancel.setOnClickListener {
            toggleDeleteMode()
            val list = adapter.mData
            for (item in list) {
                item?.selected = false
            }
            adapter.notifyDataSetChanged()
        }
        ivConfirm.setOnClickListener {
            val list = adapter.mData
            for (item in list) {
                if (item?.selected == true) {
                    File(item.path).delete()
                }
            }
            val mediaList = getMedia(getDefaultCameraPictureStorageDir())
            adapter.notifyNewData(mediaList)
            toggleDeleteMode()
        }
    }

    private fun toggleDeleteMode() {
        deleteMode = !deleteMode
        if (deleteMode) {
            ivDelete.visibility = View.GONE
            deleteLayout.visibility = View.VISIBLE
        } else {
            ivDelete.visibility = View.VISIBLE
            deleteLayout.visibility = View.GONE
        }
    }
}