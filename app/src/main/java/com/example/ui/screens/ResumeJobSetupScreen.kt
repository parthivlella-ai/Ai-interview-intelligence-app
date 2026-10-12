package com.example.ui.screens

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.WorkOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.InterviewMode
import com.example.data.JobRole
import com.example.service.AdaptiveInterviewEngine
import com.example.ui.theme.AccentViolet
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.SecondaryTeal
import com.example.ui.theme.StatusDemonstrated
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * File metadata holder for uploaded resumes
 */
data class UploadedFileInfo(
    val fileName: String,
    val sizeKb: Long,
    val isParsedSuccess: Boolean,
    val statusMessage: String
)

/**
 * TASK 4 — Improved Resume & Job Setup Screen
 *
 * Sequence:
 * Choose Job Role → Upload Resume → Provide Job Description → Review Extracted Skills → Start Interview
 *
 * 1. Header: Back navigation, title "Prepare Your Interview", subtitle.
 * 2. Target Job Role: Selectable/Searchable roles (Android, Java, Python, Web, Data Analyst, Cloud) + Custom Role.
 * 3. Resume Upload: Storage Access Framework file picker (.txt, .md, .pdf), metadata display, replace/remove actions, loading/success/error states, quick presets.
 * 4. Job Description: Multiline text field with character count, validation, and persistence.
 * 5. Extracted Skills Review: Distinguishes resume-derived (claimed) skills vs role requirements.
 * 6. Start Interview: Validation checks, prevents duplicate submissions, launches interview engine.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ResumeJobSetupScreen(
    onBack: () -> Unit,
    onStartInterview: (
        roleTitle: String,
        department: String,
        jobDescription: String,
        resumeText: String,
        mode: InterviewMode,
        questionCount: Int
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val availableRoles = remember { AdaptiveInterviewEngine.AVAILABLE_ROLES }

    // --- State: Role Selection ---
    var roleSearchQuery by remember { mutableStateOf("") }
    var selectedRoleIndex by remember { mutableIntStateOf(0) }
    var isCustomRoleMode by remember { mutableStateOf(false) }
    var customRoleTitle by remember { mutableStateOf("") }
    var customDepartment by remember { mutableStateOf("Engineering") }

    val activeRole: JobRole = remember(selectedRoleIndex, isCustomRoleMode, customRoleTitle, customDepartment) {
        if (isCustomRoleMode) {
            val title = customRoleTitle.ifBlank { "Custom Role" }
            JobRole(
                id = "custom_role",
                title = title,
                department = customDepartment.ifBlank { "Engineering" },
                description = "Custom role tailored to candidate profile.",
                keyCompetencies = listOf(
                    "$title Fundamentals",
                    "System Architecture",
                    "Performance & Scalability",
                    "Problem Solving & Testing"
                )
            )
        } else {
            availableRoles.getOrElse(selectedRoleIndex) { availableRoles.first() }
        }
    }

    // --- State: Resume Text & Upload ---
    var resumeText by remember {
        mutableStateOf(
            "Experienced Android Engineer with 4 years building high-traffic Kotlin apps. Proficient in Jetpack Compose, Kotlin Coroutines, StateFlow, Room DB, Dagger Hilt, and MVVM Clean Architecture. Proven track record optimizing app startup and background sync."
        )
    }
    var uploadedFileInfo by remember { mutableStateOf<UploadedFileInfo?>(null) }
    var isUploadingFile by remember { mutableStateOf(false) }
    var uploadErrorMessage by remember { mutableStateOf<String?>(null) }

    // --- State: Job Description ---
    var jobDescription by remember { mutableStateOf(availableRoles.first().description) }
    var hasUserEditedJobDescription by remember { mutableStateOf(false) }

    // Update default job description when role changes IF candidate hasn't manually edited
    LaunchedEffect(activeRole) {
        if (!hasUserEditedJobDescription && !isCustomRoleMode) {
            jobDescription = activeRole.description
        }
    }

    // --- State: Interview Config & Submission ---
    var selectedMode by remember { mutableStateOf(InterviewMode.FORMAL) }
    var questionCount by remember { mutableIntStateOf(4) }
    var isStartingInterview by remember { mutableStateOf(false) }

    // File picker launcher using Storage Access Framework
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            isUploadingFile = true
            uploadErrorMessage = null
            coroutineScope.launch {
                try {
                    val fileInfo = readResumeFromUri(context, uri)
                    uploadedFileInfo = fileInfo.first
                    if (fileInfo.second.isNotBlank()) {
                        resumeText = fileInfo.second
                    }
                } catch (e: Exception) {
                    uploadErrorMessage = "Failed to process resume: ${e.localizedMessage ?: "Unknown file error"}"
                    uploadedFileInfo = null
                } finally {
                    isUploadingFile = false
                }
            }
        }
    }

    // --- Validation logic ---
    val effectiveRoleTitle = if (isCustomRoleMode) customRoleTitle.trim() else activeRole.title
    val isRoleValid = effectiveRoleTitle.isNotBlank()
    val isResumeValid = resumeText.trim().length >= 15
    val isJobDescValid = jobDescription.trim().length >= 15
    val isFormValid = isRoleValid && isResumeValid && isJobDescValid

    // Filter roles for search
    val filteredRoles = remember(roleSearchQuery) {
        if (roleSearchQuery.isBlank()) {
            availableRoles
        } else {
            availableRoles.filter {
                it.title.contains(roleSearchQuery, ignoreCase = true) ||
                    it.department.contains(roleSearchQuery, ignoreCase = true)
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // A. Header
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .testTag("resume_setup_header")
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("resume_setup_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to Dashboard"
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Column {
                    Text(
                        text = "Prepare Your Interview",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Help ForkAI understand your experience and target role.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // B. Target Job Role Section
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        RoundedCornerShape(16.dp)
                    )
                    .testTag("section_job_role")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SectionBadge(number = "1")
                            Text(
                                text = "Target Job Role",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Toggle for Custom Role
                        TextButton(
                            onClick = {
                                isCustomRoleMode = !isCustomRoleMode
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.testTag("toggle_custom_role_button")
                        ) {
                            Text(
                                text = if (isCustomRoleMode) "Select Preset" else "+ Custom Role",
                                style = MaterialTheme.typography.labelMedium,
                                color = PrimaryBlue,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    if (isCustomRoleMode) {
                        // Custom Role Fields
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = customRoleTitle,
                                onValueChange = { customRoleTitle = it },
                                label = { Text("Custom Role Title (e.g. Senior iOS Engineer)") },
                                placeholder = { Text("e.g. Full Stack Developer") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("custom_role_input")
                            )

                            OutlinedTextField(
                                value = customDepartment,
                                onValueChange = { customDepartment = it },
                                label = { Text("Department / Team") },
                                placeholder = { Text("e.g. Mobile Engineering") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("custom_dept_input")
                            )
                        }
                    } else {
                        // Role Search Bar
                        OutlinedTextField(
                            value = roleSearchQuery,
                            onValueChange = { roleSearchQuery = it },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search Roles",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            trailingIcon = {
                                if (roleSearchQuery.isNotEmpty()) {
                                    IconButton(onClick = { roleSearchQuery = "" }) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Clear search",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            },
                            placeholder = { Text("Search roles (e.g. Android, Java, Python, Web)...") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("role_search_field")
                        )

                        // Role Selection Chips / Options
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            filteredRoles.forEach { role ->
                                val index = availableRoles.indexOf(role)
                                val isSelected = !isCustomRoleMode && selectedRoleIndex == index
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        isCustomRoleMode = false
                                        selectedRoleIndex = index
                                    },
                                    leadingIcon = {
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    },
                                    label = { Text(role.title, fontSize = 12.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = PrimaryBlue.copy(alpha = 0.15f),
                                        selectedLabelColor = PrimaryBlue
                                    ),
                                    modifier = Modifier.testTag("role_chip_${role.id}")
                                )
                            }
                        }

                        // Selected Role Overview Card
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(PrimaryBlue.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.WorkOutline,
                                        contentDescription = null,
                                        tint = PrimaryBlue,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = activeRole.title,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${activeRole.department} • ${activeRole.keyCompetencies.size} Target Competencies",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // C. Resume Upload Section
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        RoundedCornerShape(16.dp)
                    )
                    .testTag("section_resume_upload")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SectionBadge(number = "2")
                            Text(
                                text = "Candidate Resume",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Text(
                            text = "SAF File Picker",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Upload Card / Status Area
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (uploadedFileInfo != null) {
                            StatusDemonstrated.copy(alpha = 0.08f)
                        } else if (uploadErrorMessage != null) {
                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        },
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (uploadedFileInfo != null) StatusDemonstrated.copy(alpha = 0.3f)
                            else if (uploadErrorMessage != null) MaterialTheme.colorScheme.error.copy(alpha = 0.4f)
                            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (isUploadingFile) {
                                // Loading State
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.5.dp,
                                        color = PrimaryBlue
                                    )
                                    Column {
                                        Text(
                                            text = "Reading & Parsing Resume...",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Extracting technical text and candidate experience",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            } else if (uploadedFileInfo != null) {
                                // Success State with File Details
                                val file = uploadedFileInfo!!
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Success",
                                            tint = StatusDemonstrated,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Column {
                                            Text(
                                                text = file.fileName,
                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "${file.sizeKb} KB • ${file.statusMessage}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        IconButton(
                                            onClick = {
                                                filePickerLauncher.launch(
                                                    arrayOf("text/plain", "text/markdown", "application/pdf", "text/*")
                                                )
                                            },
                                            modifier = Modifier.testTag("replace_resume_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Refresh,
                                                contentDescription = "Replace resume",
                                                tint = PrimaryBlue,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                uploadedFileInfo = null
                                                resumeText = ""
                                            },
                                            modifier = Modifier.testTag("remove_resume_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Remove resume",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            } else if (uploadErrorMessage != null) {
                                // Error State
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ErrorOutline,
                                        contentDescription = "Error",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Text(
                                        text = uploadErrorMessage!!,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            } else {
                                // Empty State
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.UploadFile,
                                            contentDescription = null,
                                            tint = PrimaryBlue,
                                            modifier = Modifier.size(28.dp)
                                        )
                                        Column {
                                            Text(
                                                text = "Upload Resume File",
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "Supported: .txt, .md, .pdf (SAF zero-permission)",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Button(
                                        onClick = {
                                            filePickerLauncher.launch(
                                                arrayOf("text/plain", "text/markdown", "application/pdf", "text/*")
                                            )
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                        modifier = Modifier.testTag("upload_resume_button")
                                    ) {
                                        Text("Select File", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    // Quick Sample Presets
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Or fill with quick template:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = false,
                            onClick = {
                                resumeText = "Experienced Android Engineer with 4 years building high-traffic Kotlin apps. Proficient in Jetpack Compose, Kotlin Coroutines, StateFlow, Room DB, Dagger Hilt, and MVVM Clean Architecture. Proven track record optimizing app startup and background sync."
                                uploadedFileInfo = UploadedFileInfo("preset_android.txt", 1, true, "Preset Loaded")
                            },
                            label = { Text("Android Preset", fontSize = 11.sp) },
                            modifier = Modifier.testTag("preset_mobile_resume")
                        )
                        FilterChip(
                            selected = false,
                            onClick = {
                                resumeText = "Cloud Backend Engineer with 5 years designing Go & Java microservices. Extensive experience with Redis distributed caching, PostgreSQL indexing, gRPC streaming, Docker, Kubernetes, and Circuit Breaker patterns."
                                uploadedFileInfo = UploadedFileInfo("preset_backend.txt", 1, true, "Preset Loaded")
                            },
                            label = { Text("Backend Preset", fontSize = 11.sp) },
                            modifier = Modifier.testTag("preset_backend_resume")
                        )
                        FilterChip(
                            selected = false,
                            onClick = {
                                resumeText = "Data Analyst & Python Developer specializing in advanced SQL window functions, dimensional warehouse modeling, pandas ETL pipelines, statistical A/B test significance, and Tableau analytics dashboards."
                                uploadedFileInfo = UploadedFileInfo("preset_data.txt", 1, true, "Preset Loaded")
                            },
                            label = { Text("Data Analyst Preset", fontSize = 11.sp) },
                            modifier = Modifier.testTag("preset_data_resume")
                        )
                    }

                    // Editable Resume Content
                    OutlinedTextField(
                        value = resumeText,
                        onValueChange = { resumeText = it },
                        label = { Text("Extracted Resume Text / Professional Summary") },
                        placeholder = { Text("Paste or review your resume text here...") },
                        supportingText = {
                            Text(
                                text = "${resumeText.length} characters • ${if (isResumeValid) "Ready" else "Min 15 characters required"}",
                                color = if (isResumeValid) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error
                            )
                        },
                        isError = !isResumeValid && resumeText.isNotEmpty(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .testTag("resume_input_field"),
                        maxLines = 6
                    )
                }
            }
        }

        // D. Job Description Section
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        RoundedCornerShape(16.dp)
                    )
                    .testTag("section_job_description")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SectionBadge(number = "3")
                            Text(
                                text = "Job Description & Context",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        TextButton(
                            onClick = {
                                jobDescription = activeRole.description
                                hasUserEditedJobDescription = false
                            },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("Reset", style = MaterialTheme.typography.labelSmall, color = PrimaryBlue)
                        }
                    }

                    Text(
                        text = "Paste the target job description or utilize the role requirements to guide adaptive question generation.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = jobDescription,
                        onValueChange = {
                            jobDescription = it
                            hasUserEditedJobDescription = true
                        },
                        label = { Text("Job Description & Requirements") },
                        placeholder = { Text("Paste job specifications and key responsibilities...") },
                        supportingText = {
                            Text(
                                text = "${jobDescription.length} characters • ${if (isJobDescValid) "Valid" else "Min 15 characters required"}",
                                color = if (isJobDescValid) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error
                            )
                        },
                        isError = !isJobDescValid && jobDescription.isNotEmpty(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .testTag("job_description_input"),
                        maxLines = 5
                    )
                }
            }
        }

        // E. Extracted Skills Review Section
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        RoundedCornerShape(16.dp)
                    )
                    .testTag("section_skills_review")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SectionBadge(number = "4")
                            Text(
                                text = "Extracted Competencies",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = SecondaryTeal,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Text(
                        text = "Skills verified between your resume claims and role requirements will be evaluated dynamically during the interview.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Breakdown of claimed vs unassessed competencies
                    val competencies = activeRole.keyCompetencies
                    val claimedCompetencies = remember(competencies, resumeText) {
                        competencies.filter { comp ->
                            val keyword = comp.split(" ").first()
                            resumeText.contains(keyword, ignoreCase = true)
                        }
                    }
                    val roleRequirements = remember(competencies, claimedCompetencies) {
                        competencies.filterNot { it in claimedCompetencies }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = StatusDemonstrated.copy(alpha = 0.12f),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "${claimedCompetencies.size} Claimed on Resume",
                                color = StatusDemonstrated,
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "${roleRequirements.size} Role Requirements",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        competencies.forEach { comp ->
                            val isClaimed = claimedCompetencies.contains(comp)
                            SuggestionChip(
                                onClick = {},
                                label = {
                                    Text(
                                        text = comp + if (isClaimed) " • Claimed" else " • Unassessed",
                                        fontSize = 11.sp,
                                        fontWeight = if (isClaimed) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                icon = {
                                    if (isClaimed) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Claimed",
                                            tint = StatusDemonstrated,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = "Unassessed",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = if (isClaimed) PrimaryBlue.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    labelColor = if (isClaimed) PrimaryBlue else MaterialTheme.colorScheme.onSurface
                                ),
                                border = SuggestionChipDefaults.suggestionChipBorder(
                                    enabled = true,
                                    borderColor = if (isClaimed) PrimaryBlue.copy(alpha = 0.3f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                )
                            )
                        }
                    }
                }
            }
        }

        // F. Interview Execution Mode & Number of Questions
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        RoundedCornerShape(16.dp)
                    )
                    .testTag("section_interview_config")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SectionBadge(number = "5")
                        Text(
                            text = "Interview Configuration",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Formal vs Practice Mode Selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (selectedMode == InterviewMode.FORMAL)
                                    PrimaryBlue.copy(alpha = 0.12f)
                                else MaterialTheme.colorScheme.surface
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .border(
                                    width = if (selectedMode == InterviewMode.FORMAL) 2.dp else 1.dp,
                                    color = if (selectedMode == InterviewMode.FORMAL) PrimaryBlue else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { selectedMode = InterviewMode.FORMAL }
                                .testTag("mode_formal_card")
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Formal Mode",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Integrity tracking active. Results compiled in comprehensive final report.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (selectedMode == InterviewMode.PRACTICE)
                                    AccentViolet.copy(alpha = 0.12f)
                                else MaterialTheme.colorScheme.surface
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .border(
                                    width = if (selectedMode == InterviewMode.PRACTICE) 2.dp else 1.dp,
                                    color = if (selectedMode == InterviewMode.PRACTICE) AccentViolet else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { selectedMode = InterviewMode.PRACTICE }
                                .testTag("mode_practice_card")
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Practice Mode",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Instant per-question feedback dialog showing evidence, score & uncertainty.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    // Question Count Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Adaptive Question Count",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(3, 4, 5).forEach { count ->
                                FilterChip(
                                    selected = questionCount == count,
                                    onClick = { questionCount = count },
                                    label = { Text("$count Qs") },
                                    modifier = Modifier.testTag("question_count_$count")
                                )
                            }
                        }
                    }
                }
            }
        }

        // G. Start Interview Action & Validation Notice
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("section_start_interview"),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Validation Warning if form incomplete
                if (!isFormValid) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                            val missingReasons = buildList {
                                if (!isRoleValid) add("target role")
                                if (!isResumeValid) add("resume summary")
                                if (!isJobDescValid) add("job description")
                            }
                            Text(
                                text = "Please provide: ${missingReasons.joinToString(", ")} before starting.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                // Prominent Start Button
                Button(
                    onClick = {
                        if (isFormValid && !isStartingInterview) {
                            isStartingInterview = true
                            onStartInterview(
                                effectiveRoleTitle,
                                activeRole.department,
                                jobDescription.trim(),
                                resumeText.trim(),
                                selectedMode,
                                questionCount
                            )
                        }
                    },
                    enabled = isFormValid && !isStartingInterview,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryBlue,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("begin_interview_button")
                ) {
                    if (isStartingInterview) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Launching Interview...", fontWeight = FontWeight.Bold)
                    } else {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Begin Adaptive AI Interview",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

/**
 * Small numbered badge component for numbered form sections
 */
@Composable
private fun SectionBadge(number: String) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(PrimaryBlue),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = number,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Helper function to read resume metadata and plain text safely using ContentResolver
 */
private suspend fun readResumeFromUri(context: Context, uri: Uri): Pair<UploadedFileInfo, String> =
    withContext(Dispatchers.IO) {
        var fileName = "resume_document"
        var fileSizeKb = 0L

        // Query metadata from Storage Access Framework
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (cursor.moveToFirst()) {
                if (nameIndex != -1) {
                    fileName = cursor.getString(nameIndex) ?: "resume_document"
                }
                if (sizeIndex != -1) {
                    val bytes = cursor.getLong(sizeIndex)
                    fileSizeKb = (bytes / 1024L).coerceAtLeast(1L)
                }
            }
        }

        // Read text content
        val isPlainText = fileName.endsWith(".txt", ignoreCase = true) ||
            fileName.endsWith(".md", ignoreCase = true) ||
            fileName.endsWith(".json", ignoreCase = true)

        val textContent = try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                if (isPlainText) {
                    stream.bufferedReader().readText()
                } else {
                    // Binary or PDF document
                    val raw = stream.bufferedReader().readText()
                    // Filter printable ASCII/UTF-8 characters if possible
                    val printable = raw.filter { it.code in 32..126 || it == '\n' || it == '\r' || it == '\t' }
                    if (printable.length > 50) {
                        printable.take(4000)
                    } else {
                        "Uploaded PDF document: $fileName ($fileSizeKb KB). Please review and verify technical competencies in the summary below."
                    }
                }
            } ?: ""
        } catch (e: Exception) {
            ""
        }

        val info = UploadedFileInfo(
            fileName = fileName,
            sizeKb = fileSizeKb,
            isParsedSuccess = textContent.isNotBlank(),
            statusMessage = if (isPlainText) "Plain text parsed" else "Document loaded"
        )

        Pair(info, textContent)
    }
