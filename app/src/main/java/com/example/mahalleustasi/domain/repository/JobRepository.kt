package com.example.mahalleustasi.domain.repository

import com.example.mahalleustasi.core.util.Resource
import com.example.mahalleustasi.domain.model.Job
import kotlinx.coroutines.flow.Flow

interface JobRepository {
    fun getJobs(): Flow<Resource<List<Job>>>
    suspend fun createJob(job: Job): Resource<Unit>
    suspend fun getJobById(jobId: String): Resource<Job>
}
