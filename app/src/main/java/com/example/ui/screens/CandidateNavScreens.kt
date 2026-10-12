package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Work
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
import com.example.ui.components.SkillStatusChip
import com.example.ui.theme.AccentViolet
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.SecondaryTeal
import com.example.ui.theme.StatusClaimedUnvalidated
import com.example.ui.theme.StatusDemonstrated
import com.example.ui.theme.StatusNotAssessed
import com.example.ui.theme.StatusNotDemonstrated

/**
 * Candidate Destination 2: Practice
 * Focuses on simulated adaptive interview practice with instant answer feedback.
 */
@Composable
fun CandidatePracticeScreen(
    onStartPracticeSession: () -> Unit,
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

            // Practice Mode Hero Header
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, PrimaryBlue.copy(alpha = 0.3f), RoundedCornerShape(18.dp))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Adaptive Practice Lab",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Instant Evidence-Based Feedback (PDF 6.5)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(PrimaryBlue.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Psychology,
                                contentDescription = null,
                                tint = PrimaryBlue
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Practice questions adjust difficulty dynamically based on your responses. Unlike Formal Mode, Practice Mode displays an immediate breakdown of correctness, missing points, evidence quotes, and assessment uncertainty after each question.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = onStartPracticeSession,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("launch_practice_button")
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Launch Practice Interview", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Text(
                text = "Core Competency Practice Modules",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                PracticeModuleCard(
                    title = "Jetpack Compose & State Hoisting",
                    subtitle = "Unidirectional data flow, remember, collectAsStateWithLifecycle",
                    difficulty = "Intermediate",
                    onSelect = onStartPracticeSession
                )
                PracticeModuleCard(
                    title = "Kotlin Coroutines & Flow Hierarchy",
                    subtitle = "Structured concurrency, supervisorScope, cancellation propagation",
                    difficulty = "Advanced",
                    onSelect = onStartPracticeSession
                )
                PracticeModuleCard(
                    title = "Offline Persistence & Room DB",
                    subtitle = "Single source of truth, reactive flows, cache invalidation",
                    difficulty = "Advanced",
                    onSelect = onStartPracticeSession
                )
                PracticeModuleCard(
                    title = "Memory Profiling & Systrace",
                    subtitle = "Diagnosing heap leaks, GC pauses, frame drop bottlenecks",
                    difficulty = "Expert",
                    onSelect = onStartPracticeSession
                )
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = SecondaryTeal,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Practice vs Formal Mode Notice",
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.labelMedium
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "In Formal Mode, instant answer feedback is hidden until the interview concludes to avoid influencing subsequent answers, and integrity tracking is actively recorded.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
private fun PracticeModuleCard(
    title: String,
    subtitle: String,
    difficulty: String,
    onSelect: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
            .clickable { onSelect() }
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = PrimaryBlue.copy(alpha = 0.1f)
            ) {
                Text(
                    text = difficulty,
                    color = PrimaryBlue,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

/**
 * Candidate Destination 3: Skills
 * Candidate Competency Map and 4-way classification breakdown.
 * Connects directly to Room database SkillAssessmentEntity records and interview question evidence.
 */
@Composable
fun CandidateSkillsScreen(
    skills: List<SkillAssessmentEntity>,
    onStartEvaluation: () -> Unit,
    candidateName: String? = null,
    questions: List<com.example.data.local.InterviewQuestionEntity> = emptyList(),
    isLoading: Boolean = false,
    errorMessage: String? = null,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf<SkillStatus?>(null) }
    var expandedSkillId by remember { mutableStateOf<String?>(null) }

    val demonstratedSkills = remember(skills) { skills.filter { it.status == SkillStatus.DEMONSTRATED } }
    val claimedUnvalidatedSkills = remember(skills) { skills.filter { it.status == SkillStatus.CLAIMED_UNVALIDATED } }
    val notDemonstratedSkills = remember(skills) { skills.filter { it.status == SkillStatus.NOT_DEMONSTRATED } }
    val notAssessedSkills = remember(skills) { skills.filter { it.status == SkillStatus.NOT_ASSESSED } }

    val demonstratedCount = demonstratedSkills.size
    val unvalidatedCount = claimedUnvalidatedSkills.size
    val notDemonstratedCount = notDemonstratedSkills.size
    val notAssessedCount = notAssessedSkills.size

    val filteredSkills = remember(skills, selectedFilter) {
        if (selectedFilter == null) skills else skills.filter { it.status == selectedFilter }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("skills_dashboard_screen")
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // A. Header
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Column {
                Text(
                    text = "My Skills",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.testTag("skills_header_title")
                )
                Spacer(modifier = Modifier.height(4.dp))
                val subtitle = if (!candidateName.isNullOrBlank()) {
                    "Understand your strengths, $candidateName, and discover what to improve."
                } else {
                    "Understand your strengths and discover what to improve."
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("skills_header_subtitle")
                )
            }
        }

        // Error Banner with Retry
        if (!errorMessage.isNullOrBlank()) {
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("skills_error_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = errorMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        if (onRetry != null) {
                            TextButton(
                                onClick = onRetry,
                                modifier = Modifier.testTag("skills_retry_button")
                            ) {
                                Text(
                                    text = "Retry",
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Loading State
        if (isLoading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp)
                        .testTag("skills_loading_indicator"),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = PrimaryBlue,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        } else if (skills.isEmpty()) {
            // Helpful Empty State (When no assessments exist)
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            RoundedCornerShape(16.dp)
                        )
                        .testTag("skills_empty_state_card")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(PrimaryBlue.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Assessment,
                                contentDescription = null,
                                tint = PrimaryBlue,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Text(
                            text = "No Skill Assessments Found",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            text = "Skills will appear after an interview is completed. Start an adaptive interview to test your competencies and generate your personalized evidence breakdown.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            lineHeight = 18.sp,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        Button(
                            onClick = onStartEvaluation,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .testTag("empty_start_interview_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Start Your First Interview")
                        }
                    }
                }
            }
        } else {
            // B. Skill Overview (Summary Counts for 4 Required Categories)
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("skills_overview_section")
                ) {
                    Text(
                        text = "Skill Overview",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatPill(
                            count = demonstratedCount,
                            label = "Demonstrated",
                            color = StatusDemonstrated,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("count_demonstrated_pill")
                        )
                        StatPill(
                            count = unvalidatedCount,
                            label = "Unvalidated",
                            color = StatusClaimedUnvalidated,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("count_unvalidated_pill")
                        )
                        StatPill(
                            count = notDemonstratedCount,
                            label = "Not Shown",
                            color = StatusNotDemonstrated,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("count_not_demonstrated_pill")
                        )
                        StatPill(
                            count = notAssessedCount,
                            label = "Untested",
                            color = StatusNotAssessed,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("count_not_assessed_pill")
                        )
                    }
                }
            }

            // Category Filter Chips
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("skills_filter_row"),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = selectedFilter == null,
                        onClick = { selectedFilter = null },
                        label = { Text("All (${skills.size})", fontSize = 11.sp) },
                        modifier = Modifier.testTag("filter_all_chip")
                    )
                    FilterChip(
                        selected = selectedFilter == SkillStatus.DEMONSTRATED,
                        onClick = {
                            selectedFilter = if (selectedFilter == SkillStatus.DEMONSTRATED) null else SkillStatus.DEMONSTRATED
                        },
                        label = { Text("Demonstrated ($demonstratedCount)", fontSize = 11.sp) },
                        modifier = Modifier.testTag("filter_demonstrated_chip")
                    )
                    FilterChip(
                        selected = selectedFilter == SkillStatus.CLAIMED_UNVALIDATED,
                        onClick = {
                            selectedFilter = if (selectedFilter == SkillStatus.CLAIMED_UNVALIDATED) null else SkillStatus.CLAIMED_UNVALIDATED
                        },
                        label = { Text("Unvalidated ($unvalidatedCount)", fontSize = 11.sp) },
                        modifier = Modifier.testTag("filter_unvalidated_chip")
                    )
                    FilterChip(
                        selected = selectedFilter == SkillStatus.NOT_DEMONSTRATED,
                        onClick = {
                            selectedFilter = if (selectedFilter == SkillStatus.NOT_DEMONSTRATED) null else SkillStatus.NOT_DEMONSTRATED
                        },
                        label = { Text("Not Shown ($notDemonstratedCount)", fontSize = 11.sp) },
                        modifier = Modifier.testTag("filter_not_demonstrated_chip")
                    )
                }
            }

            // Category Explanations Card when viewing all or specific filters
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Classification Criteria:",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "• Demonstrated: Interview evidence supports the skill assessment.\n• Claimed but unvalidated: The resume lists the skill, but interview evidence has not verified it.\n• Not demonstrated: Available interview answers did not demonstrate the expected competency.\n• Not assessed: The interview did not sufficiently test the skill.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            // Skills List / Category Groups
            if (filteredSkills.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("skills_filter_empty_state")
                    ) {
                        Text(
                            text = "No skills found under the selected category filter.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(20.dp)
                        )
                    }
                }
            } else {
                items(filteredSkills) { skill ->
                    val isExpanded = expandedSkillId == skill.id
                    val relatedQuestion = remember(skill, questions) {
                        questions.firstOrNull { it.targetCompetency.equals(skill.skillName, ignoreCase = true) }
                            ?: questions.firstOrNull { it.targetCompetency.contains(skill.skillName, ignoreCase = true) || skill.skillName.contains(it.targetCompetency, ignoreCase = true) }
                    }

                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                1.dp,
                                when (skill.status) {
                                    SkillStatus.DEMONSTRATED -> StatusDemonstrated.copy(alpha = 0.25f)
                                    SkillStatus.CLAIMED_UNVALIDATED -> StatusClaimedUnvalidated.copy(alpha = 0.25f)
                                    SkillStatus.NOT_DEMONSTRATED -> StatusNotDemonstrated.copy(alpha = 0.25f)
                                    SkillStatus.NOT_ASSESSED -> StatusNotAssessed.copy(alpha = 0.25f)
                                },
                                RoundedCornerShape(14.dp)
                            )
                            .clickable {
                                expandedSkillId = if (isExpanded) null else skill.id
                            }
                            .testTag("skill_card_${skill.id}")
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            // Top Row: Title, Domain, Status Chip
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = skill.skillName,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = skill.domainCategory,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                SkillStatusChip(status = skill.status)
                            }

                            // Category-Specific Short Status Explanation
                            Spacer(modifier = Modifier.height(6.dp))
                            val explanationText = when (skill.status) {
                                SkillStatus.DEMONSTRATED -> "Interview evidence supports the skill assessment."
                                SkillStatus.CLAIMED_UNVALIDATED -> "The resume lists the skill, but interview evidence has not verified it."
                                SkillStatus.NOT_DEMONSTRATED -> "Available interview answers did not demonstrate the expected competency."
                                SkillStatus.NOT_ASSESSED -> "The interview did not sufficiently test the skill."
                            }
                            Text(
                                text = explanationText,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                            )

                            // Evidence Snippet (if available on the entity)
                            if (skill.evidenceSnippet.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(
                                            text = "Evidence Summary:",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = skill.evidenceSnippet,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }

                            // Expandable Details Affordance
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isExpanded) "Hide assessment evidence" else "Tap to view evidence details",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PrimaryBlue,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (isExpanded) "▲" else "▼",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PrimaryBlue
                                )
                            }

                            // D. Expanded Genuine Evidence Details
                            if (isExpanded) {
                                Spacer(modifier = Modifier.height(10.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                Spacer(modifier = Modifier.height(10.dp))

                                Column(
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.testTag("skill_evidence_details_${skill.id}")
                                ) {
                                    // 1. Competency Name
                                    DetailField(label = "Competency", value = skill.skillName)

                                    // 2. Related Interview Question
                                    if (relatedQuestion != null && relatedQuestion.questionText.isNotBlank()) {
                                        DetailField(label = "Related Question", value = relatedQuestion.questionText)
                                    } else {
                                        DetailField(
                                            label = "Related Question",
                                            value = if (skill.status == SkillStatus.NOT_ASSESSED) "No question evaluated for this competency in this session." else "Assessed across broader domain questions."
                                        )
                                    }

                                    // 3. Supporting Answer or Evidence Excerpt
                                    if (relatedQuestion != null && relatedQuestion.candidateAnswer.isNotBlank()) {
                                        DetailField(label = "Candidate Answer Excerpt", value = relatedQuestion.candidateAnswer)
                                    }

                                    if (relatedQuestion != null && relatedQuestion.evidenceExtracted.isNotBlank()) {
                                        DetailField(label = "Extracted Evidence Quote", value = relatedQuestion.evidenceExtracted)
                                    }

                                    // 4. Assessment Explanation
                                    if (relatedQuestion != null && relatedQuestion.correctnessDetails.isNotBlank()) {
                                        DetailField(label = "Assessment Evaluation", value = relatedQuestion.correctnessDetails)
                                    } else if (skill.evidenceSnippet.isNotBlank()) {
                                        DetailField(label = "Assessment Explanation", value = skill.evidenceSnippet)
                                    }

                                    // 5. Uncertainty Information
                                    if (relatedQuestion != null && relatedQuestion.uncertaintyLevel.isNotBlank()) {
                                        val uncertaintyText = buildString {
                                            append("Confidence: ")
                                            append(when (relatedQuestion.uncertaintyLevel) {
                                                "Low" -> "High certainty (Low uncertainty)"
                                                "Medium" -> "Moderate certainty"
                                                "High" -> "Low certainty (High uncertainty)"
                                                else -> relatedQuestion.uncertaintyLevel
                                            })
                                            if (relatedQuestion.uncertaintyReason.isNotBlank()) {
                                                append(" • ")
                                                append(relatedQuestion.uncertaintyReason)
                                            }
                                        }
                                        DetailField(label = "Uncertainty Information", value = uncertaintyText)
                                    } else {
                                        DetailField(label = "Uncertainty Information", value = "Standard evaluation baseline.")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Bottom CTA
            item {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onStartEvaluation,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("validate_untested_button")
                ) {
                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Validate Untested Competencies")
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun DetailField(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun StatPill(
    count: Int,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.1f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "$count", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = color)
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = color, maxLines = 1)
        }
    }
}

/**
 * Candidate Destination 4: Reports
 * List of past assessment reports with full deep-linking.
 */
/**
 * Candidate Destination 4: Reports
 * List of past assessment reports with full deep-linking, metrics summary, and role filter chips.
 */
@Composable
fun CandidateReportsScreen(
    sessions: List<InterviewSessionEntity>,
    onViewReport: (String) -> Unit,
    onStartNewInterview: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf<String?>(null) }

    val completedSessions = remember(sessions) { sessions.filter { it.isCompleted } }
    val avgScore = remember(completedSessions) {
        if (completedSessions.isNotEmpty()) completedSessions.map { it.overallScore }.average().toInt() else 0
    }
    val advancedCount = remember(completedSessions) {
        completedSessions.count { it.recruiterVerdict == "ADVANCE_TO_ONSITE" }
    }

    val availableRoles = remember(sessions) {
        sessions.map { it.roleTitle }.distinct()
    }

    val filteredSessions = remember(sessions, selectedFilter) {
        if (selectedFilter == null) sessions else sessions.filter { it.roleTitle == selectedFilter }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // A. Header
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Assessment Reports",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Evidence-based interview evaluations & recruiter decisions",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = PrimaryBlue.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = "${sessions.size} Total",
                        color = PrimaryBlue,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // B. Summary Metrics Banner (when reports exist)
        if (sessions.isNotEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${completedSessions.size}",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = PrimaryBlue
                            )
                            Text(
                                text = "Completed",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Box(
                            modifier = Modifier
                                .height(32.dp)
                                .width(1.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (completedSessions.isNotEmpty()) "$avgScore%" else "—",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = if (avgScore >= 75) StatusDemonstrated else StatusClaimedUnvalidated
                            )
                            Text(
                                text = "Avg Score",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Box(
                            modifier = Modifier
                                .height(32.dp)
                                .width(1.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$advancedCount",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = StatusDemonstrated
                            )
                            Text(
                                text = "Onsite Ready",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // C. Role Filter Chips (if more than 1 role exists)
        if (availableRoles.size > 1) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = selectedFilter == null,
                        onClick = { selectedFilter = null },
                        label = { Text("All Roles", fontSize = 11.sp) }
                    )
                    availableRoles.forEach { role ->
                        FilterChip(
                            selected = selectedFilter == role,
                            onClick = { selectedFilter = if (selectedFilter == role) null else role },
                            label = { Text(role, fontSize = 11.sp, maxLines = 1) }
                        )
                    }
                }
            }
        }

        // D. List of Sessions or Empty State
        if (filteredSessions.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Assessment,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(44.dp)
                        )
                        Text(
                            text = if (selectedFilter != null) "No reports for selected role" else "No assessment reports yet",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (selectedFilter != null)
                                "Clear the role filter above to view all assessment reports."
                            else
                                "Complete an adaptive AI interview session to generate your comprehensive skill assessment and recruiter evaluation report.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        if (selectedFilter == null && onStartNewInterview != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Button(
                                onClick = onStartNewInterview,
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Start First Interview")
                            }
                        }
                    }
                }
            }
        } else {
            items(filteredSessions) { session ->
                InterviewSessionCard(
                    session = session,
                    onViewReport = { onViewReport(session.id) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * Candidate Destination 5: Profile
 * Candidate information, local storage status, and privacy preferences.
 */
@Composable
fun CandidateProfileScreen(
    userName: String = "Alex Rivera",
    userEmail: String = "alex.rivera@example.com",
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
                text = "Candidate Profile",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = "Offline SQLite Account & Consent Preferences",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Profile Identity Card
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
                                .background(PrimaryBlue.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = PrimaryBlue,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = userName,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = userEmail,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(14.dp))

                    ProfileRow(label = "Target Role", value = "Senior Android Engineer")
                    ProfileRow(label = "Professional Experience", value = "4 Years (Native Android)")
                    ProfileRow(label = "Active Specialization", value = "Jetpack Compose & Reactive Systems")
                }
            }
        }

        // Offline Storage Info
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = Icons.Default.Storage,
                        contentDescription = null,
                        tint = StatusDemonstrated,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Local Storage & Offline Readiness",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "All interview sessions, evaluations, and skill assessments are stored locally in Room SQLite with full offline access.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Privacy & Responsible AI Safeguards (PDF Section 10)
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = SecondaryTeal,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Responsible AI & Integrity Notice (PDF Sec. 10)",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• AI evaluations link findings directly to answer evidence quotes.\n• Uncertainty is made explicit when answers lack sufficient detail.\n• Integrity monitoring events are treated as signals requiring recruiter review, never automated disqualifications.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        // Clear Placeholder for Preferences (Rule 6: simple, clearly labelled placeholder)
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
                        text = "Preferences & Settings (Placeholder)",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Feature in development: candidate notification tones, auto-save drafts, and resume PDF import settings.",
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
                    .testTag("candidate_logout_button")
            ) {
                Text(text = "Log Out of Candidate Account", fontWeight = FontWeight.Bold)
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ProfileRow(label: String, value: String) {
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
