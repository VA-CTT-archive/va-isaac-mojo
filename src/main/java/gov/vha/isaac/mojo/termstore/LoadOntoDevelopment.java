/*
 * Copyright 2015 kec.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package gov.vha.isaac.mojo.termstore;

import gov.vha.isaac.ochre.api.ConceptModel;
import gov.vha.isaac.ochre.api.LookupService;
import gov.vha.isaac.ochre.api.ObjectChronicleTaskService;
import gov.vha.isaac.ochre.api.StandardPaths;
import gov.vha.isaac.ochre.api.Util;
import java.util.concurrent.ExecutionException;
import javafx.concurrent.Task;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Goal which loads a database from eConcept files onto the GitFlow Development
 * path.
 */
@Mojo(name = "load-onto-development",
        defaultPhase = LifecyclePhase.PROCESS_RESOURCES)

public class LoadOntoDevelopment extends AbstractMojo {

    /**
     * {@code eConcept format} files to import.
     *
     */
    @Parameter(required = true)
    private String[] econFileStrings;
    
    /**
     * {@code ConceptModel} for the resulting database. {@code ConceptModel.OTF_CONCEPT_MODEL} 
     * by default. 
     */
    @Parameter(defaultValue = "OCHRE_CONCEPT_MODEL", name = "conceptModelString")
    private String conceptModelString;

    @Override
    public void execute() throws MojoExecutionException {
        try {
            ConceptModel conceptModel = ConceptModel.valueOf(conceptModelString);
            getLog().info("concept model: " + conceptModel);
            ObjectChronicleTaskService store = LookupService.getService(ObjectChronicleTaskService.class);
            Task<Integer> loadTask = store.startLoadTask(conceptModel, StandardPaths.DEVELOPMENT,
                    Util.stringArrayToPathArray(econFileStrings));
            Util.addToTaskSetAndWaitTillDone(loadTask);
        } catch (InterruptedException | ExecutionException ex) {
            throw new MojoExecutionException(ex.getLocalizedMessage(), ex);
        }
    }
}
