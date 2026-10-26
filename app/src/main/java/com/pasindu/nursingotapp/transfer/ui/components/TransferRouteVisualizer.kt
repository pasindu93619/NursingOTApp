package com.pasindu.nursingotapp.transfer.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pasindu.nursingotapp.ui.theme.ClinicalPrimaryColor
import com.pasindu.nursingotapp.ui.theme.Emerald
import com.pasindu.nursingotapp.ui.theme.OnTransferInfoContainer
import com.pasindu.nursingotapp.ui.theme.OnTransferThreeWayContainer
import com.pasindu.nursingotapp.ui.theme.TextPrimary
import com.pasindu.nursingotapp.ui.theme.TextSecondary
import com.pasindu.nursingotapp.ui.theme.TransferCardBorder
import com.pasindu.nursingotapp.ui.theme.TransferCardSurface
import com.pasindu.nursingotapp.ui.theme.TransferInfoContainer
import com.pasindu.nursingotapp.ui.theme.TransferThreeWayContainer

/**
 * Shared Transfer Route Summary card representing 2-way direct and 3-way circular routes.
 */
@Composable
fun TransferRouteVisualizer(
    matchType: String,
    originHospitalName: String,
    destinationHospitalName: String,
    modifier: Modifier = Modifier,
    destinationProvince: String? = null,
    originProvince: String? = null
) {
    val isThreeWay = matchType.equals("THREE_WAY", ignoreCase = true)
    val badgeContainer = if (isThreeWay) TransferThreeWayContainer else TransferInfoContainer
    val badgeContent = if (isThreeWay) OnTransferThreeWayContainer else OnTransferInfoContainer
    val typeLabel = if (isThreeWay) "3-Way Circular Exchange" else "Direct 2-Way Exchange"

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = TransferCardSurface),
        border = BorderStroke(1.dp, TransferCardBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = badgeContainer
                ) {
                    Text(
                        text = typeLabel,
                        color = badgeContent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                Icon(
                    imageVector = Icons.Default.SwapHoriz,
                    contentDescription = null,
                    tint = ClinicalPrimaryColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(Modifier.height(14.dp))

            // Origin
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = TransferInfoContainer,
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.LocalHospital,
                            contentDescription = null,
                            tint = ClinicalPrimaryColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Current Station",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                    Text(
                        text = originHospitalName,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                    if (!originProvince.isNullOrBlank()) {
                        Text(
                            text = originProvince,
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // Arrow down / destination transition
            Row(
                modifier = Modifier.padding(start = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = Emerald,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(18.dp))
                Text(
                    text = if (isThreeWay) "Transfers circular to" else "Transfers directly to",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }

            Spacer(Modifier.height(10.dp))

            // Destination
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = badgeContainer,
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.LocalHospital,
                            contentDescription = null,
                            tint = badgeContent,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Target Posting",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                    Text(
                        text = destinationHospitalName,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    if (!destinationProvince.isNullOrBlank()) {
                        Text(
                            text = destinationProvince,
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}
