package com.marklogic.hub.gradle.task

import com.marklogic.gradle.task.HubTask
import com.marklogic.hub.impl.HubConfigImpl
import org.gradle.api.tasks.TaskAction

class PrintPropertyDocsTask extends HubTask {

    @TaskAction
    void printPropertyDocs() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream()
        HubConfigImpl config = getHubConfig()
        config.printPropertyDocumentation(baos)
        println new String(baos.toByteArray())
    }
}
