package top.anorak01.wearcord.presentation.login

import android.graphics.Bitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import top.anorak01.wearcord.presentation.LoginReply
import top.anorak01.wearcord.presentation.MFAReply
import top.anorak01.wearcord.presentation.network.ApiResult
import top.anorak01.wearcord.presentation.network.DiscordApi

suspend fun sendLogin(email: String, password: String): ApiResult<LoginReply> {
    val body = buildJsonObject {
        put("login", email)
        put("password", password)
        put("undelete", false)
    }
    return DiscordApi.postWithoutAuth("auth/login", body)
}

suspend fun send2FA(ticket: String, code: String): ApiResult<String> {
    val body = buildJsonObject {
        put("code", code)
        put("ticket", ticket)
        put("login_source", null as String?)
        put("gift_code_sku_id", null as String?)
    }
    return when (val result = DiscordApi.postWithoutAuth<MFAReply>("auth/mfa/totp", body)) {
        is ApiResult.Failure -> result
        is ApiResult.Success -> ApiResult.Success(result.data.token)
    }
}

fun generateQrCodeBitmap(text: String, width: Int = 512, height: Int = 512): Bitmap? {
    if (text.isBlank()) return null
    return try {
        val bitMatrix: BitMatrix = MultiFormatWriter().encode(
            "https://discordapp.com/ra/$text",
            BarcodeFormat.QR_CODE,
            width,
            height,
            null,
        )
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) 0 else 255)
            }
        }
        bitmap
    } catch (_: Exception) {
        null
    }
}
