pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "alarm-app"

include(
    ":app",
    ":core:common",
    ":core:notification",
    ":core:scheduler",
    ":core:sound",
    ":core:ui",
    ":data:alarm_db",
    ":data:alarm_repository",
    ":data:alarm_scheduler_impl",
    ":feature:alarm",
    ":feature:ring",
    ":feature:widget",
    ":feature:stats",
    ":feature:sleep",
    ":feature:timer",
    ":feature:stopwatch"
)
