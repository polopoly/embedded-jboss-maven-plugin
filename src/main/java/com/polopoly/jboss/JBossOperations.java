package com.polopoly.jboss;

import org.apache.maven.plugin.MojoExecutionException;

import javax.management.MBeanException;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.OperationsException;
import java.net.URL;

/**
 * Created by bitter on 2011-10-07
 */
public class JBossOperations {

    private MBeanServerConnection _server;

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
        invoke("jboss.system:service=MainDeployer", "redeploy", url);
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
