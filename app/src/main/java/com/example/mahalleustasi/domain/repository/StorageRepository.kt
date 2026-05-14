package com.example.mahalleustasi.domain.repository

import android.net.Uri
import com.example.mahalleustasi.core.util.Resource

interface StorageRepository {
    suspend fun uploadImage(uri: Uri, path: String): Resource<String>
}
