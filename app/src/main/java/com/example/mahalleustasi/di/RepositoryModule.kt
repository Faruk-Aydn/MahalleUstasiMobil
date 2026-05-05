package com.example.mahalleustasi.di

import com.example.mahalleustasi.data.repository.AuthRepositoryImpl
import com.example.mahalleustasi.domain.repository.AuthRepository
import com.example.mahalleustasi.data.repository.JobRepositoryImpl
import com.example.mahalleustasi.domain.repository.JobRepository
import com.example.mahalleustasi.data.repository.OfferRepositoryImpl
import com.example.mahalleustasi.domain.repository.OfferRepository
import com.example.mahalleustasi.data.repository.UserRepositoryImpl
import com.example.mahalleustasi.domain.repository.UserRepository
import com.example.mahalleustasi.data.repository.ChatRepositoryImpl
import com.example.mahalleustasi.domain.repository.ChatRepository
import com.example.mahalleustasi.data.repository.ReviewRepositoryImpl
import com.example.mahalleustasi.domain.repository.ReviewRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Repository interface → Implementation bağlamaları.
 * @Binds: interface'i concrete class'a bağlar, Hilt ihtiyacı olan yerde inject eder.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        authRepositoryImpl: AuthRepositoryImpl
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindJobRepository(
        jobRepositoryImpl: JobRepositoryImpl
    ): JobRepository

    @Binds
    @Singleton
    abstract fun bindOfferRepository(
        offerRepositoryImpl: OfferRepositoryImpl
    ): OfferRepository

    @Binds
    @Singleton
    abstract fun bindUserRepository(
        userRepositoryImpl: UserRepositoryImpl
    ): UserRepository

    @Binds
    @Singleton
    abstract fun bindChatRepository(
        chatRepositoryImpl: ChatRepositoryImpl
    ): ChatRepository

    @Binds
    @Singleton
    abstract fun bindReviewRepository(
        reviewRepositoryImpl: ReviewRepositoryImpl
    ): ReviewRepository
}
