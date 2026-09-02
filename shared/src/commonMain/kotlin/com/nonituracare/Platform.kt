package com.nonituracare

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform