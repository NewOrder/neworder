package no.neworder.plugin.mojo;

import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.surefire.SurefireDependencyResolver;

import java.util.List;

public class NewOrderDependencyResolver extends SurefireDependencyResolver {
    public NewOrderDependencyResolver(ArtifactResolver artifactResolver, ArtifactFactory artifactFactory, Log log,
                                      ArtifactRepository localRepository, List<ArtifactRepository> remoteRepositories,
                                      ArtifactMetadataSource metadataSource, String pluginName) {
        super(artifactResolver, artifactFactory, log, localRepository, remoteRepositories, metadataSource,
              pluginName);
    }
}
