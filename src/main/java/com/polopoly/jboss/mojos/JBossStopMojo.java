package com.polopoly.jboss.mojos;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import com.polopoly.jboss.AbstractJBossMBeanMojo;
import com.polopoly.jboss.JBossOperations;

/**
 * Will stop the JBoss server started with jboss:start
 * @goal stop
 * @aggregator
 */
public class JBossStopMojo extends AbstractJBossMBeanMojo {

    public void execute() throws MojoExecutionException, MojoFailureException {
        info("Shutting down JBoss");
        JBossOperations operations = new JBossOperations(connect(false));
        if (!isNamingPortFree() || isStarted(operations)) {
            operations.shutDown();

            try {
                info("Waiting for JBoss to shutdown");
                while (isStarted(operations)) {
                    sleep("Interrupted while waiting for JBoss to stop");
                }
            } catch (RuntimeException re) {
            }
        } else {
            info("JBoss seems to be already down");
        }
        while(!isNamingPortFree()) {
            sleep("Interrupted while waiting for JBoss to stop");
        }
    }

    private boolean isStarted(final JBossOperations operations) {
        try {
            return operations.isStarted();
        } catch (Exception e) {
            return false;
        }
    }

}
