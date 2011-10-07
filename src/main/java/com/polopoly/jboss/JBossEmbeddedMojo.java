package com.polopoly.jboss;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.mojo.jboss.StartAndWaitMojo;
import org.codehaus.mojo.jboss.StartMojo;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.util.Expand;

import javax.management.MBeanServerConnection;
import java.io.File;

/**
 * Goal which touches a timestamp file.
 *
 * @extendsPlugin jboss
 * @extendsGoal start
 *
 * @goal run
 */
public class JBossEmbeddedMojo extends StartMojo
{
    private static final long ONE_SECOND = 1 * 1000;

    /**
     * Maximum number of retries to get JBoss JMX MBean connection.
     *
     * @parameter default-value="4" expression="${jboss.retry}"
     */
    protected int retry;

    /**
     * Wait in ms before each retry of the JBoss JMX MBean connection.
     *
     * @parameter default-value="5000" expression="${jboss.retryWait}"
     */
    protected int retryWait;

    /**
     * Time in ms to start the application server (once JMX MBean connection has been reached).
     *
     * @parameter default-value="20000" expression="${jboss.timeout}"
     */
    protected int timeout;

    /**
     * The port for the naming service.
     *
     * @parameter default-value="1099" expression="${jboss.namingPort}"
     */
    protected String namingPort;

    /**
     * The location of JBoss Home.
     *
     * @parameter expression="${project.build.directory}/embedded-jboss"
     * @required
     */
    protected File jbossHome;

    /**
     * The jboss distribution.
     * @parameter
     */
    protected JBossDistribution jbossDistribution = JBossDistribution.DEFAULT_JBOSS_DISTRIUTION;

    /**
     * Artifact Deployments
     * @parameter
     */
    protected ArtifactData[] artifactDeployments = new ArtifactData[0];

    /**
     * The Maven project object.
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * The local repository where the artifacts are located.
     *
     * @parameter expression="${localRepository}"
     */
    private ArtifactRepository localRepository;

    /** @component */
    private ArtifactResolver resolver;

    /** @component */
    private ArtifactFactory factory;

    @Override
    public void execute()
        throws MojoExecutionException
    {
        super.jbossHome = jbossHome;
        if (!jbossHome.exists()) {
            try {
                performJBossInstallation(jbossHome, jbossDistribution);

            } catch (Exception e) {
                throw new MojoExecutionException("Unable to download jboss distribution:"  + jbossDistribution, e);
            }
        }
        super.execute();

        getLog().info("Establishing JBoss Connection");
        JBossOperator operator = new JBossOperator(new Integer(namingPort), getUsername(), getPassword(), retry, retryWait/1000, getLog());
        JBossOperations operations = new JBossOperations(operator.connect());

        getLog().info("Checking JBoss State");
        // Wait until server startup is complete
        boolean started = false;
        long startTime = System.currentTimeMillis();
        while (!started && System.currentTimeMillis() - startTime < timeout)
        {
            try
            {
                Thread.sleep(ONE_SECOND);
                started = operations.isStarted();
            }
            catch (Exception e)
            {
                throw new MojoExecutionException("Unable to wait: " + e.getMessage(), e);
            }
        }
        if (!started)
        {
            throw new MojoExecutionException("JBoss AS is not stared before timeout has expired!");
        }
        getLog().info("JBoss started!");


        try {
            Thread.sleep(99999999);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void performJBossInstallation(File target, JBossDistribution jboss)
            throws ArtifactResolutionException, ArtifactNotFoundException, MojoExecutionException
    {
        // Resolve jboss
        Artifact artifact =
                factory.createBuildArtifact(jboss.groupId,
                                            jboss.artifactId,
                                            jboss.version,
                                            jboss.packaging);
        resolver.resolve(artifact, project.getRemoteArtifactRepositories(), localRepository);

        // Install jboss
        getLog().info("Installing JBoss to '" + target + "'");
        Expand expander = new Expand();
        expander.setDest(target);
        expander.setSrc(artifact.getFile());
        try {
            expander.execute();
        } catch (Exception e) {
            throw new MojoExecutionException("Unable to expand jboss archive", e);
        }

        // Make sure the execution flag is lit
        new File(target, "bin/run.sh").setExecutable(true);
    }
}