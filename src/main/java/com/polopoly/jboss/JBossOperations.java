package com.polopoly.jboss;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.MBeanException;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.OperationsException;

import org.apache.maven.plugin.MojoExecutionException;

/**
 * Created by bitter on 2011-10-07
 */
public class JBossOperations {

    private MBeanServerConnection _server;

    private Logger LOG = Logger.getLogger(getClass().getName());

    public JBossOperations(MBeanServerConnection server) {
        _server = server;
    }

    public boolean isStarted() {
        return (Boolean) getAttribute("jboss.system:type=Server", "Started");
    }

    public boolean isDeployed(URL url) throws MojoExecutionException {
        return (Boolean) invoke("jboss.system:service=MainDeployer", "isDeployed", url);
    }

    public void redeploy(URL url) throws MojoExecutionException {
        LOG.log(Level.INFO, "*****");
        LOG.log(Level.INFO, "Starting JBoss debugging information");
        LOG.log(Level.INFO, "*****");

        try {
            checkFileExists(url);
            tryReadingFile(url);

            invoke("jboss.system:service=MainDeployer", "redeploy", url);
        } finally {
            LOG.log(Level.INFO, "*****");
            LOG.log(Level.INFO, "Ending JBoss debugging information");
            LOG.log(Level.INFO, "*****");
        }
    }

    private void tryReadingFile(final URL url)
    {
        try {
            url.openStream().close();

            LOG.log(Level.INFO, "Successfully read and closed stream for given url (1)...");
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Error while reading file (1)", e);
        }

        try {
            InputStream stream = url.openStream();
            stream.close();

            LOG.log(Level.INFO, "Successfully read and closed stream for given url (2)...");
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Error while reading file (2)", e);
        }
    }

    private void checkFileExists(final URL url)
    {
        try {
            if (url.getProtocol().startsWith("file")) {
                File urlFile = new File(url.getFile());
                String path = url.toExternalForm();

                LOG.log(Level.INFO, String.format("File is '%s'", urlFile.getAbsoluteFile().getAbsolutePath()));
                LOG.log(Level.INFO, String.format("File external form is '%s'", path));

                LOG.log(Level.INFO, String.format("Does file exist according to java.io.File: %b", urlFile.exists()));

                // check if the problem is only one '/'...
                if (!path.startsWith("file://")) {
                    String newPath = path.replace("file:/", "file://");
                    File newFile = new File(new URL(newPath).getFile());

                    LOG.log(Level.INFO, String.format("Does file '%s' exist according to java.io.File: %b", newPath, newFile.exists()));
                }

                // check lowercase as well...
                String lowerCasePath = path.toLowerCase();
                File lowerCaseFile = new File(new URL(lowerCasePath).getFile());

                LOG.log(Level.INFO, String.format("Does file '%s' exist according to java.io.File: %b", lowerCasePath, lowerCaseFile.exists()));
            } else {
                LOG.log(Level.INFO, "URL is not a file url...");
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Error while checking file existance", e);
        }
    }

    public void shutDown() throws MojoExecutionException {
        invoke("jboss.system:type=Server", "shutdown");
    }

    // -----------------------------------------------------------
    // MBean manipulators
    // -----------------------------------------------------------
    private Object getAttribute(String name, String attribute) {
        return getAttribute(objectName(name), attribute);
    }

    private Object getAttribute(ObjectName name, String attribute) {
        try {
            return _server.getAttribute(name, attribute);
        } catch (OperationsException oe) {
            return null;
        } catch (Exception e) {
            throw new RuntimeException("Unable to retrieve mbean attribute '" + name + "'.'" + attribute + "'");
        }
    }

    private Object invoke(String name, String operation) throws MojoExecutionException {
        return invoke(objectName(name), operation, null, null);
    }

    private Object invoke(String name, String operation, Object value) throws MojoExecutionException {
        return invoke(objectName(name), operation, new Object[]{value}, new String[]{value.getClass().getName()});
    }

    private Object invoke(ObjectName name, String operation, Object[] values, String[] types) throws MojoExecutionException {
        try {
            return _server.invoke(name, operation, values, types);
        } catch (MBeanException e) {
            throw new MojoExecutionException("Failed to invoke operation '" + operation + "'", e);
        } catch (Exception e) {
            throw new MojoExecutionException("Unable to execute mbean operation '" + name + "'.'" + operation + "'", e);
        }
    }

    private ObjectName objectName(String name) {
        try {
            return new ObjectName(name);
        } catch (MalformedObjectNameException e) {
            throw new RuntimeException("Invalid object name '" + name + "'", e);
        }
    }
}
