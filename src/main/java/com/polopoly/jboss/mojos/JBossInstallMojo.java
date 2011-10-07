package com.polopoly.jboss.mojos;

import com.polopoly.jboss.JBossDistribution;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.Expand;

import java.io.File;

/**
 * Created by bitter on 2011-10-07
 *
 * @goal install
 */
public class JBossInstallMojo extends AbstractMojo {

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
     * Force re-installation of jboss
     *
     * @parameter expression="${jboss.reinstall}"
     */
    protected boolean reinstall;

    /**
     * The local repository where the artifacts are located.
     *
     * @parameter expression="${localRepository}"
     */
    private ArtifactRepository localRepository;


    public void execute() throws MojoExecutionException, MojoFailureException {
        if (reinstall || !jbossHome.exists()) {
            try {
                performJBossInstallation(jbossHome, jbossDistribution);

            } catch (Exception e) {
                throw new MojoExecutionException("Unable to download jboss distribution:"  + jbossDistribution, e);
            }
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

    // -----------------------------------------
    // Components
    // -----------------------------------------
    /**
      * The Maven project object.
      *
      * @parameter expression="${project}"
      * @required
      * @readonly
      */
    private MavenProject project;

    /** @component */
    private ArtifactResolver resolver;

    /** @component */
    private ArtifactFactory factory;
}
