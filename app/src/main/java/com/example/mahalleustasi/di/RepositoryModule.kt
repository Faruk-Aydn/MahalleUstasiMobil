package com.example.mahalleustasi.di

import com.example.mahalleustasi.data.repository.AuthRepositoryImpl
import com.example.mahalleustasi.domain.repository.AuthRepository
import com.example.mahalleustasi.data.repository.ChatRepositoryImpl
import com.example.mahalleustasi.domain.repository.ChatRepository
import com.example.mahalleustasi.data.repository.JobRepositoryImpl
import com.example.mahalleustasi.domain.repository.JobRepository
import com.example.mahalleustasi.data.repository.OfferRepositoryImpl
import com.example.mahalleustasi.domain.repository.OfferRepository
import com.example.mahalleustasi.data.repository.RentalRepositoryImpl
import com.example.mahalleustasi.domain.repository.RentalRepository
import com.example.mahalleustasi.data.repository.ReviewRepositoryImpl
import com.example.mahalleustasi.domain.repository.ReviewRepository
import com.example.mahalleustasi.data.repository.UserRepositoryImpl
import com.example.mahalleustasi.domain.repository.UserRepository
import com.example.mahalleustasi.data.repository.StorageRepositoryImpl
import com.example.mahalleustasi.domain.repository.StorageRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds @Singleton
    abstract fun bindJobRepository(impl: JobRepositoryImpl): JobRepository

    @Binds @Singleton
    abstract fun bindOfferRepository(impl: OfferRepositoryImpl): OfferRepository

    @Binds @Singleton
    abstract fun bindUserRepository(impl: UserRepositoryImpl): UserRepository

    @Binds @Singleton
    abstract fun bindChatRepository(impl: ChatRepositoryImpl): ChatRepository

    @Binds @Singleton
    abstract fun bindReviewRepository(impl: ReviewRepositoryImpl): ReviewRepository

    @Binds @Singleton
    abstract fun bindRentalRepository(impl: RentalRepositoryImpl): RentalRepository

    @Binds @Singleton
    abstract fun bindStorageRepository(impl: StorageRepositoryImpl): StorageRepository
}
