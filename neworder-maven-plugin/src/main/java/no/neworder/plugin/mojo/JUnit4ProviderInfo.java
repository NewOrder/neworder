package no.neworder.plugin.mojo;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.plugin.surefire.ProviderInfo;
import org.apache.maven.plugin.surefire.SurefireDependencyResolver;
import org.apache.maven.surefire.booter.Classpath;

public class JUnit4ProviderInfo implements ProviderInfo {
    private Artifact junitArtifact;
    private Artifact junitDepArtifact;
    private SurefireDependencyResolver dependencyResolver;
    private Artifact surefireBooterArtifact;

    public JUnit4ProviderInfo(Artifact junitArtifact, Artifact junitDepArtifact,
                              SurefireDependencyResolver dependencyResolver,
                              Artifact surefireBooterArtifact) {
        this.junitArtifact = junitArtifact;
        this.junitDepArtifact = junitDepArtifact;
        this.dependencyResolver = dependencyResolver;
        this.surefireBooterArtifact = surefireBooterArtifact;
    }

    @Override
    public String getProviderName() {
        return "org.apache.maven.surefire.junit4.JUnit4Provider";
    }

    @Override
    public boolean isApplicable() {
        return false;
    }

    @Override
    public Classpath getProviderClasspath() throws ArtifactResolutionException,
            ArtifactNotFoundException {
        return dependencyResolver.getProviderClasspath("surefire-junit4",
                                                       surefireBooterArtifact.getBaseVersion(),
                                                       null);
    }

    @Override
    public void addProviderProperties() {
    }
}
