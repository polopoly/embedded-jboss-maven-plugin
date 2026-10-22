package com.polopoly.jboss.mojos;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class JBossInstallMojoTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Test
    public void testUnzipToTargetWithParentDirectorySegment() throws Exception {
        final File zip = createZip();
        tmp.newFolder("dist");

        // like jbossHome when maven is started with "-f dist/../pom.xml"
        final File target = new File(tmp.getRoot(), "dist/../embedded-jboss");

        JBossInstallMojo.unzip(zip, target);

        final File runSh = new File(tmp.getRoot(), "embedded-jboss/bin/run.sh");
        Assert.assertTrue(runSh.exists());
        Assert.assertEquals("#!/bin/sh", new String(Files.readAllBytes(runSh.toPath()), StandardCharsets.UTF_8));
    }

    private File createZip() throws IOException {
        final File zip = tmp.newFile("jboss.zip");
        try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zip))) {
            out.putNextEntry(new ZipEntry("bin/"));
            out.closeEntry();
            out.putNextEntry(new ZipEntry("bin/run.sh"));
            out.write("#!/bin/sh".getBytes(StandardCharsets.UTF_8));
            out.closeEntry();
        }
        return zip;
    }
}
