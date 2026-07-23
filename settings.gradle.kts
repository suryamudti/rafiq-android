pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("androidx.*")
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google {
            content {
                includeGroupByRegex("androidx.*")
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
            }
        }
        mavenCentral()
    }
}

rootProject.name = "Rafiq App"
include(":app")
include(":core")
include(":domain")
include(":data")

// Feature modules
include(":feature:dashboard")
include(":feature:quran")
include(":feature:prayertimes")
include(":feature:qibla")
include(":feature:mosques")
include(":feature:prophets")
include(":feature:recitation")
include(":feature:zakat")
include(":feature:tasbih")
include(":feature:settings")
include(":feature:asmaulhusna")
include(":feature:calendar")
include(":feature:bookmarks")
include(":feature:prayerlog")
