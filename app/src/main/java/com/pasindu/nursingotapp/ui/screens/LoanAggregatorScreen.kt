package com.pasindu.nursingotapp.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.pow

private val LoanBackground = Color(0xFFF7F8FC)
private val Ink = Color(0xFF0F172A)
private val Slate = Color(0xFF64748B)
private val Navy = Color(0xFF172554)
private val Indigo = Color(0xFF4338CA)
private val Cyan = Color(0xFF06B6D4)
private val Orange = Color(0xFFF97316)
private val Mint = Color(0xFF10B981)
private val Rose = Color(0xFFE11D48)
private val Border = Color(0xFFE2E8F0)

@Composable
fun LoanCalculatorScreen(
    onBack: () -> Unit
) {
    var principal by remember {
        mutableStateOf("")
    }

    var interestRate by remember {
        mutableStateOf("")
    }

    var years by remember {
        mutableStateOf("")
    }

    var monthlyPayment by remember {
        mutableStateOf(0.0)
    }

    var totalInterest by remember {
        mutableStateOf(0.0)
    }

    var totalPayment by remember {
        mutableStateOf(0.0)
    }

    var hasResult by remember {
        mutableStateOf(false)
    }

    fun calculateLoan() {
        val loanAmount =
            principal
                .replace(",", "")
                .toDoubleOrNull()
                ?: 0.0

        val annualInterest =
            interestRate
                .replace(",", "")
                .toDoubleOrNull()
                ?: 0.0

        val loanYears =
            years
                .toIntOrNull()
                ?: 0

        val months = loanYears * 12

        if (
            loanAmount <= 0.0 ||
            loanYears <= 0 ||
            months <= 0
        ) {
            monthlyPayment = 0.0
            totalInterest = 0.0
            totalPayment = 0.0
            hasResult = false
            return
        }

        val monthlyRate =
            annualInterest / 100.0 / 12.0

        monthlyPayment =
            if (monthlyRate == 0.0) {
                loanAmount / months
            } else {
                val factor =
                    (1.0 + monthlyRate).pow(months)

                loanAmount *
                        (
                                monthlyRate * factor
                                ) /
                        (factor - 1.0)
            }

        totalPayment =
            monthlyPayment * months

        totalInterest =
            (totalPayment - loanAmount)
                .coerceAtLeast(0.0)

        hasResult = true
    }

    val loanAmountValue =
        principal
            .replace(",", "")
            .toDoubleOrNull()
            ?: 0.0

    val interestRatio =
        if (totalPayment > 0.0) {
            (totalInterest / totalPayment)
                .coerceIn(0.0, 1.0)
        } else {
            0.0
        }

    val animatedInterestRatio by animateFloatAsState(
        targetValue = interestRatio.toFloat(),
        animationSpec = tween(
            durationMillis = 900,
            easing = FastOutSlowInEasing
        ),
        label = "loanRatio"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LoanBackground)
    ) {

        // =====================================================
        // HEADER
        // =====================================================

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 12.dp,
                    vertical = 10.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {

            IconButton(
                onClick = onBack
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Navy
                )
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Loan Calculator",
                    color = Ink,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Black
                )

                Text(
                    text = "EMI • Interest • Repayment",
                    color = Slate,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Surface(
                color = Orange.copy(alpha = 0.10f),
                shape = RoundedCornerShape(50)
            ) {
                Text(
                    text = "SMART LOAN",
                    modifier = Modifier.padding(
                        horizontal = 10.dp,
                        vertical = 6.dp
                    ),
                    color = Orange,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(
                    rememberScrollState()
                )
                .padding(
                    horizontal = 16.dp,
                    vertical = 10.dp
                ),
            verticalArrangement =
                Arrangement.spacedBy(15.dp)
        ) {

            // =================================================
            // HERO
            // =================================================

            LoanHeroCard()

            // =================================================
            // INPUT CARD
            // =================================================

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 3.dp
                )
            ) {

                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement =
                        Arrangement.spacedBy(12.dp)
                ) {

                    Row(
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(
                                    RoundedCornerShape(13.dp)
                                )
                                .background(
                                    Indigo.copy(alpha = 0.10f)
                                ),
                            contentAlignment =
                                Alignment.Center
                        ) {
                            Icon(
                                imageVector =
                                    Icons.Default.Calculate,
                                contentDescription = null,
                                tint = Indigo,
                                modifier =
                                    Modifier.size(22.dp)
                            )
                        }

                        Spacer(
                            modifier = Modifier.width(11.dp)
                        )

                        Column {
                            Text(
                                text = "Loan Details",
                                color = Ink,
                                fontSize = 17.sp,
                                fontWeight =
                                    FontWeight.Black
                            )

                            Text(
                                text =
                                    "Enter your bank offer details",
                                color = Slate,
                                fontSize = 10.sp
                            )
                        }
                    }

                    Spacer(
                        modifier = Modifier.height(2.dp)
                    )

                    OutlinedTextField(
                        value = principal,
                        onValueChange = {
                            principal = it
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = {
                            Text("Loan Amount (LKR)")
                        },
                        leadingIcon = {
                            Icon(
                                imageVector =
                                    Icons.Default.Payments,
                                contentDescription = null,
                                tint = Indigo
                            )
                        },
                        keyboardOptions =
                            KeyboardOptions(
                                keyboardType =
                                    KeyboardType.Decimal
                            ),
                        singleLine = true,
                        shape =
                            RoundedCornerShape(16.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement =
                            Arrangement.spacedBy(10.dp)
                    ) {

                        OutlinedTextField(
                            value = interestRate,
                            onValueChange = {
                                interestRate = it
                            },
                            modifier =
                                Modifier.weight(1f),
                            label = {
                                Text("Interest %")
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector =
                                        Icons.Default.Percent,
                                    contentDescription = null,
                                    tint = Orange
                                )
                            },
                            keyboardOptions =
                                KeyboardOptions(
                                    keyboardType =
                                        KeyboardType.Decimal
                                ),
                            singleLine = true,
                            shape =
                                RoundedCornerShape(16.dp)
                        )

                        OutlinedTextField(
                            value = years,
                            onValueChange = {
                                years = it
                            },
                            modifier =
                                Modifier.weight(1f),
                            label = {
                                Text("Years")
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector =
                                        Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = Cyan
                                )
                            },
                            keyboardOptions =
                                KeyboardOptions(
                                    keyboardType =
                                        KeyboardType.Number
                                ),
                            singleLine = true,
                            shape =
                                RoundedCornerShape(16.dp)
                        )
                    }

                    Spacer(
                        modifier = Modifier.height(3.dp)
                    )

                    Button(
                        onClick = {
                            calculateLoan()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape =
                            RoundedCornerShape(16.dp),
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = Orange
                            )
                    ) {

                        Icon(
                            imageVector =
                                Icons.Default.Calculate,
                            contentDescription = null
                        )

                        Spacer(
                            modifier =
                                Modifier.width(8.dp)
                        )

                        Text(
                            text =
                                "Calculate My EMI",
                            fontSize = 14.sp,
                            fontWeight =
                                FontWeight.Black
                        )
                    }
                }
            }

            // =================================================
            // RESULTS
            // =================================================

            if (hasResult) {

                Card(
                    modifier =
                        Modifier.fillMaxWidth(),
                    shape =
                        RoundedCornerShape(28.dp),
                    colors =
                        CardDefaults.cardColors(
                            containerColor = Navy
                        )
                ) {

                    Column(
                        modifier =
                            Modifier.padding(20.dp)
                    ) {

                        Text(
                            text =
                                "YOUR LOAN SNAPSHOT",
                            color =
                                Color.White.copy(
                                    alpha = 0.60f
                                ),
                            fontSize = 9.sp,
                            fontWeight =
                                FontWeight.Black,
                            letterSpacing = 1.1.sp
                        )

                        Spacer(
                            modifier =
                                Modifier.height(7.dp)
                        )

                        Text(
                            text =
                                "Estimated Monthly EMI",
                            color = Color.White,
                            fontSize = 12.sp
                        )

                        Spacer(
                            modifier =
                                Modifier.height(2.dp)
                        )

                        AnimatedContent(
                            targetState =
                                monthlyPayment,
                            label =
                                "emiAmount"
                        ) { amount ->

                            Text(
                                text =
                                    "LKR ${amount.currency()}",
                                color =
                                    Color.White,
                                fontSize = 30.sp,
                                fontWeight =
                                    FontWeight.Black
                            )
                        }

                        Spacer(
                            modifier =
                                Modifier.height(18.dp)
                        )

                        // -------------------------------------
                        // INTEREST RING
                        // -------------------------------------

                        Row(
                            modifier =
                                Modifier.fillMaxWidth(),
                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {

                            Box(
                                modifier =
                                    Modifier.size(115.dp),
                                contentAlignment =
                                    Alignment.Center
                            ) {

                                Canvas(
                                    modifier =
                                        Modifier.size(105.dp)
                                ) {

                                    drawArc(
                                        color =
                                            Color.White.copy(
                                                alpha = 0.12f
                                            ),
                                        startAngle = -90f,
                                        sweepAngle = 360f,
                                        useCenter = false,
                                        style = Stroke(
                                            width = 12f,
                                            cap =
                                                StrokeCap.Round
                                        )
                                    )

                                    drawArc(
                                        color = Orange,
                                        startAngle = -90f,
                                        sweepAngle =
                                            360f *
                                                    animatedInterestRatio,
                                        useCenter = false,
                                        style = Stroke(
                                            width = 12f,
                                            cap =
                                                StrokeCap.Round
                                        )
                                    )
                                }

                                Column(
                                    horizontalAlignment =
                                        Alignment.CenterHorizontally
                                ) {

                                    Text(
                                        text =
                                            "${(interestRatio * 100).toInt()}%",
                                        color =
                                            Color.White,
                                        fontSize = 18.sp,
                                        fontWeight =
                                            FontWeight.Black
                                    )

                                    Text(
                                        text =
                                            "interest",
                                        color =
                                            Color.White.copy(
                                                alpha = 0.60f
                                            ),
                                        fontSize = 9.sp
                                    )
                                }
                            }

                            Spacer(
                                modifier =
                                    Modifier.width(18.dp)
                            )

                            Column(
                                modifier =
                                    Modifier.weight(1f),
                                verticalArrangement =
                                    Arrangement.spacedBy(10.dp)
                            ) {

                                LoanResultMetric(
                                    label =
                                        "Principal",
                                    value =
                                        "LKR ${loanAmountValue.currency()}",
                                    accent = Cyan
                                )

                                LoanResultMetric(
                                    label =
                                        "Interest",
                                    value =
                                        "LKR ${totalInterest.currency()}",
                                    accent = Orange
                                )

                                LoanResultMetric(
                                    label =
                                        "Repayment",
                                    value =
                                        "LKR ${totalPayment.currency()}",
                                    accent = Mint
                                )
                            }
                        }
                    }
                }
            }

            // =================================================
            // BREAKDOWN
            // =================================================

            if (hasResult) {

                Card(
                    modifier =
                        Modifier.fillMaxWidth(),
                    shape =
                        RoundedCornerShape(24.dp),
                    colors =
                        CardDefaults.cardColors(
                            containerColor =
                                Color.White
                        )
                ) {

                    Column(
                        modifier =
                            Modifier.padding(18.dp),
                        verticalArrangement =
                            Arrangement.spacedBy(10.dp)
                    ) {

                        Text(
                            text =
                                "Repayment Breakdown",
                            color = Ink,
                            fontSize = 17.sp,
                            fontWeight =
                                FontWeight.Black
                        )

                        Spacer(
                            modifier =
                                Modifier.height(2.dp)
                        )

                        BreakdownRow(
                            label =
                                "Loan Principal",
                            value =
                                "LKR ${loanAmountValue.currency()}",
                            valueColor =
                                Indigo
                        )

                        BreakdownRow(
                            label =
                                "Total Interest",
                            value =
                                "LKR ${totalInterest.currency()}",
                            valueColor =
                                Rose
                        )

                        BreakdownRow(
                            label =
                                "Total Repayment",
                            value =
                                "LKR ${totalPayment.currency()}",
                            valueColor =
                                Navy
                        )

                        BreakdownRow(
                            label =
                                "Loan Period",
                            value =
                                "${years.toIntOrNull() ?: 0} years",
                            valueColor =
                                Slate
                        )
                    }
                }
            }

            // =================================================
            // TIP
            // =================================================

            Surface(
                modifier =
                    Modifier.fillMaxWidth(),
                color =
                    Color(0xFFFFF7ED),
                shape =
                    RoundedCornerShape(20.dp)
            ) {

                Row(
                    modifier =
                        Modifier.padding(16.dp),
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                Orange.copy(
                                    alpha = 0.12f
                                )
                            ),
                        contentAlignment =
                            Alignment.Center
                    ) {
                        Text(
                            text = "!",
                            color = Orange,
                            fontSize = 17.sp,
                            fontWeight =
                                FontWeight.Black
                        )
                    }

                    Spacer(
                        modifier =
                            Modifier.width(11.dp)
                    )

                    Text(
                        text =
                            "This is an estimate. Actual bank repayment may differ because of fees, insurance, administrative charges or changing interest rates.",
                        color =
                            Color(0xFF7C2D12),
                        fontSize = 10.sp,
                        lineHeight = 15.sp
                    )
                }
            }

            Spacer(
                modifier =
                    Modifier.height(28.dp)
            )
        }
    }
}

@Composable
private fun LoanHeroCard() {

    Card(
        modifier =
            Modifier.fillMaxWidth(),
        shape =
            RoundedCornerShape(28.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = Navy
            )
    ) {

        Box(
            modifier =
                Modifier.fillMaxWidth()
        ) {

            Canvas(
                modifier =
                    Modifier.matchParentSize()
            ) {

                drawCircle(
                    color =
                        Indigo.copy(
                            alpha = 0.25f
                        ),
                    radius = 150f,
                    center =
                        androidx.compose.ui.geometry.Offset(
                            x = size.width * 0.92f,
                            y = size.height * 0.03f
                        )
                )

                drawCircle(
                    color =
                        Orange.copy(
                            alpha = 0.15f
                        ),
                    radius = 90f,
                    center =
                        androidx.compose.ui.geometry.Offset(
                            x = size.width * 0.02f,
                            y = size.height * 0.88f
                        )
                )
            }

            Row(
                modifier =
                    Modifier.padding(20.dp),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Box(
                    modifier =
                        Modifier
                            .size(58.dp)
                            .clip(
                                RoundedCornerShape(18.dp)
                            )
                            .background(
                                Color.White.copy(
                                    alpha = 0.11f
                                )
                            ),
                    contentAlignment =
                        Alignment.Center
                ) {

                    Icon(
                        imageVector =
                            Icons.Default.AccountBalance,
                        contentDescription = null,
                        tint = Color.White,
                        modifier =
                            Modifier.size(30.dp)
                    )
                }

                Spacer(
                    modifier =
                        Modifier.width(14.dp)
                )

                Column {

                    Text(
                        text =
                            "Smart Loan Planner",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight =
                            FontWeight.Black
                    )

                    Spacer(
                        modifier =
                            Modifier.height(5.dp)
                    )

                    Text(
                        text =
                            "See the real monthly impact before you commit.",
                        color =
                            Color.White.copy(
                                alpha = 0.70f
                            ),
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun LoanResultMetric(
    label: String,
    value: String,
    accent: Color
) {

    Row(
        modifier =
            Modifier.fillMaxWidth(),
        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(accent)
        )

        Spacer(
            modifier =
                Modifier.width(8.dp)
        )

        Column {

            Text(
                text = label,
                color =
                    Color.White.copy(
                        alpha = 0.56f
                    ),
                fontSize = 9.sp
            )

            Text(
                text = value,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight =
                    FontWeight.Bold
            )
        }
    }
}

@Composable
private fun BreakdownRow(
    label: String,
    value: String,
    valueColor: Color
) {

    Row(
        modifier =
            Modifier.fillMaxWidth(),
        horizontalArrangement =
            Arrangement.SpaceBetween,
        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Text(
            text = label,
            color = Slate,
            fontSize = 11.sp
        )

        Text(
            text = value,
            color = valueColor,
            fontSize = 12.sp,
            fontWeight =
                FontWeight.Bold
        )
    }
}

private fun Double.currency(): String {

    return NumberFormat
        .getNumberInstance(Locale.US)
        .apply {
            maximumFractionDigits = 2
            minimumFractionDigits = 2
        }
        .format(this)
}