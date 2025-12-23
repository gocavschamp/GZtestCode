package com.ywm.baselibray.pag

import android.content.Context
import android.util.LruCache
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import org.libpag.PAGFile
import org.libpag.PAGView
import java.io.File

class PagPlayerManager private constructor() {

    companion object {
        @Volatile
        private var instance: PagPlayerManager? = null

        fun getInstance(): PagPlayerManager {
            return instance ?: synchronized(this) {
                instance ?: PagPlayerManager().also { instance = it }
            }
        }

        private const val CACHE_DIR_NAME = "pag_cache"
    }

    // 内存缓存 - 使用软引用避免内存泄漏
    private val memoryCache = object : LruCache<String, PagCacheItem>(10) {
        override fun entryRemoved(
            evicted: Boolean,
            key: String,
            oldValue: PagCacheItem,
            newValue: PagCacheItem?
        ) {
            // 当缓存项被移除时，如果没有被使用，则释放资源
            if (oldValue.refCount <= 0) {
                releasePagFileResources(oldValue)
            }
        }
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    private var cacheDir: File? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // 正在下载的任务
    private val downloadingTasks = mutableMapOf<String, Job>()

    fun init(context: Context) {
        cacheDir = File(context.cacheDir, CACHE_DIR_NAME).apply {
            if (!exists()) mkdirs()
        }
    }

    /**
     * 加载 PAG 动画
     */
    fun loadPagAnimation(
        url: String,
        pagView: PAGView,
        autoPlay: Boolean = true,
        repeatCount: Int = -1, // -1 表示循环播放
        listener: PagLoadListener? = null
    ) {
        val cacheKey = generateCacheKey(url)

        // 检查内存缓存
        memoryCache.get(cacheKey)?.let { cacheItem ->
            synchronized(cacheItem) {
                cacheItem.refCount++
                // 直接使用 PAGFile，不检查 isValid
                if (cacheItem.pagFile != null) {
                    setPagToView(pagView, cacheItem.pagFile!!, autoPlay, repeatCount)
                    listener?.onSuccess()
                    return
                } else {
                    // 如果 PAGFile 为 null，从缓存中移除
                    memoryCache.remove(cacheKey)
                }
            }
        }

        // 检查磁盘缓存
        val cachedFile = getCachedFile(cacheKey)
        if (cachedFile?.exists() == true) {
            scope.launch {
                try {
                    val pagFile = withContext(Dispatchers.IO) {
                        try {
                            PAGFile.Load(cachedFile.absolutePath)
                        } catch (e: Exception) {
                            null
                        }
                    }
                    if (pagFile != null) {
                        val cacheItem = PagCacheItem(pagFile).apply { refCount = 1 }
                        memoryCache.put(cacheKey, cacheItem)
                        withContext(Dispatchers.Main) {
                            setPagToView(pagView, pagFile, autoPlay, repeatCount)
                            listener?.onSuccess()
                        }
                    } else {
                        // 删除损坏的缓存文件
                        cachedFile.delete()
                        withContext(Dispatchers.Main) {
                            listener?.onError("Failed to load PAG file from cache")
                        }
                    }
                } catch (e: Exception) {
                    // 删除损坏的缓存文件
                    cachedFile.delete()
                    withContext(Dispatchers.Main) {
                        listener?.onError("Error loading cached PAG: ${e.message}")
                    }
                }
            }
            return
        }

        // 需要下载
        listener?.onLoading()
        downloadPagFile(url, cacheKey, pagView, autoPlay, repeatCount, listener)
    }

    private fun downloadPagFile(
        url: String,
        cacheKey: String,
        pagView: PAGView,
        autoPlay: Boolean,
        repeatCount: Int,
        listener: PagLoadListener?
    ) {
        // 如果已经在下载，取消之前的下载
        downloadingTasks[cacheKey]?.cancel()

        val job = scope.launch {
            try {
                val request = Request.Builder().url(url).build()
                val response = withContext(Dispatchers.IO) {
                    okHttpClient.newCall(request).execute()
                }

                if (response.isSuccessful) {
                    response.body?.byteStream()?.use { inputStream ->
                        // 保存到缓存文件
                        val cachedFile = getCachedFile(cacheKey)
                        cachedFile?.outputStream()?.use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }

                        // 加载 PAG 文件
                        val pagFile = withContext(Dispatchers.IO) {
                            try {
                                PAGFile.Load(cachedFile!!.absolutePath)
                            } catch (e: Exception) {
                                null
                            }
                        }

                        if (pagFile != null) {
                            val cacheItem = PagCacheItem(pagFile).apply { refCount = 1 }
                            memoryCache.put(cacheKey, cacheItem)

                            withContext(Dispatchers.Main) {
                                setPagToView(pagView, pagFile, autoPlay, repeatCount)
                                downloadingTasks.remove(cacheKey)
                                listener?.onSuccess()
                            }
                        } else {
                            // 删除损坏的缓存文件
                            cachedFile?.delete()
                            withContext(Dispatchers.Main) {
                                downloadingTasks.remove(cacheKey)
                                listener?.onError("Failed to load downloaded PAG file")
                            }
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        downloadingTasks.remove(cacheKey)
                        listener?.onError("Download failed: ${response.code}")
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException) {
                    // 下载被取消，正常情况
                    return@launch
                }
                withContext(Dispatchers.Main) {
                    downloadingTasks.remove(cacheKey)
                    listener?.onError("Download error: ${e.message}")
                }
            }
        }

        downloadingTasks[cacheKey] = job
    }

    private fun setPagToView(
        pagView: PAGView,
        pagFile: PAGFile,
        autoPlay: Boolean,
        repeatCount: Int
    ) {
        try {
            // 先停止当前的播放
            pagView.stop()
            pagView.flush()

            // 设置新的 PAG 文件
            pagView.composition = pagFile
            pagView.setRepeatCount(repeatCount)
            pagView.addListener(object : PAGView.PAGViewListener {
                override fun onAnimationStart(p0: PAGView?) {
                    TODO("Not yet implemented")
                }

                override fun onAnimationEnd(p0: PAGView?) {
                    TODO("Not yet implemented")
                }

                override fun onAnimationCancel(p0: PAGView?) {
                    TODO("Not yet implemented")
                }

                override fun onAnimationRepeat(p0: PAGView?) {
                    TODO("Not yet implemented")
                }

                override fun onAnimationUpdate(p0: PAGView?) {
                    TODO("Not yet implemented")
                }

            })

            if (autoPlay) {
                pagView.play()
            }
        } catch (e: Exception) {
            throw RuntimeException("Failed to set PAG file to view", e)
        }
    }

    /**
     * 安全释放 PAGFile 资源
     */
    private fun releasePagFileResources(cacheItem: PagCacheItem) {
        try {
            // PAGFile 没有 destroy 方法，我们通过以下方式释放资源：
            // 1. 确保没有 PAGView 在使用这个 PAGFile
            // 2. 将引用设为 null，让 GC 自动回收
            cacheItem.pagFile = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 释放 PAGView 资源
     */
    fun releasePagView(pagView: PAGView, url: String? = null) {
        try {
            pagView.stop()
            pagView.composition = null
            pagView.flush()

            url?.let {
                val cacheKey = generateCacheKey(it)
                memoryCache.get(cacheKey)?.let { cacheItem ->
                    synchronized(cacheItem) {
                        cacheItem.refCount--
                        if (cacheItem.refCount <= 0) {
                            releasePagFileResources(cacheItem)
                            memoryCache.remove(cacheKey)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 暂停播放
     */
    fun pause(pagView: PAGView) {
        try {
            if (pagView.isPlaying) {
                pagView.stop()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 恢复播放
     */
    fun resume(pagView: PAGView) {
        try {
            if (!pagView.isPlaying && pagView.composition != null) {
                pagView.play()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 预加载 PAG 文件到缓存
     */
    fun preloadPag(url: String, listener: PagLoadListener? = null) {
        val cacheKey = generateCacheKey(url)

        // 如果已经在内存或磁盘中，不需要预加载
        if (memoryCache.get(cacheKey) != null || getCachedFile(cacheKey)?.exists() == true) {
            listener?.onSuccess()
            return
        }

        // 下载但不设置到任何 View
        downloadPagFileForPreload(url, cacheKey, listener)
    }

    private fun downloadPagFileForPreload(
        url: String,
        cacheKey: String,
        listener: PagLoadListener?
    ) {
        downloadingTasks[cacheKey]?.cancel()

        val job = scope.launch {
            try {
                val request = Request.Builder().url(url).build()
                val response = withContext(Dispatchers.IO) {
                    okHttpClient.newCall(request).execute()
                }

                if (response.isSuccessful) {
                    response.body?.byteStream()?.use { inputStream ->
                        val cachedFile = getCachedFile(cacheKey)
                        cachedFile?.outputStream()?.use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }

                        // 验证文件是否可以加载
                        val pagFile = withContext(Dispatchers.IO) {
                            try {
                                PAGFile.Load(cachedFile!!.absolutePath)
                            } catch (e: Exception) {
                                null
                            }
                        }

                        if (pagFile != null) {
                            // 预加载成功，但不放入内存缓存（等到实际使用时再加载）
                            withContext(Dispatchers.Main) {
                                downloadingTasks.remove(cacheKey)
                                listener?.onSuccess()
                            }
                        } else {
                            cachedFile?.delete()
                            withContext(Dispatchers.Main) {
                                downloadingTasks.remove(cacheKey)
                                listener?.onError("Preload failed: invalid PAG file")
                            }
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        downloadingTasks.remove(cacheKey)
                        listener?.onError("Preload download failed: ${response.code}")
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException) return@launch
                withContext(Dispatchers.Main) {
                    downloadingTasks.remove(cacheKey)
                    listener?.onError("Preload error: ${e.message}")
                }
            }
        }

        downloadingTasks[cacheKey] = job
    }

    /**
     * 清除指定 URL 的缓存
     */
    fun clearCache(url: String) {
        val cacheKey = generateCacheKey(url)

        // 清除内存缓存
        memoryCache.remove(cacheKey)?.let { cacheItem ->
            releasePagFileResources(cacheItem)
        }

        // 清除磁盘缓存
        getCachedFile(cacheKey)?.delete()

        // 取消正在下载的任务
        downloadingTasks[cacheKey]?.cancel()
        downloadingTasks.remove(cacheKey)
    }

    /**
     * 清除所有缓存
     */
    fun clearAllCache() {
        // 清除所有内存缓存
        memoryCache.snapshot().forEach { (key, cacheItem) ->
            releasePagFileResources(cacheItem)
        }
        memoryCache.evictAll()

        // 清除所有磁盘缓存
        cacheDir?.listFiles()?.forEach { file ->
            if (file.extension == "pag") {
                file.delete()
            }
        }

        // 取消所有下载任务
        downloadingTasks.values.forEach { it.cancel() }
        downloadingTasks.clear()
    }

    /**
     * 获取缓存统计信息
     */
    fun getCacheStats(): CacheStats {
        val memoryCacheSize = memoryCache.snapshot().size
        val diskCacheSize = cacheDir?.listFiles()?.count { it.extension == "pag" } ?: 0
        val downloadingCount = downloadingTasks.size

        return CacheStats(memoryCacheSize, diskCacheSize, downloadingCount)
    }

    /**
     * 停止并释放所有资源
     */
    fun destroy() {
        scope.cancel()

        // 清除所有下载任务
        downloadingTasks.values.forEach { it.cancel() }
        downloadingTasks.clear()

        // 释放所有缓存
        clearAllCache()
    }

    private fun generateCacheKey(url: String): String {
        return url.hashCode().toString()
    }

    private fun getCachedFile(cacheKey: String): File? {
        return cacheDir?.let { File(it, "$cacheKey.pag") }
    }

    data class PagCacheItem(
        var pagFile: PAGFile?,
        var refCount: Int = 0
    )

    data class CacheStats(
        val memoryCacheCount: Int,
        val diskCacheCount: Int,
        val downloadingCount: Int
    )

    interface PagLoadListener {
        fun onLoading()
        fun onSuccess()
        fun onError(error: String)
    }
}