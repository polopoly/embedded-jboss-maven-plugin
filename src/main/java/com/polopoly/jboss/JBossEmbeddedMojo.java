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
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.util.Expand;

import java.io.File;

/**
 * Goal which touches a timestamp file.
 *
 * @extendsPlugin jboss
 * @extendsGoal start-and-wait
 *
 * @goal run
 */
public class JBossEmbeddedMojo extends StartAndWaitMojo
{
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