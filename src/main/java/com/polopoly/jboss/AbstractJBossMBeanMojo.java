package com.polopoly.jboss;

import java.io.IOException;
import java.net.Socket;
import java.util.Properties;

import javax.management.MBeanServerConnection;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.maven.plugin.MojoExecutionException;
import org.jboss.security.SecurityAssociation;
import org.jboss.security.SimplePrincipal;
import org.jnp.interfaces.NamingContext;

public abstract class AbstractJBossMBeanMojo
    extends AbstractJBossMojo
{
    /**
     * Maximum number of retries to get JBoss JMX MBean connection.
     *
     * @parameter default-value="60" expression="${jboss.retry}"
     */
    protected int retry;

    /**
     * Wait in seconds before each retry of the JBoss JMX MBean connection.
     *
     * @parameter default-value="1" expression="${jboss.retryWait}"
     */
    protected int retryWait;

    /**
     * The port for the naming service.
     *
     * @parameter default-value="1099" expression="${jboss.namingPort}"
     */
    protected String namingPort;

    private volatile MBeanServerConnection _connection;

    public MBeanServerConnection connect()
        throws MojoExecutionException
    {
        if (_connection != null) {
            return _connection;
        }

        info("Waiting to retrieve JBoss JMX MBean connection... ");
        InitialContext ctx = getInitialContext();

        // Try to get JBoss jmx MBean connection
        MBeanServerConnection server = null;
        NamingException ne = null;

        for (int i = 0; i < retry; ++i) {
            try {
                server = (MBeanServerConnection) ctx.lookup( "jmx/invoker/RMIAdaptor" );
                break;
            }
            catch (NamingException e) {
                ne = e;
                info("Waiting to retrieve JBoss JMX MBean connection... ");
            }

            sleep("Thread interrupted while waiting for MBean connection");
        }

        if (server == null) {
            throw new MojoExecutionException( "Unable to get JBoss JMX MBean connection: " + ne.getMessage(), ne );
        }

        _connection = server;

        return server;
    }

    /**
     * Determine whether <code>namingPort</code> is free.
     * @return
     */
    protected boolean isNamingPortFree()
    {
        try {
            new Socket("127.0.0.1", new Integer(namingPort));
        } catch (IOException e) {
            return false;
        }

        return true;
    }

    /**
     * Set up the context information for connecting the the jboss server.
     *
     * @return
     * @throws MojoExecutionException
     */
    protected InitialContext getInitialContext()
        throws MojoExecutionException
    {
        Properties env = new Properties();

        env.put(Context.INITIAL_CONTEXT_FACTORY, "org.jnp.interfaces.NamingContextFactory");
        env.put(Context.URL_PKG_PREFIXES, "org.jboss.naming:org.jnp.interfaces");
        env.put(Context.PROVIDER_URL, "127.0.0.1:" + namingPort);
        env.put(NamingContext.JNP_DISABLE_DISCOVERY, "true");

        String username = getUsername();

        if (username != null) {
            SecurityAssociation.setPrincipal(new SimplePrincipal(username));
            SecurityAssociation.setCredential(getPassword());
        }

        try {
            return new InitialContext( env );
        } catch (NamingException e) {
            throw new MojoExecutionException("Unable to instantiate naming context: " + e.getMessage(), e);
        }
    }

    protected void sleep(String interruptedMessage)
    {
        try {
            Thread.sleep(retryWait * 1000);
        } catch (InterruptedException e) {
            warn(interruptedMessage + "\n" + e.getMessage());
        }
    }
}
