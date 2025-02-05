/* Copyright 2019 EPAM Systems.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at
http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.

See the License for the specific language governing permissions and
limitations under the License.*/

package com.epam.edp.stages.impl.cd.impl

import com.epam.edp.stages.impl.cd.Stage
import com.epam.edp.stages.impl.ci.impl.codebaseiamgestream.CodebaseImageStreams
import org.apache.commons.lang.RandomStringUtils

@Stage(name = "promote-images")
class PromoteImages {
    Script script

    void run(context) {
        script.openshift.withCluster() {
            script.openshift.withProject() {
                context.job.codebasesList.each() { codebase ->
                    script.println("[JENKINS][DEBUG] 1")
                    if ((codebase.name in context.job.applicationsToPromote) && (codebase.version != "No deploy") && (codebase.version != "noImageExists")) {
                        script.println("[JENKINS][DEBUG] 2")
                        script.println("[JENKINS][DEBUG] ${codebase.inputIs}:${codebase.version}")
                        script.println("[JENKINS][DEBUG] ${codebase.outputIs}")
                        script.openshift.tag("${codebase.inputIs}:${codebase.version}",
                                "${codebase.outputIs}:${codebase.version}")
                        script.println("[JENKINS][DEBUG] 3")
                        context.workDir = new File("/tmp/${RandomStringUtils.random(10, true, true)}")
                        script.println("[JENKINS][DEBUG] 4")
                        context.workDir.deleteDir()

                        script.println("[JENKINS][DEBUG] 5")
                        def dockerRegistryHost = context.platform.getJsonPathValue("edpcomponent", "docker-registry", ".spec.url")
                        script.println("[JENKINS][DEBUG]!!! ${dockerRegistryHost}/${codebase.outputIs}")
                        if (!dockerRegistryHost) {
                            script.error("[JENKINS][ERROR] Couldn't get docker registry server")
                        }

                        new CodebaseImageStreams(context, script)
                                .UpdateOrCreateCodebaseImageStream(codebase.outputIs, "${dockerRegistryHost}/${codebase.outputIs}", codebase.version)

                        script.println("[JENKINS][INFO] Image ${codebase.inputIs}:${codebase.version} has been promoted to ${codebase.outputIs}")
                    }
                }
            }
        }
    }
}

