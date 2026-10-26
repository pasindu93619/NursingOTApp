package com.pasindu.nursingotapp.transfer.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pasindu.nursingotapp.ui.theme.OnTransferDangerContainer
import com.pasindu.nursingotapp.ui.theme.OnTransferInfoContainer
import com.pasindu.nursingotapp.ui.theme.OnTransferSuccessContainer
import com.pasindu.nursingotapp.ui.theme.OnTransferWarningContainer
import com.pasindu.nursingotapp.ui.theme.TransferDangerContainer
import com.pasindu.nursingotapp.ui.theme.TransferInfoContainer
import com.pasindu.nursingotapp.ui.theme.TransferSuccessContainer
import com.pasindu.nursingotapp.ui.theme.TransferWarningContainer

enum class TransferBannerStyle {
    INFO,
    SUCCESS,
    WARNING,
    DANGER
}

/**
 * Shared stateful banner announcing live state changes via accessibility liveRegion.
 */
@Composable
fun TransferStatusBanner(
    title: String,
    description: String? = null,
    icon: ImageVector? = null,
    style: TransferBannerStyle = TransferBannerStyle.INFO,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null
) {
    val (containerColor, contentColor) = when (style) {
        TransferBannerStyle.INFO -> TransferInfoContainer to OnTransferInfoContainer
        TransferBannerStyle.SUCCESS -> TransferSuccessContainer to OnTransferSuccessContainer
        TransferBannerStyle.WARNING -> TransferWarningContainer to OnTransferWarningContainer
        TransferBannerStyle.DANGER -> TransferDangerContainer to OnTransferDangerContainer
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite },
        shape = RoundedCornerShape(14.dp),
        color = containerColor,
        border = BorderStroke(1.dp, contentColor.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(12.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = contentColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                if (!description.isNullOrBlank()) {
                    Text(
                        text = description,
                        color = contentColor.copy(alpha = 0.9f),
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                }
            }
            if (action != null) {
                Spacer(Modifier.width(8.dp))
                action()
            }
        }
    }
}
