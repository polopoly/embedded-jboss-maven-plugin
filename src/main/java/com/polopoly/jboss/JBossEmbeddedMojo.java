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
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.mojo.jboss.StartAndWaitMojo;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.archiver.zip.ZipUnArchiver;
import org.codehaus.plexus.util.Expand;

import java.io.File;

/**
 * Goal which touches a timestamp file.
 *
 * @extendsPlugin jboss
 * @extendsGoal start-and-wait
 *
 * @goal run
 * 
 * @phase process-sources
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
     * @required
     */
    protected JBossArtifact jbossDistribution = null;

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

    /** @component */
    private ArchiverManager archiverManager;

    @Override
    public void execute()
        throws MojoExecutionException
    {
        super.jbossHome = jbossHome;
        if (!jbossHome.exists()) {
            try {
                Artifact artifact =
                        factory.createBuildArtifact(jbossDistribution.groupId,
                                                    jbossDistribution.artifactId,
                                                    jbossDistribution.version,
                                                    "zip");
                resolver.resolve(artifact, project.getRemoteArtifactRepositories(), localRepository);

                getLog().info("Installing JBoss to '" + jbossHome + "'");
                Expand expander = new Expand();
                expander.setDest(jbossHome);
                expander.setSrc(artifact.getFile());
                expander.execute();

                new File(jbossHome, "bin/run.sh").setExecutable(true);

            } catch (Exception e) {
                throw new MojoExecutionException("Unable to download jboss distribution:"  + jbossDistribution, e);
            }
        }
        super.execute();

        try {
            Thread.sleep(99999999);
        } catch (InterruptedException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }
}