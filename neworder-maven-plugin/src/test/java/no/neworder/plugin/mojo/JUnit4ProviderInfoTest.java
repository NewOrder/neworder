package no.neworder.plugin.mojo;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.plugin.surefire.SurefireDependencyResolver;
import org.apache.maven.surefire.booter.Classpath;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

public class JUnit4ProviderInfoTest {

    private JUnit4ProviderInfo providerInfo;

    @Mock
    private SurefireDependencyResolver dependencyResolverMock;
    @Mock
    private Artifact junitDepArtifactMock;
    @Mock
    private Artifact junitArtifactMock;
    @Mock
    private Artifact surefireBooterArtifact;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        providerInfo = new JUnit4ProviderInfo(junitArtifactMock, junitDepArtifactMock,
                                              dependencyResolverMock, surefireBooterArtifact);
    }

    @Test
    public void should_provide_classpath_for_junit4() throws ArtifactResolutionException,
            ArtifactNotFoundException {
        when(dependencyResolverMock.getProviderClasspath(anyString(), anyString(), any(Artifact.class)))
                .thenReturn(new Classpath());
        
        Classpath classpath = providerInfo.getProviderClasspath();

        assertThat(classpath, is(notNullValue()));
    }
}
