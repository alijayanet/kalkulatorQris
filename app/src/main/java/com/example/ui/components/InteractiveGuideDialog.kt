package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.QrisRed
import com.example.ui.theme.QrisTeal

data class GuideStep(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val badge: String
)

@Composable
fun InteractiveGuideDialog(
    onDismiss: () -> Unit
) {
    val steps = remember {
        listOf(
            GuideStep(
                title = "Kalkulator Kasir Cerdas",
                description = "Masukkan nominal belanja pelanggan menggunakan keypad numerik lengkap dengan perhitungan perkalian, pembagian, persen, atau tombol nominal instan (+10rb, +50rb).",
                icon = Icons.Default.Calculate,
                badge = "Langkah 1 dari 5"
            ),
            GuideStep(
                title = "Generate QRIS Otomatis",
                description = "Tekan tombol 'GENERATE QRIS'. Keypad numerik akan otomatis berganti menjadi kode QRIS Dinamis nasional dengan nominal yang pas sesuai kalkulator.",
                icon = Icons.Default.QrCode,
                badge = "Langkah 2 dari 5"
            ),
            GuideStep(
                title = "Notifikasi Real-Time",
                description = "Pelanggan scan QRIS dari e-wallet/m-banking apa saja (BCA, GoPay, OVO, ShopeePay, Dana, dll). Notifikasi push muncul seketika saat pembayaran selesai.",
                icon = Icons.Default.Notifications,
                badge = "Langkah 3 dari 5"
            ),
            GuideStep(
                title = "Cetak Struk & Bagikan",
                description = "Simpan gambar QRIS langsung ke Galeri, bagikan via WhatsApp/Sosmed, atau cetak struk pembayaran via printer Bluetooth Thermal 58mm/80mm.",
                icon = Icons.Default.Print,
                badge = "Langkah 4 dari 5"
            ),
            GuideStep(
                title = "Upload QRIS & Enkripsi Data",
                description = "Gunakan menu Master QRIS untuk mengunggah QRIS toko Anda. Seluruh identitas bisnis dan data transaksi Anda dilindungi dengan Enkripsi AES-256.",
                icon = Icons.Default.Lock,
                badge = "Langkah 5 dari 5"
            )
        )
    }

    var currentStepIndex by remember { mutableIntStateOf(0) }
    val step = steps[currentStepIndex]

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("interactive_guide_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Progress Indicators
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    ) {
                        Text(
                            text = step.badge,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    TextButton(onClick = onDismiss) {
                        Text("Lewati", color = MaterialTheme.colorScheme.outline)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Animated step content
                AnimatedContent(
                    targetState = currentStepIndex,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "guide_step"
                ) { index ->
                    val current = steps[index]
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(88.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = current.icon,
                                contentDescription = current.title,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(44.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = current.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = current.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            lineHeight = 22.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Step Dots
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    steps.forEachIndexed { i, _ ->
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .size(
                                    width = if (i == currentStepIndex) 24.dp else 8.dp,
                                    height = 8.dp
                                )
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    if (i == currentStepIndex) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outlineVariant
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Navigation Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (currentStepIndex > 0) {
                        OutlinedButton(
                            onClick = { currentStepIndex-- },
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Sebelumnya")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Kembali")
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    if (currentStepIndex < steps.size - 1) {
                        Button(
                            onClick = { currentStepIndex++ },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Text("Lanjut")
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.ArrowForward, contentDescription = "Lanjut")
                        }
                    } else {
                        Button(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = "Mulai")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Mulai Sekarang")
                        }
                    }
                }
            }
        }
    }
}
