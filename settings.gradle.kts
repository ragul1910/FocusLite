pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
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

rootProject.name = "FocusLite"

include(":app")
include(":core:model")
include(":core:common")
include(":core:database")
include(":core:datastore")
include(":core:service")
include(":feature:onboarding")
include(":feature:dashboard")
include(":feature:applimits")
include(":feature:reelsblocker")
include(":feature:downtime")
include(":feature:usageanalytics")
