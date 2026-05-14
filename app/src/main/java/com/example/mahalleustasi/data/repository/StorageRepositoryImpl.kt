package com.example.mahalleustasi.data.repository

import android.net.Uri
import com.example.mahalleustasi.core.util.Resource
import com.example.mahalleustasi.domain.repository.StorageRepository
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject

class StorageRepositoryImpl @Inject constructor(
    private val storage: FirebaseStorage
) : StorageRepository {

    override suspend fun uploadImage(uri: Uri, path: String): Resource<String> {
        return try {
            val fileName = UUID.randomUUID().toString() + ".jpg"
            val fileRef = storage.reference.child(path).child(fileName)
            
            // Yükleme işlemi
            fileRef.putFile(uri).await()
            
            // URL'yi al
            val downloadUrl = fileRef.downloadUrl.await().toString()
            Resource.Success(downloadUrl)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Fotoğraf yüklenirken bir hata oluştu.")
        }
    }
}
