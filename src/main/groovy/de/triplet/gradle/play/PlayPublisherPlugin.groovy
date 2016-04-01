package de.triplet.gradle.play

import com.android.build.gradle.AppPlugin
import com.android.build.gradle.api.ApplicationVariant
import org.apache.commons.lang.StringUtils
import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project

class PlayPublisherPlugin implements Plugin<Project> {

    public static final String PLAY_STORE_GROUP = "Play Store"

    @Override
    void apply(Project project) {
        def log = project.logger

        def hasAppPlugin = project.plugins.find { p -> p instanceof AppPlugin }
        if (!hasAppPlugin) {
            throw new IllegalStateException("The 'com.android.application' plugin is required.")
        }

        HashMap<String, BulkPublishTask> bulkBootstrapTask = new HashMap<>();
        HashMap<String, BulkPublishTask> bulkPlayResourcesTask = new HashMap<>();
        HashMap<String, BulkPublishTask> bulkPlayPublishListingTask = new HashMap<>();
        HashMap<String, BulkPublishTask> bulkPlayPublishApkTask = new HashMap<>();

        PlayPublisherPluginExtension extension = project.extensions.create('play', PlayPublisherPluginExtension)

        project.android.applicationVariants.all { ApplicationVariant variant ->
            if (variant.buildType.isDebuggable()) {
                log.debug("Skipping debuggable build type ${variant.buildType.name}.")
                return
            }

            def buildTypeName = variant.buildType.name.capitalize()

            def productFlavorNames = variant.productFlavors.collect { it.name.capitalize() }

            if (productFlavorNames.isEmpty()) {
                productFlavorNames = [""]
            }

            def productFlavorName = productFlavorNames.join('')

            def flavor = StringUtils.uncapitalize(productFlavorName)

            def variationName = "${productFlavorName}${buildTypeName}"

            def bootstrapTaskName = "bootstrap${variationName}PlayResources"
            def playResourcesTaskName = "generate${variationName}PlayResources"
            def publishApkTaskName = "publishApk${variationName}"
            def publishListingTaskName = "publishListing${variationName}"
            def publishTaskName = "publish${variationName}"

            def outputData = variant.outputs.first()
            def zipAlignTask = outputData.zipAlign
            def assembleTask = outputData.assemble

            def variantData = variant.variantData

            // Create and configure bootstrap task for this variant.
            BootstrapTask bootstrapTask = project.tasks.create(bootstrapTaskName, BootstrapTask)
            bootstrapTask.extension = extension
            bootstrapTask.variant = variant

            if (StringUtils.isNotEmpty(flavor)) {
                bootstrapTask.outputFolder = new File(project.projectDir, "src/${flavor}/play")
            } else {
                bootstrapTask.outputFolder = new File(project.projectDir, "src/main/play")
            }

            bootstrapTask.description = "Downloads the play store listing for the ${variationName} build. No download of image resources. See #18."
            bootstrapTask.group = PLAY_STORE_GROUP

            addToBulkTask(extension, project, variant, bootstrapTask, bulkBootstrapTask)

            // Create and configure task to collect the play store resources.
            GeneratePlayResourcesTask playResourcesTask = project.tasks.create(playResourcesTaskName, GeneratePlayResourcesTask)

            playResourcesTask.inputs.file(new File(project.projectDir, "src/main/play"))

            if (StringUtils.isNotEmpty(flavor)) {
                playResourcesTask.inputs.file(new File(project.projectDir, "src/${flavor}/play"))
            }

            playResourcesTask.inputs.file(new File(project.projectDir, "src/${variant.buildType.name}/play"))

            if (StringUtils.isNotEmpty(flavor)) {
                playResourcesTask.inputs.file(new File(project.projectDir, "src/${variant.name}/play"))
            }

            playResourcesTask.outputFolder = new File(project.projectDir, "build/outputs/play/${variant.name}")
            playResourcesTask.description = "Collects play store resources for the ${variationName} build"
            playResourcesTask.group = PLAY_STORE_GROUP

            addToBulkTask(extension, project, variant, playResourcesTask, bulkPlayResourcesTask)


            // Create and configure publisher meta task for this variant
            PlayPublishListingTask publishListingTask = project.tasks.create(publishListingTaskName, PlayPublishListingTask)

            publishListingTask.extension = extension
            publishListingTask.variant = variant
            publishListingTask.inputFolder = playResourcesTask.outputFolder
            publishListingTask.description = "Updates the play store listing for the ${variationName} build"
            publishListingTask.group = PLAY_STORE_GROUP

            // Attach tasks to task graph.
            publishListingTask.dependsOn(playResourcesTask)

            addToBulkTask(extension, project, variant, publishListingTask, bulkPlayPublishListingTask)

            if (zipAlignTask && variantData.zipAlignEnabled) {

                // Create and configure publisher apk task for this variant.
                PlayPublishApkTask publishApkTask = project.tasks.create(publishApkTaskName, PlayPublishApkTask)

                publishApkTask.extension = extension
                publishApkTask.variant = variant
                publishApkTask.inputFolder = playResourcesTask.outputFolder
                publishApkTask.description = "Uploads the APK for the ${variationName} build"
                publishApkTask.group = PLAY_STORE_GROUP

                def publishTask = project.tasks.create(publishTaskName)
                publishTask.description = "Updates APK and play store listing for the ${variationName} build"
                publishTask.group = PLAY_STORE_GROUP

                // Attach tasks to task graph.
                publishTask.dependsOn publishApkTask
                publishTask.dependsOn publishListingTask
                publishApkTask.dependsOn playResourcesTask
                publishApkTask.dependsOn assembleTask

                addToBulkTask(extension, project, variant, publishApkTask, bulkPlayPublishApkTask)

            } else {
                log.warn("Could not find ZipAlign task. Did you specify a signingConfig for the variation ${variationName}?")
            }
        }
    }

    static void addToBulkTask(PlayPublisherPluginExtension extension, Project project, ApplicationVariant variant, DefaultTask task, HashMap<String, BulkPublishTask> bulkTasks) {

        if (extension.buildTypeName == null || !variant.buildType.name.equals(extension.buildTypeName)) return;

        BulkPublishTask bulkPublishTask = bulkTasks.get(variant.buildType.name);

        if (bulkPublishTask == null) {
            bulkPublishTask = BulkPublishTask.createBulkPublishTask(project, variant, task)
            bulkTasks.put(variant.buildType.name, bulkPublishTask)
        } else {
            bulkPublishTask.dependsOn(task)
        }
    }

}
