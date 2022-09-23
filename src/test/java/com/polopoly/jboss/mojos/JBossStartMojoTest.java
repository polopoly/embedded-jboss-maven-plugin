package com.polopoly.jboss.mojos;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

public class JBossStartMojoTest {

    final JBossStartMojo target = new JBossStartMojo();
    
    @Test
    public void testCreateUnixCommand() {
        List<String> startOpts = new ArrayList<>();
        startOpts.add("-p");
        startOpts.add("8090");

        startOpts.add("--stop");

        final String[] params = target.createUnixCommand(JBossStartMojo.ADM_STARTUP_COMMAND, startOpts);
        Assert.assertArrayEquals(new String[] {
            "./bin/run.sh",
            "-p",
            "8090",
            "--stop"
        }, params);
    }

    @Test
    public void testCreateWindowsCommand() {
        List<String> startOpts = new ArrayList<>();
        startOpts.add("-p");
        startOpts.add("8090");

        startOpts.add("--stop");

        final String[] params = target.createWindowsCommand(JBossStartMojo.ADM_STARTUP_COMMAND, startOpts);
        Assert.assertArrayEquals(new String[] {
            "cmd",
            "/c",
            ".\\bin\\run.bat",
            "-p",
            "8090",
            "--stop"
        }, params);
    }
}
