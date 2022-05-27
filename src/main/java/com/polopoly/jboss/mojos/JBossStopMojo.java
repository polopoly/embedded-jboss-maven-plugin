package com.polopoly.jboss.mojos;

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import com.polopoly.jboss.JBossOperations;

/**
 * Will stop the JBoss server started with jboss:start
 * @goal stop
 * @aggregator
 */
public class JBossStopMojo extends JBossStartMojo {

    public void execute() throws MojoExecutionException, MojoFailureException {
        stoppingJBoss();
        stoppingRPC();
    }

    private void stoppingRPC() throws MojoExecutionException {
        info("Shutting down RPC " + isRpcPortRunning());
        if (isRpcPortRunning()) {
            List<String> startOpts = new ArrayList<String>();

            if (rpcDistributionFile == null) {
                if (rpcDistribution == null) {
                    throw new MojoExecutionException("Configure rpcDistribution");
                }
                rpcDistributionFile = resolveArtifact(rpcDistribution).getFile();
            }

            startOpts.add("java");
            startOpts.add("-jar");
            startOpts.add("./lib/" + rpcDistributionFile.getName());

            startOpts.add("-p");
            startOpts.add(rpcPort);

            startOpts.add("-s");

            final String[] params = isWindows()
                    ? startOpts.toArray(new String[] {})
                    : startOpts.toArray(new String[] {});

            System.out.println("Start With\n" + arrayToString(params));

            ProcessBuilder pb = new ProcessBuilder(params);

            pb.directory(rpcHome);

            try {
                Process proc = pb.start();
                new JBossLogger(proc.getInputStream(), "out", logToConsole).start();
                new JBossLogger(proc.getErrorStream(), "err", logToConsole).start();

                proc.waitFor();
            } catch (Exception ioe) {
                throw new MojoExecutionException("Unable to stop rpc!", ioe);
            }
        }

        while (isRpcPortRunning()) {
            sleep("Interrupted while waiting for RPC to stop");
        }

        while (rpcLock.exists()) {
            sleep("Interrupted while waiting for RPC to stop");
        }
    }

    private void stoppingJBoss() throws MojoExecutionException {
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
