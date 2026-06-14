package com.example.nori_tura.util

import platform.Foundation.NSURL
import platform.UIKit.UIApplication

actual fun openUrl(url: String) {
    NSURL(string = url)?.let {
        UIApplication.sharedApplication.openURL(it)
    }
}
