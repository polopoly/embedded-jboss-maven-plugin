package com.polopoly.jboss;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.manager.WagonManager;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.wagon.authentication.AuthenticationInfo;
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
public abstract class AbstractJBossMojo extends AbstractMojo {

    /**
     * The Maven Wagon manager to use when obtaining server authentication details.
     *
     * @component role="org.apache.maven.artifact.manager.WagonManager"
     */
    private WagonManager wagonManager;

    /**
     * The local repository where the artifacts are located.
     *
     * @parameter expression="${localRepository}"
     */
    private ArtifactRepository localRepository;

    /**
     * The id of the server configuration found in Maven settings.xml. This configuration will determine the
     * username/password to use when authenticating with the JBoss server. If no value is specified, a default username
     * and password will be used.
     *
     * @parameter expression="${jboss.serverId}"
     */
    private String serverId;

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

    /**
      * The Maven project object.
      *
      * @parameter expression="${project}"
      * @required
      * @readonly
      */
    private MavenProject project;

    /** @component */
    private ArtifactResolver resolver;

    /** @component */
    private ArtifactFactory factory;


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


    protected Artifact[] resolveArtifacts(ArtifactData[] artifacts) throws MojoExecutionException {
        Artifact[] mavenArtifacts = new Artifact[artifacts.length];
        for (int i = 0; i < artifacts.length; i++) {
            Artifact mavenArtifact =
                mavenArtifacts[i] =
                        factory.createBuildArtifact(artifacts[i].groupId,
                                                    artifacts[i].artifactId,
                                                    artifacts[i].version,
                                                    artifacts[i].packaging);
            try {
                resolver.resolve(mavenArtifact, project.getRemoteArtifactRepositories(), localRepository);
            } catch (Exception e) {
                throw new MojoExecutionException("Unable to resolve artifact", e);
            }
        }
        return mavenArtifacts;
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

    protected void info(String format, Object... args) {
        getLog().info(String.format("[JBOSS] " + format, args));
    }

    protected void warn(String format, Object... args) {
        getLog().warn(String.format("[JBOSS] " + format, args));
    }

    /**
     * Get the username configured in the Maven settings.xml
     *
     * @return username
     * @throws org.apache.maven.plugin.MojoExecutionException if the server is not configured in settings.xml
     */
    protected String getUsername()
        throws MojoExecutionException
    {
        if ( serverId != null )
        {
            // obtain authenication details for specified server from wagon
            AuthenticationInfo info = wagonManager.getAuthenticationInfo( serverId );
            if ( info == null )
            {
                throw new MojoExecutionException( "Server not defined in settings.xml: " + serverId );
            }

            return info.getUserName();
        }

        return null;
    }

    /**
     * Get the password configured in Maven settings.xml
     *
     * @return The password from settings.xml
     * @throws MojoExecutionException if the server is not configured in settings.xml
     */
    protected String getPassword()
        throws MojoExecutionException
    {
        if ( serverId != null )
        {
            // obtain authenication details for specified server from wagon
            AuthenticationInfo info = wagonManager.getAuthenticationInfo( serverId );
            if ( info == null )
            {
                throw new MojoExecutionException( "Server not defined in settings.xml: " + serverId );
            }

            return info.getPassword();
        }

        return null;
    }

}
