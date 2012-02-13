/**
 This file is part of NewOrder.

 NewOrder is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 NewOrder is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Foobar.  If not, see <http://www.gnu.org/licenses/>.
 */

package no.neworder.plugin.mojo;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.surefire.AbstractSurefireMojo;
import org.apache.maven.plugin.surefire.Summary;
import org.apache.maven.plugin.surefire.SurefireHelper;
import org.apache.maven.plugin.surefire.SurefireReportParameters;
import org.apache.maven.plugin.surefire.booterclient.ChecksumCalculator;
import org.apache.maven.project.MavenProject;
import org.apache.maven.surefire.suite.RunResult;
import org.apache.maven.toolchain.ToolchainManager;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * @requiresDependencyResolution test
 * @goal neworder-test
 * @phase test
 * @threadSafe
 */
public class NewOrderMojo extends AbstractSurefireMojo implements SurefireReportParameters {

    /**
     * Set this to "true" to skip running tests, but still compile them. Its use is NOT RECOMMENDED, but quite
     * convenient on occasion.
     *
     * @parameter default-value="false" expression="${skipTests}"
     * @since 2.4
     */
    private boolean skipTests;

    /**
     * This old parameter is just like <code>skipTests</code>, but bound to the old property "maven.test.skip.exec".
     *
     * @parameter expression="${maven.test.skip.exec}"
     * @since 2.3
     * @deprecated Use skipTests instead.
     */
    private boolean skipExec;

    /**
     * Set this to "true" to bypass unit tests entirely. Its use is NOT RECOMMENDED, especially if you enable it using
     * the "maven.test.skip" property, because maven.test.skip disables both running the tests and compiling the tests.
     * Consider using the <code>skipTests</code> parameter instead.
     *
     * @parameter default-value="false" expression="${maven.test.skip}"
     */
    private boolean skip;

    /**
     * Set this to "true" to ignore a failure during testing. Its use is NOT RECOMMENDED, but quite convenient on
     * occasion.
     *
     * @parameter default-value="false" expression="${maven.test.failure.ignore}"
     */
    private boolean testFailureIgnore;

    /**
     * The base directory of the project being tested. This can be obtained in your unit test via
     * System.getProperty("basedir").
     *
     * @parameter default-value="${basedir}"
     */
    private File basedir;

    /**
     * The directory containing generated test classes of the project being tested. This will be included at the
     * beginning of the test classpath. *
     *
     * @parameter default-value="${project.build.testOutputDirectory}"
     */
    private File testClassesDirectory;

    /**
     * The directory containing generated classes of the project being tested. This will be included after the test
     * classes in the test classpath.
     *
     * @parameter default-value="${project.build.outputDirectory}"
     */
    private File classesDirectory;

    /**
     * The Maven Project Object.
     *
     * @parameter default-value="${project}"
     * @readonly
     */
    private MavenProject project;

    /**
     * List of dependencies to exclude from the test classpath. Each dependency string must follow the format
     * <i>groupId:artifactId</i>. For example: <i>org.acme:project-a</i>
     *
     * @parameter
     * @since 2.6
     */
    private List<String> classpathDependencyExcludes;

    /**
     * A dependency scope to exclude from the test classpath. The scope can be one of the following scopes:
     * <p/>
     * <ul>
     * <li><i>compile</i> - system, provided, compile
     * <li><i>runtime</i> - compile, runtime
     * <li><i>test</i> - system, provided, compile, runtime, test
     * </ul>
     *
     * @parameter default-value=""
     * @since 2.6
     */
    private String classpathDependencyScopeExclude;

    /**
     * Additional elements to be appended to the classpath.
     *
     * @parameter
     * @since 2.4
     */
    private List<String> additionalClasspathElements;

    /**
     * Base directory where all reports are written to.
     *
     * @parameter default-value="${project.build.directory}/surefire-reports"
     */
    private File reportsDirectory;

    /**
     * The test source directory containing test class sources.
     *
     * @parameter default-value="${project.build.testSourceDirectory}"
     * @required
     * @since 2.2
     */
    private File testSourceDirectory;

    /**
     * Specify this parameter to run individual tests by file name, overriding the <code>includes/excludes</code>
     * parameters. Each pattern you specify here will be used to create an include pattern formatted like
     * <code>**&#47;${test}.java</code>, so you can just type "-Dtest=MyTest" to run a single test called
     * "foo/MyTest.java".<br/>
     * This parameter overrides the <code>includes/excludes</code> parameters, and the TestNG <code>suiteXmlFiles</code>
     * parameter.
     * <p/>
     * Since 2.7.3, you can execute a limited number of methods in the test by adding #myMethod or #my*ethod. For example,
     * "-Dtest=MyTest#myMethod".  This is supported for junit 4.x and testNg.
     *
     * @parameter expression="${test}"
     */
    private String test;

    /**
     * A list of &lt;include> elements specifying the tests (by pattern) that should be included in testing. When not
     * specified and when the <code>test</code> parameter is not specified, the default includes will be <code><br/>
     * &lt;includes><br/>
     * &nbsp;&lt;include>**&#47;Test*.java&lt;/include><br/>
     * &nbsp;&lt;include>**&#47;*Test.java&lt;/include><br/>
     * &nbsp;&lt;include>**&#47;*TestCase.java&lt;/include><br/>
     * &lt;/includes><br/>
     * </code> This parameter is ignored if the TestNG <code>suiteXmlFiles</code> parameter is specified.
     *
     * @parameter
     */
    private List<String> includes;

    /**
     * A list of &lt;exclude> elements specifying the tests (by pattern) that should be excluded in testing. When not
     * specified and when the <code>test</code> parameter is not specified, the default excludes will be <code><br/>
     * &lt;excludes><br/>
     * &nbsp;&lt;exclude>**&#47;*$*&lt;/exclude><br/>
     * &lt;/excludes><br/>
     * </code> (which excludes all inner classes).<br>
     * This parameter is ignored if the TestNG <code>suiteXmlFiles</code> parameter is specified.
     *
     * @parameter
     */
    private List<String> excludes;

    /**
     * ArtifactRepository of the localRepository. To obtain the directory of localRepository in unit tests use
     * System.getProperty("localRepository").
     *
     * @parameter expression="${localRepository}"
     * @required
     * @readonly
     */
    private ArtifactRepository localRepository;

    /**
     * List of System properties to pass to the JUnit tests.
     *
     * @parameter
     * @deprecated Use systemPropertyVariables instead.
     */
    private Properties systemProperties;

    /**
     * List of System properties to pass to the JUnit tests.
     *
     * @parameter
     * @since 2.5
     */
    private Map<String, String> systemPropertyVariables;

    /**
     * List of System properties, loaded from a file, to pass to the JUnit tests.
     *
     * @parameter
     * @since 2.8.2
     */
    private File systemPropertiesFile;

    /**
     * List of properties for configuring all TestNG related configurations. This is the new preferred method of
     * configuring TestNG.
     *
     * @parameter
     * @since 2.4
     */
    private Properties properties;

    /**
     * Map of plugin artifacts.
     *
     * @parameter expression="${plugin.artifactMap}"
     * @required
     * @readonly
     */
    private Map<String, Artifact> pluginArtifactMap;

    /**
     * Map of project artifacts.
     *
     * @parameter expression="${project.artifactMap}"
     * @required
     * @readonly
     */
    private Map<String, Artifact> projectArtifactMap;

    /**
     * Option to print summary of test suites or just print the test cases that have errors.
     *
     * @parameter expression="${surefire.printSummary}" default-value="true"
     */
    private boolean printSummary;

    /**
     * Selects the formatting for the test report to be generated. Can be set as "brief" or "plain".
     *
     * @parameter expression="${surefire.reportFormat}" default-value="brief"
     */
    private String reportFormat;

    /**
     * Add custom text into report filename: TEST-testClassName-reportNameSuffix.xml,
     * testClassName-reportNameSuffix.txt and testClassName-reportNameSuffix-output.txt.
     * File TEST-testClassName-reportNameSuffix.xml has changed attributes 'testsuite'--'name'
     * and 'testcase'--'classname' - reportNameSuffix is added to the attribute value.
     *
     * @parameter expression="${surefire.reportNameSuffix}" default-value=""
     */
    private String reportNameSuffix;

    /**
     * Option to generate a file test report or just output the test report to the console.
     *
     * @parameter expression="${surefire.useFile}" default-value="true"
     */
    private boolean useFile;

    /**
     * Set this to "true" to redirect the unit test standard output to a file (found in
     * reportsDirectory/testName-output.txt).
     *
     * @parameter expression="${maven.test.redirectTestOutputToFile}" default-value="false"
     * @since 2.3
     */
    private boolean redirectTestOutputToFile;

    /**
     * Set this to "true" to cause a failure if there are no tests to run. Defaults to "false".
     *
     * @parameter expression="${failIfNoTests}"
     * @since 2.4
     */
    private Boolean failIfNoTests;

    /**
     * Option to specify the forking mode. Can be "never", "once", "always" or "perthread". "none" and "pertest" are also accepted
     * for backwards compatibility. "always" forks for each test-class. "perthread" will create "threadCount" parallel forks.
     *
     * @parameter expression="${forkMode}" default-value="once"
     * @since 2.1
     */
    private String forkMode;

    /**
     * Option to specify the jvm (or path to the java executable) to use with the forking options. For the default, the
     * jvm will be a new instance of the same VM as the one used to run Maven. JVM settings are not inherited from
     * MAVEN_OPTS.
     *
     * @parameter expression="${jvm}"
     * @since 2.1
     */
    private String jvm;

    /**
     * Arbitrary JVM options to set on the command line.
     *
     * @parameter expression="${argLine}"
     * @since 2.1
     */
    private String argLine;

    /**
     * Attach a debugger to the forked JVM. If set to "true", the process will suspend and wait for a debugger to attach
     * on port 5005. If set to some other string, that string will be appended to the argLine, allowing you to configure
     * arbitrary debuggability options (without overwriting the other options specified through the <code>argLine</code>
     * parameter).
     *
     * @parameter expression="${maven.surefire.debug}"
     * @since 2.4
     */
    private String debugForkedProcess;

    /**
     * Kill the forked test process after a certain number of seconds. If set to 0, wait forever for the process, never
     * timing out.
     *
     * @parameter expression="${surefire.timeout}"
     * @since 2.4
     */
    private int forkedProcessTimeoutInSeconds;

    /**
     * Additional environment variables to set on the command line.
     *
     * @parameter
     * @since 2.1.3
     */
    private Map<String, String> environmentVariables = new HashMap<String, String>();

    /**
     * Command line working directory.
     *
     * @parameter expression="${basedir}"
     * @since 2.1.3
     */
    private File workingDirectory;

    /**
     * When false it makes tests run using the standard classloader delegation instead of the default Maven isolated
     * classloader. Only used when forking (forkMode is not "none").<br/>
     * Setting it to false helps with some problems caused by conflicts between xml parsers in the classpath and the
     * Java 5 provider parser.
     *
     * @parameter expression="${childDelegation}" default-value="false"
     * @since 2.1
     */
    private boolean childDelegation;

    /**
     * (TestNG/JUnit47 provider with JUnit4.8+ only) Groups for this test. Only classes/methods/etc decorated with one of the groups specified here will
     * be included in test run, if specified. <br/>For JUnit, this parameter forces the use of the 4.7 provider<br/>
     * This parameter is ignored if the <code>suiteXmlFiles</code> parameter is specified.
     * .
     *
     * @parameter expression="${groups}"
     * @since 2.2
     */
    private String groups;

    /**
     * (TestNG/JUnit47 provider with JUnit4.8+ only) Excluded groups. Any methods/classes/etc with one of the groups specified in this list will
     * specifically not be run.<br/>For JUnit, this parameter forces the use of the 4.7 provider<br/>
     * This parameter is ignored if the <code>suiteXmlFiles</code> parameter is specified.
     *
     * @parameter expression="${excludedGroups}"
     * @since 2.2
     */
    private String excludedGroups;

    /**
     * (TestNG) List of &lt;suiteXmlFile> elements specifying TestNG suite xml file locations. Note that
     * <code>suiteXmlFiles</code> is incompatible with several other parameters of this plugin, like
     * <code>includes/excludes</code>.<br/>
     * This parameter is ignored if the <code>test</code> parameter is specified (allowing you to run a single test
     * instead of an entire suite).
     *
     * @parameter
     * @since 2.2
     */
    private File[] suiteXmlFiles;

    /**
     * Allows you to specify the name of the JUnit artifact. If not set, <code>junit:junit</code> will be used.
     *
     * @parameter expression="${junitArtifactName}" default-value="junit:junit"
     * @since 2.3.1
     */
    private String junitArtifactName;

    /**
     * Allows you to specify the name of the TestNG artifact. If not set, <code>org.testng:testng</code> will be used.
     *
     * @parameter expression="${testNGArtifactName}" default-value="org.testng:testng"
     * @since 2.3.1
     */
    private String testNGArtifactName;

    /**
     * (forkMode=perthread or TestNG/JUnit 4.7 provider) The attribute thread-count allows you to specify how many threads should be
     * allocated for this execution. Only makes sense to use in conjunction with the <code>parallel</code> parameter. (forkMode=perthread
     * does not support/require the <code>parallel</code> parameter)
     *
     * @parameter expression="${threadCount}"
     * @since 2.2
     */
    private int threadCount;

    /**
     * (JUnit 4.7 provider) Indicates that threadCount is per cpu core.
     *
     * @parameter expression="${perCoreThreadCount}" default-value="true"
     * @since 2.5
     */
    private boolean perCoreThreadCount;

    /**
     * (JUnit 4.7 provider) Indicates that the thread pool will be unlimited. The <code>parallel</code> parameter and
     * the actual number of classes/methods will decide. Setting this to "true" effectively disables
     * <code>perCoreThreadCount</code> and <code>threadCount</code>. Defaults to "false".
     *
     * @parameter expression="${useUnlimitedThreads}" default-value="false"
     * @since 2.5
     */
    private boolean useUnlimitedThreads;

    /**
     * (TestNG only) When you use the <code>parallel</code> attribute, TestNG will try to run all your test methods in
     * separate threads, except for methods that depend on each other, which will be run in the same thread in order to
     * respect their order of execution.
     * <p/>
     * (JUnit 4.7 provider) Supports values "classes"/"methods"/"both" to run in separate threads, as controlled by
     * <code>threadCount</code>.
     *
     * @parameter expression="${parallel}"
     * @since 2.2
     */
    private String parallel;

    /**
     * Whether to trim the stack trace in the reports to just the lines within the test, or show the full trace.
     *
     * @parameter expression="${trimStackTrace}" default-value="true"
     * @since 2.2
     */
    private boolean trimStackTrace;

    /**
     * Resolves the artifacts needed.
     *
     * @component
     */
    private ArtifactResolver artifactResolver;

    /**
     * Creates the artifact.
     *
     * @component
     */
    private ArtifactFactory artifactFactory;

    /**
     * The remote plugin repositories declared in the POM.
     *
     * @parameter expression="${project.pluginArtifactRepositories}"
     * @since 2.2
     */
    private List<ArtifactRepository> remoteRepositories;

    /**
     * For retrieval of artifact's metadata.
     *
     * @component
     */
    private ArtifactMetadataSource metadataSource;

    private Properties originalSystemProperties;

    /**
     * systemPropertyVariables + systemProperties
     */
    private Properties internalSystemProperties = new Properties();

    /**
     * Flag to disable the generation of report files in xml format.
     *
     * @parameter expression="${disableXmlReport}" default-value="false"
     * @since 2.2
     */
    private boolean disableXmlReport;

    /**
     * Option to pass dependencies to the system's classloader instead of using an isolated class loader when forking.
     * Prevents problems with JDKs which implement the service provider lookup mechanism by using the system's
     * classloader.
     *
     * @parameter expression="${surefire.useSystemClassLoader}" default-value="true"
     * @since 2.3
     */
    private boolean useSystemClassLoader;

    /**
     * By default, Surefire forks your tests using a manifest-only JAR; set this parameter to "false" to force it to
     * launch your tests with a plain old Java classpath. (See
     * http://maven.apache.org/plugins/maven-surefire-plugin/examples/class-loading.html for a more detailed explanation
     * of manifest-only JARs and their benefits.)
     * <p/>
     * Beware, setting this to "false" may cause your tests to fail on Windows if your classpath is too long.
     *
     * @parameter expression="${surefire.useManifestOnlyJar}" default-value="true"
     * @since 2.4.3
     */
    private boolean useManifestOnlyJar;

    /**
     * By default, Surefire enables JVM assertions for the execution of your test cases. To disable the assertions, set
     * this flag to "false".
     *
     * @parameter expression="${enableAssertions}" default-value="true"
     * @since 2.3.1
     */
    private boolean enableAssertions;

    /**
     * The current build session instance.
     *
     * @parameter expression="${session}"
     * @required
     * @readonly
     */
    private MavenSession session;

    /**
     * (TestNG only) Define the factory class used to create all test instances.
     *
     * @parameter expression="${objectFactory}"
     * @since 2.5
     */
    private String objectFactory;

    /**
     * @parameter default-value="${session.parallel}"
     * @readonly
     * @noinspection UnusedDeclaration
     */
    private Boolean parallelMavenExecution;

    /**
     * Defines the order the tests will be run in. Supported values are "alphabetical", "reversealphabetical", "random",
     * "hourly" (alphabetical on even hours, reverse alphabetical on odd hours), "failedfirst", "balanced" and "filesystem".
     * <p/>
     * <p/>
     * Odd/Even for hourly is determined at the time the of scanning the classpath, meaning it could change during a
     * multi-module build.
     * <p/>
     * Failed first will run tests that failed on previous run first, as well as new tests for this run.
     * <p/>
     * Balanced is only relevant with parallel=classes, and will try to optimize the run-order of the tests to
     * make all tests complete at the same time, reducing the overall execution time.
     * <p/>
     * Note that the statistics are stored in a file named .surefire-XXXXXXXXX beside pom.xml, and should not
     * be checked into version control. The "XXXXX" is the SHA1 checksum of the entire surefire configuration,
     * so different configurations will have different statistics files, meaning if you change any config
     * settings you will re-run once before new statistics data can be established.
     *
     * @parameter default-value="filesystem"
     * @since 2.7
     */
    private String runOrder;

    /**
     * @component
     */
    private ToolchainManager toolchainManager;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        super.execute();
    }

    @Override
    protected String getPluginName() {
        return "neworder-maven-plugin";
    }

    @Override
    protected boolean isSkipExecution() {
        return this.skipExec;
    }

    protected void handleSummary(Summary summary)
            throws MojoExecutionException, MojoFailureException {
        assertNoException(summary);
        assertNoFailureOrTimeout(summary);
        writeSummary(summary);
    }

    @Override
    protected void addPluginSpecificChecksumItems(ChecksumCalculator checksum) {
    }

    private void assertNoException(Summary summary)
            throws MojoExecutionException {
        if (!summary.isErrorFree()) {
            Exception cause = summary.getFirstException();
            throw new MojoExecutionException(cause.getMessage(), cause);
        }
    }

    private void assertNoFailureOrTimeout(Summary summary)
            throws MojoExecutionException {
        if (summary.isFailureOrTimeout()) {
            throw new MojoExecutionException("Failure or timeout");
        }
    }

    private void writeSummary(Summary summary)
            throws MojoFailureException {
        RunResult result = summary.getResultOfLastSuccessfulRun();
        SurefireHelper.reportExecution(this, result, getLog());
    }

    @Override
    protected String[] getDefaultIncludes() {
        return new String[]{"**/Test*.java", "**/*Test.java", "**/*TestCase.java"};
    }

    @Override
    public boolean isSkipTests() {
        return this.skipTests;
    }

    @Override
    public void setSkipTests(boolean skipTests) {
        this.skipTests = skipTests;
    }

    @Override
    public boolean isSkipExec() {
        return this.skipExec;
    }

    @Override
    public void setSkipExec(boolean b) {
        this.skipExec = b;
    }

    @Override
    public boolean isSkip() {
        return this.skip;
    }

    @Override
    public void setSkip(boolean skip) {
        this.skip = skip;
    }

    @Override
    public boolean isTestFailureIgnore() {
        return this.testFailureIgnore;
    }

    @Override
    public void setTestFailureIgnore(boolean testFailureIgnore) {
    }

    @Override
    public File getBasedir() {
        return this.basedir;
    }

    @Override
    public void setBasedir(File basedir) {
        this.basedir = basedir;
    }

    @Override
    public File getTestClassesDirectory() {
        return this.testClassesDirectory;
    }

    @Override
    public void setTestClassesDirectory(File testClassesDirectory) {
        this.testClassesDirectory = testClassesDirectory;
    }

    @Override
    public File getClassesDirectory() {
        return this.classesDirectory;
    }

    @Override
    public void setClassesDirectory(File classesDirectory) {
        this.classesDirectory = classesDirectory;
    }

    @Override
    public MavenProject getProject() {
        return this.project;
    }

    @Override
    public void setProject(MavenProject mavenProject) {
        this.project = mavenProject;
    }

    @Override
    public List getClasspathDependencyExcludes() {
        return this.classpathDependencyExcludes;
    }

    @Override
    public void setClasspathDependencyExcludes(List classpathDependencyExcludes) {
        this.classpathDependencyExcludes = classpathDependencyExcludes;
    }

    @Override
    public String getClasspathDependencyScopeExclude() {
        return this.classpathDependencyScopeExclude;
    }

    @Override
    public void setClasspathDependencyScopeExclude(String classpathDependencyScopeExclude) {
        this.classpathDependencyScopeExclude = classpathDependencyScopeExclude;
    }

    @Override
    public List getAdditionalClasspathElements() {
        return this.additionalClasspathElements;
    }

    @Override
    public void setAdditionalClasspathElements(List additionalClasspathElements) {
        this.additionalClasspathElements = additionalClasspathElements;
    }

    @Override
    public File getReportsDirectory() {
        return this.reportsDirectory;
    }

    @Override
    public void setReportsDirectory(File reportsDirectory) {
        this.reportsDirectory = reportsDirectory;
    }

    @Override
    public File getTestSourceDirectory() {
        return this.testSourceDirectory;
    }

    @Override
    public void setTestSourceDirectory(File testSourceDirectory) {
        this.testSourceDirectory = testSourceDirectory;
    }

    @Override
    public String getTest() {
        return this.test;
    }

    @Override
    public String getTestMethod() {
        if (StringUtils.isBlank(test)) {
            return null;
        }
        int index = this.test.indexOf('#');
        if (index >= 0) {
            return this.test.substring(index + 1, this.test.length());
        }
        return null;
    }

    @Override
    public void setTest(String test) {
        this.test = test;
    }

    @Override
    public List getIncludes() {
        return this.includes;
    }

    @Override
    public void setIncludes(List includes) {
        this.includes = includes;
    }

    @Override
    public List getExcludes() {
        return this.excludes;
    }

    @Override
    public void setExcludes(List excludes) {
        this.excludes = excludes;
    }

    @Override
    public ArtifactRepository getLocalRepository() {
        return this.localRepository;
    }

    @Override
    public void setLocalRepository(ArtifactRepository localRepository) {
        this.localRepository = localRepository;
    }

    @Override
    public Properties getSystemProperties() {
        return this.systemProperties;
    }

    @Override
    public void setSystemProperties(Properties systemProperties) {
        this.systemProperties = systemProperties;
    }

    @Override
    public Map getSystemPropertyVariables() {
        return this.systemPropertyVariables;
    }

    @Override
    public void setSystemPropertyVariables(Map systemPropertyVariables) {
        this.systemPropertyVariables = systemPropertyVariables;
    }

    @Override
    public File getSystemPropertiesFile() {
        return this.systemPropertiesFile;
    }

    @Override
    public void setSystemPropertiesFile(File systemPropertiesFile) {
        this.systemPropertiesFile = systemPropertiesFile;
    }

    @Override
    public Properties getProperties() {
        return this.properties;
    }

    @Override
    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    @Override
    public Map getPluginArtifactMap() {
        return this.pluginArtifactMap;
    }

    @Override
    public void setPluginArtifactMap(Map pluginArtifactMap) {
        this.pluginArtifactMap = pluginArtifactMap;
    }

    @Override
    public Map getProjectArtifactMap() {
        return this.projectArtifactMap;
    }

    @Override
    public void setProjectArtifactMap(Map projectArtifactMap) {
        this.projectArtifactMap = projectArtifactMap;
    }

    @Override
    public boolean isPrintSummary() {
        return this.printSummary;
    }

    @Override
    public void setPrintSummary(boolean printSummary) {
        this.printSummary = printSummary;
    }

    @Override
    public String getReportFormat() {
        return this.reportFormat;
    }

    @Override
    public void setReportFormat(String reportFormat) {
        this.reportFormat = reportFormat;
    }

    @Override
    public String getReportNameSuffix() {
        return this.reportNameSuffix;
    }

    @Override
    public void setReportNameSuffix(String reportNameSuffix) {
        this.reportNameSuffix = reportNameSuffix;
    }

    @Override
    public boolean isUseFile() {
        return this.useFile;
    }

    @Override
    public void setUseFile(boolean useFile) {
        this.useFile = useFile;
    }

    @Override
    public boolean isRedirectTestOutputToFile() {
        return this.redirectTestOutputToFile;
    }

    @Override
    public void setRedirectTestOutputToFile(boolean redirectTestOutputToFile) {
        this.redirectTestOutputToFile = redirectTestOutputToFile;
    }

    @Override
    public String getForkMode() {
        return this.forkMode;
    }

    @Override
    public void setForkMode(String forkMode) {
        this.forkMode = forkMode;
    }

    @Override
    public String getJvm() {
        return this.jvm;
    }

    @Override
    public void setJvm(String jvm) {
        this.jvm = jvm;
    }

    @Override
    public String getArgLine() {
        return this.argLine;
    }

    @Override
    public void setArgLine(String argLine) {
        this.argLine = argLine;
    }

    @Override
    public String getDebugForkedProcess() {
        return this.debugForkedProcess;
    }

    @Override
    public void setDebugForkedProcess(String debugForkedProcess) {
        this.debugForkedProcess = debugForkedProcess;
    }

    @Override
    public int getForkedProcessTimeoutInSeconds() {
        return this.forkedProcessTimeoutInSeconds;
    }

    @Override
    public void setForkedProcessTimeoutInSeconds(int forkedProcessTimeoutInSeconds) {
        this.forkedProcessTimeoutInSeconds = forkedProcessTimeoutInSeconds;
    }

    @Override
    public Map getEnvironmentVariables() {
        return this.environmentVariables;
    }

    @Override
    public void setEnvironmentVariables(Map environmentVariables) {
        this.environmentVariables = environmentVariables;
    }

    @Override
    public File getWorkingDirectory() {
        return this.workingDirectory;
    }

    @Override
    public void setWorkingDirectory(File workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    @Override
    public boolean isChildDelegation() {
        return this.childDelegation;
    }

    @Override
    public void setChildDelegation(boolean childDelegation) {
        this.childDelegation = childDelegation;
    }

    @Override
    public String getGroups() {
        return this.groups;
    }

    @Override
    public void setGroups(String groups) {
        this.groups = groups;
    }

    @Override
    public String getExcludedGroups() {
        return this.excludedGroups;
    }

    @Override
    public void setExcludedGroups(String excludedGroups) {
        this.excludedGroups = excludedGroups;
    }

    @Override
    public File[] getSuiteXmlFiles() {
        return this.suiteXmlFiles;
    }

    @Override
    public void setSuiteXmlFiles(File[] suiteXmlFiles) {
        this.suiteXmlFiles = suiteXmlFiles;
    }

    @Override
    public String getJunitArtifactName() {
        return this.junitArtifactName;
    }

    @Override
    public void setJunitArtifactName(String junitArtifactName) {
        this.junitArtifactName = junitArtifactName;
    }

    @Override
    public String getTestNGArtifactName() {
        return this.testNGArtifactName;
    }

    @Override
    public void setTestNGArtifactName(String testNGArtifactName) {
        this.testNGArtifactName = testNGArtifactName;
    }

    @Override
    public int getThreadCount() {
        return this.threadCount;
    }

    @Override
    public void setThreadCount(int threadCount) {
        this.threadCount = threadCount;
    }

    @Override
    public boolean getPerCoreThreadCount() {
        return this.perCoreThreadCount;
    }

    @Override
    public void setPerCoreThreadCount(boolean perCoreThreadCount) {
        this.perCoreThreadCount = perCoreThreadCount;
    }

    @Override
    public boolean getUseUnlimitedThreads() {
        return this.useUnlimitedThreads;
    }

    @Override
    public void setUseUnlimitedThreads(boolean useUnlimitedThreads) {
        this.useUnlimitedThreads = useUnlimitedThreads;
    }

    @Override
    public String getParallel() {
        return this.parallel;
    }

    @Override
    public void setParallel(String parallel) {
        this.parallel = parallel;
    }

    @Override
    public boolean isTrimStackTrace() {
        return this.trimStackTrace;
    }

    @Override
    public void setTrimStackTrace(boolean trimStackTrace) {
        this.trimStackTrace = trimStackTrace;
    }

    @Override
    public ArtifactResolver getArtifactResolver() {
        return this.artifactResolver;
    }

    @Override
    public void setArtifactResolver(ArtifactResolver artifactResolver) {
        this.artifactResolver = artifactResolver;
    }

    @Override
    public ArtifactFactory getArtifactFactory() {
        return this.artifactFactory;
    }

    @Override
    public void setArtifactFactory(ArtifactFactory artifactFactory) {
        this.artifactFactory = artifactFactory;
    }

    @Override
    public List getRemoteRepositories() {
        return this.remoteRepositories;
    }

    @Override
    public void setRemoteRepositories(List remoteRepositories) {
        this.remoteRepositories = remoteRepositories;
    }

    @Override
    public ArtifactMetadataSource getMetadataSource() {
        return this.metadataSource;
    }

    @Override
    public void setMetadataSource(ArtifactMetadataSource artifactMetadataSource) {
        this.metadataSource = artifactMetadataSource;
    }

    @Override
    public Properties getOriginalSystemProperties() {
        return this.originalSystemProperties;
    }

    @Override
    public void setOriginalSystemProperties(Properties originalSystemProperties) {
        this.originalSystemProperties = originalSystemProperties;
    }

    @Override
    public Properties getInternalSystemProperties() {
        return this.internalSystemProperties;
    }

    @Override
    public void setInternalSystemProperties(Properties internalSystemProperties) {
        this.internalSystemProperties = internalSystemProperties;
    }

    @Override
    public boolean isDisableXmlReport() {
        return this.disableXmlReport;
    }

    @Override
    public void setDisableXmlReport(boolean disableXmlReport) {
        this.disableXmlReport = disableXmlReport;
    }

    @Override
    public boolean isUseSystemClassLoader() {
        return this.useSystemClassLoader;
    }

    @Override
    public void setUseSystemClassLoader(boolean useSystemClassLoader) {
        this.useSystemClassLoader = useSystemClassLoader;
    }

    @Override
    public boolean isUseManifestOnlyJar() {
        return this.useManifestOnlyJar;
    }

    @Override
    public void setUseManifestOnlyJar(boolean useManifestOnlyJar) {
        this.useManifestOnlyJar = useManifestOnlyJar;
    }

    @Override
    public boolean isEnableAssertions() {
        return enableAssertions;
    }

    @Override
    public void setEnableAssertions(boolean enableAssertions) {
        this.enableAssertions = enableAssertions;
    }

    @Override
    public MavenSession getSession() {
        return this.session;
    }

    @Override
    public void setSession(MavenSession mavenSession) {
        this.session = mavenSession;
    }

    @Override
    public String getObjectFactory() {
        return this.objectFactory;
    }

    @Override
    public void setObjectFactory(String objectFactory) {
        this.objectFactory = objectFactory;
    }

    @Override
    public ToolchainManager getToolchainManager() {
        return this.toolchainManager;
    }

    @Override
    public void setToolchainManager(ToolchainManager toolchainManager) {
        this.toolchainManager = toolchainManager;
    }

    @Override
    public Boolean getFailIfNoTests() {
        return this.failIfNoTests;
    }

    @Override
    public void setFailIfNoTests(Boolean failIfNoTests) {
        this.failIfNoTests = failIfNoTests;
    }

    @Override
    public boolean isMavenParallel() {
        return parallelMavenExecution != null && parallelMavenExecution;
    }

    @Override
    public void setRunOrder(String runOrder) {
        this.runOrder = runOrder;
    }

    @Override
    public String getRunOrder() {
        return this.runOrder;
    }
}
