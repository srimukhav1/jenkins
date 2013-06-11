/*
 * The MIT License
 *
 * Copyright (c) 2004-2009, Sun Microsystems, Inc., Kohsuke Kawaguchi, Erik Ramfelt,
 * Yahoo! Inc., Tom Huybrechts, Olivier Lamy
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.jvnet.hudson.test;

import com.gargoylesoftware.htmlunit.AjaxController;
import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.DefaultCssErrorHandler;
import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebRequestSettings;
import com.gargoylesoftware.htmlunit.html.DomNode;
import com.gargoylesoftware.htmlunit.html.HtmlButton;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlImage;
import com.gargoylesoftware.htmlunit.html.HtmlInput;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.javascript.HtmlUnitContextFactory;
import com.gargoylesoftware.htmlunit.javascript.host.xml.XMLHttpRequest;
import com.gargoylesoftware.htmlunit.xml.XmlPage;
import hudson.ClassicPluginStrategy;
import hudson.CloseProofOutputStream;
import hudson.DNSMultiCast;
import hudson.DescriptorExtensionList;
import hudson.EnvVars;
import hudson.Extension;
import hudson.ExtensionList;
import hudson.FilePath;
import hudson.Functions;
import hudson.Launcher;
import hudson.Main;
import hudson.PluginManager;
import hudson.Util;
import hudson.WebAppMain;
import hudson.matrix.MatrixBuild;
import hudson.matrix.MatrixProject;
import hudson.matrix.MatrixRun;
import hudson.maven.MavenBuild;
import hudson.maven.MavenEmbedder;
import hudson.maven.MavenEmbedderException;
import hudson.maven.MavenModule;
import hudson.maven.MavenModuleSet;
import hudson.maven.MavenModuleSetBuild;
import hudson.maven.MavenUtil;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Computer;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.DownloadService;
import hudson.model.Executor;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Hudson;
import hudson.model.Item;
import hudson.model.JDK;
import hudson.model.Job;
import hudson.model.Label;
import hudson.model.Node;
import hudson.model.Queue;
import hudson.model.Result;
import hudson.model.RootAction;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.UpdateSite;
import hudson.model.User;
import hudson.model.View;
import hudson.remoting.Channel;
import hudson.remoting.VirtualChannel;
import hudson.remoting.Which;
import hudson.security.ACL;
import hudson.security.AbstractPasswordBasedSecurityRealm;
import hudson.security.GroupDetails;
import hudson.security.SecurityRealm;
import hudson.security.csrf.CrumbIssuer;
import hudson.slaves.CommandLauncher;
import hudson.slaves.ComputerConnector;
import hudson.slaves.ComputerListener;
import hudson.slaves.DumbSlave;
import hudson.slaves.RetentionStrategy;
import hudson.tasks.Ant;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrapperDescriptor;
import hudson.tasks.Builder;
import hudson.tasks.Mailer;
import hudson.tasks.Maven;
import hudson.tasks.Publisher;
import hudson.tools.ToolProperty;
import hudson.util.PersistedList;
import hudson.util.ReflectionUtils;
import hudson.util.StreamTaskListener;
import hudson.util.jna.GNUCLibrary;
import jenkins.model.Jenkins;
import jenkins.model.JenkinsAdaptor;
import net.sf.json.JSONObject;
import net.sourceforge.htmlunit.corejs.javascript.Context;
import net.sourceforge.htmlunit.corejs.javascript.ContextFactory;
import org.acegisecurity.AuthenticationException;
import org.acegisecurity.BadCredentialsException;
import org.acegisecurity.GrantedAuthority;
import org.acegisecurity.userdetails.UserDetails;
import org.acegisecurity.userdetails.UsernameNotFoundException;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.io.FileUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.AbstractArtifactResolutionException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.hamcrest.Matchers;
import org.junit.rules.MethodRule;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;
import org.jvnet.hudson.test.rhino.JavaScriptDebugger;
import org.kohsuke.stapler.ClassDescriptor;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.Dispatcher;
import org.kohsuke.stapler.MetaClass;
import org.kohsuke.stapler.MetaClassLoader;
import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.mortbay.jetty.MimeTypes;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.bio.SocketConnector;
import org.mortbay.jetty.security.HashUserRealm;
import org.mortbay.jetty.security.UserRealm;
import org.mortbay.jetty.webapp.Configuration;
import org.mortbay.jetty.webapp.WebAppContext;
import org.mortbay.jetty.webapp.WebXmlConfiguration;
import org.mozilla.javascript.tools.debugger.Dim;
import org.mozilla.javascript.tools.shell.Global;
import org.springframework.dao.DataAccessException;
import org.w3c.css.sac.CSSException;
import org.w3c.css.sac.CSSParseException;
import org.w3c.css.sac.ErrorHandler;
import org.xml.sax.SAXException;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import java.beans.PropertyDescriptor;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.lang.management.ThreadInfo;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.jar.Manifest;
import java.util.logging.Filter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import jenkins.model.JenkinsLocationConfiguration;
import org.acegisecurity.GrantedAuthorityImpl;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;

import static org.hamcrest.Matchers.hasXPath;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.*;
import static org.junit.matchers.JUnitMatchers.containsString;
import org.junit.rules.TemporaryFolder;

/**
 * JUnit 4.10+ style rule to allow test cases to fire up a Jenkins instance
 *
 * @see <a href="http://wiki.jenkins-ci.org/display/JENKINS/Unit+Test+JUnit4">Wiki article about unit testing in Jenkins</a>
 * @author Stephen Connolly
 * @since 1.436
 */
public class JenkinsRule implements TestRule, MethodRule, RootAction {

    private TestEnvironment env;

    private Description testDescription;

    private static JenkinsRule CURRENT = null;

    /**
     * Points to the same object as {@link #jenkins} does.
     */
    public Hudson hudson;

    public Jenkins jenkins;

    protected HudsonHomeLoader homeLoader = HudsonHomeLoader.NEW;
    /**
     * TCP/IP port that the server is listening on.
     */
    protected int localPort;
    protected Server server;

    /**
     * Where in the {@link Server} is Hudson deployed?
     * <p>
     * Just like {@link javax.servlet.ServletContext#getContextPath()}, starts with '/' but doesn't end with '/'.
     */
    protected String contextPath = "";

    /**
     * {@link Runnable}s to be invoked at {@link #after()} .
     */
    protected List<LenientRunnable> tearDowns = new ArrayList<LenientRunnable>();

    protected List<JenkinsRecipe.Runner> recipes = new ArrayList<JenkinsRecipe.Runner>();

    /**
     * Remember {@link WebClient}s that are created, to release them properly.
     */
    private List<WebClient> clients = new ArrayList<WebClient>();

    /**
     * Remember channels that are created, to release them at the end.
     */
    private List<Channel> channels = new ArrayList<Channel>();

    /**
     * JavaScript "debugger" that provides you information about the JavaScript call stack
     * and the current values of the local variables in those stack frame.
     *
     * <p>
     * Unlike Java debugger, which you as a human interfaces directly and interactively,
     * this JavaScript debugger is to be interfaced by your program (or through the
     * expression evaluation capability of your Java debugger.)
     */
    protected JavaScriptDebugger jsDebugger = new JavaScriptDebugger();

    /**
     * If this test case has additional {@link org.jvnet.hudson.test.recipes.WithPlugin} annotations, set to true.
     * This will cause a fresh {@link hudson.PluginManager} to be created for this test.
     * Leaving this to false enables the test harness to use a pre-loaded plugin manager,
     * which runs faster.
     *
     * @deprecated
     *      Use {@link #pluginManager}
     */
    public boolean useLocalPluginManager;

    /**
     * Number of seconds until the test times out.
     */
    public int timeout = 90;

    private volatile Timer timeoutTimer;

    /**
     * Set the plugin manager to be passed to {@link Jenkins} constructor.
     *
     * For historical reasons, {@link #useLocalPluginManager}==true will take the precedence.
     */
    private PluginManager pluginManager = TestPluginManager.INSTANCE;

    public JenkinsComputerConnectorTester computerConnectorTester = new JenkinsComputerConnectorTester(this);

    public Jenkins getInstance() {
        return jenkins;
    }

    /**
     * Override to set up your specific external resource.
     * @throws Throwable if setup fails (which will disable {@code after}
     */
    protected void before() throws Throwable {
        env = new TestEnvironment(testDescription);
        env.pin();
        recipe();
        AbstractProject.WORKSPACE.toString();
        User.clear();


        try {
            jenkins = hudson = newHudson();
        } catch (Exception e) {
            // if Hudson instance fails to initialize, it leaves the instance field non-empty and break all the rest of the tests, so clean that up.
            Field f = Jenkins.class.getDeclaredField("theInstance");
            f.setAccessible(true);
            f.set(null,null);
            throw e;
        }
        jenkins.setNoUsageStatistics(true); // collecting usage stats from tests are pointless.

        jenkins.setCrumbIssuer(new TestCrumbIssuer());

        jenkins.servletContext.setAttribute("app",jenkins);
        jenkins.servletContext.setAttribute("version","?");
        WebAppMain.installExpressionFactory(new ServletContextEvent(jenkins.servletContext));

        // set a default JDK to be the one that the harness is using.
        jenkins.getJDKs().add(new JDK("default",System.getProperty("java.home")));

        configureUpdateCenter();

        // expose the test instance as a part of URL tree.
        // this allows tests to use a part of the URL space for itself.
        jenkins.getActions().add(this);

        // cause all the descriptors to reload.
        // ideally we'd like to reset them to properly emulate the behavior, but that's not possible.
        Mailer.DescriptorImpl desc = Mailer.descriptor();
        // prevent NPE with eclipse
        if (desc != null) Mailer.descriptor().setHudsonUrl(getURL().toString());
        JenkinsLocationConfiguration.get().setUrl(getURL().toString());
        for( Descriptor d : jenkins.getExtensionList(Descriptor.class) )
            d.load();
    }

    /**
     * Configures the update center setting for the test.
     * By default, we load updates from local proxy to avoid network traffic as much as possible.
     */
    protected void configureUpdateCenter() throws Exception {
        final String updateCenterUrl = "http://localhost:"+ JavaNetReverseProxy.getInstance().localPort+"/update-center.json";

        // don't waste bandwidth talking to the update center
        DownloadService.neverUpdate = true;
        UpdateSite.neverUpdate = true;

        PersistedList<UpdateSite> sites = jenkins.getUpdateCenter().getSites();
        sites.clear();
        sites.add(new UpdateSite("default", updateCenterUrl));
    }

    protected void setUpTimeout() {
        if (timeout<=0)     return; // no timeout

        final Thread testThread = Thread.currentThread();
        timeoutTimer = new Timer();
        timeoutTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (timeoutTimer!=null)
                    testThread.interrupt();
            }
        }, TimeUnit.SECONDS.toMillis(timeout));
    }

    /**
     * Override to tear down your specific external resource.
     */
    protected void after() {
        try {
            if (timeoutTimer!=null) {
                timeoutTimer.cancel();
                timeoutTimer = null;
            }

            // cancel pending asynchronous operations, although this doesn't really seem to be working
            for (WebClient client : clients) {
                // unload the page to cancel asynchronous operations
                try {
                    client.getPage("about:blank");
                } catch (IOException e) {
                    // ignore
                }
                client.closeAllWindows();
            }
            clients.clear();

            for (Channel c : channels)
                try {
                    c.close();
                } catch (IOException e) {
                    // ignore
                }
            for (Channel c : channels)
                try {
                    c.join();
                } catch (InterruptedException e) {
                    // ignore
                }
            channels.clear();

        } finally {
            try {
                server.stop();
            } catch (Exception e) {
                // ignore
            }
            for (LenientRunnable r : tearDowns)
                try {
                    r.run();
                } catch (Exception e) {
                    // ignore
                }

            jenkins.cleanUp();
            ExtensionList.clearLegacyInstances();
            DescriptorExtensionList.clearLegacyInstances();

            try {
                env.dispose();
            } catch (Exception x) {
                x.printStackTrace();
            }

            // Hudson creates ClassLoaders for plugins that hold on to file descriptors of its jar files,
            // but because there's no explicit dispose method on ClassLoader, they won't get GC-ed until
            // at some later point, leading to possible file descriptor overflow. So encourage GC now.
            // see http://bugs.sun.com/view_bug.do?bug_id=4950148
            // TODO use URLClassLoader.close() in Java 7
            System.gc();
        }
    }

    /**
     * Backward compatibility with JUnit 4.8.
     */
    public Statement apply(Statement base, FrameworkMethod method, Object target) {
        return apply(base,Description.createTestDescription(method.getMethod().getDeclaringClass(), method.getName(), method.getAnnotations()));
    }

    public Statement apply(final Statement base, final Description description) {
        if (description.getAnnotation(WithoutJenkins.class) != null) {
            // request has been made to not create the instance for this test method
            return base;
        }
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                testDescription = description;
                Thread t = Thread.currentThread();
                String o = t.getName();
                t.setName("Executing "+ testDescription.getDisplayName());
                CURRENT = JenkinsRule.this;
                before();
                try {
                    System.out.println("=== Starting " + testDescription.getDisplayName());
                    // so that test code has all the access to the system
                    ACL.impersonate(ACL.SYSTEM);
                    try {
                        base.evaluate();
                    } catch (Throwable th) {
                        // allow the late attachment of a debugger in case of a failure. Useful
                        // for diagnosing a rare failure
                        try {
                            throw new BreakException();
                        } catch (BreakException e) {}

                        // dump threads
                        ThreadInfo[] threadInfos = Functions.getThreadInfos();
                        Functions.ThreadGroupMap m = Functions.sortThreadsAndGetGroupMap(threadInfos);
                        for (ThreadInfo ti : threadInfos) {
                            System.err.println(Functions.dumpThreadInfo(ti, m));
                        }
                        throw th;
                    }
                } finally {
                    after();
                    testDescription = null;
                    t.setName(o);
                    CURRENT = null;
                }
            }
        };
    }

    @SuppressWarnings("serial")
    public static class BreakException extends Exception {}

    public String getIconFileName() {
        return null;
    }

    public String getDisplayName() {
        return null;
    }

    public String getUrlName() {
        return "self";
    }

    /**
     * Creates a new instance of {@link jenkins.model.Jenkins}. If the derived class wants to create it in a different way,
     * you can override it.
     */
    protected Hudson newHudson() throws Exception {
        ServletContext webServer = createWebServer();
        File home = homeLoader.allocate();
        for (JenkinsRecipe.Runner r : recipes)
            r.decorateHome(this,home);
        return new Hudson(home, webServer, getPluginManager());
    }

    public PluginManager getPluginManager() {
        if (jenkins == null) {
            return useLocalPluginManager ? null : pluginManager;
        } else {
            return jenkins.getPluginManager();
        }
    }

    /**
     * Sets the {@link PluginManager} to be used when creating a new {@link Jenkins} instance.
     *
     * @param pluginManager
     *      null to let Jenkins create a new instance of default plugin manager, like it normally does when running as a webapp outside the test.
     */
    public void setPluginManager(PluginManager pluginManager) {
        this.useLocalPluginManager = false;
        this.pluginManager = pluginManager;
        if (jenkins!=null)
            throw new IllegalStateException("Too late to override the plugin manager");
    }

    public JenkinsRule with(PluginManager pluginManager) {
        setPluginManager(pluginManager);
        return this;
    }

    public File getWebAppRoot() throws Exception {
        return WarExploder.getExplodedDir();
    }

    /**
     * Prepares a webapp hosting environment to get {@link javax.servlet.ServletContext} implementation
     * that we need for testing.
     */
    protected ServletContext createWebServer() throws Exception {
        server = new Server();

        WebAppContext context = new WebAppContext(WarExploder.getExplodedDir().getPath(), contextPath);
        context.setClassLoader(getClass().getClassLoader());
        context.setConfigurations(new Configuration[]{new WebXmlConfiguration(), new NoListenerConfiguration()});
        server.setHandler(context);
        context.setMimeTypes(MIME_TYPES);

        SocketConnector connector = new SocketConnector();
        connector.setHeaderBufferSize(12*1024); // use a bigger buffer as Stapler traces can get pretty large on deeply nested URL

        server.setThreadPool(new ThreadPoolImpl(new ThreadPoolExecutor(10, 10, 10L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(),new ThreadFactory() {
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r);
                t.setName("Jetty Thread Pool");
                return t;
            }
        })));
        server.addConnector(connector);
        server.addUserRealm(configureUserRealm());
        server.start();

        localPort = connector.getLocalPort();

        return context.getServletContext();
    }

    /**
     * Configures a security realm for a test.
     */
    public UserRealm configureUserRealm() {
        HashUserRealm realm = new HashUserRealm();
        realm.setName("default");   // this is the magic realm name to make it effective on everywhere
        realm.put("alice","alice");
        realm.put("bob","bob");
        realm.put("charlie","charlie");

        realm.addUserToRole("alice","female");
        realm.addUserToRole("bob","male");
        realm.addUserToRole("charlie","male");

        return realm;
    }

    @TestExtension
    public static class ComputerListenerImpl extends ComputerListener {
        @Override
        public void onOnline(Computer c, TaskListener listener) throws IOException, InterruptedException {
            VirtualChannel ch = c.getChannel();
            if (ch instanceof Channel)
                CURRENT.channels.add((Channel)ch);
        }
    }


    /**
     * Returns the older default Maven, while still allowing specification of other bundled Mavens.
     */
    public Maven.MavenInstallation configureDefaultMaven() throws Exception {
        return configureDefaultMaven("apache-maven-2.2.1", Maven.MavenInstallation.MAVEN_20);
    }

    public Maven.MavenInstallation configureMaven3() throws Exception {
        Maven.MavenInstallation mvn = configureDefaultMaven("apache-maven-3.0.1", Maven.MavenInstallation.MAVEN_30);

        Maven.MavenInstallation m3 = new Maven.MavenInstallation("apache-maven-3.0.1",mvn.getHome(), NO_PROPERTIES);
        jenkins.getDescriptorByType(Maven.DescriptorImpl.class).setInstallations(m3);
        return m3;
    }

    /**
     * Locates Maven2 and configure that as the only Maven in the system.
     */
    public Maven.MavenInstallation configureDefaultMaven(String mavenVersion, int mavenReqVersion) throws Exception {
        // first if we are running inside Maven, pick that Maven, if it meets the criteria we require..
        // does it exists in the buildDirectory see maven-junit-plugin systemProperties
        // buildDirectory -> ${project.build.directory} (so no reason to be null ;-) )
        String buildDirectory = System.getProperty( "buildDirectory", "./target/classes/" );
        File mavenAlreadyInstalled = new File(buildDirectory, mavenVersion);
        if (mavenAlreadyInstalled.exists()) {
            Maven.MavenInstallation
                    mavenInstallation = new Maven.MavenInstallation("default",mavenAlreadyInstalled.getAbsolutePath(), NO_PROPERTIES);
            jenkins.getDescriptorByType(Maven.DescriptorImpl.class).setInstallations(mavenInstallation);
            return mavenInstallation;
        }

        // Does maven.home point to a Maven installation which satisfies mavenReqVersion?
        String home = System.getProperty("maven.home");
        if(home!=null) {
            Maven.MavenInstallation mavenInstallation = new Maven.MavenInstallation("default",home, NO_PROPERTIES);
            if (mavenInstallation.meetsMavenReqVersion(createLocalLauncher(), mavenReqVersion)) {
                jenkins.getDescriptorByType(Maven.DescriptorImpl.class).setInstallations(mavenInstallation);
                return mavenInstallation;
            }
        }

        // otherwise extract the copy we have.
        // this happens when a test is invoked from an IDE, for example.
        LOGGER.warning("Extracting a copy of Maven bundled in the test harness. " +
                "To avoid a performance hit, set the system property 'maven.home' to point to a Maven2 installation.");
        FilePath mvn = jenkins.getRootPath().createTempFile("maven", "zip");
        mvn.copyFrom(HudsonTestCase.class.getClassLoader().getResource(mavenVersion + "-bin.zip"));
        File mvnHome =  new File(buildDirectory);//createTmpDir();
        mvn.unzip(new FilePath(mvnHome));
        // TODO: switch to tar that preserves file permissions more easily
        if(!Functions.isWindows())
            GNUCLibrary.LIBC.chmod(new File(mvnHome,mavenVersion+"/bin/mvn").getPath(),0755);

        Maven.MavenInstallation mavenInstallation = new Maven.MavenInstallation("default",
                new File(mvnHome,mavenVersion).getAbsolutePath(), NO_PROPERTIES);
		jenkins.getDescriptorByType(Maven.DescriptorImpl.class).setInstallations(mavenInstallation);
		return mavenInstallation;
    }

    /**
     * Extracts Ant and configures it.
     */
    public Ant.AntInstallation configureDefaultAnt() throws Exception {
        Ant.AntInstallation antInstallation;
        if (System.getenv("ANT_HOME") != null) {
            antInstallation = new Ant.AntInstallation("default", System.getenv("ANT_HOME"), NO_PROPERTIES);
        } else {
            LOGGER.warning("Extracting a copy of Ant bundled in the test harness. " +
                    "To avoid a performance hit, set the environment variable ANT_HOME to point to an  Ant installation.");
            FilePath ant = jenkins.getRootPath().createTempFile("ant", "zip");
            ant.copyFrom(HudsonTestCase.class.getClassLoader().getResource("apache-ant-1.8.1-bin.zip"));
            File antHome = createTmpDir();
            ant.unzip(new FilePath(antHome));
            // TODO: switch to tar that preserves file permissions more easily
            if(!Functions.isWindows())
                GNUCLibrary.LIBC.chmod(new File(antHome,"apache-ant-1.8.1/bin/ant").getPath(),0755);

            antInstallation = new Ant.AntInstallation("default", new File(antHome,"apache-ant-1.8.1").getAbsolutePath(),NO_PROPERTIES);
        }
		jenkins.getDescriptorByType(Ant.DescriptorImpl.class).setInstallations(antInstallation);
		return antInstallation;
    }

//
// Convenience methods
//

    public FreeStyleProject createFreeStyleProject() throws IOException {
        return createFreeStyleProject(createUniqueProjectName());
    }

    public FreeStyleProject createFreeStyleProject(String name) throws IOException {
        return jenkins.createProject(FreeStyleProject.class, name);
    }

    public MatrixProject createMatrixProject() throws IOException {
        return createMatrixProject(createUniqueProjectName());
    }

    public MatrixProject createMatrixProject(String name) throws IOException {
        return jenkins.createProject(MatrixProject.class, name);
    }

    /**
     * Creates a empty Maven project with an unique name.
     *
     * @see #configureDefaultMaven()
     */
    public MavenModuleSet createMavenProject() throws IOException {
        return createMavenProject(createUniqueProjectName());
    }

    /**
     * Creates a empty Maven project with the given name.
     *
     * @see #configureDefaultMaven()
     */
    public MavenModuleSet createMavenProject(String name) throws IOException {
        return jenkins.createProject(MavenModuleSet.class,name);
    }

    /**
     * Creates a simple folder that other jobs can be placed in.
     * @since 1.494
     */
    public MockFolder createFolder(String name) throws IOException {
        return jenkins.createProject(MockFolder.class, name);
    }

    protected String createUniqueProjectName() {
        return "test"+jenkins.getItems().size();
    }

    /**
     * Creates {@link hudson.Launcher.LocalLauncher}. Useful for launching processes.
     */
    public Launcher.LocalLauncher createLocalLauncher() {
        return new Launcher.LocalLauncher(StreamTaskListener.fromStdout());
    }

    /**
     * Allocates a new temporary directory for the duration of this test.
     * @deprecated Use {@link TemporaryFolder} instead.
     */
    @Deprecated
    public File createTmpDir() throws IOException {
        return env.temporaryDirectoryAllocator.allocate();
    }

    public DumbSlave createSlave() throws Exception {
        return createSlave("",null);
    }

    /**
     * Creates and launches a new slave on the local host.
     */
    public DumbSlave createSlave(Label l) throws Exception {
    	return createSlave(l, null);
    }

    /**
     * Creates a test {@link hudson.security.SecurityRealm} that recognizes username==password as valid.
     */
    public DummySecurityRealm createDummySecurityRealm() {
        return new DummySecurityRealm();
    }

    /** @see #createDummySecurityRealm */
    public static class DummySecurityRealm extends AbstractPasswordBasedSecurityRealm {

        private final Map<String,Set<String>> groupsByUser = new HashMap<String,Set<String>>();

        DummySecurityRealm() {}

        @Override
        protected UserDetails authenticate(String username, String password) throws AuthenticationException {
            if (username.equals(password))
                return loadUserByUsername(username);
            throw new BadCredentialsException(username);
        }

        @Override
        public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException,
                DataAccessException {
            List<GrantedAuthority> auths = new ArrayList<GrantedAuthority>();
            auths.add(AUTHENTICATED_AUTHORITY);
            Set<String> groups = groupsByUser.get(username);
            if (groups != null) {
                for (String g : groups) {
                    auths.add(new GrantedAuthorityImpl(g));
                }
            }
            return new org.acegisecurity.userdetails.User(username,"",true,true,true,true, auths.toArray(new GrantedAuthority[auths.size()]));
        }

        @Override
        public GroupDetails loadGroupByGroupname(final String groupname) throws UsernameNotFoundException, DataAccessException {
            for (Set<String> groups : groupsByUser.values()) {
                if (groups.contains(groupname)) {
                    return new GroupDetails() {
                        @Override
                        public String getName() {
                            return groupname;
                        }
                    };
                }
            }
            throw new UsernameNotFoundException(groupname);
        }

        /** Associate some groups with a username. */
        public void addGroups(String username, String... groups) {
            Set<String> gs = groupsByUser.get(username);
            if (gs == null) {
                groupsByUser.put(username, gs = new TreeSet<String>());
            }
            gs.addAll(Arrays.asList(groups));
        }

    }

    /**
     * Returns the URL of the webapp top page.
     * URL ends with '/'.
     */
    public URL getURL() throws IOException {
        return new URL("http://localhost:"+localPort+contextPath+"/");
    }

    public DumbSlave createSlave(EnvVars env) throws Exception {
        return createSlave("",env);
    }

    public DumbSlave createSlave(Label l, EnvVars env) throws Exception {
        return createSlave(l==null ? null : l.getExpression(), env);
    }

    /**
     * Creates a slave with certain additional environment variables
     */
    public DumbSlave createSlave(String labels, EnvVars env) throws Exception {
        synchronized (jenkins) {
            int sz = jenkins.getNodes().size();
            return createSlave("slave" + sz,labels,env);
    	}
    }

    public DumbSlave createSlave(String nodeName, String labels, EnvVars env) throws Exception {
        synchronized (jenkins) {
            DumbSlave slave = new DumbSlave(nodeName, "dummy",
    				createTmpDir().getPath(), "1", Node.Mode.NORMAL, labels==null?"":labels, createComputerLauncher(env), RetentionStrategy.NOOP, Collections.EMPTY_LIST);
    		jenkins.addNode(slave);
    		return slave;
    	}
    }

    public PretendSlave createPretendSlave(FakeLauncher faker) throws Exception {
        synchronized (jenkins) {
            int sz = jenkins.getNodes().size();
            PretendSlave slave = new PretendSlave("slave" + sz, createTmpDir().getPath(), "", createComputerLauncher(null), faker);
    		jenkins.addNode(slave);
    		return slave;
        }
    }

    /**
     * Creates a {@link hudson.slaves.CommandLauncher} for launching a slave locally.
     *
     * @param env
     *      Environment variables to add to the slave process. Can be null.
     */
    public CommandLauncher createComputerLauncher(EnvVars env) throws URISyntaxException, MalformedURLException {
        int sz = jenkins.getNodes().size();
        return new CommandLauncher(
                String.format("\"%s/bin/java\" %s -jar \"%s\"",
                        System.getProperty("java.home"),
                        SLAVE_DEBUG_PORT>0 ? " -Xdebug -Xrunjdwp:transport=dt_socket,server=y,address="+(SLAVE_DEBUG_PORT+sz): "",
                        new File(jenkins.getJnlpJars("slave.jar").getURL().toURI()).getAbsolutePath()),
                env);
    }

    /**
     * Create a new slave on the local host and wait for it to come onilne
     * before returning.
     */
    public DumbSlave createOnlineSlave() throws Exception {
        return createOnlineSlave(null);
    }

    /**
     * Create a new slave on the local host and wait for it to come onilne
     * before returning.
     */
    public DumbSlave createOnlineSlave(Label l) throws Exception {
        return createOnlineSlave(l, null);
    }

    /**
     * Create a new slave on the local host and wait for it to come online
     * before returning
     */
    @SuppressWarnings({"deprecation"})
    public DumbSlave createOnlineSlave(Label l, EnvVars env) throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        ComputerListener waiter = new ComputerListener() {
                                            @Override
                                            public void onOnline(Computer C, TaskListener t) {
                                                latch.countDown();
                                                unregister();
                                            }
                                        };
        waiter.register();

        DumbSlave s = createSlave(l, env);
        latch.await();

        return s;
    }

    /**
     * Blocks until the ENTER key is hit.
     * This is useful during debugging a test so that one can inspect the state of Hudson through the web browser.
     */
    public void interactiveBreak() throws Exception {
        System.out.println("Jenkins is running at http://localhost:"+localPort+"/");
        new BufferedReader(new InputStreamReader(System.in)).readLine();
    }

    /**
     * Returns the last item in the list.
     */
    public <T> T last(List<T> items) {
        return items.get(items.size()-1);
    }

    /**
     * Pauses the execution until ENTER is hit in the console.
     * <p>
     * This is often very useful so that you can interact with Hudson
     * from an browser, while developing a test case.
     */
    public void pause() throws IOException {
        new BufferedReader(new InputStreamReader(System.in)).readLine();
    }

    /**
     * Performs a search from the search box.
     */
    public Page search(String q) throws Exception {
        return new WebClient().search(q);
    }

    /**
     * Hits the Hudson system configuration and submits without any modification.
     */
    public void configRoundtrip() throws Exception {
        submit(createWebClient().goTo("configure").getFormByName("config"));
    }

    /**
     * Loads a configuration page and submits it without any modifications, to
     * perform a round-trip configuration test.
     * <p>
     * See http://wiki.jenkins-ci.org/display/JENKINS/Unit+Test#UnitTest-Configurationroundtriptesting
     */
    public <P extends Job> P configRoundtrip(P job) throws Exception {
        submit(createWebClient().getPage(job,"configure").getFormByName("config"));
        return job;
    }

    public <P extends Item> P configRoundtrip(P job) throws Exception {
        submit(createWebClient().getPage(job, "configure").getFormByName("config"));
        return job;
    }

    /**
     * Performs a configuration round-trip testing for a builder.
     */
    public <B extends Builder> B configRoundtrip(B before) throws Exception {
        FreeStyleProject p = createFreeStyleProject();
        p.getBuildersList().add(before);
        configRoundtrip((Item)p);
        return (B)p.getBuildersList().get(before.getClass());
    }

    /**
     * Performs a configuration round-trip testing for a publisher.
     */
    public <P extends Publisher> P configRoundtrip(P before) throws Exception {
        FreeStyleProject p = createFreeStyleProject();
        p.getPublishersList().add(before);
        configRoundtrip((Item) p);
        return (P)p.getPublishersList().get(before.getClass());
    }

    public <C extends ComputerConnector> C configRoundtrip(C before) throws Exception {
        computerConnectorTester.connector = before;
        submit(createWebClient().goTo("self/computerConnectorTester/configure").getFormByName("config"));
        return (C)computerConnectorTester.connector;
    }

    public User configRoundtrip(User u) throws Exception {
        submit(createWebClient().goTo(u.getUrl()+"/configure").getFormByName("config"));
        return u;
    }

    public <N extends Node> N configRoundtrip(N node) throws Exception {
        submit(createWebClient().goTo("/computer/" + node.getNodeName() + "/configure").getFormByName("config"));
        return (N)jenkins.getNode(node.getNodeName());
    }

    public <V extends View> V configRoundtrip(V view) throws Exception {
        submit(createWebClient().getPage(view, "configure").getFormByName("viewConfig"));
        return view;
    }


    /**
     * Asserts that the outcome of the build is a specific outcome.
     */
    public <R extends Run> R assertBuildStatus(Result status, R r) throws Exception {
        if(status==r.getResult())
            return r;

        // dump the build output in failure message
        String msg = "unexpected build status; build log was:\n------\n" + getLog(r) + "\n------\n";
        if(r instanceof MatrixBuild) {
            MatrixBuild mb = (MatrixBuild)r;
            for (MatrixRun mr : mb.getRuns()) {
                msg+="--- "+mr.getParent().getCombination()+" ---\n"+getLog(mr)+"\n------\n";
            }
        }
        assertThat(msg, r.getResult(), is(status));
        return r;
    }

    /** Determines whether the specifed HTTP status code is generally "good" */
    public boolean isGoodHttpStatus(int status) {
        if ((400 <= status) && (status <= 417)) {
            return false;
        }
        if ((500 <= status) && (status <= 505)) {
            return false;
        }
        return true;
    }

    /** Assert that the specifed page can be served with a "good" HTTP status,
     * eg, the page is not missing and can be served without a server error
     * @param page
     */
    public void assertGoodStatus(Page page) {
        assertThat(isGoodHttpStatus(page.getWebResponse().getStatusCode()), is(true));
    }


    public <R extends Run> R assertBuildStatusSuccess(R r) throws Exception {
        assertBuildStatus(Result.SUCCESS, r);
        return r;
    }

    public <R extends Run> R assertBuildStatusSuccess(Future<? extends R> r) throws Exception {
        assertThat("build was actually scheduled", r, Matchers.notNullValue());
        return assertBuildStatusSuccess(r.get());
    }

    public <J extends AbstractProject<J,R>,R extends AbstractBuild<J,R>> R buildAndAssertSuccess(J job) throws Exception {
        return assertBuildStatusSuccess(job.scheduleBuild2(0));
    }

    /**
     * Avoids need for cumbersome {@code this.<J,R>buildAndAssertSuccess(...)} type hints under JDK 7 javac (and supposedly also IntelliJ).
     */
    public FreeStyleBuild buildAndAssertSuccess(FreeStyleProject job) throws Exception {
        return assertBuildStatusSuccess(job.scheduleBuild2(0));
    }
    public MavenModuleSetBuild buildAndAssertSuccess(MavenModuleSet job) throws Exception {
        return assertBuildStatusSuccess(job.scheduleBuild2(0));
    }
    public MavenBuild buildAndAssertSuccess(MavenModule job) throws Exception {
        return assertBuildStatusSuccess(job.scheduleBuild2(0));
    }

    /**
     * Asserts that the console output of the build contains the given substring.
     */
    public void assertLogContains(String substring, Run run) throws Exception {
        assertThat(getLog(run), containsString(substring));
    }

    /**
     * Asserts that the console output of the build does not contain the given substring.
     */
    public void assertLogNotContains(String substring, Run run) throws Exception {
        assertThat(getLog(run), not(containsString(substring)));
    }

    /**
     * Get entire log file (this method is deprecated in hudson.model.Run,
     * but in tests it is OK to load entire log).
     */
    public static String getLog(Run run) throws IOException {
        return Util.loadFile(run.getLogFile(), run.getCharset());
    }

    /**
     * Asserts that the XPath matches.
     */
    public void assertXPath(HtmlPage page, String xpath) {
        assertThat(page.getDocumentElement(), hasXPath(xpath));
    }

    /** Asserts that the XPath matches the contents of a DomNode page. This
     * variant of assertXPath(HtmlPage page, String xpath) allows us to
     * examine XmlPages.
     * @param page
     * @param xpath
     */
    public void assertXPath(DomNode page, String xpath) {
        List<? extends Object> nodes = page.getByXPath(xpath);
        assertThat("There should be an object that matches XPath:" + xpath, nodes.isEmpty(), is(false));
    }

    public void assertXPathValue(DomNode page, String xpath, String expectedValue) {
        org.w3c.dom.Node node = page.getFirstByXPath(xpath);
        assertThat(node, hasXPath(xpath, is(expectedValue)));
    }

    public void assertXPathValueContains(DomNode page, String xpath, String needle) {
        org.w3c.dom.Node node = page.getFirstByXPath(xpath);
        assertThat(node, hasXPath(xpath, Matchers.containsString(needle)));
    }

    public void assertXPathResultsContainText(DomNode page, String xpath, String needle) {
        List<? extends Object> nodes = page.getByXPath(xpath);
        assertThat("no nodes matching xpath found", nodes.isEmpty(), is(false));
        boolean found = false;
        for (Object o : nodes) {
            if (o instanceof org.w3c.dom.Node) {
                org.w3c.dom.Node n = (org.w3c.dom.Node) o;
                String textString = n.getTextContent();
                if ((textString != null) && textString.contains(needle)) {
                    found = true;
                    break;
                }
            }
        }
        assertThat("needle found in haystack", found, is(true));
    }

    /**
     * Makes sure that all the images in the page loads successfully.
     * (By default, HtmlUnit doesn't load images.)
     */
    public void assertAllImageLoadSuccessfully(HtmlPage p) {
        for (HtmlImage img : p.<HtmlImage>selectNodes("//IMG")) {
            try {
                img.getHeight();
            } catch (IOException e) {
                throw new Error("Failed to load "+img.getSrcAttribute(),e);
            }
        }
    }


    public void assertStringContains(String message, String haystack, String needle) {
        assertThat(message, haystack, Matchers.containsString(needle));
    }

    public void assertStringContains(String haystack, String needle) {
        assertThat(haystack, Matchers.containsString(needle));
    }

    /**
     * Asserts that help files exist for the specified properties of the given instance.
     *
     * @param type
     *      The describable class type that should have the associated help files.
     * @param properties
     *      ','-separated list of properties whose help files should exist.
     */
    public void assertHelpExists(final Class<? extends Describable> type, final String properties) throws Exception {
        executeOnServer(new Callable<Object>() {
            public Object call() throws Exception {
                Descriptor d = jenkins.getDescriptor(type);
                WebClient wc = createWebClient();
                for (String property : listProperties(properties)) {
                    String url = d.getHelpFile(property);
                    assertThat("Help file for the property " + property + " is missing on " + type, url,
                            Matchers.notNullValue());
                    wc.goTo(url); // make sure it successfully loads
                }
                return null;
            }
        });
    }

    /**
     * Tokenizes "foo,bar,zot,-bar" and returns "foo,zot" (the token that starts with '-' is handled as
     * a cancellation.
     */
    private List<String> listProperties(String properties) {
        List<String> props = new ArrayList<String>(Arrays.asList(properties.split(",")));
        for (String p : props.toArray(new String[props.size()])) {
            if (p.startsWith("-")) {
                props.remove(p);
                props.remove(p.substring(1));
            }
        }
        return props;
    }

    /**
     * Submits the form.
     *
     * Plain {@link com.gargoylesoftware.htmlunit.html.HtmlForm#submit()} doesn't work correctly due to the use of YUI in Hudson.
     */
    public HtmlPage submit(HtmlForm form) throws Exception {
        return (HtmlPage) form.submit((HtmlButton) last(form.getHtmlElementsByTagName("button")));
    }

    /**
     * Submits the form by clikcing the submit button of the given name.
     *
     * @param name
     *      This corresponds to the @name of &lt;f:submit />
     */
    public HtmlPage submit(HtmlForm form, String name) throws Exception {
        for( HtmlElement e : form.getHtmlElementsByTagName("button")) {
            HtmlElement p = (HtmlElement)e.getParentNode().getParentNode();
            if(p.getAttribute("name").equals(name)) {
                // To make YUI event handling work, this combo seems to be necessary
                // the click will trigger _onClick in buton-*.js, but it doesn't submit the form
                // (a comment alluding to this behavior can be seen in submitForm method)
                // so to complete it, submit the form later.
                //
                // Just doing form.submit() doesn't work either, because it doesn't do
                // the preparation work needed to pass along the name of the button that
                // triggered a submission (more concretely, m_oSubmitTrigger is not set.)
                ((HtmlButton)e).click();
                return (HtmlPage)form.submit((HtmlButton)e);
            }
        }
        throw new AssertionError("No such submit button with the name "+name);
    }

    public HtmlInput findPreviousInputElement(HtmlElement current, String name) {
        return (HtmlInput)current.selectSingleNode("(preceding::input[@name='_."+name+"'])[last()]");
    }

    public HtmlButton getButtonByCaption(HtmlForm f, String s) {
        for (HtmlElement b : f.getHtmlElementsByTagName("button")) {
            if(b.getTextContent().trim().equals(s))
                return (HtmlButton)b;
        }
        return null;
    }

    /**
     * Creates a {@link TaskListener} connected to stdout.
     */
    public TaskListener createTaskListener() {
        return new StreamTaskListener(new CloseProofOutputStream(System.out));
    }

    /**
     * Asserts that two JavaBeans are equal as far as the given list of properties are concerned.
     *
     * <p>
     * This method takes two objects that have properties (getXyz, isXyz, or just the public xyz field),
     * and makes sure that the property values for each given property are equals (by using {@link org.junit.Assert#assertThat(Object, org.hamcrest.Matcher)})
     *
     * <p>
     * Property values can be null on both objects, and that is OK, but passing in a property that doesn't
     * exist will fail an assertion.
     *
     * <p>
     * This method is very convenient for comparing a large number of properties on two objects,
     * for example to verify that the configuration is identical after a config screen roundtrip.
     *
     * @param lhs
     *      One of the two objects to be compared.
     * @param rhs
     *      The other object to be compared
     * @param properties
     *      ','-separated list of property names that are compared.
     * @since 1.297
     */
    public void assertEqualBeans(Object lhs, Object rhs, String properties) throws Exception {
        assertThat("LHS", lhs, notNullValue());
        assertThat("RHS", rhs, notNullValue());
        for (String p : properties.split(",")) {
            PropertyDescriptor pd = PropertyUtils.getPropertyDescriptor(lhs, p);
            Object lp,rp;
            if(pd==null) {
                // field?
                try {
                    Field f = lhs.getClass().getField(p);
                    lp = f.get(lhs);
                    rp = f.get(rhs);
                } catch (NoSuchFieldException e) {
                    assertThat("No such property " + p + " on " + lhs.getClass(), pd, notNullValue());
                    return;
                }
            } else {
                lp = PropertyUtils.getProperty(lhs, p);
                rp = PropertyUtils.getProperty(rhs, p);
            }

            if (lp!=null && rp!=null && lp.getClass().isArray() && rp.getClass().isArray()) {
                // deep array equality comparison
                int m = Array.getLength(lp);
                int n = Array.getLength(rp);
                assertThat("Array length is different for property " + p, n, is(m));
                for (int i=0; i<m; i++)
                    assertThat(p + "[" + i + "] is different", Array.get(rp, i), is(Array.get(lp,i)));
                return;
            }

            assertThat("Property " + p + " is different", rp, is(lp));
        }
    }

    public void setQuietPeriod(int qp) {
        JenkinsAdaptor.setQuietPeriod(jenkins, qp);
    }

    /**
     * Works like {@link #assertEqualBeans(Object, Object, String)} but figure out the properties
     * via {@link org.kohsuke.stapler.DataBoundConstructor}
     */
    public void assertEqualDataBoundBeans(Object lhs, Object rhs) throws Exception {
        if (lhs==null && rhs==null)     return;
        if (lhs==null)      fail("lhs is null while rhs="+rhs);
        if (rhs==null)      fail("rhs is null while lhs="+lhs);

        Constructor<?> lc = findDataBoundConstructor(lhs.getClass());
        Constructor<?> rc = findDataBoundConstructor(rhs.getClass());
        assertThat("Data bound constructor mismatch. Different type?", (Constructor)rc, is((Constructor)lc));

        List<String> primitiveProperties = new ArrayList<String>();

        String[] names = ClassDescriptor.loadParameterNames(lc);
        Class<?>[] types = lc.getParameterTypes();
        assertThat(types.length, is(names.length));
        for (int i=0; i<types.length; i++) {
            Object lv = ReflectionUtils.getPublicProperty(lhs, names[i]);
            Object rv = ReflectionUtils.getPublicProperty(rhs, names[i]);

            if (Iterable.class.isAssignableFrom(types[i])) {
                Iterable lcol = (Iterable) lv;
                Iterable rcol = (Iterable) rv;
                Iterator ltr,rtr;
                for (ltr=lcol.iterator(), rtr=rcol.iterator(); ltr.hasNext() && rtr.hasNext();) {
                    Object litem = ltr.next();
                    Object ritem = rtr.next();

                    if (findDataBoundConstructor(litem.getClass())!=null) {
                        assertEqualDataBoundBeans(litem,ritem);
                    } else {
                        assertThat(ritem, is(litem));
                    }
                }
                assertThat("collection size mismatch between " + lhs + " and " + rhs, ltr.hasNext() ^ rtr.hasNext(),
                        is(false));
            } else
            if (findDataBoundConstructor(types[i])!=null || (lv!=null && findDataBoundConstructor(lv.getClass())!=null) || (rv!=null && findDataBoundConstructor(rv.getClass())!=null)) {
                // recurse into nested databound objects
                assertEqualDataBoundBeans(lv,rv);
            } else {
                primitiveProperties.add(names[i]);
            }
        }

        // compare shallow primitive properties
        if (!primitiveProperties.isEmpty())
            assertEqualBeans(lhs,rhs,Util.join(primitiveProperties,","));
    }

    /**
     * Makes sure that two collections are identical via {@link #assertEqualDataBoundBeans(Object, Object)}
     */
    public void assertEqualDataBoundBeans(List<?> lhs, List<?> rhs) throws Exception {
        assertThat(rhs.size(), is(lhs.size()));
        for (int i=0; i<lhs.size(); i++)
            assertEqualDataBoundBeans(lhs.get(i),rhs.get(i));
    }

    public Constructor<?> findDataBoundConstructor(Class<?> c) {
        for (Constructor<?> m : c.getConstructors()) {
            if (m.getAnnotation(DataBoundConstructor.class)!=null)
                return m;
        }
        return null;
    }

    /**
     * Gets the descriptor instance of the current Hudson by its type.
     */
    public <T extends Descriptor<?>> T get(Class<T> d) {
        return jenkins.getDescriptorByType(d);
    }


    /**
     * Returns true if Hudson is building something or going to build something.
     */
    public boolean isSomethingHappening() {
        if (!jenkins.getQueue().isEmpty())
            return true;
        for (Computer n : jenkins.getComputers())
            if (!n.isIdle())
                return true;
        return false;
    }

    /**
     * Waits until Hudson finishes building everything, including those in the queue.
     * <p>
     * This method uses a default time out to prevent infinite hang in the automated test execution environment.
     */
    public void waitUntilNoActivity() throws Exception {
        waitUntilNoActivityUpTo(60*1000);
    }

    /**
     * Waits until Hudson finishes building everything, including those in the queue, or fail the test
     * if the specified timeout milliseconds is
     */
    public void waitUntilNoActivityUpTo(int timeout) throws Exception {
        long startTime = System.currentTimeMillis();
        int streak = 0;

        while (true) {
            Thread.sleep(10);
            if (isSomethingHappening())
                streak=0;
            else
                streak++;

            if (streak>5)   // the system is quiet for a while
                return;

            if (System.currentTimeMillis()-startTime > timeout) {
                List<Queue.Executable> building = new ArrayList<Queue.Executable>();
                for (Computer c : jenkins.getComputers()) {
                    for (Executor e : c.getExecutors()) {
                        if (e.isBusy())
                            building.add(e.getCurrentExecutable());
                    }
                }
                throw new AssertionError(String.format("Hudson is still doing something after %dms: queue=%s building=%s",
                        timeout, Arrays.asList(jenkins.getQueue().getItems()), building));
            }
        }
    }


//
// recipe methods. Control the test environments.
//

    /**
     * Called during the {@link #before()} to give a test case an opportunity to
     * control the test environment in which Hudson is run.
     *
     * <p>
     * One could override this method and call a series of {@code withXXX} methods,
     * or you can use the annotations with {@link Recipe} meta-annotation.
     */
    public void recipe() throws Exception {
        recipeLoadCurrentPlugin();
        // look for recipe meta-annotation
        try {
            for (final Annotation a : testDescription.getAnnotations()) {
                JenkinsRecipe r = a.annotationType().getAnnotation(JenkinsRecipe.class);
                if(r==null)     continue;
                final JenkinsRecipe.Runner runner = r.value().newInstance();
                recipes.add(runner);
                tearDowns.add(new LenientRunnable() {
                    public void run() throws Exception {
                        runner.tearDown(JenkinsRule.this,a);
                    }
                });
                runner.setup(this,a);
            }
        } catch (NoSuchMethodException e) {
            // not a plain JUnit test.
        }
    }

    /**
     * If this test harness is launched for a Jenkins plugin, locate the <tt>target/test-classes/the.jpl</tt>
     * and add a recipe to install that to the new Jenkins.
     *
     * <p>
     * This file is created by <tt>maven-hpi-plugin</tt> at the testCompile phase when the current
     * packaging is <tt>jpi</tt>.
     */
    public void recipeLoadCurrentPlugin() throws Exception {
    	final Enumeration<URL> jpls = getClass().getClassLoader().getResources("the.jpl");
        final Enumeration<URL> hpls = getClass().getClassLoader().getResources("the.hpl");

        final List<URL> all = Collections.list(jpls);
        all.addAll(Collections.list(hpls));
        
        if(all.isEmpty())    return; // nope
        
        recipes.add(new JenkinsRecipe.Runner() {
            @Override
            public void decorateHome(JenkinsRule testCase, File home) throws Exception {
            	
            	for (URL hpl : all) {

                    // make the plugin itself available
                    Manifest m = new Manifest(hpl.openStream());
                    String shortName = m.getMainAttributes().getValue("Short-Name");
                    if(shortName==null)
                        throw new Error(hpl+" doesn't have the Short-Name attribute");
                    FileUtils.copyURLToFile(hpl, new File(home, "plugins/" + shortName + ".jpl"));

                    // make dependency plugins available
                    // TODO: probably better to read POM, but where to read from?
                    // TODO: this doesn't handle transitive dependencies

                    // Tom: plugins are now searched on the classpath first. They should be available on
                    // the compile or test classpath. As a backup, we do a best-effort lookup in the Maven repository
                    // For transitive dependencies, we could evaluate Plugin-Dependencies transitively.

                    String dependencies = m.getMainAttributes().getValue("Plugin-Dependencies");
                    if(dependencies!=null) {
                        MavenEmbedder embedder = MavenUtil
                                .createEmbedder(new StreamTaskListener(System.out, Charset.defaultCharset()),
                                        (File) null, null);
                        for( String dep : dependencies.split(",")) {
                            String[] tokens = dep.split(":");
                            String artifactId = tokens[0];
                            String version = tokens[1];
                            File dependencyJar=resolveDependencyJar(embedder,artifactId,version);

                            File dst = new File(home, "plugins/" + artifactId + ".jpi");
                            if(!dst.exists() || dst.lastModified()!=dependencyJar.lastModified()) {
                                FileUtils.copyFile(dependencyJar, dst);
                            }
                        }
                    }
                }
            }

            private File resolveDependencyJar(MavenEmbedder embedder, String artifactId, String version) throws Exception {
                // try to locate it from manifest
                Enumeration<URL> manifests = getClass().getClassLoader().getResources("META-INF/MANIFEST.MF");
                while (manifests.hasMoreElements()) {
                    URL manifest = manifests.nextElement();
                    InputStream is = manifest.openStream();
                    Manifest m = new Manifest(is);
                    is.close();

                    if (artifactId.equals(m.getMainAttributes().getValue("Short-Name")))
                        return Which.jarFile(manifest);
                }

                // need to search multiple group IDs
                // TODO: extend manifest to include groupID:artifactID:version
                Exception resolutionError=null;
                for (String groupId : new String[]{"org.jvnet.hudson.plugins","org.jvnet.hudson.main"}) {

                    // first try to find it on the classpath.
                    // this takes advantage of Maven POM located in POM
                    URL dependencyPomResource = getClass().getResource("/META-INF/maven/"+groupId+"/"+artifactId+"/pom.xml");
                    if (dependencyPomResource != null) {
                        // found it
                        return Which.jarFile(dependencyPomResource);
                    } else {

                    	try {
                    		// currently the most of the plugins are still hpi
                            return resolvePluginFile(embedder, artifactId, version, groupId, "hpi");
                    	} catch(AbstractArtifactResolutionException x){
                    		try {
                    			// but also try with the new jpi
                    		    return resolvePluginFile(embedder, artifactId, version, groupId, "jpi");
                    		} catch(AbstractArtifactResolutionException x2){
                                // could be a wrong groupId
                                resolutionError = x;
                    		}
                    	}

                    }
                }

                throw new Exception("Failed to resolve plugin: "+artifactId+" version "+version,resolutionError);
            }
            
            private File resolvePluginFile(MavenEmbedder embedder, String artifactId, String version, String groupId, String type)
					throws MavenEmbedderException, ComponentLookupException, AbstractArtifactResolutionException {
				final Artifact jpi = embedder.createArtifact(groupId, artifactId, version, "compile"/*doesn't matter*/, type);
				embedder.resolve(jpi, Arrays.asList(embedder.createRepository("http://maven.glassfish.org/content/groups/public/","repo")),embedder.getLocalRepository());
                if (jpi.getFile() == null) {
                    throw new ArtifactNotFoundException("cannot find plugin dependency", jpi);
                }
				return jpi.getFile();
				
			}
        });
    }

    public JenkinsRule withNewHome() {
        return with(HudsonHomeLoader.NEW);
    }

    public JenkinsRule withExistingHome(File source) throws Exception {
        return with(new HudsonHomeLoader.CopyExisting(source));
    }

    /**
     * Declares that this test case expects to start with one of the preset data sets.
     * See {@code test/src/main/preset-data/}
     * for available datasets and what they mean.
     */
    public JenkinsRule withPresetData(String name) {
        name = "/" + name + ".zip";
        URL res = getClass().getResource(name);
        if(res==null)   throw new IllegalArgumentException("No such data set found: "+name);

        return with(new HudsonHomeLoader.CopyExisting(res));
    }

    public JenkinsRule with(HudsonHomeLoader homeLoader) {
        this.homeLoader = homeLoader;
        return this;
    }


    /**
     * Executes the given closure on the server, by the servlet request handling thread,
     * in the context of an HTTP request.
     *
     * <p>
     * In {@link HudsonTestCase}, a thread that's executing the test code is different from the thread
     * that carries out HTTP requests made through {@link WebClient}. But sometimes you want to
     * make assertions and other calls with side-effect from within the request handling thread.
     *
     * <p>
     * This method allows you to do just that. It is useful for testing some methods that
     * require {@link org.kohsuke.stapler.StaplerRequest} and {@link org.kohsuke.stapler.StaplerResponse}, or getting the credential
     * of the current user (via {@link jenkins.model.Jenkins#getAuthentication()}, and so on.
     *
     * @param c
     *      The closure to be executed on the server.
     * @return
     *      The return value from the closure.
     * @throws Exception
     *      If a closure throws any exception, that exception will be carried forward.
     */
    public <V> V executeOnServer(Callable<V> c) throws Exception {
        return createWebClient().executeOnServer(c);
    }

    /**
     * Sometimes a part of a test case may ends up creeping into the serialization tree of {@link hudson.model.Saveable#save()},
     * so detect that and flag that as an error.
     */
    private Object writeReplace() {
        throw new AssertionError("JenkinsRule " + testDescription.getDisplayName() + " is not supposed to be serialized");
    }

    /**
     * This is to assist Groovy test clients who are incapable of instantiating the inner classes properly.
     */
    public WebClient createWebClient() {
        return new WebClient();
    }

    /**
     * Extends {@link com.gargoylesoftware.htmlunit.WebClient} and provide convenience methods
     * for accessing Hudson.
     */
    public class WebClient extends com.gargoylesoftware.htmlunit.WebClient {
        private static final long serialVersionUID = 5808915989048338267L;

        public WebClient() {
            // default is IE6, but this causes 'n.doScroll('left')' to fail in event-debug.js:1907 as HtmlUnit doesn't implement such a method,
            // so trying something else, until we discover another problem.
            super(BrowserVersion.FIREFOX_2);

//            setJavaScriptEnabled(false);
            setPageCreator(HudsonPageCreator.INSTANCE);
            clients.add(this);
            // make ajax calls run as post-action for predictable behaviors that simplify debugging
            setAjaxController(new AjaxController() {
                private static final long serialVersionUID = -5844060943564822678L;
                public boolean processSynchron(HtmlPage page, WebRequestSettings settings, boolean async) {
                    return false;
                }
            });

            setCssErrorHandler(new ErrorHandler() {
                final ErrorHandler defaultHandler = new DefaultCssErrorHandler();

                public void warning(CSSParseException exception) throws CSSException {
                    if (!ignore(exception))
                        defaultHandler.warning(exception);
                }

                public void error(CSSParseException exception) throws CSSException {
                    if (!ignore(exception))
                        defaultHandler.error(exception);
                }

                public void fatalError(CSSParseException exception) throws CSSException {
                    if (!ignore(exception))
                        defaultHandler.fatalError(exception);
                }

                private boolean ignore(CSSParseException e) {
                    return e.getURI().contains("/yui/");
                }
            });

            // if no other debugger is installed, install jsDebugger,
            // so as not to interfere with the 'Dim' class.
            getJavaScriptEngine().getContextFactory().addListener(new ContextFactory.Listener() {
                public void contextCreated(Context cx) {
                    if (cx.getDebugger() == null)
                        cx.setDebugger(jsDebugger, null);
                }

                public void contextReleased(Context cx) {
                }
            });

            // avoid a hang by setting a time out. It should be long enough to prevent
            // false-positive timeout on slow systems
            setTimeout(60*1000);
        }

        /**
         * Logs in to Hudson.
         */
        public WebClient login(String username, String password) throws Exception {
            HtmlPage page = goTo("/login");
//            page = (HtmlPage) page.getFirstAnchorByText("Login").click();

            HtmlForm form = page.getFormByName("login");
            form.getInputByName("j_username").setValueAttribute(username);
            form.getInputByName("j_password").setValueAttribute(password);
            form.submit(null);
            return this;
        }

        /**
         * Logs in to Hudson, by using the user name as the password.
         *
         * <p>
         * See {@link HudsonTestCase#configureUserRealm()} for how the container is set up with the user names
         * and passwords. All the test accounts have the same user name and password.
         */
        public WebClient login(String username) throws Exception {
            login(username,username);
            return this;
        }

        /**
         * Executes the given closure on the server, by the servlet request handling thread,
         * in the context of an HTTP request.
         *
         * <p>
         * In {@link HudsonTestCase}, a thread that's executing the test code is different from the thread
         * that carries out HTTP requests made through {@link WebClient}. But sometimes you want to
         * make assertions and other calls with side-effect from within the request handling thread.
         *
         * <p>
         * This method allows you to do just that. It is useful for testing some methods that
         * require {@link org.kohsuke.stapler.StaplerRequest} and {@link org.kohsuke.stapler.StaplerResponse}, or getting the credential
         * of the current user (via {@link jenkins.model.Jenkins#getAuthentication()}, and so on.
         *
         * @param c
         *      The closure to be executed on the server.
         * @return
         *      The return value from the closure.
         * @throws Exception
         *      If a closure throws any exception, that exception will be carried forward.
         */
        public <V> V executeOnServer(final Callable<V> c) throws Exception {
            final Exception[] t = new Exception[1];
            final List<V> r = new ArrayList<V>(1);  // size 1 list

            ClosureExecuterAction cea = jenkins.getExtensionList(RootAction.class).get(ClosureExecuterAction.class);
            UUID id = UUID.randomUUID();
            cea.add(id,new Runnable() {
                public void run() {
                    try {
                        StaplerResponse rsp = Stapler.getCurrentResponse();
                        rsp.setStatus(200);
                        rsp.setContentType("text/html");
                        r.add(c.call());
                    } catch (Exception e) {
                        t[0] = e;
                    }
                }
            });
            goTo("closures/?uuid="+id);

            if (t[0]!=null)
                throw t[0];
            return r.get(0);
        }

        public HtmlPage search(String q) throws IOException, SAXException {
            HtmlPage top = goTo("");
            HtmlForm search = top.getFormByName("search");
            search.getInputByName("q").setValueAttribute(q);
            return (HtmlPage)search.submit(null);
        }

        /**
         * Short for {@code getPage(r,"")}, to access the top page of a build.
         */
        public HtmlPage getPage(Run r) throws IOException, SAXException {
            return getPage(r,"");
        }

        /**
         * Accesses a page inside {@link Run}.
         *
         * @param relative
         *      Relative URL within the build URL, like "changes". Doesn't start with '/'. Can be empty.
         */
        public HtmlPage getPage(Run r, String relative) throws IOException, SAXException {
            return goTo(r.getUrl()+relative);
        }

        public HtmlPage getPage(Item item) throws IOException, SAXException {
            return getPage(item,"");
        }

        public HtmlPage getPage(Item item, String relative) throws IOException, SAXException {
            return goTo(item.getUrl()+relative);
        }

        public HtmlPage getPage(Node item) throws IOException, SAXException {
            return getPage(item,"");
        }

        public HtmlPage getPage(Node item, String relative) throws IOException, SAXException {
            return goTo(item.toComputer().getUrl()+relative);
        }

        public HtmlPage getPage(View view) throws IOException, SAXException {
            return goTo(view.getUrl());
        }

        public HtmlPage getPage(View view, String relative) throws IOException, SAXException {
            return goTo(view.getUrl()+relative);
        }

        /**
         * @deprecated
         *      This method expects a full URL. This method is marked as deprecated to warn you
         *      that you probably should be using {@link #goTo(String)} method, which accepts
         *      a relative path within the Hudson being tested. (IOW, if you really need to hit
         *      a website on the internet, there's nothing wrong with using this method.)
         */
        @SuppressWarnings("unchecked")
        @Override
        public Page getPage(String url) throws IOException, FailingHttpStatusCodeException {
            return super.getPage(url);
        }

        /**
         * Requests a page within Hudson.
         *
         * @param relative
         *      Relative path within Hudson. Starts without '/'.
         *      For example, "job/test/" to go to a job top page.
         */
        public HtmlPage goTo(String relative) throws IOException, SAXException {
            Page p = goTo(relative, "text/html");
            if (p instanceof HtmlPage) {
                return (HtmlPage) p;
            } else {
                throw new AssertionError("Expected text/html but instead the content type was "+p.getWebResponse().getContentType());
            }
        }

        public Page goTo(String relative, String expectedContentType) throws IOException, SAXException {
            Page p = super.getPage(getContextPath() + relative);
            assertThat(p.getWebResponse().getContentType(), is(expectedContentType));
            return p;
        }

        /** Loads a page as XML. Useful for testing Hudson's xml api, in concert with
         * assertXPath(DomNode page, String xpath)
         * @param path   the path part of the url to visit
         * @return  the XmlPage found at that url
         * @throws IOException
         * @throws SAXException
         */
        public XmlPage goToXml(String path) throws IOException, SAXException {
            Page page = goTo(path, "application/xml");
            if (page instanceof XmlPage)
                return (XmlPage) page;
            else
                return null;
        }

        /**
         * Verify that the server rejects an attempt to load the given page.
         * @param url a URL path (relative to Jenkins root)
         * @param statusCode the expected failure code (such as {@link HttpURLConnection#HTTP_FORBIDDEN})
         * @since 1.504
         */
        public void assertFails(String url, int statusCode) throws Exception {
            try {
                fail(url + " should have been rejected but produced: " + super.getPage(getContextPath() + url).getWebResponse().getContentAsString());
            } catch (FailingHttpStatusCodeException x) {
                assertEquals(statusCode, x.getStatusCode());
            }
        }

        /**
         * Returns the URL of the webapp top page.
         * URL ends with '/'.
         */
        public String getContextPath() throws IOException {
            return getURL().toExternalForm();
        }

        /**
         * Adds a security crumb to the quest
         */
        public WebRequestSettings addCrumb(WebRequestSettings req) {
            NameValuePair crumb[] = { new NameValuePair() };

            crumb[0].setName(jenkins.getCrumbIssuer().getDescriptor().getCrumbRequestField());
            crumb[0].setValue(jenkins.getCrumbIssuer().getCrumb( null ));

            req.setRequestParameters(Arrays.asList( crumb ));
            return req;
        }

        /**
         * Creates a URL with crumb parameters relative to {{@link #getContextPath()}
         */
        public URL createCrumbedUrl(String relativePath) throws IOException {
            CrumbIssuer issuer = jenkins.getCrumbIssuer();
            String crumbName = issuer.getDescriptor().getCrumbRequestField();
            String crumb = issuer.getCrumb(null);

            return new URL(getContextPath()+relativePath+"?"+crumbName+"="+crumb);
        }

        /**
         * Makes an HTTP request, process it with the given request handler, and returns the response.
         */
        public HtmlPage eval(final Runnable requestHandler) throws IOException, SAXException {
            ClosureExecuterAction cea = jenkins.getExtensionList(RootAction.class).get(ClosureExecuterAction.class);
            UUID id = UUID.randomUUID();
            cea.add(id,requestHandler);
            return goTo("closures/?uuid="+id);
        }

        /**
         * Starts an interactive JavaScript debugger, and break at the next JavaScript execution.
         *
         * <p>
         * This is useful during debugging a test so that you can step execute and inspect state of JavaScript.
         * This will launch a Swing GUI, and the method returns immediately.
         *
         * <p>
         * Note that installing a debugger appears to make an execution of JavaScript substantially slower.
         *
         * <p>
         * TODO: because each script block evaluation in HtmlUnit is done in a separate Rhino context,
         * if you step over from one script block, the debugger fails to kick in on the beginning of the next script
         * block.
         * This makes it difficult to set a break point on arbitrary script block in the HTML page. We need to fix this
         * by tweaking {@link org.mozilla.javascript.tools.debugger.Dim.StackFrame#onLineChange(Context, int)}.
         */
        public Dim interactiveJavaScriptDebugger() {
            Global global = new Global();
            HtmlUnitContextFactory cf = getJavaScriptEngine().getContextFactory();
            global.init(cf);

            Dim dim = org.mozilla.javascript.tools.debugger.Main.mainEmbedded(cf, global, "Rhino debugger: " + testDescription.getDisplayName());

            // break on exceptions. this catch most of the errors
            dim.setBreakOnExceptions(true);

            return dim;
        }
    }

    // needs to keep reference, or it gets GC-ed.
    private static final Logger XML_HTTP_REQUEST_LOGGER = Logger.getLogger(XMLHttpRequest.class.getName());

    static {
        // screen scraping relies on locale being fixed.
        Locale.setDefault(Locale.ENGLISH);

        {// enable debug assistance, since tests are often run from IDE
            Dispatcher.TRACE = true;
            MetaClass.NO_CACHE=true;
            // load resources from the source dir.
            File dir = new File("src/main/resources");
            if(dir.exists() && MetaClassLoader.debugLoader==null)
                try {
                    MetaClassLoader.debugLoader = new MetaClassLoader(
                        new URLClassLoader(new URL[]{dir.toURI().toURL()}));
                } catch (MalformedURLException e) {
                    throw new AssertionError(e);
                }
        }

        // suppress INFO output from Spring, which is verbose
        Logger.getLogger("org.springframework").setLevel(Level.WARNING);

        // hudson-behavior.js relies on this to decide whether it's running unit tests.
        Main.isUnitTest = true;

        // prototype.js calls this method all the time, so ignore this warning.
        XML_HTTP_REQUEST_LOGGER.setFilter(new Filter() {
            public boolean isLoggable(LogRecord record) {
                return !record.getMessage().contains("XMLHttpRequest.getResponseHeader() was called before the respon"
                        + "se was available.");
            }
        });

        // remove the upper bound of the POST data size in Jetty.
        System.setProperty("org.mortbay.jetty.Request.maxFormContentSize","-1");
    }

    private static final Logger LOGGER = Logger.getLogger(HudsonTestCase.class.getName());

    public static final List<ToolProperty<?>> NO_PROPERTIES = Collections.<ToolProperty<?>>emptyList();

    /**
     * Specify this to a TCP/IP port number to have slaves started with the debugger.
     */
    public static int SLAVE_DEBUG_PORT = Integer.getInteger(HudsonTestCase.class.getName()+".slaveDebugPort",-1);

    public static final MimeTypes MIME_TYPES = new MimeTypes();
    static {
        MIME_TYPES.addMimeMapping("js","application/javascript");
        Functions.DEBUG_YUI = true;

        // during the unit test, predictably releasing classloader is important to avoid
        // file descriptor leak.
        ClassicPluginStrategy.useAntClassLoader = true;

        // DNS multicast support takes up a lot of time during tests, so just disable it altogether
        // this also prevents tests from falsely advertising Hudson
        DNSMultiCast.disabled = true;

        if (!Functions.isWindows()) {
            try {
                GNUCLibrary.LIBC.unsetenv("MAVEN_OPTS");
                GNUCLibrary.LIBC.unsetenv("MAVEN_DEBUG_OPTS");
            } catch (Exception e) {
                LOGGER.log(Level.WARNING,"Failed to cancel out MAVEN_OPTS",e);
            }
        }
    }

    public static class TestBuildWrapper extends BuildWrapper {
        public Result buildResultInTearDown;

        @Override
        public Environment setUp(AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {
            return new BuildWrapper.Environment() {
                @Override
                public boolean tearDown(AbstractBuild build, BuildListener listener) throws IOException, InterruptedException {
                    buildResultInTearDown = build.getResult();
                    return true;
                }
            };
        }

        @Extension
        public static class TestBuildWrapperDescriptor extends BuildWrapperDescriptor {
            @Override
            public boolean isApplicable(AbstractProject<?, ?> project) {
                return true;
            }

            @Override
            public BuildWrapper newInstance(StaplerRequest req, JSONObject formData) {
                throw new UnsupportedOperationException();
            }

            @Override
            public String getDisplayName() {
                return this.getClass().getName();
            }
        }
    }

    public Description getTestDescription() {
        return testDescription;
    }
}
