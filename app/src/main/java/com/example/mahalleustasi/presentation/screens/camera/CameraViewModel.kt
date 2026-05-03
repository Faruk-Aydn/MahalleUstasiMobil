package com.example.mahalleustasi.presentation.screens.camera

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mahalleustasi.core.util.Resource
import com.example.mahalleustasi.domain.model.AiAnalysisResult
import com.example.mahalleustasi.domain.usecase.ai.AnalyzeImageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val analyzeImageUseCase: AnalyzeImageUseCase
) : ViewModel() {

    private val _analysisState = MutableStateFlow<Resource<AiAnalysisResult>>(Resource.Idle)
    val analysisState: StateFlow<Resource<AiAnalysisResult>> = _analysisState.asStateFlow()

    fun analyzeImage(bitmap: Bitmap) {
        viewModelScope.launch {
            analyzeImageUseCase(bitmap).collect { result ->
                _analysisState.value = result
            }
        }
    }

    fun resetState() {
        _analysisState.value = Resource.Idle
    }
}
