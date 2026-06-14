package com.example.nori_tura.util

import androidx.compose.ui.geometry.Offset

expect fun encodeSignatureToPngBase64(paths: List<List<Offset>>, width: Int, height: Int): String
