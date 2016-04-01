package de.triplet.gradle.play

import com.android.build.gradle.api.ApplicationVariant
import org.gradle.api.DefaultTask
import org.gradle.api.Project

/**
 * A task that depends on several {@link PlayPublishTask}
 */
class BulkPublishTask extends DefaultTask {

    static BulkPublishTask createBulkPublishTask(
            Project project, ApplicationVariant variant, DefaultTask task) {

        BulkPublishTask bulkPublishTask = project.tasks.create("All'${task.getClass().getSimpleName()}'${variant.buildType.name.capitalize()}", BulkPublishTask);

        bulkPublishTask.group = PlayPublisherPlugin.PLAY_STORE_GROUP
        bulkPublishTask.description = "Runs '${task.getClass().getSimpleName()}' for all variants of build Type '${variant.buildType.name}'"
        //bulkPublishTask.outputs.upToDateWhen { false }

        bulkPublishTask.dependsOn(task)

        return bulkPublishTask;
    }


}
