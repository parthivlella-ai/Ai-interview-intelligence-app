package com.example.ui.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.CandidateTab
import com.example.viewmodel.RecruiterTab

data class NavItem<T>(
    val tab: T,
    val label: String,
    val icon: ImageVector,
    val testTag: String
)

val CANDIDATE_NAV_ITEMS = listOf(
    NavItem(CandidateTab.HOME, "Home", Icons.Default.Home, "cand_nav_home"),
    NavItem(CandidateTab.PRACTICE, "Practice", Icons.Default.Psychology, "cand_nav_practice"),
    NavItem(CandidateTab.SKILLS, "Skills", Icons.Default.BarChart, "cand_nav_skills"),
    NavItem(CandidateTab.REPORTS, "Reports", Icons.Default.Description, "cand_nav_reports"),
    NavItem(CandidateTab.PROFILE, "Profile", Icons.Default.Person, "cand_nav_profile")
)

val RECRUITER_NAV_ITEMS = listOf(
    NavItem(RecruiterTab.DASHBOARD, "Dashboard", Icons.Default.Dashboard, "rec_nav_dashboard"),
    NavItem(RecruiterTab.CANDIDATES, "Candidates", Icons.Default.People, "rec_nav_candidates"),
    NavItem(RecruiterTab.SESSIONS, "Sessions", Icons.Default.Schedule, "rec_nav_sessions"),
    NavItem(RecruiterTab.ASSESSMENTS, "Assessments", Icons.Default.AssignmentTurnedIn, "rec_nav_assessments"),
    NavItem(RecruiterTab.PROFILE, "Profile", Icons.Default.AccountCircle, "rec_nav_profile")
)

@Composable
fun CandidateBottomNavBar(
    selectedTab: CandidateTab,
    onTabSelected: (CandidateTab) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
        windowInsets = WindowInsets.navigationBars,
        modifier = modifier.testTag("candidate_bottom_nav_bar")
    ) {
        CANDIDATE_NAV_ITEMS.forEach { item ->
            val isSelected = selectedTab == item.tab
            NavigationBarItem(
                selected = isSelected,
                onClick = { onTabSelected(item.tab) },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label
                    )
                },
                label = {
                    Text(
                        text = item.label,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.testTag(item.testTag)
            )
        }
    }
}

@Composable
fun RecruiterBottomNavBar(
    selectedTab: RecruiterTab,
    onTabSelected: (RecruiterTab) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
        windowInsets = WindowInsets.navigationBars,
        modifier = modifier.testTag("recruiter_bottom_nav_bar")
    ) {
        RECRUITER_NAV_ITEMS.forEach { item ->
            val isSelected = selectedTab == item.tab
            NavigationBarItem(
                selected = isSelected,
                onClick = { onTabSelected(item.tab) },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label
                    )
                },
                label = {
                    Text(
                        text = item.label,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.tertiary,
                    selectedTextColor = MaterialTheme.colorScheme.tertiary,
                    indicatorColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.18f),
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.testTag(item.testTag)
            )
        }
    }
}
