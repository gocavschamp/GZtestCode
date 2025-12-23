pluginManagement {
    repositories {
        maven { url = uri("https://www.jitpack.io") }
        maven { url = uri("https://jitpack.io") }

        // 添加阿里云镜像
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/central") }
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }

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
        maven { url = uri("https://www.jitpack.io") }
        maven { url = uri("https://jitpack.io") }

        // 添加阿里云镜像
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/central") }
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }

        google()
        mavenCentral()

    }
}

rootProject.name = "FirstApplication"
include(":app")
include(":baselibray")
include(":homelogic")
include(":baseapi")
include(":basetools")
include(":easyPhotos")
include(":animplayer")
