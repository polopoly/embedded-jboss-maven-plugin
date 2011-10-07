package com.polopoly.jboss.mojos;

import com.polopoly.jboss.AbstractJBossMojo;
import com.polopoly.jboss.JBossOperations;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * Created by bitter on 2011-10-07
 * @execute goal="start"
 * @goal start-and-wait
 */
public class JBossStartAndWait extends AbstractJBossMojo {

    public void execute() throws MojoExecutionException, MojoFailureException {
        info("Waiting for JBoss to become ready");
        JBossOperations operations = new JBossOperations(connect());
        waitForJBossToStart(operations);
    }

    private void waitForJBossToStart(JBossOperations operations) {
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
