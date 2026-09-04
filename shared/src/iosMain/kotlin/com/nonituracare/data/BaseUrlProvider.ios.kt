package com.nonituracare.data

import platform.Foundation.NSBundle

actual fun getBaseUrl(): String =
    (NSBundle.mainBundle.objectForInfoDictionaryKey("BASE_URL") as? String)
        ?: "https://nori-tura.primeworld.tech"
