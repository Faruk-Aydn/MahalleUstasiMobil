package com.example.mahalleustasi.domain.repository

import com.example.mahalleustasi.core.util.Resource
import com.example.mahalleustasi.domain.model.Job
import com.example.mahalleustasi.domain.model.JobStatus
import kotlinx.coroutines.flow.Flow

interface JobRepository {
    fun getJobs(): Flow<Resource<List<Job>>>
    fun getJobsByUserId(userId: String): Flow<Resource<List<Job>>>
    suspend fun createJob(job: Job): Resource<Unit>
    suspend fun getJobById(jobId: String): Resource<Job>
    suspend fun updateJobStatus(jobId: String, status: JobStatus, acceptedOfferId: String? = null): Resource<Unit>
}

