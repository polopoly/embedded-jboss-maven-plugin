package com.polopoly.jboss.mojos;

import com.polopoly.jboss.AbstractJBossMBeanMojo;
import com.polopoly.jboss.JBossOperations;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * Will stop the JBoss server started with jboss:start
 * @goal stop
 * @aggregator
 */
public class JBossStopMojo extends AbstractJBossMBeanMojo {

    public void execute() throws MojoExecutionException, MojoFailureException {
        info("Shutting down JBoss");
        JBossOperations operations = new JBossOperations(connect());
        operations.shutDown();
        try {
            info("Waiting for JBoss to shutdown");
            while(operations.isStarted()) {
                sleep("Interrupted while waiting for JBoss to stop");
            }
        } catch (RuntimeException re){ }
        while(!isNamingPortFree()) {
            sleep("Interrupted while waiting for JBoss to stop");
        }
    }
}
