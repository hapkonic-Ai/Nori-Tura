package com.example.nori_tura

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform