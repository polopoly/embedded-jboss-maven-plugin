package com.polopoly.jboss.mojos;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import com.polopoly.jboss.Environment;
import com.polopoly.jboss.JBossOperations;

/**
 * Will download, install, and start a pre-configured JBoss Application Server on localhost and deploy all listed deployments.
 * If the installation directory already exists it will only be started.
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
     * If true, pipes stdout and stderr from the jboss process to the maven console.
     *
     * @parameter default-value="false" expression="${jboss.logToConsole}"
     */
    protected boolean logToConsole;

    /**
     * The command to startIfNamingPortIsFree JBoss.
     */
    public static final String STARTUP_COMMAND = "run";

    /**
     * Custom environment variables to pass to the ADM "run" command.
     *
     * @parameter
     */
    protected Environment[] admEnvironments;

    /**
     * The set of system options to pass to the ADM "run" command.
     *
     * @parameter default-value="" expression="${jboss.admSystemOptions}"
     */
    protected String admSystemOptions;

    /**
     * The set of options to pass to the ADM "run" command.
     *
     * @parameter default-value="" expression="${jboss.admStartOptions}"
     */
    protected String admStartOptions;

    /**
     * The set of system options to pass to the ADM "run" command.
     *
     * @parameter default-value="" expression="${jboss.admSettings}"
     */
    protected String admSettings;

    /**
     * The set of system options to pass to the ADM "run" command.
     *
     * @parameter default-value="" expression="${project.build.directory}/embedded-adm/conf/settings.yml"
     */
    protected File admSettingsFile;

    /**
     * The command to start ADM.
     */
    public static final String ADM_STARTUP_COMMAND = "bin/run";

    /**
     * Will install a JBoss server and start it. If 'namingPort' is occupied the mojo will abort with an exception.
     *
     * @throws MojoExecutionException
     * @throws MojoFailureException
     */
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        installAdmIfNotAlreadyInstalled();

        startAdmIfPortIsFree();

        if (isNamingPortFree()) {
            info("JBoss is already running?");
            throw new MojoExecutionException("There is already a process occupying port " + namingPort);
        }

        // Do installation (if not installed)
        installIfNotAlreadyInstalled();

        // Launch jboss
        startIfNamingPortIsFree();

        // Deploy artifacts
        try {
            deployAndWait();
        } catch (MojoExecutionException | MojoFailureException e) {
            info("Stopping jboss due to failed deployment: " + e.getMessage());
            stop();
            throw e;
        }
    }

    private void stop() {
        try {
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
        } catch (MojoExecutionException e) {
            warn("cannot stop jboss: {}", e.getMessage());
        }
    }

    private boolean isStarted(final JBossOperations operations) {
        try {
            return operations.isStarted();
        } catch (Exception e) {
            return false;
        }
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
            startOpts.add(bindAddress);

            final String[] params = isWindows()
                ? createWindowsCommand(startOpts)
                : createUnixCommand(startOpts);

            System.out.println("Start With\n" + arrayToString(params));

            ProcessBuilder pb = new ProcessBuilder(params);

            pb.directory(new File(jbossHome, "bin"));
            pb.environment().put("JBOSS_HOME", jbossHome.getAbsolutePath());

            setupEnvironments(environments, pb);

            try {
                Process proc = pb.start();
                new JBossLogger(proc.getInputStream(), "out", logToConsole).start();
                new JBossLogger(proc.getErrorStream(), "err", logToConsole).start();
            } catch (Exception ioe) {
                throw new MojoExecutionException("Unable to startIfNamingPortIsFree jboss!", ioe);
            }
        }

        // Wait for jboss to become ready
        JBossOperations operations = new JBossOperations(connect(true));
        for (int i = 0; i < retry; i++) {
            if (operations.isStarted()) {
                break;
            }

            sleep("Interrupted while waiting for JBoss to start");
        }
    }

    /**
     * Start ADM If <code>admPort</code> is free.
     *
     * @throws MojoExecutionException
     */
    protected void startAdmIfPortIsFree()
        throws MojoExecutionException
    {
        if (!shouldStartAdm()) {
            return;
        }
        if (!isAdmPortRunning()) {
            info("Starting ADM Content Services");

            File settings = null;
            if ((admSettingsFile != null) && (admSettings != null) && !admSettings.trim().isEmpty()) {
                if (admSettingsFile.exists()) {
                    //noinspection ResultOfMethodCallIgnored
                    admSettingsFile.delete();
                }
                final Path settingsPath = admSettingsFile.toPath().toAbsolutePath();
                info("Writing settings to " + settingsPath);
                try {
                    Files.write(
                        settingsPath,
                        admSettings.getBytes(StandardCharsets.UTF_8),
                        StandardOpenOption.CREATE,
                        StandardOpenOption.CREATE
                    );
                    settings = settingsPath.toFile();
                } catch (IOException e) {
                    throw new MojoExecutionException("Cannot create " + settingsPath);
                }
            }

            final List<String> startOpts = new ArrayList<>();

            if (admSystemOptions != null) {
                startOpts.addAll(Arrays.asList(admSystemOptions.replaceAll("\r\n", " ")
                                                               .replaceAll("\n", " ")
                                                               .split("\\s+")));
            }

            if (admDistributionFile == null) {
                if (admDistribution == null) {
                    throw new MojoExecutionException("Configure admDistribution");
                }
                admDistributionFile = resolveArtifact(admDistribution).getFile();
            }

            if (settings != null) {
                startOpts.add("-f");
                startOpts.add(settings.getAbsolutePath());
            }

            startOpts.add("-p");
            startOpts.add(admPort);

            if (admData != null) {
                startOpts.add("--db");
                startOpts.add(admData.getAbsolutePath());
            }

            startOpts.add("--lock");
            startOpts.add(admLock.getAbsolutePath());

            if (admStartOptions != null) {
                startOpts.addAll(Arrays.asList(admStartOptions.split("\\s+")));
            }

            final String[] params = isWindows()
                ? createWindowsCommand(ADM_STARTUP_COMMAND, startOpts)
                : createUnixCommand(ADM_STARTUP_COMMAND, startOpts);

            info("Start With\n" + arrayToString(params));

            ProcessBuilder pb = new ProcessBuilder(params);

            pb.directory(admHome);

            setupEnvironments(admEnvironments, pb);

            try {
                info("starting");
                Process proc = pb.start();
                new ADMLogger(proc.getInputStream(), "out", logToConsole).start();
                new ADMLogger(proc.getErrorStream(), "err", logToConsole).start();

                Thread.sleep(2000);
            } catch (Exception ioe) {
                throw new MojoExecutionException("Unable to startAdmIfPortIsFree ADM Content Services!", ioe);
            }
        }

        for (int i = 0; i < retry; i++) {
            if (isAdmPortRunning()) {
                break;
            }

            sleep("Interrupted while waiting for ADM Content Services to start");
        }

        if (!isAdmPortRunning()) {
            throw new MojoExecutionException("Unable to startAdmIfPortIsFree ADM Content Services!");
        }
    }

    protected void setupEnvironments(final Environment[] environments,
                                     final ProcessBuilder pb) {
        if (environments != null) {
            for (Environment env : environments) {
                if (env.getName() != null && env.getName().length() > 0 &&
                    env.getValue() != null && env.getValue().length() > 0) {
                    final String value = env.getValue()
                                            .replaceAll("\r\n", " ")
                                            .replaceAll("\n", " ")
                                            .replaceAll("\\s+", " ");
                    pb.environment().put(env.getName(), value);
                }
            }
        }
    }

    String arrayToString(final String[] params) {
        return String.join(" ", params);
    }

    protected String[] createWindowsCommand(final List<String> startOpts) {
        return createWindowsCommand(STARTUP_COMMAND, startOpts);
    }

    protected String[] createWindowsCommand(final String cmdName,
                                            final List<String> startOpts) {
        final String windowsCommand = ".\\" + cmdName.replaceAll("/", "\\\\") + ".bat";

        List<String> commandWithOptions = new ArrayList<>(Arrays.asList("cmd", "/c"));
        commandWithOptions.add(windowsCommand);
        commandWithOptions.addAll(startOpts);

        return commandWithOptions.toArray(new String[0]);
    }

    protected String[] createUnixCommand(final List<String> startOpts) {
        return createUnixCommand(STARTUP_COMMAND, startOpts);
    }

    protected String[] createUnixCommand(final String cmdName,
                                         final List<String> startOpts) {
        final String unixCommand = "./" + cmdName + ".sh";
        List<String> commandWithOptions = new ArrayList<>();

        commandWithOptions.add(unixCommand);
        commandWithOptions.addAll(startOpts);

        return commandWithOptions.toArray(new String[0]);
    }

    class JBossLogger extends ProcessLogger {

        JBossLogger(final InputStream stream, final String logName, final boolean log) {
            super("JBOSS", stream, logName, log);
        }
    }

    class ADMLogger extends ProcessLogger {

        ADMLogger(final InputStream stream, final String logName, final boolean log) {
            super("ADM", stream, logName, log);
        }
    }

    abstract class ProcessLogger
        extends Thread
    {
        private final String prefix;
        private final BufferedReader stream;
        private final String logName;
        private final boolean log;

        ProcessLogger(final String prefix,
                      final InputStream stream,
                      final String logName,
                      final boolean log) {
            this.prefix = prefix;
            this.stream = new BufferedReader(new InputStreamReader(stream));
            this.logName = logName;
            this.log = log;
        }

        public void run() {
            String line;

            try {
                while ((line = stream.readLine()) != null) {
                    if (log) {
                        info(prefix, logName, line);
                    }
                }
            } catch (IOException ignore) {
            }
        }

        private void info(Object... args) {
            getLog().info(String.format("[%s] -- log(%s) -- %s", args));
        }
    }

}
