package com.polopoly.jboss.mojos;

import com.polopoly.jboss.JBossOperations;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * Created by bitter on 2011-10-07
 * @goal start-and-wait
 * @aggregator
 */
public class JBossStartAndWait extends JBossStartMojo {

    public void execute() throws MojoExecutionException, MojoFailureException {

        info("Determining if JBoss needs to be installed");
        install();

        info("Starting JBoss");
        start();

        info("Waiting for JBoss to become ready");
        waitForJBossToStart();
    }

    protected void waitForJBossToStart() throws MojoExecutionException {
        JBossOperations operations = new JBossOperations(connect());
        for (int i = 0; i < retry; i++) {
            if (operations.isStarted()) {
                break;
            }
            try {
                Thread.sleep(retryWait);
            } catch (InterruptedException e) {}
        }
    }
}
