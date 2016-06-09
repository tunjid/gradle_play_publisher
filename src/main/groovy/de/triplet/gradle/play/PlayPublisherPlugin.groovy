package de.triplet.gradle.play

import com.android.build.gradle.AppPlugin
import com.android.build.gradle.api.ApplicationVariant
import org.apache.commons.lang.StringUtils
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.UnknownTaskException

class PlayPublisherPlugin implements Plugin<Project> {

    public static final String PLAY_STORE_GROUP = "Play Store"

    @Override
    void apply(Project project) {
        def log = project.logger

        def hasAppPlugin = project.plugins.find { p -> p instanceof AppPlugin }
        if (!hasAppPlugin) {
            throw new IllegalStateException("The 'com.android.application' plugin is required.")
        }

        PlayPublisherPluginExtension extension = project.extensions.create('play', PlayPublisherPluginExtension)

        project.android.applicationVariants.all { ApplicationVariant variant ->

            if (variant.buildType.isDebuggable()) {
                log.debug("Skipping debuggable build type ${variant.buildType.name}.")
                return
            }

            def productFlavorName = getProductFlavorName(variant)

            def flavor = StringUtils.uncapitalize(productFlavorName)

            def variationName = getVariantName(variant)

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

            addToBulkTask(extension, project, variant, bootstrapTask)

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

            addToBulkTask(extension, project, variant, playResourcesTask)

            // Create and configure publisher meta task for this variant
            PlayPublishListingTask publishListingTask = project.tasks.create(publishListingTaskName, PlayPublishListingTask)

            publishListingTask.extension = extension
            publishListingTask.variant = variant
            publishListingTask.inputFolder = playResourcesTask.outputFolder
            publishListingTask.description = "Updates the play store listing for the ${variationName} build"
            publishListingTask.group = PLAY_STORE_GROUP

            // Attach tasks to task graph.
            publishListingTask.dependsOn(playResourcesTask)

            addToBulkTask(extension, project, variant, publishListingTask)

            if (zipAlignTask && variantData.zipAlignEnabled) {

                // Create and configure publisher apk task for this variant.
                PlayPublishApkTask publishApkTask = project.tasks.create(publishApkTaskName, PlayPublishApkTask)

                publishApkTask.extension = extension
                publishApkTask.variant = variant
                publishApkTask.inputFolder = playResourcesTask.outputFolder
                publishApkTask.description = "Uploads the APK for the ${variationName} build"
                publishApkTask.group = PLAY_STORE_GROUP

                PublishTask publishTask = project.tasks.create(publishTaskName, PublishTask)
                publishTask.description = "Updates APK and play store listing for the ${variationName} build"
                publishTask.group = PLAY_STORE_GROUP

                // Attach tasks to task graph.
                publishTask.dependsOn publishApkTask
                publishTask.dependsOn publishListingTask
                publishApkTask.dependsOn playResourcesTask
                publishApkTask.dependsOn assembleTask

                addToBulkTask(extension, project, variant, publishApkTask)
                addToBulkTask(extension, project, variant, publishTask)

            } else {
                log.warn("Could not find ZipAlign task. Did you specify a signingConfig for the variation ${variationName}?")
            }
        }
    }

    /**
     * Adds a task as a dependency of a bulk task, creating the bulk task if neccessary
     * @param extension used to check if a build type for a bulk task was defined
     * @param project used to create the Bulk task
     * @param variant the variant for which a task is being added
     * @param parentTask the task being added as a dependency for the bulk task
     */
    static void addToBulkTask(PlayPublisherPluginExtension extension, Project project,
                              ApplicationVariant variant, Task parentTask) {

        if (extension.buildType == null || !extension.buildType.equals(variant.buildType.name)) {
            return
        }

        BulkTask bulkTask

        String bulkTaskName = BulkTask.getBulkTaskName(parentTask, extension.buildType)

        try {
            bulkTask = (BulkTask) project.getTasks().getByName(bulkTaskName)
        }
        catch (UnknownTaskException e) { // BulkPublish task hasn't been created. Create it.
            project.logger.debug("{$bulkTaskName} did not exist, creating it")
            bulkTask = BulkTask.createBulkTask(project, parentTask, extension.buildType)
        }

        bulkTask.dependsOn(parentTask)
    }

    public static String getProductFlavorName(ApplicationVariant variant) {

        def productFlavorNames = variant.productFlavors.collect { it.name.capitalize() }

        if (productFlavorNames.isEmpty()) {
            productFlavorNames = [""]
        }

        return productFlavorNames.join('')
    }

    public static String getVariantName(ApplicationVariant variant) {
        def buildTypeName = variant.buildType.name.capitalize()

        return "${getProductFlavorName(variant)}${buildTypeName}"
    }
}
