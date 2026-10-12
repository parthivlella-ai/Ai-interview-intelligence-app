package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.HighlightOff
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.InterviewMode
import com.example.data.SkillStatus
import com.example.data.local.InterviewSessionEntity
import com.example.data.local.SkillAssessmentEntity
import com.example.service.AdaptiveInterviewEngine
import com.example.ui.theme.AccentViolet
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.SecondaryTeal
import com.example.ui.theme.StatusClaimedUnvalidated
import com.example.ui.theme.StatusDemonstrated
import com.example.ui.theme.StatusNotDemonstrated

/**
 * Recruiter Destination 2: Candidates
 * Comprehensive Candidate Review & Side-by-Side Comparison against Job-Required Competencies.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecruiterCandidatesScreen(
    sessions: List<InterviewSessionEntity>,
    allSkills: List<SkillAssessmentEntity> = emptyList(),
    onInspectReport: (String) -> Unit,
    onOpenReview: (InterviewSessionEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedRoleFilter by remember { mutableStateOf<String?>(null) }
    var selectedStatusFilter by remember { mutableStateOf<String?>(null) } // "ALL", "COMPLETED", "IN_PROGRESS", "ADVANCE", "FURTHER"
    var sortBy by remember { mutableStateOf("SCORE_DESC") } // "SCORE_DESC", "SCORE_ASC", "DATE_DESC", "NAME_ASC"

    // Candidate Comparison State: selection of 2 or more candidates for the same job
    var comparisonMode by remember { mutableStateOf(false) }
    val selectedForComparison = remember { mutableStateListOf<String>() } // session IDs

    // Distinct job roles available from actual sessions
    val distinctRoles = remember(sessions) {
        sessions.map { it.roleTitle }.distinct().sorted()
    }

    // Filtered & sorted candidates list
    val filteredSessions = remember(sessions, searchQuery, selectedRoleFilter, selectedStatusFilter, sortBy) {
        sessions
            .filter { session ->
                val matchesSearch = searchQuery.isBlank() ||
                        session.candidateName.contains(searchQuery, ignoreCase = true) ||
                        session.roleTitle.contains(searchQuery, ignoreCase = true) ||
                        session.roleDepartment.contains(searchQuery, ignoreCase = true)
                val matchesRole = selectedRoleFilter == null || session.roleTitle == selectedRoleFilter
                val matchesStatus = when (selectedStatusFilter) {
                    "COMPLETED" -> session.isCompleted
                    "IN_PROGRESS" -> !session.isCompleted
                    "ADVANCE" -> session.recruiterVerdict == "ADVANCE_TO_ONSITE"
                    "FURTHER" -> session.recruiterVerdict == "FURTHER_EVALUATION"
                    else -> true
                }
                matchesSearch && matchesRole && matchesStatus
            }
            .sortedWith { a, b ->
                when (sortBy) {
                    "SCORE_DESC" -> b.overallScore.compareTo(a.overallScore)
                    "SCORE_ASC" -> a.overallScore.compareTo(b.overallScore)
                    "DATE_DESC" -> b.createdAt.compareTo(a.createdAt)
                    "NAME_ASC" -> a.candidateName.compareTo(b.candidateName, ignoreCase = true)
                    else -> 0
                }
            }
    }

    // Sessions selected for comparison
    val comparisonSessions = remember(selectedForComparison.toList(), sessions) {
        sessions.filter { selectedForComparison.contains(it.id) }
    }

    // If Comparison view is active and we have >= 2 candidates, render Comparison Mode
    if (comparisonMode && comparisonSessions.size >= 2) {
        CandidateComparisonView(
            candidates = comparisonSessions,
            allSkills = allSkills,
            onBack = {
                comparisonMode = false
            },
            onInspectReport = onInspectReport,
            onOpenReview = onOpenReview,
            modifier = modifier
        )
        return
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Candidate Review & Evidence",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Evaluate verified interview submissions and compare role competencies",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Comparison Mode Toggle Button
                FilledTonalButton(
                    onClick = {
                        if (comparisonMode) {
                            comparisonMode = false
                        } else {
                            if (selectedForComparison.size >= 2) {
                                comparisonMode = true
                            } else {
                                // Select first 2 matching candidates by default if available
                                val candidatePool = if (filteredSessions.size >= 2) filteredSessions else sessions
                                if (candidatePool.size >= 2) {
                                    selectedForComparison.clear()
                                    selectedForComparison.add(candidatePool[0].id)
                                    selectedForComparison.add(candidatePool[1].id)
                                    comparisonMode = true
                                }
                            }
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = if (selectedForComparison.size >= 2) AccentViolet else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (selectedForComparison.size >= 2) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.testTag("toggle_comparison_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.CompareArrows,
                        contentDescription = "Compare",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (selectedForComparison.size >= 2) "Compare (${selectedForComparison.size})" else "Compare",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Summary Metric Bar
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricChip(
                    label = "Total Candidates",
                    value = "${sessions.size}",
                    color = PrimaryBlue,
                    modifier = Modifier.weight(1f)
                )
                MetricChip(
                    label = "Completed",
                    value = "${sessions.count { it.isCompleted }}",
                    color = StatusDemonstrated,
                    modifier = Modifier.weight(1f)
                )
                MetricChip(
                    label = "Advance Recs",
                    value = "${sessions.count { it.recruiterVerdict == "ADVANCE_TO_ONSITE" }}",
                    color = AccentViolet,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Search Input
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search by candidate name, role, or department...") },
                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_candidates_field"),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
        }

        // Role & Status Filter Chips
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Filter by Target Job Role:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FilterChip(
                        selected = selectedRoleFilter == null,
                        onClick = { selectedRoleFilter = null },
                        label = { Text("All Roles (${sessions.size})", fontSize = 11.sp) }
                    )
                    distinctRoles.forEach { role ->
                        val count = sessions.count { it.roleTitle == role }
                        FilterChip(
                            selected = selectedRoleFilter == role,
                            onClick = {
                                selectedRoleFilter = if (selectedRoleFilter == role) null else role
                            },
                            label = { Text("$role ($count)", fontSize = 11.sp) }
                        )
                    }
                }
            }
        }

        // Sorting & Status Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status Filter
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = selectedStatusFilter == null,
                        onClick = { selectedStatusFilter = null },
                        label = { Text("All", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = selectedStatusFilter == "ADVANCE",
                        onClick = { selectedStatusFilter = if (selectedStatusFilter == "ADVANCE") null else "ADVANCE" },
                        label = { Text("Advance", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = selectedStatusFilter == "FURTHER",
                        onClick = { selectedStatusFilter = if (selectedStatusFilter == "FURTHER") null else "FURTHER" },
                        label = { Text("Needs Review", fontSize = 11.sp) }
                    )
                }

                // Sort Dropdown Button
                var showSortMenu by remember { mutableStateOf(false) }
                Box {
                    TextButton(onClick = { showSortMenu = true }) {
                        Icon(imageVector = Icons.Default.Sort, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = when (sortBy) {
                                "SCORE_DESC" -> "Score (High-Low)"
                                "SCORE_ASC" -> "Score (Low-High)"
                                "DATE_DESC" -> "Most Recent"
                                else -> "Name"
                            },
                            fontSize = 11.sp
                        )
                    }
                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Score (High to Low)") },
                            onClick = { sortBy = "SCORE_DESC"; showSortMenu = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Score (Low to High)") },
                            onClick = { sortBy = "SCORE_ASC"; showSortMenu = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Most Recent Date") },
                            onClick = { sortBy = "DATE_DESC"; showSortMenu = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Candidate Name (A-Z)") },
                            onClick = { sortBy = "NAME_ASC"; showSortMenu = false }
                        )
                    }
                }
            }
        }

        // Comparison Instruction Bar when selecting
        if (selectedForComparison.isNotEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = AccentViolet.copy(alpha = 0.12f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${selectedForComparison.size} candidates selected for side-by-side comparison",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = AccentViolet
                        )
                        if (selectedForComparison.size >= 2) {
                            Button(
                                onClick = { comparisonMode = true },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = AccentViolet),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text("Launch Compare", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Text(
                                text = "Select at least 1 more",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Candidate List Items
        if (filteredSessions.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No candidate records found",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Try adjusting your search query, role filter, or status criteria.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(filteredSessions) { session ->
                val isSelected = selectedForComparison.contains(session.id)
                val sessionSkills = allSkills.filter { it.sessionId == session.id }

                CandidateReviewCard(
                    session = session,
                    skills = sessionSkills,
                    isSelectedForComparison = isSelected,
                    onToggleComparison = {
                        if (isSelected) {
                            selectedForComparison.remove(session.id)
                        } else {
                            selectedForComparison.add(session.id)
                        }
                    },
                    onInspectReport = { onInspectReport(session.id) },
                    onOpenReview = { onOpenReview(session) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * Detailed Candidate Review Card displaying real verified profile, role competencies,
 * strengths, validation gaps, and comparison selection checkbox.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CandidateReviewCard(
    session: InterviewSessionEntity,
    skills: List<SkillAssessmentEntity>,
    isSelectedForComparison: Boolean,
    onToggleComparison: () -> Unit,
    onInspectReport: () -> Unit,
    onOpenReview: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = if (isSelectedForComparison) 2.dp else 1.dp,
                color = if (isSelectedForComparison) AccentViolet else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                shape = RoundedCornerShape(16.dp)
            )
            .testTag("candidate_card_${session.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Checkbox for comparison + Name + Overall Score
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = isSelectedForComparison,
                    onCheckedChange = { onToggleComparison() },
                    modifier = Modifier.testTag("compare_checkbox_${session.id}")
                )

                Spacer(modifier = Modifier.width(6.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = session.candidateName,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "${session.roleTitle} • ${session.roleDepartment}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Overall Score Badge
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (session.overallScore >= 80) StatusDemonstrated.copy(alpha = 0.15f) else PrimaryBlue.copy(alpha = 0.12f)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "${session.overallScore}%",
                            color = if (session.overallScore >= 80) StatusDemonstrated else PrimaryBlue,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "Score",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 9.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Status Bar: Completion date + Verdict + Interview Mode
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Verdict Pill
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when (session.recruiterVerdict) {
                        "ADVANCE_TO_ONSITE" -> StatusDemonstrated.copy(alpha = 0.15f)
                        "FURTHER_EVALUATION" -> StatusClaimedUnvalidated.copy(alpha = 0.15f)
                        "ARCHIVED" -> MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                ) {
                    Text(
                        text = when (session.recruiterVerdict) {
                            "ADVANCE_TO_ONSITE" -> "Advance to On-site"
                            "FURTHER_EVALUATION" -> "Requires Further Evaluation"
                            "ARCHIVED" -> "Archived"
                            else -> "Pending Review"
                        },
                        color = when (session.recruiterVerdict) {
                            "ADVANCE_TO_ONSITE" -> StatusDemonstrated
                            "FURTHER_EVALUATION" -> StatusClaimedUnvalidated
                            "ARCHIVED" -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                // Completion status & questions evaluated
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (session.isCompleted) Icons.Default.CheckCircle else Icons.Default.Schedule,
                        contentDescription = null,
                        tint = if (session.isCompleted) StatusDemonstrated else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (session.isCompleted) "Completed (${session.totalQuestions} Qs)" else "In Progress (${session.currentQuestionIndex}/${session.totalQuestions} Qs)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Evidence-Backed Assessed Skills Pills (4 Categories)
            if (skills.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Evidence-Assessed Competencies:",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    skills.forEach { skill ->
                        SkillEvidenceMiniPill(skill = skill)
                    }
                }
            }

            // Strengths Summary from Session
            if (session.summaryStrengths.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = StatusDemonstrated.copy(alpha = 0.08f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = StatusDemonstrated,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = session.summaryStrengths,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Areas Requiring Validation / Gaps
            if (session.summaryGaps.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = StatusClaimedUnvalidated.copy(alpha = 0.08f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = StatusClaimedUnvalidated,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = session.summaryGaps,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Recruiter Notes if recorded
            if (session.recruiterNotes.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Recruiter Notes: \"${session.recruiterNotes}\"",
                    style = MaterialTheme.typography.bodySmall.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(8.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onToggleComparison,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = if (isSelectedForComparison) Icons.Default.Check else Icons.Default.CompareArrows,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isSelectedForComparison) "Selected for Compare" else "Add to Compare",
                        fontSize = 11.sp,
                        color = if (isSelectedForComparison) AccentViolet else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onInspectReport,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Inspect Report", fontSize = 11.sp)
                    }

                    Button(
                        onClick = onOpenReview,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentViolet),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Recruiter Verdict", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * Mini skill evidence pill reflecting the 4 explicit states:
 * - Demonstrated (Green)
 * - Claimed but unvalidated (Yellow)
 * - Not demonstrated (Red)
 * - Not assessed (Grey)
 */
@Composable
private fun SkillEvidenceMiniPill(skill: SkillAssessmentEntity) {
    val (bgColor, textColor, label) = when (skill.status) {
        SkillStatus.DEMONSTRATED -> Triple(StatusDemonstrated.copy(alpha = 0.15f), StatusDemonstrated, "Demonstrated")
        SkillStatus.CLAIMED_UNVALIDATED -> Triple(StatusClaimedUnvalidated.copy(alpha = 0.15f), StatusClaimedUnvalidated, "Claimed (Unvalidated)")
        SkillStatus.NOT_DEMONSTRATED -> Triple(StatusNotDemonstrated.copy(alpha = 0.15f), StatusNotDemonstrated, "Not Demonstrated")
        SkillStatus.NOT_ASSESSED -> Triple(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant, "Not Assessed")
    }

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = bgColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(textColor)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "${skill.skillName} • $label",
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = textColor
            )
        }
    }
}

/**
 * Candidate Comparison View
 * Side-by-side or stacked multi-candidate comparison against job-required skills,
 * evidence snippets, uncertainty, and hiring recommendations.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CandidateComparisonView(
    candidates: List<InterviewSessionEntity>,
    allSkills: List<SkillAssessmentEntity>,
    onBack: () -> Unit,
    onInspectReport: (String) -> Unit,
    onOpenReview: (InterviewSessionEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    // Primary job role for this comparison (or combined if multiple)
    val primaryRole = candidates.firstOrNull()?.roleTitle ?: "Role Competencies"

    // Required skills from catalog for the target role
    val roleCatalog = remember(primaryRole) {
        AdaptiveInterviewEngine.AVAILABLE_ROLES.firstOrNull { it.title == primaryRole }
    }
    val requiredCompetencies = remember(roleCatalog, candidates, allSkills) {
        val fromCatalog = roleCatalog?.keyCompetencies ?: emptyList()
        val fromSkills = candidates.flatMap { c ->
            allSkills.filter { it.sessionId == c.id }.map { it.skillName }
        }.distinct()
        (fromCatalog + fromSkills).distinct()
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back to Candidates")
                }
                Spacer(modifier = Modifier.width(4.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Candidate Comparison",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Evaluating ${candidates.size} Candidates for: $primaryRole",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Candidates Summary Grid / Header Cards
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, AccentViolet.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Comparison Summary",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        candidates.forEach { candidate ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier
                                    .weight(1f)
                                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = candidate.candidateName,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = "${candidate.overallScore}%",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 18.sp,
                                        color = if (candidate.overallScore >= 80) StatusDemonstrated else PrimaryBlue
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = when (candidate.recruiterVerdict) {
                                            "ADVANCE_TO_ONSITE" -> StatusDemonstrated.copy(alpha = 0.15f)
                                            "FURTHER_EVALUATION" -> StatusClaimedUnvalidated.copy(alpha = 0.15f)
                                            else -> MaterialTheme.colorScheme.surfaceVariant
                                        }
                                    ) {
                                        Text(
                                            text = candidate.recruiterVerdict.replace("_", " "),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = when (candidate.recruiterVerdict) {
                                                "ADVANCE_TO_ONSITE" -> StatusDemonstrated
                                                "FURTHER_EVALUATION" -> StatusClaimedUnvalidated
                                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                                            },
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        TextButton(
                                            onClick = { onInspectReport(candidate.id) },
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text("Report", fontSize = 10.sp)
                                        }
                                        TextButton(
                                            onClick = { onOpenReview(candidate) },
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text("Verdict", fontSize = 10.sp, color = AccentViolet)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section: Required Competencies Comparison Matrix
        item {
            Text(
                text = "Job-Required Competency Breakdown",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = "Comparing verified answer evidence, proficiency, and unvalidated claims",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        items(requiredCompetencies) { compName ->
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f), RoundedCornerShape(14.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = compName,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Row showing each candidate's evaluation on this skill
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        candidates.forEach { candidate ->
                            val skill = allSkills.firstOrNull {
                                it.sessionId == candidate.id &&
                                (it.skillName.equals(compName, ignoreCase = true) || compName.contains(it.skillName, ignoreCase = true) || it.skillName.contains(compName.split(" ").first(), ignoreCase = true))
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier
                                    .weight(1f)
                                    .border(
                                        1.dp,
                                        when (skill?.status) {
                                            SkillStatus.DEMONSTRATED -> StatusDemonstrated.copy(alpha = 0.3f)
                                            SkillStatus.CLAIMED_UNVALIDATED -> StatusClaimedUnvalidated.copy(alpha = 0.3f)
                                            SkillStatus.NOT_DEMONSTRATED -> StatusNotDemonstrated.copy(alpha = 0.3f)
                                            else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                                        },
                                        RoundedCornerShape(10.dp)
                                    )
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text(
                                        text = candidate.candidateName,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    if (skill != null) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = when (skill.status) {
                                                SkillStatus.DEMONSTRATED -> StatusDemonstrated.copy(alpha = 0.15f)
                                                SkillStatus.CLAIMED_UNVALIDATED -> StatusClaimedUnvalidated.copy(alpha = 0.15f)
                                                SkillStatus.NOT_DEMONSTRATED -> StatusNotDemonstrated.copy(alpha = 0.15f)
                                                SkillStatus.NOT_ASSESSED -> MaterialTheme.colorScheme.surfaceVariant
                                            }
                                        ) {
                                            Text(
                                                text = when (skill.status) {
                                                    SkillStatus.DEMONSTRATED -> "Demonstrated (${skill.proficiencyPercent}%)"
                                                    SkillStatus.CLAIMED_UNVALIDATED -> "Claimed (Unvalidated)"
                                                    SkillStatus.NOT_DEMONSTRATED -> "Not Demonstrated (${skill.proficiencyPercent}%)"
                                                    SkillStatus.NOT_ASSESSED -> "Not Assessed"
                                                },
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = when (skill.status) {
                                                    SkillStatus.DEMONSTRATED -> StatusDemonstrated
                                                    SkillStatus.CLAIMED_UNVALIDATED -> StatusClaimedUnvalidated
                                                    SkillStatus.NOT_DEMONSTRATED -> StatusNotDemonstrated
                                                    SkillStatus.NOT_ASSESSED -> MaterialTheme.colorScheme.onSurfaceVariant
                                                },
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }

                                        if (skill.evidenceSnippet.isNotBlank()) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = skill.evidenceSnippet,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    } else {
                                        Text(
                                            text = "Not assessed in this interview",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section: Resume-to-Job Strengths & Gaps Side-by-Side
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Demonstrated Strengths vs. Knowledge Gaps",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        candidates.forEach { candidate ->
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = candidate.candidateName,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Spacer(modifier = Modifier.height(6.dp))

                                // Strengths
                                Text(
                                    text = "Strengths:",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = StatusDemonstrated
                                )
                                Text(
                                    text = candidate.summaryStrengths.ifBlank { "Baseline competence demonstrated." },
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.sp
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                // Gaps
                                Text(
                                    text = "Validation Gaps:",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = StatusClaimedUnvalidated
                                )
                                Text(
                                    text = candidate.summaryGaps.ifBlank { "No major gaps detected." },
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun MetricChip(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp,
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Recruiter Destination 3: Sessions
 * Audit log of all completed and in-progress interview sessions.
 */
@Composable
fun RecruiterSessionsScreen(
    sessions: List<InterviewSessionEntity>,
    onInspectReport: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var modeFilter by remember { mutableStateOf<InterviewMode?>(null) }
    val filtered = if (modeFilter == null) sessions else sessions.filter { it.mode == modeFilter }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Interview Sessions Log",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = "Audit tracking across Formal and Practice interview cycles",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Mode Filter Chips
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = modeFilter == null,
                    onClick = { modeFilter = null },
                    label = { Text("All (${sessions.size})", fontSize = 11.sp) }
                )
                FilterChip(
                    selected = modeFilter == InterviewMode.FORMAL,
                    onClick = { modeFilter = if (modeFilter == InterviewMode.FORMAL) null else InterviewMode.FORMAL },
                    label = { Text("Formal (${sessions.count { it.mode == InterviewMode.FORMAL }})", fontSize = 11.sp) }
                )
                FilterChip(
                    selected = modeFilter == InterviewMode.PRACTICE,
                    onClick = { modeFilter = if (modeFilter == InterviewMode.PRACTICE) null else InterviewMode.PRACTICE },
                    label = { Text("Practice (${sessions.count { it.mode == InterviewMode.PRACTICE }})", fontSize = 11.sp) }
                )
            }
        }

        items(filtered) { session ->
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "${session.roleTitle} Session",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "Candidate: ${session.candidateName}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (session.isCompleted) StatusDemonstrated.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = if (session.isCompleted) "Completed" else "In Progress",
                                color = if (session.isCompleted) StatusDemonstrated else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${session.mode.name} Mode • ${session.totalQuestions} Questions Evaluated",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        TextButton(onClick = { onInspectReport(session.id) }) {
                            Text("Inspect Evidence →", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * Recruiter Destination 4: Assessments
 * Review decisions, official recruiter notes, and verdicts.
 */
@Composable
fun RecruiterAssessmentsScreen(
    sessions: List<InterviewSessionEntity>,
    onOpenReview: (InterviewSessionEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val pending = sessions.filter { it.recruiterVerdict == "PENDING_REVIEW" }
    val reviewed = sessions.filter { it.recruiterVerdict != "PENDING_REVIEW" }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Assessment Review & Decisions",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = "PDF Section 6.7 • Recruiter Evaluation Hub",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Pending Reviews Header
        item {
            Text(
                text = "Pending Recruiter Reviews (${pending.size})",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = StatusClaimedUnvalidated
            )
        }

        if (pending.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "All candidate assessments have been reviewed.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        } else {
            items(pending) { session ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, StatusClaimedUnvalidated.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = session.candidateName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                            Text(text = "${session.overallScore}/100", fontWeight = FontWeight.Bold, color = PrimaryBlue)
                        }
                        Text(text = session.roleTitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { onOpenReview(session) },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentViolet),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Record Assessment Verdict & Notes")
                        }
                    }
                }
            }
        }

        // Completed Reviews Header
        item {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Finalized Decisions (${reviewed.size})",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        }

        items(reviewed) { session ->
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = session.candidateName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = when (session.recruiterVerdict) {
                                "ADVANCE_TO_ONSITE" -> StatusDemonstrated.copy(alpha = 0.15f)
                                else -> PrimaryBlue.copy(alpha = 0.15f)
                            }
                        ) {
                            Text(
                                text = session.recruiterVerdict.replace("_", " "),
                                color = when (session.recruiterVerdict) {
                                    "ADVANCE_TO_ONSITE" -> StatusDemonstrated
                                    else -> PrimaryBlue
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    if (session.recruiterNotes.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "\"${session.recruiterNotes}\"",
                            style = MaterialTheme.typography.bodySmall.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { onOpenReview(session) }) {
                            Text("Update Verdict", fontSize = 12.sp, color = AccentViolet)
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * Recruiter Destination 5: Profile
 * Recruiter organization workspace settings.
 */
@Composable
fun RecruiterProfileScreen(
    recruiterName: String = "Elena Rostova",
    recruiterEmail: String = "elena.recruiter@techscale.io",
    onLogout: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Recruiter Workspace Profile",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = "Talent Acquisition Organization Settings",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(AccentViolet.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Business,
                                contentDescription = null,
                                tint = AccentViolet,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = recruiterName,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = recruiterEmail,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(14.dp))

                    RecruiterInfoRow(label = "Company / Organization", value = "TechScale Innovations")
                    RecruiterInfoRow(label = "Department", value = "Engineering Talent Acquisition")
                    RecruiterInfoRow(label = "Active Requisitions", value = "4 Roles (Android, Cloud, ML, PM)")
                    RecruiterInfoRow(label = "Database Persistence", value = "Room SQLite (Local Offline)")
                }
            }
        }

        // Clear Placeholder for Organization Settings (Rule 6: simple, clearly labelled placeholder)
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Recruiter Preferences & ATS Sync (Placeholder)",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Feature in development: automated ATS candidate export, webhook notifications, and custom rubric weighting configuration.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Logout Action
        item {
            OutlinedButton(
                onClick = onLogout,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("recruiter_logout_button")
            ) {
                Text(text = "Log Out of Recruiter Account", fontWeight = FontWeight.Bold)
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun RecruiterInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun RecruiterReviewDialog(
    session: InterviewSessionEntity,
    onDismiss: () -> Unit,
    onSave: (sessionId: String, notes: String, verdict: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var reviewNotes by remember(session.id) { mutableStateOf(session.recruiterNotes) }
    var selectedVerdict by remember(session.id) {
        mutableStateOf(if (session.recruiterVerdict == "PENDING_REVIEW") "ADVANCE_TO_ONSITE" else session.recruiterVerdict)
    }

    val scrollState = rememberScrollState()

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(scrollState)
            ) {
                Text(
                    text = "Recruiter Assessment Review",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "Candidate: ${session.candidateName} • ${session.roleTitle}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Hiring Recommendation / Verdict:",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )

                Spacer(modifier = Modifier.height(8.dp))

                listOf(
                    "ADVANCE_TO_ONSITE" to "Advance to Technical On-site",
                    "FURTHER_EVALUATION" to "Requires Further Assessment",
                    "ARCHIVED" to "Keep on File / Archived"
                ).forEach { (key, label) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedVerdict = key }
                            .padding(vertical = 4.dp)
                    ) {
                        RadioButton(
                            selected = selectedVerdict == key,
                            onClick = { selectedVerdict = key }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = label, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Official Recruiter Notes & Evidence Context:",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = reviewNotes,
                    onValueChange = { reviewNotes = it },
                    placeholder = { Text("Enter rationale, feedback for hiring team, and integrity review comments...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .testTag("recruiter_notes_field"),
                    maxLines = 5
                )

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            onSave(session.id, reviewNotes, selectedVerdict)
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentViolet),
                        modifier = Modifier.testTag("save_recruiter_decision_button")
                    ) {
                        Text("Save & Notify Candidate", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

