package com.example.courserec.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;

@Component
public class DatabaseInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseInitializer.class);

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;
    private final Environment environment;

    public DatabaseInitializer(JdbcTemplate jdbcTemplate, PasswordEncoder passwordEncoder, Environment environment) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        bootstrapAdminUser();
        seedDemoCourses();
    }

    private void bootstrapAdminUser() {
        String adminEmail = System.getenv("ADMIN_EMAIL");
        if (adminEmail == null || adminEmail.isBlank()) {
            adminEmail = environment.getProperty("ADMIN_EMAIL");
        }

        String adminPassword = System.getenv("ADMIN_PASSWORD");
        if (adminPassword == null || adminPassword.isBlank()) {
            adminPassword = environment.getProperty("ADMIN_PASSWORD");
        }

        if (adminEmail != null && !adminEmail.isBlank() && adminPassword != null && !adminPassword.isBlank()) {
            String normalizedEmail = adminEmail.trim().toLowerCase();
            String encodedPassword = passwordEncoder.encode(adminPassword.trim());

            Integer existing = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM users WHERE LOWER(email) = ?",
                    Integer.class,
                    normalizedEmail
            );

            if (existing != null && existing > 0) {
                jdbcTemplate.update(
                        "UPDATE users SET role = 'ADMIN', password_hash = ? WHERE LOWER(email) = ?",
                        encodedPassword,
                        normalizedEmail
                );
                log.info("Bootstrapped admin account updated: {}", normalizedEmail);
            } else {
                jdbcTemplate.update(
                        "INSERT INTO users (name, email, password_hash, role, academic_background, proficiency_level, career_goal) VALUES (?, ?, ?, 'ADMIN', ?, ?, ?)",
                        "Administrator",
                        normalizedEmail,
                        encodedPassword,
                        "",
                        "ADVANCED",
                        ""
                );
                log.info("Bootstrapped admin account created: {}", normalizedEmail);
            }
        }
    }

    private void seedDemoCourses() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM courses WHERE is_demo = true",
                Integer.class
        );

        if (count != null && count >= 14) {
            log.info("Demo courses already seeded (count={}), skipping seed.", count);
            return;
        }

        log.info("Seeding demo courses...");

        record DemoCourse(
                String title,
                String description,
                String category,
                String difficultyLevel,
                int durationHours,
                String platform,
                String courseUrl,
                String careerGoals,
                String prerequisites,
                List<String> topics,
                List<String> skills
        ) {}

        List<DemoCourse> demoCourses = List.of(
                new DemoCourse(
                        "Full-Stack Web Development with React and Node.js",
                        "Master modern web development by building responsive front-end applications with React and scalable backend microservices with Node.js and Express.",
                        "Web Development",
                        "BEGINNER",
                        40,
                        "Coursera",
                        "https://www.coursera.org/learn/full-stack-web-development",
                        "Full Stack Developer, Web Developer, Frontend Engineer",
                        "Basic HTML and CSS knowledge",
                        List.of("React", "Node.js", "Express", "REST APIs", "JavaScript", "HTML", "CSS"),
                        List.of("React", "Node.js", "JavaScript", "REST APIs", "Express")
                ),
                new DemoCourse(
                        "Machine Learning Specialization: Theory to Practice",
                        "Comprehensive deep dive into supervised learning, neural networks, unsupervised learning, and recommender systems with Python and Scikit-Learn.",
                        "Artificial Intelligence",
                        "INTERMEDIATE",
                        60,
                        "Coursera",
                        "https://www.coursera.org/specializations/machine-learning-introduction",
                        "Machine Learning Engineer, Data Scientist, AI Researcher",
                        "Python programming basics, Linear Algebra, Calculus",
                        List.of("Machine Learning", "Supervised Learning", "Neural Networks", "Python", "Scikit-Learn"),
                        List.of("Python", "Machine Learning", "Scikit-Learn", "Deep Learning", "Data Science")
                ),
                new DemoCourse(
                        "Cloud Architecture and Solutions Engineering on AWS",
                        "Design resilient, highly available, and fault-tolerant cloud architectures using core AWS services, containerized workloads, and infrastructure as code.",
                        "Cloud Computing",
                        "INTERMEDIATE",
                        35,
                        "Udemy",
                        "https://www.udemy.com/course/aws-cloud-architect-solutions",
                        "Cloud Architect, DevOps Engineer, Cloud Solutions Engineer",
                        "Networking fundamentals, Linux command line",
                        List.of("AWS", "Cloud Computing", "Docker", "Serverless", "Microservices", "Terraform"),
                        List.of("AWS", "Cloud Computing", "Docker", "Terraform", "DevOps")
                ),
                new DemoCourse(
                        "Data Structures and Algorithms Masterclass in Java",
                        "Rigorous study of fundamental data structures, algorithmic complexity, graph theory, trees, dynamic programming, and technical interview preparation.",
                        "Computer Science",
                        "INTERMEDIATE",
                        50,
                        "edX",
                        "https://www.edx.org/course/data-structures-algorithms-java",
                        "Software Engineer, Backend Developer, Systems Programmer",
                        "Object-oriented programming in Java",
                        List.of("Data Structures", "Algorithms", "Java", "Dynamic Programming", "Graph Theory", "Sorting"),
                        List.of("Java", "Data Structures", "Algorithms", "Problem Solving")
                ),
                new DemoCourse(
                        "Defensive Cybersecurity and Network Protection",
                        "Learn core cybersecurity principles, threat modeling, network traffic analysis, vulnerability mitigation, and defensive security posture.",
                        "Cybersecurity",
                        "BEGINNER",
                        30,
                        "Coursera",
                        "https://www.coursera.org/learn/defensive-cybersecurity",
                        "Security Analyst, Cybersecurity Specialist, Information Security Officer",
                        "General computer networking knowledge",
                        List.of("Cybersecurity", "Network Security", "Cryptography", "Threat Modeling", "Firewalls"),
                        List.of("Cybersecurity", "Network Security", "Cryptography", "Vulnerability Assessment")
                ),
                new DemoCourse(
                        "Cross-Platform Mobile Development with Flutter and Dart",
                        "Build beautiful, natively compiled mobile applications for iOS and Android from a single codebase using Flutter and the Dart programming language.",
                        "Mobile Development",
                        "BEGINNER",
                        45,
                        "Udemy",
                        "https://www.udemy.com/course/flutter-dart-mobile-development",
                        "Mobile Developer, Flutter Engineer, App Developer",
                        "Basic programming concepts",
                        List.of("Flutter", "Dart", "Mobile Development", "State Management", "iOS", "Android"),
                        List.of("Flutter", "Dart", "Mobile Development", "Cross-Platform Development")
                ),
                new DemoCourse(
                        "Deep Learning, PyTorch, and Computer Vision",
                        "Explore advanced deep learning architectures, convolutional neural networks, transformer vision models, and real-time object detection systems.",
                        "Artificial Intelligence",
                        "ADVANCED",
                        70,
                        "Coursera",
                        "https://www.coursera.org/learn/deep-learning-computer-vision",
                        "Computer Vision Engineer, AI Specialist, Deep Learning Engineer",
                        "Python proficiency, Machine Learning basics, Matrix Calculus",
                        List.of("Deep Learning", "Computer Vision", "PyTorch", "Convolutional Neural Networks", "Object Detection"),
                        List.of("Deep Learning", "PyTorch", "Computer Vision", "Neural Networks", "Python")
                ),
                new DemoCourse(
                        "DevOps Practices: CI/CD, Kubernetes, and Observability",
                        "Automate modern application lifecycles using continuous integration pipelines, Kubernetes orchestration, Helm packaging, and Prometheus monitoring.",
                        "DevOps",
                        "INTERMEDIATE",
                        40,
                        "Pluralsight",
                        "https://www.pluralsight.com/courses/devops-kubernetes-cicd",
                        "DevOps Engineer, Site Reliability Engineer, Infrastructure Engineer",
                        "Linux fundamentals, Git version control",
                        List.of("DevOps", "CI/CD", "Kubernetes", "Docker", "Prometheus", "Helm"),
                        List.of("DevOps", "Kubernetes", "Docker", "CI/CD", "Linux")
                ),
                new DemoCourse(
                        "Relational Database Design and High-Performance SQL",
                        "Master relational modeling, indexing internals, transaction isolation levels, query execution planning, and SQL tuning for production MySQL and PostgreSQL.",
                        "Database Engineering",
                        "INTERMEDIATE",
                        30,
                        "edX",
                        "https://www.edx.org/course/database-design-sql-optimization",
                        "Database Administrator, Backend Developer, Data Architect",
                        "Basic SQL querying knowledge",
                        List.of("SQL", "Database Design", "Indexing", "MySQL", "PostgreSQL", "Query Optimization"),
                        List.of("SQL", "Database Design", "MySQL", "PostgreSQL", "Query Optimization")
                ),
                new DemoCourse(
                        "User Experience Design Systems and Figma Prototyping",
                        "Conduct user research, design reusable design systems, craft accessible UI components, and build interactive high-fidelity prototypes in Figma.",
                        "UI/UX Design",
                        "BEGINNER",
                        25,
                        "Udemy",
                        "https://www.udemy.com/course/ui-ux-design-figma-mastery",
                        "UI/UX Designer, Product Designer, Frontend Designer",
                        "None",
                        List.of("UI Design", "UX Research", "Figma", "Design Systems", "Prototyping", "Accessibility"),
                        List.of("Figma", "UI Design", "UX Design", "Prototyping", "Design Systems")
                ),
                new DemoCourse(
                        "Distributed Microservices Architecture with Go and gRPC",
                        "Architect high-throughput, low-latency microservices in Go utilizing gRPC protocol buffers, distributed consensus, concurrent worker pools, and Kafka.",
                        "Software Engineering",
                        "ADVANCED",
                        55,
                        "Coursera",
                        "https://www.coursera.org/learn/distributed-microservices-go",
                        "Backend Architect, Distributed Systems Engineer, Principal Engineer",
                        "Intermediate Go programming, Distributed systems concepts",
                        List.of("Go", "Distributed Systems", "Microservices", "gRPC", "Concurrency", "Kafka"),
                        List.of("Go", "Microservices", "Distributed Systems", "gRPC", "Concurrency")
                ),
                new DemoCourse(
                        "Big Data Processing with Apache Spark and Apache Kafka",
                        "Build real-time streaming and distributed batch processing pipelines for massive datasets with Spark Structured Streaming, Kafka clusters, and Delta Lake.",
                        "Data Science",
                        "ADVANCED",
                        65,
                        "edX",
                        "https://www.edx.org/course/big-data-spark-kafka",
                        "Data Engineer, Big Data Architect, Analytics Engineer",
                        "Python or Scala proficiency, SQL, Distributed computing basics",
                        List.of("Data Engineering", "Apache Spark", "Apache Kafka", "ETL", "Streaming Data"),
                        List.of("Data Engineering", "Apache Spark", "Apache Kafka", "Python", "SQL")
                ),
                new DemoCourse(
                        "Enterprise Frontend Engineering with Next.js and TypeScript",
                        "Build production-grade web applications leveraging Next.js App Router, React Server Components, TypeScript type safety, and modern performance optimization.",
                        "Web Development",
                        "INTERMEDIATE",
                        35,
                        "Udemy",
                        "https://www.udemy.com/course/enterprise-nextjs-typescript",
                        "Frontend Engineer, Full Stack Developer, React Engineer",
                        "Proficiency in JavaScript and modern React",
                        List.of("TypeScript", "Next.js", "React", "Server Components", "Tailwind CSS", "Web Performance"),
                        List.of("TypeScript", "Next.js", "React", "Tailwind CSS", "Frontend Development")
                ),
                new DemoCourse(
                        "Python for Data Analysis and Scientific Computing",
                        "Clean, manipulate, analyze, and visualize structured datasets using Python, Pandas, NumPy, and Seaborn for business insights and exploratory research.",
                        "Data Science",
                        "BEGINNER",
                        30,
                        "Coursera",
                        "https://www.coursera.org/learn/python-data-analysis",
                        "Data Analyst, Business Intelligence Analyst, Data Scientist",
                        "Basic computer literacy",
                        List.of("Python", "Pandas", "NumPy", "Data Analysis", "Matplotlib", "Jupyter"),
                        List.of("Python", "Pandas", "NumPy", "Data Analysis", "Data Visualization")
                )
        );

        int inserted = 0;
        for (DemoCourse course : demoCourses) {
            Integer existing = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM courses WHERE title = ? AND is_demo = true",
                    Integer.class,
                    course.title()
            );
            if (existing != null && existing > 0) {
                continue;
            }
            GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(
                        "INSERT INTO courses (title, description, category, difficulty_level, duration_hours, platform, course_url, career_goals, prerequisites, is_demo) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                        Statement.RETURN_GENERATED_KEYS
                );
                ps.setString(1, course.title());
                ps.setString(2, course.description());
                ps.setString(3, course.category());
                ps.setString(4, course.difficultyLevel());
                ps.setInt(5, course.durationHours());
                ps.setString(6, course.platform());
                ps.setString(7, course.courseUrl());
                ps.setString(8, course.careerGoals());
                ps.setString(9, course.prerequisites());
                ps.setBoolean(10, true);
                return ps;
            }, keyHolder);

            long courseId;
            if (keyHolder.getKey() != null) {
                courseId = keyHolder.getKey().longValue();
            } else if (keyHolder.getKeys() != null && !keyHolder.getKeys().isEmpty()) {
                courseId = ((Number) keyHolder.getKeys().values().iterator().next()).longValue();
            } else {
                Long fetchedId = jdbcTemplate.queryForObject("SELECT id FROM courses WHERE title = ? AND is_demo = true", Long.class, course.title());
                courseId = fetchedId != null ? fetchedId : 0L;
            }

            for (String topic : course.topics()) {
                jdbcTemplate.update(
                        "INSERT INTO course_topics (course_id, topic_name) VALUES (?, ?)",
                        courseId,
                        topic
                );
            }

            for (String skill : course.skills()) {
                jdbcTemplate.update(
                        "INSERT INTO course_skills (course_id, skill_name) VALUES (?, ?)",
                        courseId,
                        skill
                );
            }
            inserted++;
        }

        log.info("Seeded {} missing demo courses with topics and skills.", inserted);
    }
}
