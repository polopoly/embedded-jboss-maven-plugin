package com.polopoly.jboss.mojos;

import com.polopoly.jboss.AbstractJBossMojo;
import com.polopoly.jboss.JBossDistribution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.Expand;

import java.io.File;

/**
 * Created by bitter on 2011-10-07
 *
 * @goal install
 */
public class JBossInstallMojo extends AbstractJBossMojo {

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
     * The jboss distribution. If specified the takes precedence over jbossDistribution.
     * @parameter
     */
    protected File jbossDistributionFile;

    /**
     * Force re-installation of jboss
     *
     * @parameter expression="${jboss.reinstall}"
     */
    protected boolean reinstall;


    public void execute() throws MojoExecutionException, MojoFailureException {
        if (reinstall || !jbossHome.exists()) {
            try {
                if (jbossDistributionFile == null) {
                    jbossDistributionFile = resolveArtifact(jbossDistribution).getFile();
                }

                // Install jboss
                info("Installing '%s' to '%s'", jbossDistributionFile, jbossHome);
                Expand expander = new Expand();
                expander.setSrc(jbossDistributionFile);
                expander.setDest(jbossHome);
                try {
                    expander.execute();
                } catch (Exception e) {
                    throw new MojoExecutionException("Unable to expand jboss archive", e);
                }

                // Make sure the execution flag is lit
                new File(jbossHome, "bin/run.sh").setExecutable(true);

            } catch (Exception e) {
                throw new MojoExecutionException("Unable to download jboss distribution:"  + jbossDistribution, e);
            }
        }
    }
}
