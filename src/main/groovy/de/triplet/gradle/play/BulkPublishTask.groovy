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

        BulkPublishTask bulkPublishTask = project.tasks.create("All${getTaskName(task)}s${variant.buildType.name.capitalize()}", BulkPublishTask);

        bulkPublishTask.group = PlayPublisherPlugin.PLAY_STORE_GROUP
        bulkPublishTask.description = "Runs ${getTaskName(task)} for all variants of build Type '${variant.buildType.name}'"
        //bulkPublishTask.outputs.upToDateWhen { false }

        bulkPublishTask.dependsOn(task)

        return bulkPublishTask;
    }


    static String getTaskName(DefaultTask task) {
        String simpleName = task.getClass().getSimpleName()
        String[] split = simpleName.split("_")

        return split.length > 1 ? split[0] : simpleName
    }
}
