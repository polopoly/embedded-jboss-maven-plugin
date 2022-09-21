package com.polopoly.jboss.mojos;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.Expand;

import com.polopoly.jboss.AbstractJBossMBeanMojo;
import com.polopoly.jboss.AdmDistribution;
import com.polopoly.jboss.ArtifactData;
import com.polopoly.jboss.JBossDistribution;

/**
 * Will download and install a pre-configured JBoss Application Server
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
     * The jboss distribution (in the form of artifact coordinates).
     * @parameter
     */
    protected JBossDistribution jbossDistribution = JBossDistribution.DEFAULT_JBOSS_DISTRIUTION;

    /**
     * The jboss distribution (in the form of path to file). If specified takes precedence over jbossDistribution.
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
     * Patch artifacts that will be applied to the jboss distribution
     * @parameter
     */
    protected ArtifactData[] jbossPatches = new ArtifactData[0];

    /**
     * Patch files that will be applied to the jboss distribution
     * @parameter
     */
    protected File[] jbossPatchFiles = new File[0];

    /**
     * Force re-installation of jboss
     * @parameter default-value="false" expression="${jboss.reinstall}"
     */
    protected boolean reinstall;

    /**
     * The location of the JBOSS lock file.
     *
     * @parameter default-value="${project.build.directory}/embedded-jboss/server/default/tmp/run.pid"
     */
    protected File jbossLock;

    /**
     * The location of ADM Content Services Home.
     *
     * @parameter default-value="${project.build.directory}/embedded-adm"
     * @required
     */
    protected File admHome;

    /**
     * The location of ADM Content Services data.
     *
     * @parameter
     */
    protected File admData;

    /**
     * The location of ADM Content Services lock file.
     *
     * @parameter default-value="${project.build.directory}/embedded-adm/locks/adm.lock"
     * @required
     */
    protected File admLock;

    /**
     * The adm content services distribution (in the form of artifact coordinates).
     * @parameter
     */
    protected AdmDistribution admDistribution;

    /**
     * The adm content services distribution (in the form of path to file). If specified takes precedence over admDistribution.
     * @parameter
     */
    protected File admDistributionFile;

    /**
     * Patch artifacts that will be applied to the supplied server id
     * @parameter
     */
    protected ArtifactData[] admPatches = new ArtifactData[0];

    /**
     * Patch files that will be applied to the supplied server id
     * @parameter
     */
    protected File[] admPatchFiles = new File[0];

    /**
     * Force re-installation of adm content services
     * @parameter default-value="false" expression="${adm.reinstall}"
     */
    protected boolean admUpdate;


    public void execute() throws MojoExecutionException, MojoFailureException {
        if (!reinstall && isAdmInstalled()) {
            info("ADM Content Services are already installed?");
            throw new MojoExecutionException("There is already a directory called " + new File(admHome, "lib").getAbsolutePath());
        }

        if (!reinstall && isInstalled()) {
            info("JBoss is already installed?");
            throw new MojoExecutionException("There is already a directory called " + new File(jbossHome, "bin").getAbsolutePath());
        }

        installAdmIfNotAlreadyInstalled();
        installIfNotAlreadyInstalled();
    }

    /**
     * Determine if jboss is installed
     * @return
     */
    protected boolean isInstalled() {
        return new File(jbossHome, "bin").exists();
    }

    /**
     * Determine if adm is installed
     * @return
     */
    protected boolean isAdmInstalled() {
        return new File(admHome, "lib").exists();
    }

    /**
     * Will install a new jboss if either <code>reinstall</code> is set or there is no jboss at the <code>jbossHome</code> location.
     * @throws MojoExecutionException
     * @throws MojoFailureException
     */
    protected void installIfNotAlreadyInstalled() throws MojoExecutionException, MojoFailureException {
        if (reinstall || !isInstalled()) {
            info("Installing JBoss");
            try {
                if (jbossDistributionFile == null) {
                    jbossDistributionFile = resolveArtifact(jbossDistribution).getFile();
                }
                resolveArtifacts(jbossPatches);
                resolveArtifacts(serverPatches);

                // Install jboss
                info("Installing '%s' to '%s'", jbossDistributionFile, jbossHome);
                unzip(jbossDistributionFile, jbossHome);
                applyPatches(jbossPatches, jbossPatchFiles, jbossHome);
                applyPatches(serverPatches, serverPatchFiles, new File(jbossHome, "server/" + serverId));

                // Make sure the execution flag is lit
                //noinspection ResultOfMethodCallIgnored
                new File(jbossHome, "bin/run.sh").setExecutable(true);
            } catch (Exception e) {
                throw new MojoExecutionException("Configure Download/Configure JBoss", e);
            }
        }
    }

    /**
     * Will install a new adm if either <code>reinstall</code> is set or there is no adm at the <code>admHome</code> location.
     * @throws MojoExecutionException
     * @throws MojoFailureException
     */
    protected void installAdmIfNotAlreadyInstalled() throws MojoExecutionException, MojoFailureException {
        if (shouldStartAdm() && (reinstall || !isAdmInstalled())) {
            if (isAdmPortRunning()) {
                throw new MojoExecutionException("ADM is still running but the install directory is missing, " +
                    "you have to kill it manually, check the pid in the '/internal/running' response.");
            }
            info("Installing ADM Content Services");
            try {
                setupAdmDistributionFile();

                doInstallAdm(true);
            } catch (Exception e) {
                throw new MojoExecutionException("Configure Download/Configure ADM Content Services", e);
            }
        } else if (shouldStartAdm()) {
            try {
                setupAdmDistributionFile();
                String end = "";
                if (admDistribution.classifier != null) {
                    end += "-" + admDistribution.classifier;
                }
                if (admDistribution.type != null) {
                    end += "." + admDistribution.type;
                } else {
                    end += ".jar";
                }
                boolean updateInstall = admUpdate;
                if (!updateInstall && admDistributionFile.getAbsolutePath().endsWith("-SNAPSHOT" + end)) {
                    info("snapshot detected");
                    updateInstall = true;
                }
                if (updateInstall) {
                    if (isAdmPortRunning()) {
                        throw new MojoExecutionException("ADM is still running, you have to stop it calling " +
                            new File(new File(admHome, "bin"), "run.sh").getAbsolutePath() +
                            " --stop -p " + admPort);
                    }
                    info("repeat adm install");
                    doInstallAdm(false);
                }
            } catch (Exception e) {
                throw new MojoExecutionException("Configure Download/Configure ADM Content Services", e);
            }
        }
    }

    private void setupAdmDistributionFile() throws MojoExecutionException {
        if (admDistributionFile == null) {
            if (admDistribution == null) {
                throw new MojoExecutionException("Configure admDistribution");
            }
            admDistributionFile = resolveArtifact(admDistribution).getFile();
        }
    }

    protected boolean shouldStartAdm() {
        return admDistributionFile != null || admDistribution != null;
    }

    private void doInstallAdm(final boolean applyPatches) throws MojoExecutionException {
        // Install adm
        info("Installing '%s' to '%s'", admDistributionFile, admHome);
        if (!applyPatches) {
            info("Empty lib directory");
            emptyDirectory(new File(admHome, "lib"));
        }
        unzip(admDistributionFile, admHome);
        if (applyPatches) {
        applyPatches(admPatches, admPatchFiles, admHome);
        }

        // Make sure the execution flag is lit
        //noinspection ResultOfMethodCallIgnored
        new File(new File(admHome, "bin"), "run.sh").setExecutable(true);
    }

    private void emptyDirectory(final File directory) {
        if (directory.exists() && directory.isDirectory()) {
            final File[] files = directory.listFiles();
            if (files != null) {
                for (final File file : files) {
                    if (file.exists() && !file.isDirectory()) {
                        if (!file.delete()) {
                            warn("Cannot delete " + file);
                        }
                    }
                }
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
