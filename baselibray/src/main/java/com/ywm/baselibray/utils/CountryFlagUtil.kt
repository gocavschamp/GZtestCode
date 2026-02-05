package com.ywm.baselibray.utils

import android.content.Context
import android.graphics.drawable.Drawable


/**
 * 国家国旗资源工具类
 */
object CountryFlagUtil {

    /**
     * 根据国家代码获取国旗资源ID
     * @param countryCode 国家代码（如：us, cn, uk等）
     * @return 国旗资源ID，如果找不到返回0
     */
    fun getFlagResourceId(context: Context, countryCode: String): Int {
        if (countryCode.isBlank()) return 0

        // 处理特殊国家代码（Java关键字等）
        val safeCountryCode = when (countryCode.lowercase()) {
            "do" -> "flag_do"  // 多米尼加，避免Java关键字冲突
            "if" -> "flag_if"  // 避免Java关键字冲突
            "for" -> "flag_for" // 避免Java关键字冲突
            else -> "flag_${countryCode.lowercase()}"
        }

        return context.resources.getIdentifier(
            safeCountryCode,
            "drawable",
            context.packageName
        )
    }

    /**
     * 根据国家代码获取国旗Drawable
     * @param countryCode 国家代码（如：us, cn, uk等）
     * @return 国旗Drawable，如果找不到返回null
     */
    fun getFlagDrawable(context: Context, countryCode: String): Drawable? {
        val resourceId = getFlagResourceId(context, countryCode)
        return if (resourceId != 0) {
            context.getDrawable(resourceId)
        } else {
            null
        }
    }

    /**
     * 安全的获取国旗Drawable，如果找不到返回默认国旗
     * @param countryCode 国家代码
     * @param defaultFlagResId 默认国旗资源ID（可选）
     * @return 国旗Drawable
     */
    fun getFlagDrawableSafe(
        context: Context,
        countryCode: String,
        defaultFlagResId: Int = 0
    ): Drawable? {
        val drawable = getFlagDrawable(context, countryCode)
        return if (drawable != null) {
            drawable
        } else if (defaultFlagResId != 0) {
            context.getDrawable(defaultFlagResId)
        } else {
            null
        }
    }
}

/**
 * Context扩展函数 - 根据国家代码获取国旗资源ID
 */
fun Context.getFlagResourceId(countryCode: String): Int {
    return CountryFlagUtil.getFlagResourceId(this, countryCode)
}

/**
 * Context扩展函数 - 根据国家代码获取国旗Drawable
 */
fun Context.getFlagDrawable(countryCode: String): Drawable? {
    return CountryFlagUtil.getFlagDrawable(this, countryCode)
}

/**
 * Context扩展函数 - 安全的获取国旗Drawable
 */
fun Context.getFlagDrawableSafe(
    countryCode: String,
    defaultFlagResId: Int = 0
): Drawable? {
    return CountryFlagUtil.getFlagDrawableSafe(this, countryCode, defaultFlagResId)
}

/**
 * String扩展函数 - 将国家代码转换为国旗资源ID
 */
fun String.toFlagResourceId(context: Context): Int {
    return CountryFlagUtil.getFlagResourceId(context, this)
}

/**
 * String扩展函数 - 将国家代码转换为国旗Drawable
 */
fun String.toFlagDrawable(context: Context): Drawable? {
    return CountryFlagUtil.getFlagDrawable(context, this)
}