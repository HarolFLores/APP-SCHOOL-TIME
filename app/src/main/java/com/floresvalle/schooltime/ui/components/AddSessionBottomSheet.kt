package com.floresvalle.schooltime.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSessionBottomSheet(
    onDismiss: () -> Unit,
    onAddSession: (name: String, date: String, start: String, end: String, modality: String, room: String?, url: String?) -> Unit
) {
    val todayStr = remember {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        sdf.format(Date())
    }

    var courseName by remember { mutableStateOf("") }
    var sessionDate by remember { mutableStateOf(todayStr) }
    var startTime by remember { mutableStateOf("08:00") }
    var endTime by remember { mutableStateOf("10:00") }
    var modalityIndex by remember { mutableIntStateOf(0) }
    var locationRoom by remember { mutableStateOf("") }

    val modalities = listOf("Presencial", "Virtual", "Híbrido")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Nueva Sesión de Clase",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            OutlinedTextField(
                value = courseName,
                onValueChange = { courseName = it },
                label = { Text("Nombre del Curso / Asignatura") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = sessionDate,
                onValueChange = { sessionDate = it },
                label = { Text("Fecha (YYYY-MM-DD)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = startTime,
                    onValueChange = { startTime = it },
                    label = { Text("Inicio (HH:mm)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = endTime,
                    onValueChange = { endTime = it },
                    label = { Text("Fin (HH:mm)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            Text("Modalidad", style = MaterialTheme.typography.labelLarge)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                modalities.forEachIndexed { index, label ->
                    SegmentedButton(
                        selected = modalityIndex == index,
                        onClick = { modalityIndex = index },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = modalities.size)
                    ) {
                        Text(label)
                    }
                }
            }

            OutlinedTextField(
                value = locationRoom,
                onValueChange = { locationRoom = it },
                label = { Text(if (modalityIndex == 1) "Enlace de Reunión (URL)" else "Aula / Pabellón") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    if (courseName.isNotBlank()) {
                        val modality = modalities[modalityIndex]
                        val room = if (modalityIndex != 1) locationRoom.ifBlank { null } else null
                        val url = if (modalityIndex == 1) locationRoom.ifBlank { null } else null
                        onAddSession(courseName, sessionDate, startTime, endTime, modality, room, url)
                        onDismiss()
                    }
                },
                enabled = courseName.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Crear Sesión")
            }
        }
    }
}
