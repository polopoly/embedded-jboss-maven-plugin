package com.polopoly.jboss.mojos;

import com.polopoly.jboss.AbstractJBossMojo;
import com.polopoly.jboss.JBossOperations;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * Created by bitter on 2011-10-07
 * @goal stop
 */
public class JBossStopMojo extends AbstractJBossMojo {

    public void execute() throws MojoExecutionException, MojoFailureException {
        info("Shutting down...");
        new JBossOperations(connect()).shutDown();
    }
}
