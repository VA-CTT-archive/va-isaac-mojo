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
package gov.vha.isaac.mojo.hk2;

import gov.vha.isaac.ochre.api.LookupService;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

/**
 *
 * @author kec
 */
@Mojo( name = "shutdown-isaac",
        defaultPhase = LifecyclePhase.PROCESS_RESOURCES)

public class ShutdownIsaac extends AbstractMojo {

    @Override
    public void execute()
            throws MojoExecutionException {
        LookupService.shutdownIsaac();
    }
}
