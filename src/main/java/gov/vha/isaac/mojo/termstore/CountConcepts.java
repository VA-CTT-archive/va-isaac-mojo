package gov.vha.isaac.mojo.termstore;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.ihtsdo.otf.tcc.api.store.TerminologyStoreDI;
import org.ihtsdo.otf.tcc.lookup.Hk2Looker;

import java.io.IOException;

/**
 * Created by kec on 9/6/14.
 */
@Mojo( name = "count-concepts",
        defaultPhase = LifecyclePhase.PROCESS_RESOURCES)
public class CountConcepts extends AbstractMojo {

    public void execute()
            throws MojoExecutionException {
        TerminologyStoreDI store = Hk2Looker.getService(TerminologyStoreDI.class);
        try {
            getLog().info("Concept count: " + store.getConceptCount());
        } catch (IOException e) {
            throw new MojoExecutionException(e.getLocalizedMessage(), e);
        }
    }
}
