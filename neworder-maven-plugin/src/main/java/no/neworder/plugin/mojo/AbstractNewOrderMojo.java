package no.neworder.plugin.mojo;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.surefire.*;
import org.apache.maven.plugin.surefire.booterclient.ChecksumCalculator;
import org.apache.maven.surefire.booter.Classpath;

import java.util.Arrays;

public abstract class AbstractNewOrderMojo extends AbstractMojo
        implements SurefireExecutionParameters {

    private SurefireDependencyResolver dependencyResolver;
    private Artifact surefireBooterArtifact;

    protected abstract String getPluginName();

    protected abstract boolean isSkipExecution();

    protected abstract void addPluginSpecificChecksumItems(ChecksumCalculator checksum);

    protected abstract String[] getDefaultIncludes();

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        Artifact junitDepArtifact = getJunitDepArtifact();

        dependencyResolver =
                new NewOrderDependencyResolver(getArtifactResolver(), getArtifactFactory(), getLog(),
                                               getLocalRepository(), getRemoteRepositories(), getMetadataSource(),
                                               getPluginName());
    }

    private Artifact getJunitArtifact()
    {
        return getProjectArtifactMap().get(getJunitArtifactName());
    }

    private Artifact getJunitDepArtifact()
    {
        return getProjectArtifactMap().get("junit:junit-dep");
    }

}
