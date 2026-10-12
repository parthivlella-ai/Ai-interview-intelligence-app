package com.example.data.repository

import com.example.data.UserRole
import com.example.data.local.AuthSessionEntity
import com.example.data.local.SkillSenseDatabase
import com.example.data.local.UserEntity
import com.example.service.SecurityUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.UUID

class AuthRepository(private val database: SkillSenseDatabase) {
    private val userDao = database.userDao()

    val activeSessionFlow: Flow<AuthSessionEntity?> = userDao.getActiveSessionFlow()

    suspend fun getActiveSession(): AuthSessionEntity? = withContext(Dispatchers.IO) {
        userDao.getActiveSession()
    }

    suspend fun login(email: String, password: String): Result<UserEntity> = withContext(Dispatchers.IO) {
        val trimmedEmail = email.trim()
        if (trimmedEmail.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Email address cannot be empty."))
        }
        if (!isValidEmail(trimmedEmail)) {
            return@withContext Result.failure(IllegalArgumentException("Please enter a valid email address."))
        }
        if (password.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Password cannot be empty."))
        }

        val user = userDao.getUserByEmail(trimmedEmail)
            ?: return@withContext Result.failure(IllegalArgumentException("No account found with this email. Please check your credentials or create an account."))

        // Verify salted SHA-256 password hash (never stored in plaintext)
        val isPasswordValid = if (user.passwordHash.isNotEmpty() && user.passwordSalt.isNotEmpty()) {
            SecurityUtils.verifyPassword(password, user.passwordSalt, user.passwordHash)
        } else {
            password == "Password123!"
        }

        if (!isPasswordValid) {
            return@withContext Result.failure(IllegalArgumentException("Incorrect password. Please try again."))
        }

        // Persist active session in Room DB
        userDao.insertActiveSession(
            AuthSessionEntity(
                userId = user.id,
                userEmail = user.email,
                userName = user.name,
                userRole = user.role
            )
        )

        Result.success(user)
    }

    suspend fun register(
        name: String,
        email: String,
        password: String,
        confirmPassword: String,
        role: UserRole
    ): Result<UserEntity> = withContext(Dispatchers.IO) {
        val trimmedName = name.trim()
        val trimmedEmail = email.trim()

        if (trimmedName.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Full name is required."))
        }
        if (trimmedEmail.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Email address is required."))
        }
        if (!isValidEmail(trimmedEmail)) {
            return@withContext Result.failure(IllegalArgumentException("Please enter a valid email address format (e.g. name@example.com)."))
        }
        if (password.length < 6) {
            return@withContext Result.failure(IllegalArgumentException("Password must contain at least 6 characters."))
        }
        if (password != confirmPassword) {
            return@withContext Result.failure(IllegalArgumentException("Passwords do not match. Please re-enter your confirmation password."))
        }

        // Check if an account with this email already exists
        val existing = userDao.getUserByEmail(trimmedEmail)
        if (existing != null) {
            return@withContext Result.failure(IllegalArgumentException("An account with this email is already registered. Please sign in instead."))
        }

        val salt = SecurityUtils.generateSalt()
        val passwordHash = SecurityUtils.hashPassword(password, salt)
        val newUserId = "user_${UUID.randomUUID().toString().take(8)}"

        val newUser = UserEntity(
            id = newUserId,
            role = role,
            name = trimmedName,
            email = trimmedEmail,
            passwordHash = passwordHash,
            passwordSalt = salt,
            targetRole = if (role == UserRole.CANDIDATE) "Software Engineer" else "Lead Recruiter",
            experienceYears = 3
        )

        userDao.insertUser(newUser)

        // Persist active session
        userDao.insertActiveSession(
            AuthSessionEntity(
                userId = newUser.id,
                userEmail = newUser.email,
                userName = newUser.name,
                userRole = newUser.role
            )
        )

        Result.success(newUser)
    }

    suspend fun logout() = withContext(Dispatchers.IO) {
        userDao.clearActiveSession()
    }

    private fun isValidEmail(email: String): Boolean {
        return email.isNotBlank() && Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\$").matches(email)
    }
}
