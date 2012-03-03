package no.neworder.plugin.mojo;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class NewOrderMojoTest {

    private NewOrderMojo newOrderMojo;

    private Map<String, Artifact> projectArtifactMap = new HashMap<String, Artifact>();

    @Before
    public void setUp() {
        newOrderMojo = new NewOrderMojo();

        newOrderMojo.setProjectArtifactMap(projectArtifactMap);
    }
    
    @Test
    public void should_execute_tests() throws MojoExecutionException, MojoFailureException {
        newOrderMojo.execute();
    }
}
