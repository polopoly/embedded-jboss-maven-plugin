package com.polopoly.jboss.mojos;

import com.polopoly.jboss.AbstractJBossMBeanMojo;
import com.polopoly.jboss.ArtifactData;
import com.polopoly.jboss.JBossOperations;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by bitter on 2011-10-07
 *
 * @goal deploy
 * @aggregator
 */
public class JBossDeployMojo extends JBossStartAndWait {

    /**
     * File to be deployed
     *
     * @parameter expression="${jboss.deploy.file}"
     */
    protected File file;

    /**
     * Artifacts to be deployed
     * 
     * @parameter
     */
    protected ArtifactData[] deployments = new ArtifactData[0];


    public void execute() throws MojoExecutionException, MojoFailureException {

        info("Determining if JBoss needs to be installed");
        install();

        info("Starting JBoss");
        start();

        info("Waiting for JBoss to become ready");
        waitForJBossToStart();

        info("Deploying artifacts");
        deployAndWait();
    }

    protected void deployAndWait() throws MojoExecutionException, MojoFailureException {
        JBossOperations operations = new JBossOperations(connect());
        if (file != null) {
            redeploy(operations, file);
        }

        Artifact[] deploymentArtifacts = resolveArtifacts(deployments);
        for (Artifact artifact : deploymentArtifacts) {
            redeploy(operations, artifact.getFile());
        }
        for (Artifact artifact : deploymentArtifacts) {
            waiForDeployment(operations, artifact.getFile());
        }
        info("All deployments done!");
    }

    protected void waiForDeployment(JBossOperations operations, File file) throws MojoExecutionException {
        try {
            waitForDeployment(operations, file.toURI().toURL());
        } catch (MalformedURLException e) {
            throw new MojoExecutionException("The file path could not be converted into a url", e);
        }
    }

    protected void waitForDeployment(JBossOperations operations, URL url) throws MojoExecutionException {
        info("Waiting for: " + url);
        for (int i = 0; i < retry && !operations.isDeployed(url); ++i)
        {
            try {
                Thread.sleep(retryWait);
            }
            catch ( InterruptedException e )
            {
                warn("Thread interrupted while waiting for deplyment: " + e.getMessage());
            }
        }
    }

    protected void redeploy(JBossOperations operations, File file) throws MojoExecutionException {
        try {
            redeploy(operations, file.toURI().toURL());
        } catch (MalformedURLException e) {
            throw new MojoExecutionException("The file path could not be converted into a url", e);
        }
    }

    protected void redeploy(JBossOperations operations, URL url) throws MojoExecutionException {
        info("Deploying: " + url);
        operations.redeploy(url);
    }
}
