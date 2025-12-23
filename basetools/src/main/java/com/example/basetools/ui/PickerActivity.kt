package com.example.basetools.ui

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.alibaba.android.arouter.facade.annotation.Route
import com.bumptech.glide.Glide
import com.example.basetools.R
import com.example.basetools.databinding.ActivityPickerBinding
import com.example.basetools.engine.GlideEngine
import com.huantansheng.easyphotos.EasyPhotos
import com.huantansheng.easyphotos.callback.SelectCallback
import com.huantansheng.easyphotos.constant.Type
import com.huantansheng.easyphotos.models.album.entity.Photo
import com.huantansheng.easyphotos.utils.permission.PermissionUtil
import com.huantansheng.easyphotos.utils.permission.PermissionUtil.PermissionCallBack
import top.limuyang2.photolibrary.LPhotoHelper
import top.limuyang2.photolibrary.activity.LPhotoPickerActivity
import top.limuyang2.photolibrary.util.LPPImageType
import top.limuyang2.photolibrary.util.LPPImageType.GIF
import top.limuyang2.photolibrary.util.LPPImageType.HEIF
import top.limuyang2.photolibrary.util.LPPImageType.JPEG
import top.limuyang2.photolibrary.util.LPPImageType.PNG
import top.limuyang2.photolibrary.util.LPPImageType.WEBP
import java.util.ArrayList
import kotlin.collections.addAll


@Route(path = "/picker/pickerImage/image")
class PickerActivity : AppCompatActivity(){
    private val CHOOSE_PHOTO_REQUEST: Int = 1001
    private lateinit var binding: ActivityPickerBinding
    private val selectedPhotoList = ArrayList<Photo?>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPickerBinding.inflate(layoutInflater)
        val root = binding.root

        setContentView(root)

        binding.picker.setOnClickListener {
            EasyPhotos.createAlbum(this, false, false, GlideEngine.instance!!)
                .setFileProviderAuthority("com.example.firstapplication.fileprovider")
                .setCount(1)
                .setCleanMenu(false)
                .setPuzzleMenu(false)
                .setGif(false)
                .setVideo(false)
                .start(101);
//            EasyPhotos.createAlbum(this, true, false, GlideEngine.instance!!)
//                .setFileProviderAuthority("com.example.firstapplication.fileprovider")
//                .start(101);
//            LPhotoHelper.Builder()
//                .maxChooseCount(6) //最多选几个
//                .columnsNumber(3) //每行显示几列图片
//                .imageType(ArrayList<String>().apply {
//                    addAll(JPEG.getType())
//                    addAll(PNG.getType())
//                    addAll(WEBP.getType())
//                }.toTypedArray()) // 文件类型
//                .pauseOnScroll(false) // 是否滑动暂停加载图片显示
//                .isSingleChoose(false) // 是否是单选
//                .isOpenLastAlbum(false) // 是否直接打开最后一次选择的相册
//                .build()
//                .start(this, CHOOSE_PHOTO_REQUEST)
//                .theme(theme) // 设置主题
        }
        binding.pickerGIf.setOnClickListener {
            EasyPhotos.createAlbum(this, false, false, GlideEngine.instance!!)
                .setFileProviderAuthority("com.example.firstapplication.fileprovider")
                .setCount(1)
                .setCleanMenu(false)
                .setPuzzleMenu(false)
                .setPuzzleMenu(false)
                .filter(Type.GIF)
                .start(101);
//            LPhotoHelper.Builder()
//                .maxChooseCount(6) //最多选几个
//                .columnsNumber(3) //每行显示几列图片
//                .imageType(ArrayList<String>().apply {
//                    addAll(GIF.getType())
//                }.toTypedArray()) // 文件类型
//                .pauseOnScroll(false) // 是否滑动暂停加载图片显示
//                .isSingleChoose(false) // 是否是单选
//                .isOpenLastAlbum(false) // 是否直接打开最后一次选择的相册
//                .build()
//                .start(this, CHOOSE_PHOTO_REQUEST)
//                .theme(theme) // 设置主题

        }
        binding.pickerEdit.setOnClickListener {
            EasyPhotos.createAlbum(this, true, false, GlideEngine.instance!!)
                .setFileProviderAuthority("com.example.firstapplication.fileprovider")
                .setCount(9)
                .onlyVideo()
                .start(101);
        }
        binding.pickerNoEdit.setOnClickListener {
            EasyPhotos.createAlbum(this, true, false, GlideEngine.instance!!)
                .setFileProviderAuthority("com.example.firstapplication.fileprovider")
                .setCount(9)
                .setGif(true)
                .start(101);
        }
        if (PermissionUtil.checkPermissionsInActivity(this,
                Manifest.permission.READ_EXTERNAL_STORAGE)) {
            preLoadAlbums();
        }
//        binding = PickerActivityBinding.inflate(layoutInflater)


    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String?>,
        grantResults: IntArray,
        deviceId: Int
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults, deviceId)
        PermissionUtil.onPermissionResult(
            this, permissions, grantResults,
            object : PermissionCallBack {
                override fun onSuccess() {
                    preLoadAlbums()
                }

                override fun onShouldShow() {
                }

                override fun onFailed() {
                }
            })
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
//        if (albumItemsAdView != null) {
//            if (albumItemsAdView.getParent() != null) {
//                ((albumItemsAdView.getParent()) as FrameLayout).removeAllViews()
//            }
//        }
//        if (photosAdView != null) {
//            if (photosAdView.getParent() != null) {
//                ((photosAdView.getParent()) as FrameLayout).removeAllViews()
//            }
//        }
        if (RESULT_OK == resultCode) {
            //相机或相册回调
            if (requestCode == 101) {
                //返回对象集合：如果你需要了解图片的宽、高、大小、用户是否选中原图选项等信息，可以用这个
                val resultPhotos: ArrayList<Photo?>? =
                    data?.getParcelableArrayListExtra<Photo?>(EasyPhotos.RESULT_PHOTOS)
                selectedPhotoList.clear()
                selectedPhotoList.addAll(resultPhotos!!)
                Glide.with(this).load(selectedPhotoList.elementAtOrNull(0)?.path).into(binding.image)
                return
            }
            if (resultCode == CHOOSE_PHOTO_REQUEST){
                val selectedPhotos = LPhotoHelper.getSelectedPhotos(data)
//                selectedPhotoList.clear()
//                selectedPhotoList.addAll(selectedPhotos.elementAtOrNull(0)?.toString()?:"")
                Glide.with(this).load(selectedPhotos.elementAtOrNull(0)?.toString()?:"").into(binding.image)

            }

            //为拼图选择照片的回调
            if (requestCode == 102) {
                val resultPhotos: ArrayList<Photo?>? =
                    data!!.getParcelableArrayListExtra<Photo?>(EasyPhotos.RESULT_PHOTOS)
                if (resultPhotos!!.size == 1) {
                    resultPhotos.add(resultPhotos.get(0))
                }
                selectedPhotoList.clear()
                selectedPhotoList.addAll(resultPhotos)
                GlideEngine.instance?.let { engine ->
                    EasyPhotos.createAlbum(this, true, false, engine)
                        .setFileProviderAuthority("com.example.firstapplication.fileprovider")
                        .start(101)
                } ?: run {
                    // 处理 engine 为空的情况
                    Toast.makeText(this, "图片加载引擎初始化失败", Toast.LENGTH_SHORT).show()
                }
//                EasyPhotos.startPuzzleWithPhotos(
//                    this, selectedPhotoList,
//                    Environment.getExternalStorageDirectory().getAbsolutePath(),
//                    "AlbumBuilder", 103, false, GlideEngine.instance!!
//                )
                return
            }

            //拼图回调
            if (requestCode == 103) {
                val puzzlePhoto: Photo? = data!!.getParcelableExtra(EasyPhotos.RESULT_PHOTOS)
                selectedPhotoList.clear()
                selectedPhotoList.add(puzzlePhoto)
                Glide.with(this).load(selectedPhotoList.elementAtOrNull(0)?.path).into(binding.image)

            }
        } else if (RESULT_CANCELED == resultCode) {
            Toast.makeText(getApplicationContext(), "cancel", Toast.LENGTH_SHORT).show()
        }
    }
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        PermissionUtil.onPermissionResult(
            this, permissions, grantResults,
            object : PermissionCallBack {
                override fun onSuccess() {
                    preLoadAlbums()
                }

                override fun onShouldShow() {
                }

                override fun onFailed() {
                }
            })
    }
    /**
     * 预加载相册扫描，可以增加点速度，写不写都行
     * 该方法如果没有授权读取权限的话，是无效的，所以外部加不加权限控制都可以，加的话保证执行，不加也不影响程序正常使用。
     */
    private fun preLoadAlbums() {
        EasyPhotos.preLoad(this)
    }

}