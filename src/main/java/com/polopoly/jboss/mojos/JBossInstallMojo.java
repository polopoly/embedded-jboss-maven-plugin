package com.polopoly.jboss.mojos;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.Expand;
import org.codehaus.plexus.util.FileUtils;

import com.polopoly.jboss.AbstractJBossMBeanMojo;
import com.polopoly.jboss.ArtifactData;
import com.polopoly.jboss.ArtifactDeployData;
import com.polopoly.jboss.JBossDistribution;
import com.polopoly.jboss.RpcDistribution;

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
     * The location of RPC Services Home.
     *
     * @parameter default-value="${project.build.directory}/embedded-rpc"
     * @required
     */
    protected File rpcHome;

    /**
     * The location of RPC Services data.
     *
     * @parameter default-value="${project.build.directory}/embedded-rpc/data"
     * @required
     */
    protected File rpcData;

    /**
     * The named of RPC Services webapps folder (it would be a children of rpcHome).
     *
     * @parameter default-value="webapps"
     * @required
     */
    protected String rpcWebappsName;

    /**
     * The location of RPC Services lock file.
     *
     * @parameter default-value="${project.build.directory}/embedded-rpc/lock/rpc.lock"
     * @required
     */
    protected File rpcLock;

    /**
     * The rpc distribution (in the form of artifact coordinates).
     * @parameter
     */
    protected RpcDistribution rpcDistribution;

    /**
     * The rpc distribution (in the form of path to file). If specified takes precedence over rpcDistribution.
     * @parameter
     */
    protected File rpcDistributionFile;

    /**
     * Patch artifacts that will be applied to the supplied server id
     * @parameter
     */
    protected ArtifactData[] rpcPatches = new ArtifactData[0];

    /**
     * War artifacts that will be downloaded to the supplied server id
     * @parameter
     */
    protected ArtifactDeployData[] rpcDeployments = new ArtifactDeployData[0];

    /**
     * Patch files that will be applied to the supplied server id
     * @parameter
     */
    protected File[] rpcPatchFiles = new File[0];

    /**
     * War files that will be applied to the supplied server id
     * @parameter
     */
    protected File[] rpcDeploymentsFiles = new File[0];

    public void execute() throws MojoExecutionException, MojoFailureException {
        if (!reinstall && isRpcInstalled()) {
            info("RPC is already installed?");
            throw new MojoExecutionException("There is already a directory called " + new File(rpcHome, "lib").getAbsolutePath());
        }

        if (!reinstall && isInstalled()) {
            info("JBoss is already installed?");
            throw new MojoExecutionException("There is already a directory called " + new File(jbossHome, "bin").getAbsolutePath());
        }

        installRpcIfNotAlreadyInstalled();
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
     * Determine if jboss is installed
     * @return
     */
    protected boolean isRpcInstalled() {
        return new File(rpcHome, "lib").exists();
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
                new File(jbossHome, "bin/run.sh").setExecutable(true);

            } catch (Exception e) {
                throw new MojoExecutionException("Configure Download/Configure JBoss", e);
            }
        }
    }

    /**
     * Will install a new rpc if either <code>reinstall</code> is set or there is no rpc at the <code>rpcHome</code> location.
     * @throws MojoExecutionException
     * @throws MojoFailureException
     */
    protected void installRpcIfNotAlreadyInstalled() throws MojoExecutionException, MojoFailureException {
        if (reinstall || !isRpcInstalled()) {
            info("Installing RPC");
            try {
                if (rpcDistributionFile == null) {
                    if (rpcDistribution == null) {
                        throw new MojoExecutionException("Configure rpcDistribution");
                    }
                    rpcDistributionFile = resolveArtifact(rpcDistribution).getFile();
                }

                // Install rpc
                info("Installing '%s' to '%s'", rpcDistributionFile, rpcHome);
                final File lib = new File(rpcHome, "lib");
                Files.createDirectories(lib.toPath());
                Files.copy(rpcDistributionFile.toPath(), new File(lib, rpcDistributionFile.getName()).toPath());
                //unzip(jbossDistributionFile, jbossHome);

                final File logs = new File(rpcHome, "logs");
                Files.createDirectories(logs.toPath());

                resolveArtifacts(rpcPatches);
                applyPatches(rpcPatches, rpcPatchFiles, rpcHome);

                // Make sure the execution flag is lit
                //new File(jbossHome, "bin/run.sh").setExecutable(true);

            } catch (Exception e) {
                throw new MojoExecutionException("Configure Download/Configure RPC", e);
            }
        } else {
            try {
                if (rpcDistributionFile == null) {
                    if (rpcDistribution == null) {
                        throw new MojoExecutionException("Configure rpcDistribution");
                    }
                    rpcDistributionFile = resolveArtifact(rpcDistribution).getFile();
                }
                if (rpcDistributionFile.getAbsolutePath().endsWith("-SNAPSHOT.jar")) {
                    info("Installing '%s' to '%s'", rpcDistributionFile, rpcHome);
                    final File lib = new File(rpcHome, "lib");
                    Files.createDirectories(lib.toPath());
                    Files.copy(rpcDistributionFile.toPath(), new File(lib, rpcDistributionFile.getName()).toPath(), StandardCopyOption.REPLACE_EXISTING);
                    //unzip(jbossDistributionFile, jbossHome);

                    final File logs = new File(rpcHome, "logs");
                    Files.createDirectories(logs.toPath());
                }
            } catch (Exception e) {
                throw new MojoExecutionException("Configure Download/Configure RPC", e);
            }
        }
    }

    protected void deployAdmCoreWars() throws MojoExecutionException {
        try {
            final File warDirs = new File(rpcHome, rpcWebappsName);
            Files.createDirectories(warDirs.toPath());

            resolveArtifacts(rpcDeployments);
            copyArtifacts(rpcDeployments, rpcDeploymentsFiles, warDirs);
        } catch (IOException e) {
            throw new MojoExecutionException("cannot deploy adm-core deployments", e);
        }
    }

    private void copyArtifacts(final ArtifactDeployData[] patches,
                               final File[] patchFiles,
                               final File targetDir) throws MojoExecutionException {
        List<File> fileList = new ArrayList<>();
        Map<String, String> nameMap = new HashMap<>();
        final Artifact[] artifacts = resolveArtifacts(patches);
        assert artifacts.length == patches.length;
        for (int i = 0; i < artifacts.length; i++) {
            final Artifact artifact = artifacts[i];
            final ArtifactDeployData data = patches[i];
            final File file = artifact.getFile();
            fileList.add(file);
            final String ext = FileUtils.getExtension(file.getName());
            String name = Optional.ofNullable(data.name)
                                  .orElse(file.getName());
            if (!name.endsWith(ext)) {
                if (!ext.startsWith(".")) {
                    name += ".";
                }
                name += ext;
            }
            nameMap.put(file.getAbsolutePath(), name);
        }
        fileList.addAll(Arrays.asList(patchFiles));
        for (final File patchFile : fileList) {
            final String name = nameMap.getOrDefault(patchFile.getAbsolutePath(), patchFile.getName());
            final File target = new File(targetDir, name);
            info("Copying '%s' to '%s'", patchFile, target);
            try {
                Files.copy(patchFile.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                throw new MojoExecutionException("Cannot copy " + patchFile, e);
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
