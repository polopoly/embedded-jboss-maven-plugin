package com.polopoly.jboss;

import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.mojo.jboss.JBossServerUtil;
import org.jboss.security.SecurityAssociation;
import org.jboss.security.SimplePrincipal;

import javax.management.MBeanServerConnection;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.File;
import java.io.IOException;
import java.rmi.RMISecurityManager;
import java.util.Properties;

/**
 * Created by bitter on 2011-10-07
 */
public abstract class AbstractJBossMBeanMojo extends AbstractJBossMojo {

    /**
     * Maximum number of retries to get JBoss JMX MBean connection.
     *
     * @parameter default-value="10" expression="${jboss.retry}"
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


    public MBeanServerConnection connect() throws MojoExecutionException {

        InitialContext ctx = getInitialContext();

        // Try to get JBoss jmx MBean connection
        MBeanServerConnection server = null;
        NamingException ne = null;
        for ( int i = 0; i < retry; ++i )
        {
            try
            {
                server = (MBeanServerConnection) ctx.lookup( "jmx/invoker/RMIAdaptor" );
                break;
            }
            catch ( NamingException e )
            {
                ne = e;
                info("Waiting to retrieve JBoss JMX MBean connection... ");
            }
            try {
                Thread.sleep(retryWait);
            }
            catch ( InterruptedException e )
            {
                warn("Thread interrupted while waiting for MBean connection: " + e.getMessage());
            }
        }

        if ( server == null )
        {
            throw new MojoExecutionException( "Unable to get JBoss JMX MBean connection: " + ne.getMessage(), ne );
        }
        return server;
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
        env.put( Context.INITIAL_CONTEXT_FACTORY, "org.jnp.interfaces.NamingContextFactory" );
        env.put( Context.URL_PKG_PREFIXES, "org.jboss.naming:org.jnp.interfaces" );
        env.put( Context.PROVIDER_URL, "127.0.0.1:" + namingPort );

        String username = getUsername();
        if ( username != null )
        {
            SecurityAssociation.setPrincipal(new SimplePrincipal(username));
            SecurityAssociation.setCredential(getPassword());
        }

        try
        {
            return new InitialContext( env );
        }
        catch ( NamingException e )
        {
            throw new MojoExecutionException( "Unable to instantiate naming context: " + e.getMessage(), e );
        }
    }

    /**
     * Set up the security manager to allow remote code to execute.
     */
    protected void initializeRMISecurityPolicy() {
        try
        {
            File policyFile = File.createTempFile( "jboss-client", ".policy" );
            policyFile.deleteOnExit();
            JBossServerUtil.writeSecurityPolicy(policyFile);
            // Get sthe canonical file which expands the shortened directory names in Windows
            policyFile = policyFile.getCanonicalFile();
            System.setProperty( "java.security.policy", policyFile.toURI().toString() );
            System.setSecurityManager( new RMISecurityManager() );
        }
        catch ( IOException e )
        {
            warn("Unable to create security policy file for loading remote classes: " + e.getMessage(), e);
            warn("Will try to load required classes from local classpath.");
        }
        catch ( SecurityException e )
        {
            warn("Unable to set security manager for loading remote classes: " + e.getMessage(), e);
            warn("Will try to load required classes from local classpath.");
        }
    }
}
