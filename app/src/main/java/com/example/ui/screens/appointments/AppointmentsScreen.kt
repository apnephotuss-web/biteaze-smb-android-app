package com.example.ui.screens.appointments

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.database.entity.OrderEntity
import com.example.core.voice.OperationMode
import com.example.core.voice.VoiceState
import com.example.ui.components.VoiceTriggerFAB
import com.example.ui.screens.billing.BillingViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppointmentsScreen(
    viewModel: BillingViewModel = viewModel(
        factory = BillingViewModel.Factory(LocalContext.current.applicationContext as android.app.Application)
    )
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Tabs: 0: Today, 1: Tomorrow, 2: All Appointments
    var selectedDayOffset by remember { mutableStateOf(0) }
    val dayNames = listOf("Today", "Tomorrow", "All Appointments")

    var showCreateDialog by remember { mutableStateOf(false) }

    // Dialog state fields
    var inputName by remember { mutableStateOf("") }
    var inputPhone by remember { mutableStateOf("") }
    var inputDate by remember { mutableStateOf("") }
    var inputTime by remember { mutableStateOf("") }
    var inputPeriod by remember { mutableStateOf("Morning") }
    var inputDurationMinutes by remember { mutableStateOf<Int?>(60) } // Default 1 hour
    var isVoiceProcessing by remember { mutableStateOf(false) }

    // Trigger local date and time defaults
    LaunchedEffect(showCreateDialog) {
        if (showCreateDialog) {
            val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val sdfTime = SimpleDateFormat("HH:mm", Locale.getDefault())
            val now = Calendar.getInstance()
            if (inputDate.isBlank()) inputDate = sdfDate.format(now.time)
            if (inputTime.isBlank()) inputTime = sdfTime.format(now.time)
        }
    }

    // Speech-to-Appointment Quick Creator helper
    fun fallbackLocalVoiceCommand(text: String) {
        val lower = text.lowercase()
        // Format expected: [client name], [date/day], appointment, [time], [appointment length]
        // Example: "John Doe, tomorrow, appointment, 10 AM, 60 minutes"
        val parts = text.split(",").map { it.trim() }
        
        var parsedName = ""
        var parsedDate = ""
        var parsedTime = ""
        var parsedDurationMinutes = 60
        
        if (parts.size >= 4) {
            parsedName = parts[0].split(" ").joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
            val datePart = parts[1].lowercase()
            val timePart = parts[3].lowercase()
            
            // Time part
            if (timePart.contains(":")) {
               parsedTime = timePart.replace(Regex("[^0-9:amp ]"), "").trim().uppercase()
            } else {
               // Extract number and AM/PM
               val num = timePart.replace(Regex("[^0-9]"), "").toIntOrNull()
               if (num != null) {
                   val isPm = timePart.contains("pm")
                   val isAm = timePart.contains("am")
                   val finalHour = if (isPm && num < 12) num + 12 else if (isAm && num == 12) 0 else num
                   parsedTime = String.format(Locale.getDefault(), "%02d:00", finalHour)
               }
            }
            
            // Date part
            val now = Calendar.getInstance()
            val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            if (datePart.contains("tomorrow")) {
                val tomorrow = Calendar.getInstance()
                tomorrow.add(Calendar.DAY_OF_YEAR, 1)
                parsedDate = sdfDate.format(tomorrow.time)
            } else if (datePart.contains("today")) {
                 parsedDate = sdfDate.format(now.time)
            } else {
                // simple fuzzy match for month
                val months = listOf("january", "february", "march", "april", "may", "june", "july", "august", "september", "october", "november", "december")
                for ((idx, m) in months.withIndex()) {
                    if (datePart.contains(m)) {
                        val numStr = datePart.replace(Regex("[^0-9]"), "")
                        val dayVal = numStr.toIntOrNull() ?: 1
                        val cal = Calendar.getInstance()
                        cal.set(Calendar.MONTH, idx)
                        cal.set(Calendar.DAY_OF_MONTH, dayVal)
                        if (cal.before(now)) cal.add(Calendar.YEAR, 1)
                        parsedDate = sdfDate.format(cal.time)
                        break
                    }
                }
            }
            
            // Duration part
            if (parts.size >= 5) {
               val durStr = parts[4].lowercase()
               val num = durStr.replace(Regex("[^0-9]"), "").toIntOrNull()
               if (num != null) {
                  if (durStr.contains("hour") || durStr.contains("hr")) {
                      parsedDurationMinutes = num * 60
                  } else {
                      parsedDurationMinutes = num
                  }
               }
            }
        }

        // Pre-fill state and open dialog!
        if (parsedName.isNotBlank()) inputName = parsedName
        if (parsedDate.isNotBlank()) inputDate = parsedDate
        if (parsedTime.isNotBlank()) inputTime = parsedTime
        inputDurationMinutes = parsedDurationMinutes

        showCreateDialog = true
        Toast.makeText(context, "Voice pre-filled (Pattern match)", Toast.LENGTH_SHORT).show()
    }

    fun processVoiceCommand(text: String) {
        if (text.isBlank()) return
        isVoiceProcessing = true
        scope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val currentDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            
            val simplifiedServicesJson = "[" + state.products.joinToString(",") { 
                "{\"id\":\"${it.id}\",\"name\":\"${it.name.replace("\"", "\\\"")}\"}" 
            } + "]"
            val simplifiedStaffJson = "[" + state.staffList.joinToString(",") { 
                "{\"id\":\"${it.id}\",\"name\":\"${it.name.replace("\"", "\\\"")}\"}" 
            } + "]"

            try {
                val parser = com.example.core.voice.GeminiParser()
                val result = parser.parseBookingVoiceCommand(
                    rawText = text,
                    currentDateStr = currentDateStr,
                    servicesJson = simplifiedServicesJson,
                    staffJson = simplifiedStaffJson
                )

                launch(kotlinx.coroutines.Dispatchers.Main) {
                    isVoiceProcessing = false
                    if (result != null) {
                        inputName = result.clientName
                        inputDate = result.date
                        inputTime = result.time
                        inputDurationMinutes = result.durationMinutes
                        
                        val staffLabel = if (!result.matchedStaffName.isNullOrBlank()) "with ${result.matchedStaffName}" else ""
                        val serviceLabel = if (!result.matchedServiceName.isNullOrBlank()) "for ${result.matchedServiceName}" else ""
                        
                        val extraInfo = listOfNotNull(serviceLabel.ifBlank { null }, staffLabel.ifBlank { null }).joinToString(" ")
                        if (extraInfo.isNotBlank()) {
                            inputPhone = extraInfo
                        } else {
                            inputPhone = ""
                        }
                        
                        val hour = result.time.split(":").firstOrNull()?.toIntOrNull() ?: 12
                        inputPeriod = when {
                            hour < 12 -> "Morning"
                            hour < 17 -> "Afternoon"
                            else -> "Evening"
                        }

                        showCreateDialog = true
                        Toast.makeText(context, "Gemini parsed booking successfully!", Toast.LENGTH_LONG).show()
                    } else {
                        fallbackLocalVoiceCommand(text)
                    }
                }
            } catch (e: Exception) {
                launch(kotlinx.coroutines.Dispatchers.Main) {
                    isVoiceProcessing = false
                    fallbackLocalVoiceCommand(text)
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header with simple styling
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "All Bookings",
                        fontWeight = FontWeight.Black,
                        fontSize = 22.sp,
                        color = Color(0xFF0F172A)
                    )
                }
            }

            if (isVoiceProcessing) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    color = Color(0xFFF97316),
                    trackColor = Color(0xFFE2E8F0)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Navigation tabs or Day Selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                dayNames.forEachIndexed { index, name ->
                    val isSelected = selectedDayOffset == index
                    val labelCal = Calendar.getInstance()
                    labelCal.add(Calendar.DAY_OF_YEAR, index)
                    val dayStr = if (index < 2) {
                        "${labelCal.get(Calendar.DAY_OF_MONTH)} ${labelCal.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault())}"
                    } else {
                        "Upcoming"
                    }

                    Card(
                        onClick = { selectedDayOffset = index },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) Color(0xFFF97316) else Color.White
                        ),
                        border = BorderStroke(1.dp, if (isSelected) Color(0xFFF97316) else Color(0xFFE2E8F0)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("appointment_tab_$index")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (isSelected) Color.White else Color(0xFF0F172A),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = dayStr,
                                fontSize = 11.sp,
                                color = if (isSelected) Color.White.copy(alpha = 0.8f) else Color(0xFF64748B)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Display of either Calendars List or Upcoming List
            if (selectedDayOffset == 2) {
                // Show list of all future/upcoming appointments
                val futureAppointments = state.orders.filter {
                    it.scheduledStartTime != null && it.scheduledStartTime >= System.currentTimeMillis() - 1800000L
                }.sortedBy { it.scheduledStartTime ?: 0L }

                if (futureAppointments.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Outlined.Event,
                                contentDescription = null,
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No upcoming appointments", color = Color(0xFF94A3B8), fontSize = 14.sp)
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(futureAppointments) { order ->
                            val dateStr = SimpleDateFormat("EEE, d MMM yyyy", Locale.getDefault()).format(Date(order.scheduledStartTime ?: 0L))
                            val timeStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(order.scheduledStartTime ?: 0L))

                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
                                shape = RoundedCornerShape(24.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .shadow(elevation = 1.dp, shape = RoundedCornerShape(24.dp), clip = false)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = order.customerName ?: "Walk-in Guest",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = Color(0xFF0F172A)
                                        )
                                        if (!order.customerPhone.isNullOrBlank()) {
                                            Text(
                                                text = "Phone: ${order.customerPhone}",
                                                fontSize = 13.sp,
                                                color = Color(0xFF64748B)
                                            )
                                        }
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(top = 4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Schedule,
                                                contentDescription = null,
                                                tint = Color(0xFFF97316),
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "$dateStr | $timeStr",
                                                fontSize = 13.sp,
                                                color = Color(0xFF0F172A),
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }

                                    // Button to trigger active POS billing context
                                    Button(
                                        onClick = {
                                            // Active bill context launcher or detail action can be triggered from POS screen
                                            Toast.makeText(context, "Appointment loaded. Go to the Billing tab to checkout.", Toast.LENGTH_LONG).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFF7ED), contentColor = Color(0xFFF97316)),
                                        border = BorderStroke(1.dp, Color(0xFFFFEDD5)),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text("Selected", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Show local Day hourly slots list (All 24 hours)
                val hoursList = (0..23).toList()
                val listState = androidx.compose.foundation.lazy.rememberLazyListState(initialFirstVisibleItemIndex = 9)
                val targetDayCal = Calendar.getInstance()
                targetDayCal.add(Calendar.DAY_OF_YEAR, selectedDayOffset)

                LazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(hoursList) { hour ->
                        val displayHourStr = when {
                            hour < 12 -> "${hour}:00 AM"
                            hour == 12 -> "12:00 PM"
                            else -> "${hour - 12}:00 PM"
                        }

                        val slotCal = Calendar.getInstance()
                        slotCal.add(Calendar.DAY_OF_YEAR, selectedDayOffset)
                        slotCal.set(Calendar.HOUR_OF_DAY, hour)
                        slotCal.set(Calendar.MINUTE, 0)
                        slotCal.set(Calendar.SECOND, 0)
                        slotCal.set(Calendar.MILLISECOND, 0)
                        val slotStartMillis = slotCal.timeInMillis

                        val matchingBookings = state.orders.filter { order ->
                            if (order.scheduledStartTime == null) return@filter false
                            val ordCal = Calendar.getInstance()
                            ordCal.timeInMillis = order.scheduledStartTime

                            val bookingDayMatches = ordCal.get(Calendar.DAY_OF_YEAR) == slotCal.get(Calendar.DAY_OF_YEAR) &&
                                    ordCal.get(Calendar.YEAR) == slotCal.get(Calendar.YEAR)
                            val bookingHourMatches = ordCal.get(Calendar.HOUR_OF_DAY) == hour

                            bookingDayMatches && bookingHourMatches
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = displayHourStr,
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp,
                                color = Color(0xFF0F172A),
                                textAlign = TextAlign.End,
                                modifier = Modifier
                                    .width(72.dp)
                                    .padding(end = 10.dp, top = 12.dp)
                            )

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                matchingBookings.forEach { booking ->
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF7ED)),
                                        border = BorderStroke(1.dp, Color(0xFFFFEDD5)),
                                        shape = RoundedCornerShape(24.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .shadow(elevation = 1.dp, shape = RoundedCornerShape(24.dp), clip = false)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = booking.customerName ?: "Walk-in Guest",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 15.sp,
                                                    color = Color(0xFFC2410C)
                                                )
                                                val dur = booking.estimatedDuration ?: 60
                                                val durLabel = if (dur % 60 == 0) "${dur / 60} hr${if (dur / 60 > 1) "s" else ""}" else "$dur min"
                                                Text(
                                                    text = "Length: $durLabel",
                                                    fontSize = 12.sp,
                                                    color = Color(0xFFC2410C).copy(alpha = 0.8f)
                                                )
                                                if (!booking.customerPhone.isNullOrBlank()) {
                                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                                                        Text(
                                                            text = booking.customerPhone,
                                                            fontSize = 12.sp,
                                                            color = Color(0xFFC2410C).copy(alpha = 0.7f)
                                                        )
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        IconButton(
                                                            onClick = { 
                                                                val clipboardManager = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                                                val clip = android.content.ClipData.newPlainText("phone", booking.customerPhone)
                                                                clipboardManager.setPrimaryClip(clip)
                                                                Toast.makeText(context, "Copied number", Toast.LENGTH_SHORT).show()
                                                            },
                                                            modifier = Modifier.size(20.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.ContentCopy,
                                                                contentDescription = "Copy phone",
                                                                tint = Color(0xFFF97316),
                                                                modifier = Modifier.size(14.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                val isPastSlot = slotStartMillis < System.currentTimeMillis() - 3600000L
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isPastSlot) Color(0xFFF1F5F9) else Color.White
                                    ),
                                    border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
                                    shape = RoundedCornerShape(24.dp),
                                    onClick = {
                                        val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                        val sdfTime = SimpleDateFormat("hh:mm a", Locale.getDefault())
                                        inputDate = sdfDate.format(Date(slotStartMillis))
                                        inputTime = sdfTime.format(Date(slotStartMillis))
                                        inputName = ""
                                        inputPhone = ""
                                        inputDurationMinutes = 30
                                        showCreateDialog = true
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .shadow(elevation = 1.dp, shape = RoundedCornerShape(24.dp), clip = false)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (matchingBookings.isEmpty()) (if (isPastSlot) "Past Slot" else "Slot Available") else "+ Add another booking",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = if (isPastSlot) Color(0xFF94A3B8) else Color(0xFF0F172A),
                                            modifier = Modifier.padding(start = 4.dp)
                                        )
                                        Icon(
                                            imageVector = Icons.Default.AddCircleOutline,
                                            contentDescription = "Book",
                                            tint = if (isPastSlot) Color(0xFF94A3B8) else Color(0xFF10B981),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Floating Action Buttons in bottom right corner (Voice microphone and Add manual appointment button)
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Voice Command Microphone trigger
            VoiceTriggerFAB(
                mode = OperationMode.BILLING_MODE,
                onIntentRecognized = { intent ->
                    // Since it returns custom format or text, let's process it
                    // The VoiceTriggerFAB gives VoiceCommandIntent. If unrecognized, it has raw text!
                    if (intent is com.example.core.voice.VoiceCommandIntent.Unrecognized) {
                        processVoiceCommand(intent.rawText)
                    } else {
                        // Attempt to extract rawText or search
                        Toast.makeText(context, "Try saying: 'book slot for Jane at 10 AM'", Toast.LENGTH_LONG).show()
                    }
                }
            )

            // Manual Creator FAB
            FloatingActionButton(
                onClick = {
                    inputName = ""
                    inputPhone = ""
                    inputDate = ""
                    inputTime = ""
                    inputDurationMinutes = 30
                    showCreateDialog = true
                },
                containerColor = Color(0xFFF97316),
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.testTag("appointments_floating_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "New Appointment"
                )
            }
        }

        // Beautiful Manual Create Dialog
        if (showCreateDialog) {
            androidx.compose.ui.window.Dialog(
                onDismissRequest = { showCreateDialog = false },
                properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.White
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                    ) {
                        Text(
                            text = "Create Booking",
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            color = Color(0xFF0F172A),
                            modifier = Modifier.padding(bottom = 24.dp)
                        )

                        // Client Name Field
                        OutlinedTextField(
                            value = inputName,
                            onValueChange = { inputName = it },
                            label = { Text("Client Name") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFF97316),
                                unfocusedBorderColor = Color(0xFFE2E8F0)
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Client Phone Field
                        OutlinedTextField(
                            value = inputPhone,
                            onValueChange = { inputPhone = it },
                            label = { Text("Client Phone (Optional)") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFF97316),
                                unfocusedBorderColor = Color(0xFFE2E8F0)
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        val cal = Calendar.getInstance()
                        // Date Field
                        OutlinedTextField(
                            value = inputDate,
                            onValueChange = { inputDate = it },
                            label = { Text("Date (YYYY-MM-DD)") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true,
                            readOnly = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFF97316),
                                unfocusedBorderColor = Color(0xFFE2E8F0)
                            ),
                            trailingIcon = {
                                IconButton(onClick = {
                                    android.app.DatePickerDialog(
                                        context,
                                        { _, year, month, dayOfMonth ->
                                            inputDate = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, dayOfMonth)
                                        },
                                        cal.get(Calendar.YEAR),
                                        cal.get(Calendar.MONTH),
                                        cal.get(Calendar.DAY_OF_MONTH)
                                    ).show()
                                }) {
                                    Icon(Icons.Default.Event, contentDescription = "Select Date")
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Time Field
                        OutlinedTextField(
                            value = inputTime,
                            onValueChange = { inputTime = it },
                            label = { Text("Time (HH:MM AM/PM)") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true,
                            readOnly = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFF97316),
                                unfocusedBorderColor = Color(0xFFE2E8F0)
                            ),
                            trailingIcon = {
                                IconButton(onClick = {
                                    android.app.TimePickerDialog(
                                        context,
                                        { _, hourOfDay, minute ->
                                            val amPm = if (hourOfDay >= 12) "PM" else "AM"
                                            val displayHour = if (hourOfDay % 12 == 0) 12 else hourOfDay % 12
                                            inputTime = String.format(Locale.getDefault(), "%02d:%02d %s", displayHour, minute, amPm)
                                        },
                                        cal.get(Calendar.HOUR_OF_DAY),
                                        cal.get(Calendar.MINUTE),
                                        false
                                    ).show()
                                }) {
                                    Icon(Icons.Default.AccessTime, contentDescription = "Select Time")
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Appointment Length",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF64748B),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf(30 to "30 Mins", 60 to "1 Hour", 120 to "2 Hours", 180 to "3 Hours").forEach { (min, label) ->
                                val isSelected = inputDurationMinutes == min
                                Card(
                                    onClick = { inputDurationMinutes = min },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) Color(0xFFFFF7ED) else Color.White
                                    ),
                                    border = BorderStroke(1.dp, if (isSelected) Color(0xFFF97316) else Color(0xFFE2E8F0)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isSelected) Color(0xFFF97316) else Color(0xFF64748B),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 10.dp),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Custom Length
                        var customHoursInput by remember { mutableStateOf("") }
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Text("Custom duration (hrs):", fontSize = 12.sp, color = Color(0xFF64748B), modifier = Modifier.padding(end = 8.dp))
                            OutlinedTextField(
                                value = customHoursInput,
                                onValueChange = { 
                                    customHoursInput = it
                                    val hrs = it.toIntOrNull()
                                    if (hrs != null) {
                                        inputDurationMinutes = hrs * 60
                                    }
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.width(100.dp),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFFF97316),
                                    unfocusedBorderColor = Color(0xFFE2E8F0)
                                )
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        // Cancel and Confirm actions
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = { showCreateDialog = false },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF1F5F9), contentColor = Color(0xFF475569)),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Cancel", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }

                            Button(
                                onClick = {
                                    if (inputName.isBlank()) {
                                        Toast.makeText(context, "Please enter client name", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    if (inputDate.isBlank() || inputTime.isBlank()) {
                                        Toast.makeText(context, "Please select date and time", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }

                                    // Parse Inputs to mills
                                    val myCal = Calendar.getInstance()
                                    val dateParts = inputDate.split("-")
                                    if (dateParts.size == 3) {
                                        myCal.set(Calendar.YEAR, dateParts[0].toIntOrNull() ?: myCal.get(Calendar.YEAR))
                                        myCal.set(Calendar.MONTH, (dateParts[1].toIntOrNull() ?: 1) - 1)
                                        myCal.set(Calendar.DAY_OF_MONTH, dateParts[2].toIntOrNull() ?: myCal.get(Calendar.DAY_OF_MONTH))
                                    }

                                    val cleanTime = inputTime.uppercase()
                                    val timeParts = cleanTime.replace(" AM", "").replace(" PM", "").split(":")
                                    if (timeParts.size >= 2) {
                                        var hour = timeParts[0].trim().toIntOrNull() ?: 9
                                        val minute = timeParts[1].trim().toIntOrNull() ?: 0
                                        if (cleanTime.contains("PM") && hour < 12) {
                                            hour += 12
                                        } else if (cleanTime.contains("AM") && hour == 12) {
                                            hour = 0
                                        }
                                        myCal.set(Calendar.HOUR_OF_DAY, hour)
                                        myCal.set(Calendar.MINUTE, minute)
                                        myCal.set(Calendar.SECOND, 0)
                                        myCal.set(Calendar.MILLISECOND, 0)
                                    }

                                    val scheduledMillis = myCal.timeInMillis

                                    scope.launch {
                                        val newId = UUID.randomUUID().toString()
                                        val order = OrderEntity(
                                            id = newId,
                                            createdAt = System.currentTimeMillis(),
                                            totalAmount = 0.0,
                                            subtotal = 0.0,
                                            taxAmount = 0.0,
                                            paymentMethod = "CASH",
                                            paymentMode = "CASH",
                                            status = "HELD",
                                            shiftId = "shift-1",
                                            customerPhone = inputPhone.ifBlank { null },
                                            customerName = inputName,
                                            displayId = newId.takeLast(4),
                                            scheduledStartTime = scheduledMillis,
                                            estimatedDuration = inputDurationMinutes
                                        )

                                        viewModel.insertOrderWithEmptyItems(order)
                                        showCreateDialog = false
                                        Toast.makeText(context, "Appointment created successfully!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF97316)),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Create", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
