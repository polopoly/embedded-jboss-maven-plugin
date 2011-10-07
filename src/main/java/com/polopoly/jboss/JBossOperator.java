package com.polopoly.jboss;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.mojo.jboss.JBossServerUtil;
import org.jboss.security.SecurityAssociation;
import org.jboss.security.SimplePrincipal;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
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
public class JBossOperator {

    private final int _port;
    private final String _username;
    private final String _password;

    private final Log _log;
    private final int _numberOfRetries;
    private final int _secondsBetweenRetries;

    public JBossOperator(int port,
                         String username, String password,
                         int numberOfRetries, int secondsBetweenRetries,
                         Log log)
    {
        _port = port;
        _username = username;
        _password = password;

        _numberOfRetries = numberOfRetries;
        _secondsBetweenRetries = secondsBetweenRetries;

        _log = log;
    }


    public MBeanServerConnection connect() throws MojoExecutionException {

        InitialContext ctx = getInitialContext();

        // Try to get JBoss jmx MBean connection
        MBeanServerConnection server = null;
        NamingException ne = null;
        for ( int i = 0; i < _numberOfRetries; ++i )
        {
            try
            {
                Thread.sleep(_secondsBetweenRetries * 1000);
                server = (MBeanServerConnection) ctx.lookup( "jmx/invoker/RMIAdaptor" );
                break;
            }
            catch ( NamingException e )
            {
                ne = e;
                _log.info("Waiting to retrieve JBoss JMX MBean connection... ");
            }
            catch ( InterruptedException e )
            {
                _log.warn("Thread interrupted while waiting for MBean connection: " + e.getMessage());
                e.printStackTrace();
            }
        }

        if ( server == null )
        {
            throw new MojoExecutionException( "Unable to get JBoss JMX MBean connection: " + ne.getMessage(), ne );
        }
        return server;
    }

    /**
     * Check if the server has finished startup. Will throw one of several exceptions if the server connection fails.
     *
     * @param server
     * @return
     * @throws Exception
     */
    protected boolean isStarted( MBeanServerConnection server )
        throws Exception
    {
        ObjectName serverMBeanName = new ObjectName( "jboss.system:type=Server" );
        return ( (Boolean) server.getAttribute( serverMBeanName, "Started" ) ).booleanValue();
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
        env.put( Context.PROVIDER_URL, "127.0.0.1:" + _port );

        if ( _username != null )
        {
            SecurityAssociation.setPrincipal(new SimplePrincipal(_username));
            SecurityAssociation.setCredential(_password);
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
            _log.warn("Unable to create security policy file for loading remote classes: " + e.getMessage(), e);
            _log.warn("Will try to load required classes from local classpath.");
        }
        catch ( SecurityException e )
        {
            _log.warn("Unable to set security manager for loading remote classes: " + e.getMessage(), e);
            _log.warn("Will try to load required classes from local classpath.");
        }
    }
}
