package com.floresvalle.schooltime.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Domain
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.floresvalle.schooltime.ui.theme.SchoolTimeTheme
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSessionBottomSheet(
    onDismiss: () -> Unit,
    onAddSession: (name: String, date: String, start: String, end: String, modality: String, room: String?, url: String?) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    var courseName by remember { mutableStateOf("") }
    var selectedModalityIndex by remember { mutableIntStateOf(0) }
    val modalities = listOf("VIRTUAL", "PRESENTIAL")
    val modalityLabels = listOf("Virtual", "Presencial")
    
    var roomOrUrl by remember { mutableStateOf("") }

    val todayDate = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }

    var startTime by remember { mutableStateOf("08:00") }
    var endTime by remember { mutableStateOf("10:00") }

    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    val isFormValid = courseName.isNotBlank() && roomOrUrl.isNotBlank()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Nueva Clase",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth()
            ) {
                modalities.forEachIndexed { index, modality ->
                    SegmentedButton(
                        selected = selectedModalityIndex == index,
                        onClick = { selectedModalityIndex = index },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = modalities.size)
                    ) {
                        Text(modalityLabels[index])
                    }
                }
            }

            OutlinedTextField(
                value = courseName,
                onValueChange = { courseName = it },
                label = { Text("Nombre de la Materia") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Book, contentDescription = "Materia")
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                singleLine = true
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = startTime,
                    onValueChange = {},
                    label = { Text("Inicio") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Schedule, contentDescription = "Hora Inicio")
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    readOnly = true,
                    singleLine = true,
                    interactionSource = remember { MutableInteractionSource() }.also { interactionSource ->
                        LaunchedEffect(interactionSource) {
                            interactionSource.interactions.collect {
                                if (it is PressInteraction.Release) {
                                    showStartTimePicker = true
                                }
                            }
                        }
                    }
                )

                OutlinedTextField(
                    value = endTime,
                    onValueChange = {},
                    label = { Text("Fin") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Schedule, contentDescription = "Hora Fin")
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    readOnly = true,
                    singleLine = true,
                    interactionSource = remember { MutableInteractionSource() }.also { interactionSource ->
                        LaunchedEffect(interactionSource) {
                            interactionSource.interactions.collect {
                                if (it is PressInteraction.Release) {
                                    showEndTimePicker = true
                                }
                            }
                        }
                    }
                )
            }

            OutlinedTextField(
                value = roomOrUrl,
                onValueChange = { roomOrUrl = it },
                label = { Text(if (selectedModalityIndex == 0) "Enlace de Videollamada" else "Aula / Pabellón") },
                leadingIcon = {
                    Icon(
                        imageVector = if (selectedModalityIndex == 0) Icons.Default.Link else Icons.Default.Domain,
                        contentDescription = "Ubicación"
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    val currentModality = modalities[selectedModalityIndex]
                    onAddSession(
                        courseName,
                        todayDate,
                        startTime,
                        endTime,
                        currentModality,
                        if (currentModality == "PRESENTIAL") roomOrUrl else null,
                        if (currentModality == "VIRTUAL") roomOrUrl else null
                    )
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = isFormValid,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("Guardar Clase", style = MaterialTheme.typography.titleMedium)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showStartTimePicker) {
        TimePickerDialogCustom(
            onDismissRequest = { showStartTimePicker = false },
            onTimeSelected = { hour, minute ->
                startTime = String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
                showStartTimePicker = false
            }
        )
    }

    if (showEndTimePicker) {
        TimePickerDialogCustom(
            onDismissRequest = { showEndTimePicker = false },
            onTimeSelected = { hour, minute ->
                endTime = String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
                showEndTimePicker = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialogCustom(
    onDismissRequest: () -> Unit,
    onTimeSelected: (Int, Int) -> Unit
) {
    val timePickerState = rememberTimePickerState(
        initialHour = 8,
        initialMinute = 0,
        is24Hour = true
    )

    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(onClick = { onTimeSelected(timePickerState.hour, timePickerState.minute) }) {
                Text("Aceptar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancelar")
            }
        },
        text = {
            TimePicker(state = timePickerState)
        }
    )
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AddSessionBottomSheetPreview() {
    SchoolTimeTheme {
        AddSessionBottomSheet(
            onDismiss = {},
            onAddSession = { _, _, _, _, _, _, _ -> }
        )
    }
}
