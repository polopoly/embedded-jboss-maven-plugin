package com.polopoly.jboss.mojos;

import com.polopoly.jboss.AbstractJBossMBeanMojo;
import com.polopoly.jboss.ArtifactData;
import com.polopoly.jboss.JBossDistribution;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.Expand;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by bitter on 2011-10-07
 *
 * @goal install
 * @aggregator
 */
public class JBossInstallMojo extends AbstractJBossMBeanMojo {

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
     * Patch artifacts that will be applied to the supplied server id
     * @parameter
     */
    protected ArtifactData[] serverPatches = new ArtifactData[0];

    /**
     * Patch files that will be applied to the supplied server id
     * @parameter
     */
    protected File[] serverPatchFiles = new File[0];


    /**
     * Patch artifacts that will be applied to the supplied server id
     * @parameter
     */
    protected ArtifactData[] jbossPatches = new ArtifactData[0];

    /**
     * Patch files that will be applied to the supplied server id
     * @parameter
     */
    protected File[] jbossPatchFiles = new File[0];

    /**
     * Force re-installation of jboss
     *
     * @parameter expression="${jboss.reinstall}"
     */
    protected boolean reinstall;

    public void execute() throws MojoExecutionException, MojoFailureException {
        install();
    }

    protected void install() throws MojoExecutionException, MojoFailureException {
        info(jbossHome.getAbsolutePath());
        if (reinstall || !jbossHome.exists()) {
            try {
                if (jbossDistributionFile == null) {
                    jbossDistributionFile = resolveArtifact(jbossDistribution).getFile();
                }

                // Install jboss
                info("Installing '%s' to '%s'", jbossDistributionFile, jbossHome);
                unzip(jbossDistributionFile, jbossHome);
                applyPatches(jbossPatches, jbossPatchFiles, jbossHome);
                applyPatches(serverPatches, serverPatchFiles, new File(jbossHome, "server/" + serverId));

                // Make sure the execution flag is lit
                new File(jbossHome, "bin/run.sh").setExecutable(true);

            } catch (Exception e) {
                throw new MojoExecutionException("Unable to download jboss distribution:"  + jbossDistribution, e);
            }
        }
    }

    private List<File> applyPatches(ArtifactData[] patches, File[] patchFiles, File target) throws MojoExecutionException {
        List<File> patchList = new ArrayList<File>();
        for (Artifact artifact : resolveArtifacts(patches)) {
            patchList.add(artifact.getFile());
        }
        patchList.addAll(Arrays.asList(patchFiles));
        for (File patchFile : patchList) {
            info("Applying patch '%s' to '%s'", patchFile, target);
            unzip(patchFile, target);
        }
        return patchList;
    }

    private static void unzip(File src, File target) throws MojoExecutionException {
        Expand expander = new Expand();
        expander.setSrc(src);
        expander.setDest(target);
        try {
            expander.execute();
        } catch (Exception e) {
            throw new MojoExecutionException("Unable to expand jboss archive", e);
        }
    }
}
