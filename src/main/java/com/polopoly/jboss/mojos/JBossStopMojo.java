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

    /**
     * Wait for locks to disappear when stopping
     *
     * @parameter default-value="false" expression="${jboss.waitLock}"
     */
    protected boolean jbossWaitLock;

    public void execute() throws MojoExecutionException, MojoFailureException {
        stoppingJBoss();
        stoppingAdm();
    }

    private void stoppingAdm() throws MojoExecutionException {
        if (!shouldStartAdm()) {
            return;
        }
        info("Shutting down ADM Content Services " + isAdmPortRunning());
        if (!admLock.exists()) {
            info("lockFile does not exists");
            if (!isAdmPortRunning()) {
                info("ADM Content Services are already stopped");
                return;
            }
        }
        if (isAdmPortRunning()) {
            List<String> startOpts = new ArrayList<>();

            if (admDistributionFile == null) {
                if (admDistribution == null) {
                    throw new MojoExecutionException("Configure admDistribution");
                }
                admDistributionFile = resolveArtifact(admDistribution).getFile();
            }

            startOpts.add("-p");
            startOpts.add(admPort);

            startOpts.add("--stop");

            final String[] params = isWindows()
                ? createWindowsCommand(ADM_STARTUP_COMMAND, startOpts)
                : createUnixCommand(ADM_STARTUP_COMMAND, startOpts);

            info("Stop With\n" + arrayToString(params));

            ProcessBuilder pb = new ProcessBuilder(params);

            pb.directory(admHome);

            setupEnvironments(admEnvironments, pb);

            try {
                Process proc = pb.start();
                new ADMLogger(proc.getInputStream(), "out", logToConsole).start();
                new ADMLogger(proc.getErrorStream(), "err", logToConsole).start();

                proc.waitFor();
            } catch (Exception ioe) {
                throw new MojoExecutionException("Unable to stop ADM Content Services!", ioe);
            }
        }

        int maxRetry = retry;
        while (isAdmPortRunning()) {
            if (maxRetry-- <= 0) {
                throw new MojoExecutionException("timeout waiting for ADM Content Services to stop");
            }
            sleep("Interrupted while waiting for ADM Content Services to stop");
        }

        while (admLock.exists()) {
            if (maxRetry-- <= 0) {
                throw new MojoExecutionException("timeout waiting for ADM Content Services to stop");
            }
            sleep("Interrupted while waiting for ADM Content Services to stop");
        }
        info("ADM Content Services stopped!");
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
            } catch (RuntimeException ignore) {
            }
        } else {
            info("JBoss seems to be already down");
        }
        while(!isNamingPortFree()) {
            sleep("Interrupted while waiting for JBoss to stop");
        }
        debug("jbossWaitLock -> " + jbossWaitLock);
        if (jbossWaitLock) {
            if (jbossLock.exists()) {
                info("Waiting for " + jbossLock + " to disappear");
            }
            int maxRetry = retry;
            debug("jbossLock " + jbossLock.getAbsolutePath() + " exists: " + jbossLock.exists());
            while (jbossLock.exists()) {
                if (maxRetry-- <= 0) {
                    throw new MojoExecutionException("timeout waiting for JBOSS to stop");
                }
                sleep("Interrupted while waiting for JBOSS to stop");
                debug("exists: " + jbossLock.exists());
            }
        }
        info("JBOSS stopped!");
    }

    private boolean isStarted(final JBossOperations operations) {
        try {
            return operations.isStarted();
        } catch (Exception e) {
            return false;
        }
    }

}
