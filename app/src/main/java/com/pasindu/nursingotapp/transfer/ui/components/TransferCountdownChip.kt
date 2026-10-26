package com.pasindu.nursingotapp.transfer.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pasindu.nursingotapp.ui.theme.OnTransferDangerContainer
import com.pasindu.nursingotapp.ui.theme.OnTransferWarningContainer
import com.pasindu.nursingotapp.ui.theme.TransferDangerContainer
import com.pasindu.nursingotapp.ui.theme.TransferWarningContainer

/**
 * Shared countdown chip with WCAG 2.2 AA/AAA compliant contrast.
 * High-urgency (<6h) switches to danger container.
 */
@Composable
fun TransferCountdownChip(
    text: String,
    isUrgent: Boolean = false,
    modifier: Modifier = Modifier
) {
    val containerColor = if (isUrgent) TransferDangerContainer else TransferWarningContainer
    val contentColor = if (isUrgent) OnTransferDangerContainer else OnTransferWarningContainer

    Surface(
        shape = RoundedCornerShape(999.dp),
        color = containerColor,
        border = BorderStroke(1.dp, contentColor.copy(alpha = 0.35f)),
        modifier = modifier.heightIn(min = 28.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            Icon(
                imageVector = Icons.Default.AccessTime,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(5.dp))
            Text(
                text = text,
                color = contentColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
