package com.aritxonly.bdfun.infra.bili.message

import android.net.Uri
import com.aritxonly.bdfun.core.domain.ChatUploadedImage
import com.aritxonly.bdfun.infra.bili.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

internal suspend fun BiliApiContext.imUploadImage(reference: String): ChatUploadedImage = withContext(Dispatchers.IO) {
    val uri = Uri.parse(reference)
    val resolver = appContext.contentResolver
    val bytes = checkNotNull(resolver.openInputStream(uri)) { "无法读取图片" }.use { it.readBytes() }
    val mime = resolver.getType(uri) ?: "image/jpeg"
    val body = MultipartBody.Builder().setType(MultipartBody.FORM)
        .addFormDataPart("file_up", "image.${mime.substringAfter('/')}", bytes.toRequestBody(mime.toMediaTypeOrNull()))
        .addFormDataPart("biz", "im").addFormDataPart("csrf", csrf).build()
    val request = Request.Builder().url("$BILI_API/x/dynamic/feed/draw/upload_bfs")
        .header("User-Agent", BILI_UA).header("Referer", "https://www.bilibili.com/").post(body).build()
    http.await(request).use { response ->
        check(response.isSuccessful) { "图片上传失败 (${response.code})" }
        val root = JSONObject(checkNotNull(response.body).string())
        check(root.optInt("code") == 0) { root.optString("message", "图片上传失败") }
        val data = root.getJSONObject("data")
        ChatUploadedImage(data.getString("image_url"), data.getInt("image_width"), data.getInt("image_height"), data.optDouble("img_size"), mime.substringAfter('/'))
    }
}
