package com.example.ui.screens

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SkillStatus
import com.example.data.local.IntegrityEventEntity
import com.example.data.local.InterviewQuestionEntity
import com.example.data.local.InterviewSessionEntity
import com.example.data.local.SkillAssessmentEntity
import com.example.ui.components.IntegrityEventItem
import com.example.ui.components.SkillStatusChip
import com.example.ui.theme.AccentViolet
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.SecondaryTeal
import com.example.ui.theme.StatusClaimedUnvalidated
import com.example.ui.theme.StatusDemonstrated
import com.example.ui.theme.StatusNotAssessed
import com.example.ui.theme.StatusNotDemonstrated
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FinalAssessmentReportScreen(
    session: InterviewSessionEntity?,
    questions: List<InterviewQuestionEntity>,
    skills: List<SkillAssessmentEntity>,
    integrityEvents: List<IntegrityEventEntity>,
    onBack: () -> Unit,
    authorizedCandidateId: String? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var expandedQuestionId by remember { mutableStateOf<String?>(null) }
    var expandedSkillId by remember { mutableStateOf<String?>(null) }

    // Empty / Missing State
    if (session == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp)
                .testTag("report_empty_state"),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    text = "Report Not Found or Inactive",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "No assessment session is currently selected or data is unavailable.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Button(
                    onClick = onBack,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text("Return to Dashboard")
                }
            }
        }
        return
    }

    // Role-based Candidate Access Authorization Check
    if (authorizedCandidateId != null && session.candidateId != authorizedCandidateId) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp)
                .testTag("report_unauthorized_state"),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    text = "Access Restricted",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.error
                )
                Text(
                    text = "You are only authorized to access your own interview assessment reports.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Button(
                    onClick = onBack,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text("Return to Dashboard")
                }
            }
        }
        return
    }

    val dateFormat = remember { SimpleDateFormat("MMMM dd, yyyy • hh:mm a", Locale.getDefault()) }
    val formattedDate = remember(session.createdAt) {
        dateFormat.format(Date(session.createdAt))
    }

    // 4 Official Skill Categories
    val demonstratedSkills = remember(skills) { skills.filter { it.status == SkillStatus.DEMONSTRATED } }
    val claimedUnvalidatedSkills = remember(skills) { skills.filter { it.status == SkillStatus.CLAIMED_UNVALIDATED } }
    val notDemonstratedSkills = remember(skills) { skills.filter { it.status == SkillStatus.NOT_DEMONSTRATED } }
    val notAssessedSkills = remember(skills) { skills.filter { it.status == SkillStatus.NOT_ASSESSED } }

    val totalSkillsCount = skills.size
    val validationCoveragePct = remember(totalSkillsCount, demonstratedSkills) {
        if (totalSkillsCount > 0) ((demonstratedSkills.size.toFloat() / totalSkillsCount) * 100).toInt() else 0
    }

    val avgQuestionScore = remember(questions) {
        val evaluated = questions.filter { it.score > 0 }
        if (evaluated.isNotEmpty()) evaluated.map { it.score }.average().toInt() else session.overallScore
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("final_assessment_report_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Top Bar & Quick Actions
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("report_back_button")
                    ) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Column {
                        Text(
                            text = "Assessment Report",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Evidence-Based Adaptive Interview Findings",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(
                                Intent.EXTRA_TEXT,
                                """
                                ForkAI Assessment Report
                                Candidate: ${session.candidateName}
                                Role: ${session.roleTitle} (${session.roleDepartment})
                                Status: ${if (session.isCompleted) "Completed" else "In Progress"}
                                Overall Score: ${session.overallScore}/100
                                Recruiter Decision: ${session.recruiterVerdict.replace("_", " ")}
                                Verified Skills: ${demonstratedSkills.size}/$totalSkillsCount
                                Strengths: ${session.summaryStrengths}
                                Areas for Improvement: ${session.summaryGaps}
                                """.trimIndent()
                            )
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share Assessment Report"))
                    },
                    modifier = Modifier.testTag("share_report_button")
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = "Share Report", tint = PrimaryBlue)
                }
            }
        }

        // Incomplete / In-Progress Banner (if interview was not completed)
        if (!session.isCompleted) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = StatusClaimedUnvalidated.copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, StatusClaimedUnvalidated.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("partial_report_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = StatusClaimedUnvalidated,
                            modifier = Modifier.size(24.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Partial Assessment Session",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = StatusClaimedUnvalidated
                            )
                            Text(
                                text = "This interview was concluded before completing all adaptive rounds. Displayed findings reflect evaluated questions only.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // 2. Executive Assessment Header & Metadata
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
                    .testTag("executive_summary_card")
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    // Row 1: Candidate name + Circular Score Badge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = session.candidateName,
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${session.roleTitle} • ${session.roleDepartment}",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = PrimaryBlue
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Conducted: $formattedDate",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Circular Overall Score
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        session.overallScore >= 80 -> StatusDemonstrated
                                        session.overallScore >= 65 -> StatusClaimedUnvalidated
                                        else -> StatusNotDemonstrated
                                    }
                                )
                                .testTag("overall_score_badge"),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${session.overallScore}",
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    fontSize = 22.sp
                                )
                                Text(
                                    text = "Score",
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    Spacer(modifier = Modifier.height(14.dp))

                    // Row 2: Status Chips & Mode
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Session Status Badge
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (session.isCompleted) StatusDemonstrated.copy(alpha = 0.12f) else StatusClaimedUnvalidated.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = if (session.isCompleted) "✓ COMPLETED" else "◷ IN PROGRESS",
                                color = if (session.isCompleted) StatusDemonstrated else StatusClaimedUnvalidated,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        // Mode Badge
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = PrimaryBlue.copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = "${session.mode.name} MODE",
                                color = PrimaryBlue,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        // Recruiter Decision Badge
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = when (session.recruiterVerdict) {
                                "ADVANCE_TO_ONSITE" -> StatusDemonstrated.copy(alpha = 0.15f)
                                "FURTHER_EVALUATION" -> StatusClaimedUnvalidated.copy(alpha = 0.15f)
                                else -> PrimaryBlue.copy(alpha = 0.15f)
                            }
                        ) {
                            Text(
                                text = session.recruiterVerdict.replace("_", " "),
                                color = when (session.recruiterVerdict) {
                                    "ADVANCE_TO_ONSITE" -> StatusDemonstrated
                                    "FURTHER_EVALUATION" -> StatusClaimedUnvalidated
                                    else -> PrimaryBlue
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 3 Metric Pills: Evaluated Questions, Coverage, Integrity Events
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ReportMetricPill(
                            label = "Questions",
                            value = "${questions.size}",
                            subtitle = "Evaluated",
                            modifier = Modifier.weight(1f)
                        )
                        ReportMetricPill(
                            label = "Validation",
                            value = "$validationCoveragePct%",
                            subtitle = "${demonstratedSkills.size}/$totalSkillsCount Skills",
                            modifier = Modifier.weight(1f)
                        )
                        ReportMetricPill(
                            label = "Integrity",
                            value = "${integrityEvents.size}",
                            subtitle = if (integrityEvents.isEmpty()) "Clean" else "Flags Logged",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Strengths & Improvement Areas
                    if (session.summaryStrengths.isNotBlank()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Demonstrated Strengths:",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = StatusDemonstrated
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = session.summaryStrengths,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (session.summaryGaps.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Areas Requiring Further Validation / Growth:",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = StatusClaimedUnvalidated
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = session.summaryGaps,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // 3. Resume-to-Job Skill Gap Summary & 4-Category Breakdown
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Resume-to-Job Skill Gap Breakdown",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "Structured 4-category classification of job requirements against interview evidence.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Summary Pill Counts for the 4 categories
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("report_skill_categories_pills"),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CategoryCountPill(
                    count = demonstratedSkills.size,
                    label = "Demonstrated",
                    color = StatusDemonstrated,
                    modifier = Modifier.weight(1f)
                )
                CategoryCountPill(
                    count = claimedUnvalidatedSkills.size,
                    label = "Unvalidated",
                    color = StatusClaimedUnvalidated,
                    modifier = Modifier.weight(1f)
                )
                CategoryCountPill(
                    count = notDemonstratedSkills.size,
                    label = "Not Shown",
                    color = StatusNotDemonstrated,
                    modifier = Modifier.weight(1f)
                )
                CategoryCountPill(
                    count = notAssessedSkills.size,
                    label = "Untested",
                    color = StatusNotAssessed,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // The 4 Category Cards
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Category 1: Demonstrated
                ReportSkillCategoryCard(
                    title = "Demonstrated Competencies",
                    description = "Verified with concrete evidence and structured reasoning during the interview.",
                    skills = demonstratedSkills,
                    questions = questions,
                    status = SkillStatus.DEMONSTRATED,
                    expandedSkillId = expandedSkillId,
                    onToggleSkill = { id -> expandedSkillId = if (expandedSkillId == id) null else id }
                )

                // Category 2: Claimed but unvalidated
                ReportSkillCategoryCard(
                    title = "Claimed but Unvalidated",
                    description = "Claimed in resume or setup, but candidate response lacked sufficient technical depth or certainty.",
                    skills = claimedUnvalidatedSkills,
                    questions = questions,
                    status = SkillStatus.CLAIMED_UNVALIDATED,
                    expandedSkillId = expandedSkillId,
                    onToggleSkill = { id -> expandedSkillId = if (expandedSkillId == id) null else id }
                )

                // Category 3: Not demonstrated
                ReportSkillCategoryCard(
                    title = "Not Demonstrated",
                    description = "Tested during the session but the candidate's answer was incorrect or missing fundamental concepts.",
                    skills = notDemonstratedSkills,
                    questions = questions,
                    status = SkillStatus.NOT_DEMONSTRATED,
                    expandedSkillId = expandedSkillId,
                    onToggleSkill = { id -> expandedSkillId = if (expandedSkillId == id) null else id }
                )

                // Category 4: Not assessed
                ReportSkillCategoryCard(
                    title = "Not Assessed",
                    description = "Listed in target role requirements, but unexamined in this interview session.",
                    skills = notAssessedSkills,
                    questions = questions,
                    status = SkillStatus.NOT_ASSESSED,
                    expandedSkillId = expandedSkillId,
                    onToggleSkill = { id -> expandedSkillId = if (expandedSkillId == id) null else id }
                )
            }
        }

        // 4. Evidence-Based Answer & Question Audit
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Evidence-Based Question Audit (${questions.size} Questions)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "Inspect exact answers, extracted evidence quotes, correctness evaluations, and adaptive reasons.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (questions.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No questions recorded for this interview session.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    questions.forEach { q ->
                        val isExpanded = expandedQuestionId == q.id
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                                .clickable { expandedQuestionId = if (isExpanded) null else q.id }
                                .testTag("question_audit_${q.orderIndex}")
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Q${q.orderIndex}: ${q.targetCompetency}",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = PrimaryBlue.copy(alpha = 0.1f)
                                            ) {
                                                Text(
                                                    text = q.difficulty.name,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = PrimaryBlue,
                                                    fontWeight = FontWeight.SemiBold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }

                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = when {
                                                    q.score >= 80 -> StatusDemonstrated.copy(alpha = 0.12f)
                                                    q.score >= 60 -> StatusClaimedUnvalidated.copy(alpha = 0.12f)
                                                    else -> StatusNotDemonstrated.copy(alpha = 0.12f)
                                                }
                                            ) {
                                                Text(
                                                    text = "${q.score}/100",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = when {
                                                        q.score >= 80 -> StatusDemonstrated
                                                        q.score >= 60 -> StatusClaimedUnvalidated
                                                        else -> StatusNotDemonstrated
                                                    },
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }

                                            if (q.uncertaintyLevel.isNotBlank()) {
                                                Text(
                                                    text = "Uncertainty: ${q.uncertaintyLevel}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }

                                    Icon(
                                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                                        tint = PrimaryBlue
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = q.questionText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                AnimatedVisibility(visible = isExpanded) {
                                    Column(
                                        modifier = Modifier.padding(top = 10.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                                        // Candidate Answer
                                        Column {
                                            Text(
                                                text = "Candidate Response:",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = q.candidateAnswer.ifBlank { "No response recorded." },
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }

                                        // Extracted Evidence Quote
                                        if (q.evidenceExtracted.isNotBlank()) {
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = PrimaryBlue.copy(alpha = 0.08f),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Column(modifier = Modifier.padding(10.dp)) {
                                                    Text(
                                                        text = "Extracted Evidence Quote:",
                                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                        color = PrimaryBlue
                                                    )
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text(
                                                        text = "\"${q.evidenceExtracted}\"",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                }
                                            }
                                        }

                                        // Correctness & Evaluation Details
                                        if (q.correctnessDetails.isNotBlank()) {
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Column(modifier = Modifier.padding(10.dp)) {
                                                    Text(
                                                        text = "Evaluation Summary:",
                                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text(
                                                        text = q.correctnessDetails,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                }
                                            }
                                        }

                                        // Adaptive Selection Reason
                                        if (q.adaptiveReason.isNotBlank()) {
                                            Text(
                                                text = "Adaptive Selection Reason: ${q.adaptiveReason}",
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
            }
        }

        // 5. Interview Integrity Events Timeline
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Interview Integrity Timeline",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "Chronological events logged during monitoring with severity ratings and recruiter notes.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (integrityEvents.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = StatusDemonstrated.copy(alpha = 0.08f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, StatusDemonstrated.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = StatusDemonstrated,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "No anomalous integrity events detected. Clear session validation.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        } else {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    integrityEvents.forEach { event ->
                        IntegrityEventItem(
                            eventType = event.eventType,
                            severity = event.severity,
                            confidence = event.confidence,
                            description = event.description,
                            reviewerNotes = event.recruiterReviewNotes,
                            timestamp = event.timestamp
                        )
                    }
                }
            }
        }

        // 6. Recruiter Notes & Official Decision Section
        if (session.recruiterNotes.isNotBlank()) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, PrimaryBlue.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = PrimaryBlue,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Recruiter Assessment Notes",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = PrimaryBlue
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = session.recruiterNotes,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // 7. Bottom Navigation Action
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onBack,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("back_to_dashboard_btn")
            ) {
                Text(text = "Return to Dashboard", fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

/**
 * Enhanced Category Card supporting expansion for evidence and question inspection
 */
@Composable
private fun ReportSkillCategoryCard(
    title: String,
    description: String,
    skills: List<SkillAssessmentEntity>,
    questions: List<InterviewQuestionEntity>,
    status: SkillStatus,
    expandedSkillId: String?,
    onToggleSkill: (String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                when (status) {
                    SkillStatus.DEMONSTRATED -> StatusDemonstrated.copy(alpha = 0.35f)
                    SkillStatus.CLAIMED_UNVALIDATED -> StatusClaimedUnvalidated.copy(alpha = 0.35f)
                    SkillStatus.NOT_DEMONSTRATED -> StatusNotDemonstrated.copy(alpha = 0.35f)
                    SkillStatus.NOT_ASSESSED -> StatusNotAssessed.copy(alpha = 0.35f)
                },
                RoundedCornerShape(14.dp)
            )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Surface(
                        shape = CircleShape,
                        color = when (status) {
                            SkillStatus.DEMONSTRATED -> StatusDemonstrated.copy(alpha = 0.15f)
                            SkillStatus.CLAIMED_UNVALIDATED -> StatusClaimedUnvalidated.copy(alpha = 0.15f)
                            SkillStatus.NOT_DEMONSTRATED -> StatusNotDemonstrated.copy(alpha = 0.15f)
                            SkillStatus.NOT_ASSESSED -> StatusNotAssessed.copy(alpha = 0.15f)
                        }
                    ) {
                        Text(
                            text = "${skills.size}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = when (status) {
                                SkillStatus.DEMONSTRATED -> StatusDemonstrated
                                SkillStatus.CLAIMED_UNVALIDATED -> StatusClaimedUnvalidated
                                SkillStatus.NOT_DEMONSTRATED -> StatusNotDemonstrated
                                SkillStatus.NOT_ASSESSED -> StatusNotAssessed
                            },
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
                SkillStatusChip(status = status)
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            if (skills.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "None in this category.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.padding(10.dp)
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    skills.forEach { skill ->
                        val isExpanded = expandedSkillId == skill.id
                        val relatedQuestion = questions.firstOrNull {
                            it.targetCompetency.equals(skill.skillName, ignoreCase = true) ||
                            it.targetCompetency.contains(skill.skillName, ignoreCase = true) ||
                            skill.skillName.contains(it.targetCompetency, ignoreCase = true)
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onToggleSkill(skill.id) }
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = skill.skillName,
                                            fontWeight = FontWeight.SemiBold,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = skill.domainCategory,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Icon(
                                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                                        tint = PrimaryBlue,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                if (skill.evidenceSnippet.isNotBlank() && !isExpanded) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = skill.evidenceSnippet,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                AnimatedVisibility(visible = isExpanded) {
                                    Column(
                                        modifier = Modifier.padding(top = 8.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                                        // Assessment explanation
                                        if (skill.evidenceSnippet.isNotBlank()) {
                                            Text(
                                                text = "Assessment Explanation:",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = PrimaryBlue
                                            )
                                            Text(
                                                text = skill.evidenceSnippet,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }

                                        // Related Question & Answer Quote
                                        if (relatedQuestion != null) {
                                            Text(
                                                text = "Related Question (Q${relatedQuestion.orderIndex}):",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = relatedQuestion.questionText,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )

                                            if (relatedQuestion.candidateAnswer.isNotBlank()) {
                                                Text(
                                                    text = "Answer Extract:",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text = relatedQuestion.candidateAnswer,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }

                                            if (relatedQuestion.uncertaintyLevel.isNotBlank()) {
                                                Text(
                                                    text = "Assessment Confidence: ${if (relatedQuestion.uncertaintyLevel == "Low") "High Certainty" else "${relatedQuestion.uncertaintyLevel} Uncertainty"}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = PrimaryBlue,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        } else if (status == SkillStatus.NOT_ASSESSED) {
                                            Text(
                                                text = "Reason: Untested during this interview session. Recommend evaluating in next round.",
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
            }
        }
    }
}

@Composable
private fun ReportMetricPill(
    label: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = PrimaryBlue,
                fontSize = 9.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun CategoryCountPill(
    count: Int,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = color.copy(alpha = 0.12f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$count",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
