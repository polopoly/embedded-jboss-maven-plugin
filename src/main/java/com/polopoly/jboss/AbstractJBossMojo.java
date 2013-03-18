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

/**
 * Created by bitter on 2011-10-08
 */
public abstract class AbstractJBossMojo extends AbstractMojo {

    /**
     * The Maven Wagon manager to use when obtaining server authentication details.
     *
     * @component role="org.apache.maven.artifact.manager.WagonManager"
     */
    private WagonManager wagonManager;

    /**
     * The id of the server configuration found in Maven settings.xml. This configuration will determine the
     * username/password to use when authenticating with the JBoss server. If no value is specified, a default username
     * and password will be used.
     *
     * @parameter default-value="default" expression="${jboss.serverId}"
     */
    protected String serverId;

    /**
     * The local repository where the artifacts are located.
     *
     * @parameter expression="${localRepository}"
     * @required
     * @readonly
     */
    private ArtifactRepository localRepository;
    
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

    /**
     * Create Artifact object and make sure it is locally available.
     * @param artifact
     * @return
     * @throws MojoExecutionException
     */
    protected Artifact resolveArtifact(ArtifactData artifact) throws MojoExecutionException {
        return resolveArtifacts(new ArtifactData[]{ artifact} )[0];
    }

    /**
     * Create Artifact objects and make sure they are locally available.
     * @param artifacts
     * @return
     * @throws MojoExecutionException
     */
    protected Artifact[] resolveArtifacts(ArtifactData[] artifacts) throws MojoExecutionException {
        Artifact[] mavenArtifacts = new Artifact[artifacts.length];
        for (int i = 0; i < artifacts.length; i++) {
            Artifact mavenArtifact =
                mavenArtifacts[i] =
                        factory.createArtifactWithClassifier(artifacts[i].groupId,
                                                             artifacts[i].artifactId,
                                                             artifacts[i].version,
                                                             artifacts[i].type,
                                                             artifacts[i].classifier);
            try {
                resolver.resolve(mavenArtifact, project.getRemoteArtifactRepositories(), localRepository);
            } catch (Exception e) {
                throw new MojoExecutionException("Unable to resolve artifact", e);
            }
        }
        return mavenArtifacts;
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
    
    protected void info(String format, Object... args) {
        String message = args.length == 0 ? "[JBOSS] " + format : String.format("[JBOSS] " + format, args);
        getLog().info(message);
    }

    protected void warn(String format, Object... args) {
        String message = args.length == 0 ? "[JBOSS] " + format : String.format("[JBOSS] " + format, args);
        getLog().warn(message);
    }
}
