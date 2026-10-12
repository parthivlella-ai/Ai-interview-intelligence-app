package com.example.service

import com.example.data.EvaluatedAnswer
import com.example.data.JobRole
import com.example.data.QuestionDifficulty
import com.example.data.SkillStatus
import java.util.UUID

object AdaptiveInterviewEngine {

    val AVAILABLE_ROLES = listOf(
        JobRole(
            id = "android_dev",
            title = "Android Developer",
            department = "Mobile Core",
            description = "Develop high-scale native Android apps utilizing Jetpack Compose, Kotlin Coroutines, Room DB, clean architecture, and performance profiling.",
            keyCompetencies = listOf(
                "Jetpack Compose & Declarative UI",
                "Kotlin Coroutines & Flow",
                "Offline Architecture & Room DB",
                "Android Performance & Memory Leaks",
                "App Architecture & Modularization"
            )
        ),
        JobRole(
            id = "java_dev",
            title = "Java Developer",
            department = "Enterprise Systems",
            description = "Build enterprise services using Spring Boot, JPA/Hibernate, multithreading, REST APIs, and relational databases.",
            keyCompetencies = listOf(
                "Spring Boot & Microservices",
                "Java Multithreading & Memory Model",
                "Hibernate & Database Transactions",
                "RESTful API & Service Design",
                "Unit Testing & Clean Code"
            )
        ),
        JobRole(
            id = "python_dev",
            title = "Python Developer",
            department = "Backend & Automation",
            description = "Develop scalable backend services, asynchronous APIs, data ingestion scripts, and cloud microservices in Python.",
            keyCompetencies = listOf(
                "Python Asynchronous Programming (asyncio)",
                "FastAPI / Django REST Framework",
                "Database Query Optimization & ORM",
                "API Security & Authentication",
                "Data Structures & Algorithmic Efficiency"
            )
        ),
        JobRole(
            id = "web_dev",
            title = "Web Developer",
            department = "Frontend & Web Engineering",
            description = "Construct responsive, performant web applications with modern TypeScript, React / Next.js, state management, and web accessibility.",
            keyCompetencies = listOf(
                "TypeScript & Modern JavaScript",
                "React State Management & Component Lifecycle",
                "Web Performance & Core Web Vitals",
                "REST & GraphQL Integration",
                "Responsive Design & Accessibility (a11y)"
            )
        ),
        JobRole(
            id = "data_analyst",
            title = "Data Analyst",
            department = "Business Intelligence & Analytics",
            description = "Analyze complex datasets, generate actionable business intelligence, write high-performance SQL queries, and build analytical dashboards.",
            keyCompetencies = listOf(
                "Advanced SQL & Window Functions",
                "Data Modeling & Warehousing",
                "Statistical Analysis & Hypothesis Testing",
                "Business Intelligence & Dashboarding",
                "Data Cleaning & ETL Pipelines"
            )
        ),
        JobRole(
            id = "backend_dev",
            title = "Cloud Backend Engineer",
            department = "Platform Infrastructure",
            description = "Build distributed microservices, low-latency APIs, distributed caching, database indexing, and fault-tolerant cloud pipelines.",
            keyCompetencies = listOf(
                "Distributed Systems & Concurrency",
                "RESTful APIs & gRPC Architecture",
                "Database Indexing & Query Tuning",
                "Distributed Caching (Redis/Memcached)",
                "System Resilience & Circuit Breakers"
            )
        ),
        JobRole(
            id = "ml_engineer",
            title = "Machine Learning Engineer",
            department = "AI Systems & Intelligence",
            description = "Train, optimize, and deploy machine learning models, on-device inference, feature stores, and evaluate model latency and drift.",
            keyCompetencies = listOf(
                "Deep Learning Frameworks (PyTorch/TF)",
                "Model Optimization & Quantization",
                "Data Pipelines & Feature Engineering",
                "LLM Inference & Prompt Optimization",
                "Evaluation Metrics & Drift Detection"
            )
        ),
        JobRole(
            id = "product_mgr",
            title = "Technical Product Manager",
            department = "Product Strategy",
            description = "Lead product discovery, metrics definition, engineering trade-offs, roadmap execution, and user journey optimization.",
            keyCompetencies = listOf(
                "Product Strategy & Vision",
                "Technical Feasibility & Trade-offs",
                "Data Analysis & Metric Design",
                "Stakeholder Alignment & Agile Leadership",
                "User Experience & Empathy"
            )
        )
    )

    data class QuestionTemplate(
        val questionText: String,
        val targetCompetency: String,
        val difficulty: QuestionDifficulty,
        val keywordsExpected: List<String>,
        val rationaleTemplate: String
    )

    // Dynamic question catalog for adaptive questioning
    private val ANDROID_QUESTIONS = listOf(
        QuestionTemplate(
            questionText = "How do you structure unidirectional data flow in Jetpack Compose, and what mechanisms prevent unnecessary recompositions when state updates?",
            targetCompetency = "Jetpack Compose & Declarative UI",
            difficulty = QuestionDifficulty.INTERMEDIATE,
            keywordsExpected = listOf("state", "hoisting", "mutableStateFlow", "remember", "recomposition", "collectAsState", "immutable"),
            rationaleTemplate = "Evaluates foundational architecture and reactive UI composition."
        ),
        QuestionTemplate(
            questionText = "Compare CoroutineScope, supervisorScope, and CoroutineExceptionHandler. What happens when a child coroutine encounters an unhandled exception?",
            targetCompetency = "Kotlin Coroutines & Flow",
            difficulty = QuestionDifficulty.ADVANCED,
            keywordsExpected = listOf("supervisorscope", "exception", "cancellation", "parent", "child", "handler", "structured concurrency"),
            rationaleTemplate = "Advanced validation of asynchronous concurrency and error propagation."
        ),
        QuestionTemplate(
            questionText = "How would you design an offline-first mobile sync engine with Room, handling conflict resolution, optimistic UI updates, and unstable connectivity?",
            targetCompetency = "Offline Architecture & Room DB",
            difficulty = QuestionDifficulty.ADVANCED,
            keywordsExpected = listOf("room", "single source of truth", "flow", "workmanager", "sync", "conflict", "optimistic"),
            rationaleTemplate = "Assesses system resilience and local data persistence under real-world constraints."
        ),
        QuestionTemplate(
            questionText = "Walk me through how you detect, isolate, and fix severe memory leaks and frame drops in a production Android app with heavy image and list scrolling.",
            targetCompetency = "Android Performance & Memory Leaks",
            difficulty = QuestionDifficulty.EXPERT,
            keywordsExpected = listOf("leakcanary", "profiler", "systrace", "perfetto", "heap", "gc", "jank", "baseline profiles"),
            rationaleTemplate = "Probing high-level diagnostic skills and production profiling expertise."
        ),
        QuestionTemplate(
            questionText = "In a multi-module Android project, how do you manage dependency injection and feature navigation without creating tight circular coupling?",
            targetCompetency = "App Architecture & Modularization",
            difficulty = QuestionDifficulty.EXPERT,
            keywordsExpected = listOf("hilt", "dagger", "modularization", "api", "implementation", "interface", "navigation", "loose coupling"),
            rationaleTemplate = "Evaluates large-scale code organization and scalable build design."
        )
    )

    private val BACKEND_QUESTIONS = listOf(
        QuestionTemplate(
            questionText = "What are the core trade-offs between REST and gRPC for internal microservice communication, particularly regarding throughput and streaming?",
            targetCompetency = "RESTful APIs & gRPC Architecture",
            difficulty = QuestionDifficulty.INTERMEDIATE,
            keywordsExpected = listOf("grpc", "protobuf", "http/2", "binary", "rest", "json", "latency", "streaming"),
            rationaleTemplate = "Validates API protocol selection and network performance reasoning."
        ),
        QuestionTemplate(
            questionText = "How do you prevent cache breakdown (stampede) and cache penetration in high-throughput distributed systems using Redis?",
            targetCompetency = "Distributed Caching (Redis/Memcached)",
            difficulty = QuestionDifficulty.ADVANCED,
            keywordsExpected = listOf("cache", "stampede", "mutex", "bloom filter", "ttl", "redis", "penetration"),
            rationaleTemplate = "Tests caching resilience under peak traffic spikes."
        ),
        QuestionTemplate(
            questionText = "Explain how PostgreSQL or MySQL B-Tree indexing works under the hood. When does a database optimizer choose a sequential scan over an index scan?",
            targetCompetency = "Database Indexing & Query Tuning",
            difficulty = QuestionDifficulty.ADVANCED,
            keywordsExpected = listOf("b-tree", "index", "selectivity", "cardinality", "explain", "sequential scan", "io"),
            rationaleTemplate = "Examines deep relational database storage mechanics."
        ),
        QuestionTemplate(
            questionText = "Describe how you implement the Circuit Breaker pattern with fallback mechanisms to protect downstream systems during sudden cascading failures.",
            targetCompetency = "System Resilience & Circuit Breakers",
            difficulty = QuestionDifficulty.EXPERT,
            keywordsExpected = listOf("circuit breaker", "half-open", "resilience4j", "fallback", "threshold", "cascading"),
            rationaleTemplate = "Assesses distributed fault tolerance and mission-critical reliability."
        )
    )

    private val JAVA_QUESTIONS = listOf(
        QuestionTemplate(
            questionText = "How does the Java Memory Model handle thread visibility, and how do volatile and synchronized prevent race conditions under high concurrency?",
            targetCompetency = "Java Multithreading & Memory Model",
            difficulty = QuestionDifficulty.INTERMEDIATE,
            keywordsExpected = listOf("volatile", "synchronized", "happens-before", "thread", "memory", "visibility", "lock"),
            rationaleTemplate = "Examines core Java multithreading and thread synchronization fundamentals."
        ),
        QuestionTemplate(
            questionText = "How do you design and structure microservices with Spring Boot, handling distributed configurations, dependency injection, and health checks?",
            targetCompetency = "Spring Boot & Microservices",
            difficulty = QuestionDifficulty.ADVANCED,
            keywordsExpected = listOf("spring", "boot", "injection", "actuator", "microservice", "config", "rest"),
            rationaleTemplate = "Evaluates modern Java microservice development with Spring framework."
        ),
        QuestionTemplate(
            questionText = "Explain the difference between optimistic and pessimistic locking in Hibernate/JPA, and how transaction isolation levels prevent phantom reads.",
            targetCompetency = "Hibernate & Database Transactions",
            difficulty = QuestionDifficulty.ADVANCED,
            keywordsExpected = listOf("optimistic", "pessimistic", "locking", "transaction", "isolation", "phantom", "jpa"),
            rationaleTemplate = "Tests database concurrency and data integrity in Java ORM."
        )
    )

    private val PYTHON_QUESTIONS = listOf(
        QuestionTemplate(
            questionText = "Explain how Python's asyncio event loop handles cooperative multitasking, and when you should use multiprocessing over threading in Python.",
            targetCompetency = "Python Asynchronous Programming (asyncio)",
            difficulty = QuestionDifficulty.INTERMEDIATE,
            keywordsExpected = listOf("asyncio", "event loop", "await", "coroutine", "thread", "gil", "multiprocessing"),
            rationaleTemplate = "Validates comprehension of Python concurrency models and non-blocking I/O."
        ),
        QuestionTemplate(
            questionText = "How do you structure high-throughput asynchronous APIs with FastAPI, handling Pydantic validation, dependency injection, and database connection pooling?",
            targetCompetency = "FastAPI / Django REST Framework",
            difficulty = QuestionDifficulty.ADVANCED,
            keywordsExpected = listOf("fastapi", "pydantic", "validation", "async", "pool", "sqlalchemy", "orm"),
            rationaleTemplate = "Probes modern Python backend architecture and API performance tuning."
        ),
        QuestionTemplate(
            questionText = "How does Python's memory management and garbage collection (reference counting plus cyclic GC) work, and how do you optimize memory consumption?",
            targetCompetency = "Data Structures & Algorithmic Efficiency",
            difficulty = QuestionDifficulty.ADVANCED,
            keywordsExpected = listOf("reference count", "gc", "cyclic", "memory", "generator", "slots", "optimization"),
            rationaleTemplate = "Assesses memory profiling and internal Python runtime optimization."
        )
    )

    private val WEB_QUESTIONS = listOf(
        QuestionTemplate(
            questionText = "How does React's reconciliation algorithm and Virtual DOM diffing operate, and what practices avoid unnecessary re-renders in large component trees?",
            targetCompetency = "React State Management & Component Lifecycle",
            difficulty = QuestionDifficulty.INTERMEDIATE,
            keywordsExpected = listOf("reconciliation", "virtual dom", "usememo", "usecallback", "memo", "state", "props"),
            rationaleTemplate = "Evaluates frontend component architecture and reactive UI rendering efficiency."
        ),
        QuestionTemplate(
            questionText = "What architectural strategies and metrics do you use to diagnose and optimize Core Web Vitals, specifically LCP, INP, and CLS?",
            targetCompetency = "Web Performance & Core Web Vitals",
            difficulty = QuestionDifficulty.ADVANCED,
            keywordsExpected = listOf("lcp", "inp", "cls", "core web vitals", "bundle", "code splitting", "hydration"),
            rationaleTemplate = "Validates modern web performance auditing and production asset optimization."
        ),
        QuestionTemplate(
            questionText = "How do you leverage TypeScript's type system (generics, mapped types, conditional types) to build resilient, self-documenting web applications?",
            targetCompetency = "TypeScript & Modern JavaScript",
            difficulty = QuestionDifficulty.ADVANCED,
            keywordsExpected = listOf("typescript", "generic", "interface", "type", "union", "narrowing", "mapped"),
            rationaleTemplate = "Tests static type safety and scalable frontend code design."
        )
    )

    private val DATA_QUESTIONS = listOf(
        QuestionTemplate(
            questionText = "Explain how SQL window functions (such as ROW_NUMBER, RANK, DENSE_RANK, and LAG/LEAD) work, and how they differ from GROUP BY aggregations.",
            targetCompetency = "Advanced SQL & Window Functions",
            difficulty = QuestionDifficulty.INTERMEDIATE,
            keywordsExpected = listOf("window", "partition by", "order by", "rank", "dense_rank", "group by", "lag"),
            rationaleTemplate = "Assesses advanced data querying and analytical SQL logic."
        ),
        QuestionTemplate(
            questionText = "How do you design a Kimball-style dimensional data warehouse schema (star vs snowflake) to ensure high query performance for business reporting?",
            targetCompetency = "Data Modeling & Warehousing",
            difficulty = QuestionDifficulty.ADVANCED,
            keywordsExpected = listOf("star", "snowflake", "fact", "dimension", "schema", "grain", "normalization"),
            rationaleTemplate = "Probes dimensional data modeling and analytical schema design."
        ),
        QuestionTemplate(
            questionText = "How do you formulate hypothesis testing, calculate statistical significance (p-values, confidence intervals), and avoid common A/B testing pitfalls?",
            targetCompetency = "Statistical Analysis & Hypothesis Testing",
            difficulty = QuestionDifficulty.ADVANCED,
            keywordsExpected = listOf("hypothesis", "significance", "p-value", "confidence interval", "sample size", "type i error"),
            rationaleTemplate = "Validates quantitative rigor and statistical decision-making."
        )
    )

    private val QUESTION_CATALOG = mapOf(
        "Android Developer" to ANDROID_QUESTIONS,
        "Senior Android Engineer" to ANDROID_QUESTIONS,
        "Java Developer" to JAVA_QUESTIONS,
        "Python Developer" to PYTHON_QUESTIONS,
        "Web Developer" to WEB_QUESTIONS,
        "Data Analyst" to DATA_QUESTIONS,
        "Cloud Backend Engineer" to BACKEND_QUESTIONS
    )

    fun getQuestionsForRole(roleTitle: String, resumeSnippet: String): List<QuestionTemplate> {
        val templates = QUESTION_CATALOG[roleTitle]
            ?: QUESTION_CATALOG.entries.firstOrNull {
                roleTitle.contains(it.key, ignoreCase = true) || it.key.contains(roleTitle, ignoreCase = true)
            }?.value
            ?: QUESTION_CATALOG.values.first()
        return templates
    }

    fun selectAdaptiveQuestion(
        roleTitle: String,
        questionIndex: Int,
        totalQuestions: Int,
        previousScores: List<Int>,
        resumeText: String,
        testedCompetencies: Set<String>
    ): Pair<QuestionTemplate, String> {
        val pool = getQuestionsForRole(roleTitle, resumeText)
        val avgPreviousScore = if (previousScores.isEmpty()) 75 else previousScores.average().toInt()

        // Filter for unassessed competencies first
        val unassessed = pool.filter { it.targetCompetency !in testedCompetencies }
        val candidateQuestions = if (unassessed.isNotEmpty()) unassessed else pool

        // Adaptive difficulty adjustment:
        val selected = when {
            avgPreviousScore >= 85 -> {
                candidateQuestions.firstOrNull { it.difficulty == QuestionDifficulty.EXPERT || it.difficulty == QuestionDifficulty.ADVANCED }
                    ?: candidateQuestions.first()
            }
            avgPreviousScore <= 65 -> {
                candidateQuestions.firstOrNull { it.difficulty == QuestionDifficulty.INTERMEDIATE || it.difficulty == QuestionDifficulty.FOUNDATIONAL }
                    ?: candidateQuestions.first()
            }
            else -> candidateQuestions.first()
        }

        // Adaptive Reason generation (PDF 5.1: Provide an explanation for why a question was selected)
        val resumeClaimed = resumeText.contains(selected.targetCompetency.split(" ").first(), ignoreCase = true)
        val reason = when {
            questionIndex == 1 -> "Initial baseline question: assessing core competencies highlighted in target job description."
            avgPreviousScore >= 85 && resumeClaimed -> "Adapted difficulty to ${selected.difficulty.name} based on strong previous answers (${avgPreviousScore}% avg); verifying advanced resume claim."
            avgPreviousScore >= 85 -> "Elevated to ${selected.difficulty.name} tier to probe candidate's edge-case competency limit."
            avgPreviousScore <= 65 -> "Adjusted focus to test foundational understanding in ${selected.targetCompetency} following uncertain answers."
            else -> "Systematically assessing competency '${selected.targetCompetency}' which has not yet been validated."
        }

        return Pair(selected, reason)
    }

    /**
     * Evaluates candidate's typed/spoken answer against target competency criteria.
     * Produces correctness, missing aspects, supporting evidence, uncertainty metric, and skill delta.
     */
    fun evaluateAnswer(
        questionText: String,
        targetCompetency: String,
        answer: String,
        difficulty: QuestionDifficulty,
        expectedKeywords: List<String>
    ): EvaluatedAnswer {
        val trimmedAnswer = answer.trim()
        val wordCount = trimmedAnswer.split("\\s+".toRegex()).filter { it.isNotBlank() }.size

        // If answer is empty or trivially brief
        if (wordCount < 6) {
            return EvaluatedAnswer(
                score = 25,
                correctness = "Answer was too brief to demonstrate actionable competency.",
                missingPoints = "Expected detailed explanation of architecture, trade-offs, and implementation approach.",
                supportingEvidence = "Minimal response provided: \"${trimmedAnswer.take(50)}\"",
                uncertaintyLevel = "High",
                uncertaintyReason = "Candidate provided insufficient detail for evidence-based assessment.",
                skillUpdated = targetCompetency,
                skillStatusResult = SkillStatus.NOT_DEMONSTRATED
            )
        }

        // Keyword and semantic density matching
        val lowerAnswer = trimmedAnswer.lowercase()
        val matchedKeywords = expectedKeywords.filter { lowerAnswer.contains(it.lowercase()) }
        val matchRatio = if (expectedKeywords.isNotEmpty()) matchedKeywords.size.toFloat() / expectedKeywords.size else 0.5f

        // Calculate score based on keyword coverage, elaboration length, and depth
        var baseScore = (matchRatio * 55).toInt() + 35
        if (wordCount > 35) baseScore += 8
        if (wordCount > 70) baseScore += 5
        val finalScore = baseScore.coerceIn(40, 96)

        // Extract evidence snippets
        val sentences = trimmedAnswer.split(Regex("[.!?]\\s+"))
        val evidenceSnippet = sentences.firstOrNull { sentence ->
            matchedKeywords.any { kw -> sentence.contains(kw, ignoreCase = true) }
        } ?: sentences.firstOrNull() ?: trimmedAnswer.take(120)

        // Uncertainty determination (PDF 5.2: Indicate uncertainty when the answer does not provide enough evidence)
        val (uncertainty, uncertaintyReason) = when {
            finalScore >= 75 && wordCount >= 20 -> Pair("Low", "Clear explanation with specific technical terminology and direct reasoning.")
            finalScore in 55..74 -> Pair("Medium", "Candidate touched on correct principles, but lacked depth in failure modes or performance nuances.")
            else -> Pair("High", "High variance in answer quality; does not adequately substantiate resume claims.")
        }

        // Determine skill classification status (PDF 5.4 & 9)
        val status = when {
            finalScore >= 75 -> SkillStatus.DEMONSTRATED
            finalScore in 55..74 -> SkillStatus.CLAIMED_UNVALIDATED
            else -> SkillStatus.NOT_DEMONSTRATED
        }

        val correctness = when {
            finalScore >= 85 -> "Strong technical accuracy. Effectively addressed core principles and practical mechanics of $targetCompetency."
            finalScore >= 70 -> "Adequate baseline comprehension. Covered primary concepts of $targetCompetency with sensible reasoning."
            else -> "Incomplete explanation. Demonstrated superficial awareness but missed critical implementation aspects."
        }

        val missing = when {
            finalScore >= 85 -> "Could additionally discuss edge cases under extreme concurrency, scaling limits, or production monitoring."
            finalScore >= 70 -> "Lacked concrete discussion of: ${expectedKeywords.filterNot { matchedKeywords.contains(it) }.take(3).joinToString(", ")}."
            else -> "Failed to address core requirements: ${expectedKeywords.take(3).joinToString(", ")}."
        }

        return EvaluatedAnswer(
            score = finalScore,
            correctness = correctness,
            missingPoints = missing,
            supportingEvidence = "Evidence identified: \"$evidenceSnippet\"",
            uncertaintyLevel = uncertainty,
            uncertaintyReason = uncertaintyReason,
            skillUpdated = targetCompetency,
            skillStatusResult = status
        )
    }
}
