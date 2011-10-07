package com.polopoly.jboss.mojos;

import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.mojo.jboss.StartMojo;

import java.io.File;
import java.io.IOException;
import java.net.Socket;

/**
 * Created by bitter on 2011-10-07
 *
 * @extendsPlugin jboss
 * @extendsGoal start
 *
 * @execute goal="install"
 * @goal start
 */
public class JBossStartMojo extends StartMojo {

    /**
     * The location of JBoss Home.
     *
     * @parameter expression="${project.build.directory}/embedded-jboss"
     * @required
     */
    protected File jbossHome;

    /**
     * The port for the naming service.
     *
     * @parameter default-value="1099" expression="${jboss.namingPort}"
     */
    protected String namingPort;

    public void execute() throws MojoExecutionException {
        super.jbossHome = jbossHome;
        try {
            new Socket("127.0.0.1", new Integer(namingPort));
        } catch (IOException e) {
            super.execute();
        }
    }
}
