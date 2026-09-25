package com.paydart.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paydart.app.ui.theme.DartBorder
import com.paydart.app.ui.theme.DartCyan
import com.paydart.app.ui.theme.DartDarkBackground
import com.paydart.app.ui.theme.DartSurface
import com.paydart.app.ui.theme.DartSurfaceVariant
import com.paydart.app.ui.theme.DartTextPrimary
import com.paydart.app.ui.theme.DartTextSecondary
import com.paydart.app.upi.UpiAppRegistry
import com.paydart.app.upi.UpiPaymentRequest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfirmPaymentSheet(
    request: UpiPaymentRequest,
    preferredPackage: String?,
    sheetState: SheetState,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val targetAppName = UpiAppRegistry.getAppName(context, preferredPackage)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DartSurface,
        dragHandle = {
            Surface(
                modifier = Modifier.padding(vertical = 12.dp),
                color = DartBorder,
                shape = RoundedCornerShape(3.dp)
            ) {
                Spacer(modifier = Modifier.size(width = 36.dp, height = 4.dp))
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Confirm Payment",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = DartTextPrimary
                )
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = DartSurfaceVariant,
                    border = BorderStroke(1.dp, DartBorder)
                ) {
                    Text(
                        text = targetAppName,
                        color = DartCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Payee & Amount details card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = DartDarkBackground,
                border = BorderStroke(1.dp, DartBorder)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "PAYING TO",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = DartTextSecondary,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = request.displayName,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = DartTextPrimary
                    )
                    Text(
                        text = request.payeeVpa,
                        fontSize = 13.sp,
                        color = DartTextSecondary
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 14.dp),
                        color = DartBorder
                    )

                    Text(
                        text = "AMOUNT",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = DartTextSecondary,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    val amountStr = request.formattedAmount()
                    if (amountStr != null) {
                        Text(
                            text = amountStr,
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Bold,
                            color = DartCyan
                        )
                    } else {
                        Text(
                            text = "Amount not specified in QR",
                            fontSize = 14.sp,
                            color = DartTextSecondary
                        )
                    }

                    if (!request.note.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Note: ${request.note}",
                            fontSize = 13.sp,
                            color = DartTextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, DartBorder),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Cancel", modifier = Modifier.size(18.dp))
                    Text(text = " Cancel", color = DartTextPrimary)
                }

                Button(
                    onClick = onConfirm,
                    colors = ButtonDefaults.buttonColors(containerColor = DartCyan),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1.5f)
                ) {
                    Text(
                        text = "Pay Now ",
                        color = DartDarkBackground,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Confirm",
                        tint = DartDarkBackground,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
