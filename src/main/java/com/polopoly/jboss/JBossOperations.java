package com.polopoly.jboss;

import javax.management.*;
import java.io.IOException;

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

    public void shutDown() {
        invoke("jboss.system:type=Server", "shutdown", null, null);
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
        } catch (Exception e) {
            throw new RuntimeException("Unable to retrieve mbean attribute '" + name + "'.'" + attribute + "'");
        }
    }

    private Object invoke(String name, String operation, Object[] types, String[] values) {
        return invoke(objectName(name), operation, types, values);
    }

    private Object invoke(ObjectName name, String operation, Object[] types, String[] values) {
        try {
            return _server.invoke(name, operation, types, values);
        } catch (Exception e) {
            throw new RuntimeException("Unable to execute mbean operation '" + name + "'.'" + operation + "'");
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
