package com.polopoly.jboss.mojos;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by bitter on 2011-10-07
 *
 * @goal start
 * @aggregator
 */
public class JBossStartMojo extends JBossInstallMojo {

    /**
     * The set of options to pass to the JBoss "run" command.
     *
     * @parameter default-value="" expression="${jboss.startOptions}"
     */
    protected String startOptions;

    /**
     * The command to start JBoss.
     */
    public static final String STARTUP_COMMAND = "run";

    public void execute() throws MojoExecutionException, MojoFailureException {
        if (!isStarted()) {
            // Do installation
            install();

            // Launch jboss
            start();
            
        } else {
            info("Server already started");
        }
    }

    protected boolean isStarted() {
        try {
            new Socket("127.0.0.1", new Integer(namingPort));
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    /**
     * Call the JBoss startup or shutdown script.
     *
     * @throws MojoExecutionException
     */
    protected void start()
        throws MojoExecutionException
    {
        if (!isStarted()) {
            if (startOptions == null) {
                startOptions = "";
            }
            if (serverId != null) {
                startOptions += " -c " + serverId;
            }

            String osName = System.getProperty("os.name");
            String[] jbossStartCommand = osName.startsWith( "Windows" ) ? createWindowsCommand() : createUnixCommand();
            String[] jbossStartEnvironment = new String[] { "JBOSS_HOME=" + jbossHome.getAbsolutePath() };

            try {
                Process proc = Runtime.getRuntime().exec(jbossStartCommand, jbossStartEnvironment, new File(jbossHome, "bin"));
                new JBossLogger(proc.getInputStream()).start();
                new JBossLogger(proc.getErrorStream()).start();

            } catch (IOException ioe) {
                throw new MojoExecutionException("Unable to start jboss!", ioe);
            }
        }
    }

    private String[] createWindowsCommand() {
        return
                new String[] {
                    "cmd.exe",
                    "/C",
                    "cd /D " + jbossHome + "\\bin & set JBOSS_HOME=\"" + jbossHome + "\"&" + STARTUP_COMMAND + ".bat " + startOptions };

    }

    private String[] createUnixCommand()
    {
        File jbossUnixCommand = new File(new File(jbossHome, "bin"), STARTUP_COMMAND + ".sh");
        List<String> commandWithOptions = new ArrayList<String>();
        commandWithOptions.add(jbossUnixCommand.getAbsolutePath());
        commandWithOptions.addAll(Arrays.asList(startOptions.trim().split("\\s+")));

        return commandWithOptions.toArray(new String[commandWithOptions.size()]);
    }

    private class JBossLogger extends Thread {
        private final BufferedReader _stream;
        JBossLogger(InputStream stream) {
            _stream = new BufferedReader(new InputStreamReader(stream));
        }

        public void run() {

            String line;
            try {
                while ((line = _stream.readLine()) != null) {
                    //info(line);
                }

            } catch (IOException ioe) {}
        }
    }
}
