pluginManagement {
    repositories {
        // 国内镜像优先，提高网络访问速度
        maven("https://maven.aliyun.com/repository/gradle-plugin") {
            name = "AliyunGradlePlugin"
        }
        maven("https://maven.aliyun.com/repository/google") {
            name = "AliyunGoogle"
        }
        maven("https://maven.aliyun.com/repository/central") {
            name = "AliyunCentral"
        }
        
        // 官方仓库作为备用
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
        id("com.android.application") version "8.6.1"
        kotlin("android") version "2.0.0"
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // 与pluginManagement保持一致，镜像优先
        maven("https://maven.aliyun.com/repository/google") {
            name = "AliyunGoogle"
        }
        maven("https://maven.aliyun.com/repository/central") {
            name = "AliyunCentral"
        }
        maven("https://maven.aliyun.com/repository/public") {
            name = "AliyunPublic"
        }
        
        // 官方仓库作为备用
        google()
        mavenCentral()
    }
}

rootProject.name = "sms-email-gateway"
include(":app")