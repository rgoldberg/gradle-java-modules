import org.gradle.api.internal.FeaturePreviews.Feature.GROOVY_COMPILATION_AVOIDANCE

rootProject.name = "experimental-jigsaw"

enableFeaturePreview(GROOVY_COMPILATION_AVOIDANCE.name)
