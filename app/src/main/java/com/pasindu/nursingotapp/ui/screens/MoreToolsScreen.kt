package com.pasindu.nursingotapp.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WorkHistory
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pasindu.nursingotapp.ui.theme.AiAccentColor
import com.pasindu.nursingotapp.ui.theme.AppBackground
import com.pasindu.nursingotapp.ui.theme.ClinicalAiGradient
import com.pasindu.nursingotapp.ui.theme.ClinicalPrimaryColor
import com.pasindu.nursingotapp.ui.theme.Emerald
import com.pasindu.nursingotapp.ui.theme.Purple
import com.pasindu.nursingotapp.ui.theme.SurfaceWhite
import com.pasindu.nursingotapp.ui.theme.TextPrimary
import com.pasindu.nursingotapp.ui.theme.TextSecondary

private data class MoreTool(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val accent: Color,
    val surface: Color,
    val route: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreToolsScreen(
    onNavigate: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val tools = remember {
        listOf(
            MoreTool(
                "Nurse Command Center",
                "Your daily workload, wellness and next-action view.",
                Icons.Default.AutoAwesome,
                AiAccentColor,
                Color(0xFFF2EDFF),
                "nurse_command_center"
            ),
            MoreTool(
                "Care Pulse",
                "Review your wellness signals and recovery picture.",
                Icons.Default.HealthAndSafety,
                Emerald,
                Color(0xFFEAFBF5),
                "care_pulse"
            ),
            MoreTool(
                "Smart Insights",
                "Explore OT patterns, claims and workload trends.",
                Icons.Default.Analytics,
                ClinicalPrimaryColor,
                Color(0xFFEAF6FF),
                "analytics"
            ),
            MoreTool(
                "Knowledge & CPD",
                "Circulars, study resources and professional development.",
                Icons.Default.School,
                Purple,
                Color(0xFFF2EDFF),
                "knowledge_hub"
            ),
            MoreTool(
                "Clinical Planning",
                "ISBAR notes, clinical tasks and bedside planning.",
                Icons.Default.TaskAlt,
                ClinicalPrimaryColor,
                Color(0xFFEAF6FF),
                "clinical_planning"
            ),
            MoreTool(
                "Pay Sheet Bank",
                "Open your saved payment and document records.",
                Icons.Default.WorkHistory,
                Emerald,
                Color(0xFFEAFBF5),
                "pay_sheet_bank"
            ),
            MoreTool(
                "Profile",
                "Manage your nurse profile and core work settings.",
                Icons.Default.Person,
                Purple,
                Color(0xFFF2EDFF),
                "profile"
            )
        )
    }

    Scaffold(
        containerColor = AppBackground,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                title = {
                    Column {
                        Text("More", color = TextPrimary, fontWeight = FontWeight.Black)
                        Text(
                            "Everything else in your NursingOS workspace",
                            color = TextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppBackground)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(26.dp),
                    color = Color.Transparent
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(ClinicalAiGradient, RoundedCornerShape(26.dp))
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            "NURSINGOS WORKSPACE",
                            color = SurfaceWhite.copy(alpha = 0.72f),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.4.sp
                        )
                        Text(
                            "Find your next workspace",
                            color = SurfaceWhite,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            "Use the five main destinations for everyday work. This space keeps supporting tools close without overcrowding the main navigation.",
                            color = SurfaceWhite.copy(alpha = 0.86f),
                            fontSize = 10.sp,
                            lineHeight = 15.sp
                        )
                    }
                }
            }

            item {
                Text(
                    "Supporting workspaces",
                    color = TextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(top = 4.dp, start = 2.dp)
                )
            }

            items(tools) { tool ->
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn() + slideInVertically(initialOffsetY = { 12 })
                ) {
                    MoreToolCard(tool) {
                        onNavigate(tool.route)
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expanded = !expanded },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(15.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.WaterDrop,
                            contentDescription = null,
                            tint = ClinicalPrimaryColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Navigation philosophy",
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                if (expanded) "Main destinations stay stable; deeper tools open only when needed. This keeps clinical workflows focused and reduces back-stack clutter."
                                else "Why are some tools here instead of the bottom bar?",
                                color = TextSecondary,
                                fontSize = 10.sp,
                                lineHeight = 14.sp
                            )
                        }
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MoreToolCard(tool: MoreTool, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(46.dp),
                shape = RoundedCornerShape(14.dp),
                color = tool.surface
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        tool.icon,
                        contentDescription = null,
                        tint = tool.accent,
                        modifier = Modifier.size(23.dp)
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    tool.title,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    tool.description,
                    color = TextSecondary,
                    fontSize = 10.sp,
                    lineHeight = 14.sp
                )
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "Open ${tool.title}",
                tint = tool.accent.copy(alpha = 0.75f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
