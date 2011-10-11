package com.polopoly.jboss.mojos;

import com.polopoly.jboss.AbstractJBossMBeanMojo;
import com.polopoly.jboss.JBossOperations;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * Created by bitter on 2011-10-07
 * @goal stop
 * @aggregator
 */
public class JBossStopMojo extends AbstractJBossMBeanMojo {

    public void execute() throws MojoExecutionException, MojoFailureException {
        info("Shutting down...");
        new JBossOperations(connect()).shutDown();
    }
}
