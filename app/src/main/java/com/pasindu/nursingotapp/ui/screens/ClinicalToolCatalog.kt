package com.pasindu.nursingotapp.ui.screens

import androidx.compose.ui.graphics.Color

internal data class ClinicalToolCatalogItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val description: String,
    val icon: String,
    val accent: Color,
    val keywords: List<String>,
    val sinhalaKeywords: List<String>
)
