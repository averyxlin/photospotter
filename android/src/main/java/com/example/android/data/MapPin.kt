package com.example.android.data

import com.google.android.gms.maps.model.LatLng

data class MapPin (
    val title: String,
    val description: String,
    val latLng: LatLng,
    val image: String? // Assuming image is stored as a file path or URL
)