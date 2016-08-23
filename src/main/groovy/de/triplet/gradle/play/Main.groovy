package de.triplet.gradle.play

import org.gradle.api.DefaultTask
import org.gradle.api.DomainObjectCollection
import org.gradle.api.Project
import org.gradle.api.internal.DefaultDomainObjectCollection
import org.gradle.testfixtures.ProjectBuilder

class Main {
    static void main(String... args) {

        System.out.println("HERE")
        Project project = ProjectBuilder.builder().build()
        BulkTask bulkTask
        DomainObjectCollection<DefaultTask> list

        list = new DefaultDomainObjectCollection<DefaultTask>(DefaultTask.class, new ArrayList<>())

        list.add(createTestTask(project, "a"))
        list.add(createTestTask(project, "b"))
        list.add(createTestTask(project, "c"))
        list.add(createTestTask(project, "d"))


        list.all { DefaultTask parentTask ->
            addToBulkTask(project, parentTask, bulkTask)
        }
    }

    /**
     * Adds a task as a dependency of a bulk task, creating the bulk task if neccessary
     * @param extension used to check if a build type for a bulk task was defined
     * @param project used to create the Bulk task
     * @param variant the variant for which a task is being added
     * @param parentTask the task being added as a dependency for the bulk task
     */
    static void addToBulkTask(Project project, DefaultTask parentTask, BulkTask bulkTask) {

        if(bulkTask == null) {
            bulkTask = BulkTask.createBulkTask(project, parentTask, "DummyTask")
        }

        bulkTask.dependsOn(parentTask)
    }

    static DefaultTask createTestTask(Project project, String name) {
        DefaultTask defaultTask = project.tasks.create(name, DefaultTask);

        defaultTask.group = PlayPublisherPlugin.PLAY_STORE_GROUP
        defaultTask.description = "Test for {$name}"

        return defaultTask
    }
}
