package com.polopoly.jboss.mojos;

import com.polopoly.jboss.Environment;
import com.polopoly.jboss.JBossOperations;
import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.wagon.ConnectionException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

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
     * Will install a JBoss server and start it. If 'namingPort' is occupied the mojo will abort with an exception.
     *
     * @throws MojoExecutionException
     * @throws MojoFailureException
     */
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {


        try {
            System.out.println("Attempting to start ZooKeeper.");
            runScript(kafkaHome, "bin/zookeeper-server-start.sh config/zookeeper.properties >zookeeper.log & disown");
            final int zooPort = Integer.parseInt(getProperty(new File(kafkaHome, "config/zookeeper.properties"), "clientPort"));
            waitFor(zooPort);
            System.out.println("ZooKeeper is running.");

            System.out.println("Attempting to start Kafka.");
            runScript(kafkaHome, "bin/kafka-server-start.sh config/server.properties >kafka.log & disown");
            final int kafkaPort = Integer.parseInt(getProperty(new File(kafkaHome, "config/server.properties"), "port"));
            waitFor(kafkaPort);
            System.out.println("Kafka is running.");

            System.out.println("Attempting to start Solr.");
            Process solrProcess = runScript(solrHome, "bin/solr start -e cloud -noprompt");
            solrProcess.waitFor();
            waitFor(8983);
            waitFor(7574);
            System.out.println("Solr is running.");

            Runtime.getRuntime().addShutdownHook(new Thread(){
                @Override
                public void run() {
                    try {
                        System.out.println("Stopping Kafka.");
                        runScript(kafkaHome, "bin/kafka-server-stop.sh");
                        waitUntil(kafkaPort);
                        System.out.println("Kafka stopped.");

                        System.out.println("Stopping ZooKeeper.");
                        runScript(kafkaHome, "lsof -t -i :" + zooPort + " | xargs kill -15");
                        waitUntil(zooPort);
                        System.out.println("ZooKeeper stopped.");

                        System.out.println("Stoppng Solr.");
                        Process stopProcess = runScript(solrHome, "bin/solr stop -all");
                        stopProcess.waitFor();
                        waitUntil(8983);
                        waitUntil(7574);
                        System.out.println("Solr stopped.");
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });
        } catch (Exception e) {
            throw new MojoExecutionException("External Servers could not be started.");
        }

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
            startOpts.add(bindAddress);

            String osName = System.getProperty("os.name");

            ProcessBuilder pb = new ProcessBuilder(osName.startsWith("Windows") ? createWindowsCommand(startOpts) : createUnixCommand(startOpts));

            pb.directory(new File(jbossHome, "bin"));
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
        String jbossWindowsCommand = STARTUP_COMMAND + ".bat";
        List<String> commandWithOptions = new ArrayList<String>();

        commandWithOptions.addAll(Arrays.asList("cmd", "/c"));
        commandWithOptions.add(jbossWindowsCommand);
        commandWithOptions.addAll(startOpts);

        return commandWithOptions.toArray(new String[0]);
    }

    private String[] createUnixCommand(final List<String> startOpts)
    {
        String jbossUnixCommand = "./" + STARTUP_COMMAND + ".sh";
        List<String> commandWithOptions = new ArrayList<String>();

        commandWithOptions.add(jbossUnixCommand);
        commandWithOptions.addAll(startOpts);

        return commandWithOptions.toArray(new String[commandWithOptions.size()]);
    }

    private Process runScript(File executingDir, String script) throws IOException {
        File file = File.createTempFile("file" + Math.random(), ".sh", executingDir);
        file.setExecutable(true);
        file.deleteOnExit();

        FileWriter fw = null;
        try{
            fw = new FileWriter(file);
            fw.write(script);
        } finally {
            IOUtils.closeQuietly(fw);
        }

        return new ProcessBuilder()
                .directory(executingDir)
                .inheritIO()
                .command(file.getAbsolutePath())
                .start();
    }

    private String getProperty(File file, String key) throws IOException {
        Properties props = new Properties();
        FileInputStream in = null;
        try {
            in = new FileInputStream(file);
            props.load(in);
        } finally {
            IOUtils.closeQuietly(in);
        }

        return (String) props.get(key);
    }

    private void waitFor(int port) throws ConnectionException {
        for(int tries = 0; tries < 3; tries++) {
            try {
                new Socket("localhost", port).close();
                return;
            } catch (IOException e) {
                System.out.println("Server localhost:" + port + " not up yet. Retrying...");
                sleep(3000);
            }
        }
        throw new ConnectionException("Server did not respond after 3 tries.");
    }

    private void waitUntil(int port) {
        try {
            Socket socket = new Socket("localhost", port);
            InputStream in = socket.getInputStream();
            while(in != null && in.read() != -1) {
                sleep(1000);
            }
        } catch (IOException e) {
        }
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


    static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
