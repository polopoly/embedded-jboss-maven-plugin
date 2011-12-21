package com.polopoly.jboss.mojos;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import com.polopoly.jboss.Environment;
import com.polopoly.jboss.JBossOperations;

/**
 * This mojo will install a JBoss server and start it. If 'namingPort' is occupied the mojo will abort with an exception.
 *
 * @goal start
 * @aggregator
 */
public class JBossStartMojo
    extends JBossDeployMojo
{
    /**
     * The set of options to pass to the JBoss "run" command.
     *
     * @parameter default-value="" expression="${jboss.startOptions}"
     */
    protected String startOptions;

    /**
     * Custom environment variables to pass to the JBoss "run" command.
     *
     * @parameter
     */
    protected Environment[] environments;

    /**
     * Pipes stdout and stderr from the jboss process to the maven console.
     *
     * @parameter expression="${jboss.logToConsole}"
     */
    protected boolean logToConsole;

    /**
     * The command to startIfNamingPortIsFree JBoss.
     */
    public static final String STARTUP_COMMAND = "run";

    /**
     * Will install a JBoss server and start it. If 'namingPort' is occupied the mojo will abort with an exception.
     *
     * @throws MojoExecutionException
     * @throws MojoFailureException
     */
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        if (isNamingPortFree()) {
            info("JBoss is already running?");
            throw new MojoExecutionException("There is already a process occupying port " + namingPort);
        }

        // Do installation (if not installed)
        installIfNotAlreadyInstalled();

        // Launch jboss
        startIfNamingPortIsFree();

        // Deploy artifacts
        deployAndWait();
    }

    /**
     * Start JBoss If <code>namingPort</code> is free.
     *
     * @throws MojoExecutionException
     */
    protected void startIfNamingPortIsFree()
        throws MojoExecutionException
    {
        if (!isNamingPortFree()) {
            info("Starting JBoss");

            List<String> startOpts = new ArrayList<String>();

            if (startOptions != null) {
                startOpts.addAll(Arrays.asList(startOptions.split("\\s+")));
            }

            if (serverId != null) {
                startOpts.add("-c");
                startOpts.add(serverId);
            }

            startOpts.add("-b");
            startOpts.add("localhost");

            String osName = System.getProperty("os.name");

            ProcessBuilder pb = new ProcessBuilder(osName.startsWith("Windows") ? createWindowsCommand(startOpts) : createUnixCommand(startOpts));

            pb.directory(jbossHome);
            pb.environment().put("JBOSS_HOME", jbossHome.getAbsolutePath());

            if (environments != null) {
                for (Environment env : environments) {
                    if (env.getName() != null && env.getName().length() > 0 &&
                        env.getValue() != null && env.getValue().length() > 0)
                    {
                        pb.environment().put(env.getName(), env.getValue());
                    }
                }
            }

            try {
                Process proc = pb.start();
                new JBossLogger(proc.getInputStream(), "out", logToConsole).start();
                new JBossLogger(proc.getErrorStream(), "err", logToConsole).start();
            } catch (Exception ioe) {
                throw new MojoExecutionException("Unable to startIfNamingPortIsFree jboss!", ioe);
            }
        }

        // Wait for jboss to become ready
        JBossOperations operations = new JBossOperations(connect());
        for (int i = 0; i < retry; i++) {
            if (operations.isStarted()) {
                break;
            }

            sleep("Interrupted while waiting for JBoss to start");
        }
    }

    private String[] createWindowsCommand(final List<String> startOpts)
    {
        File jbossWindowsCommand = new File(new File(jbossHome, "bin"), STARTUP_COMMAND + ".bat");

        List<String> commandWithOptions = new ArrayList<String>();

        commandWithOptions.addAll(Arrays.asList("cmd", "/c"));
        commandWithOptions.add(jbossWindowsCommand.getAbsolutePath());
        commandWithOptions.addAll(startOpts);

        return commandWithOptions.toArray(new String[0]);
    }

    private String[] createUnixCommand(final List<String> startOpts)
    {
        File jbossUnixCommand = new File(new File(jbossHome, "bin"), STARTUP_COMMAND + ".sh");

        List<String> commandWithOptions = new ArrayList<String>();

        commandWithOptions.add(jbossUnixCommand.getAbsolutePath());
        commandWithOptions.addAll(startOpts);

        return commandWithOptions.toArray(new String[commandWithOptions.size()]);
    }

    private class JBossLogger
        extends Thread
    {
        private final BufferedReader _stream;

        private final String _logName;
        private final boolean _log;

        JBossLogger(final InputStream stream,
                    final String logName,
                    final boolean log)
        {
            _stream = new BufferedReader(new InputStreamReader(stream));

            _logName = logName;
            _log = log;
        }

        public void run()
        {
            String line;

            try {
                while ((line = _stream.readLine()) != null) {
                    if (_log) {
                      info(" -- log(%s) -- %s", _logName, line);
                    }
                }
            } catch (IOException ioe) {}
        }
    }
}
