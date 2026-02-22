package com.debk007.template.di

import com.debk007.template.network.ApiService
import com.debk007.template.repository.Repository
import com.debk007.template.repository.RepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun providesApiService(): ApiService {
        val json = Json {
            ignoreUnknownKeys = true // Prevents crashes if API adds new fields
            coerceInputValues = true
        }
        
        return Retrofit.Builder()
            // Hardcoding the Anime API URL since local.properties is missing
            .baseUrl("https://api.jikan.moe/v4/") 
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(ApiService::class.java)
    }

    @Provides
    @Singleton
    fun providesRepository(repositoryImpl: RepositoryImpl): Repository = repositoryImpl
}
