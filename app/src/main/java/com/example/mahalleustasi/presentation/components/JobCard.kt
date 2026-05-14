package com.example.mahalleustasi.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.mahalleustasi.domain.model.Job
import com.example.mahalleustasi.domain.model.JobStatus
import com.example.mahalleustasi.ui.theme.BrandOrange80
import com.example.mahalleustasi.ui.theme.ForestGreen40

@Composable
fun JobCard(
    job: Job,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick    = onClick,
        modifier   = modifier.fillMaxWidth(),
        shape      = RoundedCornerShape(16.dp),
        elevation  = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors     = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment   = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Kategori chip
                CategoryChip(label = job.category.displayName)

                // Durum chip
                StatusChip(status = job.status)
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text       = job.title,
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines   = 2,
                overflow   = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text     = job.description,
                style    = MaterialTheme.typography.bodySmall,
                color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Konum
                Text(
                    text  = "📍 ${job.location.address.ifBlank { "Konum belirtilmedi" }}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )

                // Bütçe
                if (!job.budget.isNullOrBlank()) {
                    Text(
                        text       = job.budget,
                        style      = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color      = BrandOrange80
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text  = "👤 ${job.postedByUserName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text  = "${job.offerCount} teklif",
                    style = MaterialTheme.typography.labelSmall,
                    color = ForestGreen40
                )
            }
        }
    }
}

@Composable
private fun CategoryChip(label: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(ForestGreen40.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text  = label,
            style = MaterialTheme.typography.labelSmall,
            color = ForestGreen40,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun StatusChip(status: JobStatus) {
    val (bg, fg) = when (status) {
        JobStatus.OPEN         -> ForestGreen40.copy(0.15f) to ForestGreen40
        JobStatus.IN_PROGRESS  -> BrandOrange80.copy(0.15f) to BrandOrange80
        JobStatus.WAITING_CONFIRMATION -> Color(0xFF673AB7).copy(0.15f) to Color(0xFF673AB7)
        JobStatus.COMPLETED    -> Color.Gray.copy(0.15f)    to Color.Gray
        JobStatus.CANCELLED    -> MaterialTheme.colorScheme.error.copy(0.15f) to MaterialTheme.colorScheme.error
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text  = status.displayName,
            style = MaterialTheme.typography.labelSmall,
            color = fg,
            fontWeight = FontWeight.SemiBold
        )
    }
}
