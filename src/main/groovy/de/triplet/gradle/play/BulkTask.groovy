package de.triplet.gradle.play

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.Task

/**
 * A task that depends on several {@link DefaultTask} mostly of type {@link PlayPublishTask}
 */
class BulkTask extends DefaultTask {

    static BulkTask createBulkTask(Project project, Task task, String buildType) {

        BulkTask bulkTask = project.tasks.create(getBulkTaskName(task, buildType), BulkTask);

        bulkTask.group = PlayPublisherPlugin.PLAY_STORE_GROUP
        bulkTask.description = "Runs ${getTaskName(task)} for all variants of build type '${buildType}'"

        return bulkTask;
    }

/**
 * Removes the appended _Decorated from {@link Class#getSimpleName} for the {@link Task}
 * @param task the task to get a name for
 * @return A name more easily read
 */
    static String getTaskName(Task task) {
        String simpleName = task.getClass().getSimpleName()
        String[] split = simpleName.split("_")

        return split.length > 1 ? split[0] : simpleName
    }

    static String getBulkTaskName(Task task, String buildType) {
        return "All${getTaskName(task)}s${buildType.capitalize()}"
    }

}
