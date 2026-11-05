package com.darkforge.bloodsouls.util

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import java.util.Locale

object LocaleHelper {

    fun applyLocale(context: Context, languageName: String): Context {
        val localeCode = when (languageName.uppercase()) {
            "AFRIKAANS" -> "af"
            "XHOSA" -> "xh"
            "TSWANA" -> "tn"
            else -> "en"
        }

        val locale = Locale(localeCode)
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocales(LocaleList(locale))
        } else {
            @Suppress("DEPRECATION")
            config.locale = locale
        }

        @Suppress("DEPRECATION")
        context.resources.updateConfiguration(config, context.resources.displayMetrics)
        return context.createConfigurationContext(config)
    }
}
