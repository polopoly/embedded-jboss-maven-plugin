package com.polopoly.jboss;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.building.SettingsProblem;
import org.apache.maven.settings.crypto.DefaultSettingsDecryptionRequest;
import org.apache.maven.settings.crypto.SettingsDecrypter;
import org.apache.maven.settings.crypto.SettingsDecryptionResult;

public abstract class AbstractJBossMojo extends AbstractMojo {

    /**
     * The Maven settings, used to obtain server authentication details.
     */
    @Parameter(defaultValue = "${settings}", readonly = true)
    private Settings settings;

    /**
     * Used to decrypt the server passwords found in Maven settings.xml.
     */
    @Component
    private SettingsDecrypter settingsDecrypter;

    /**
     * The id of the server configuration found in Maven settings.xml. This configuration will determine the
     * username/password to use when authenticating with the JBoss server. If no value is specified, a default username
     * and password will be used.
     */
    @Parameter(property = "jboss.serverId", defaultValue = "default")
    protected String serverId;

    /**
     * The local repository where the artifacts are located.
     */
    @Parameter(defaultValue = "${localRepository}", required = true, readonly = true)
    private ArtifactRepository localRepository;

    /**
      * The Maven project object.
      */
    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    private MavenProject project;

    @Component
    private ArtifactResolver resolver;

    @Component
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
     * @return username, null if the server is not configured in settings.xml
     * @throws MojoExecutionException if the server password cannot be decrypted
     */
    protected String getUsername()
        throws MojoExecutionException
    {
        Server server = getServer();
        return server != null ? server.getUsername() : null;
    }

    /**
     * Get the password configured in Maven settings.xml
     *
     * @return The password from settings.xml, null if the server is not configured in settings.xml
     * @throws MojoExecutionException if the server password cannot be decrypted
     */
    protected String getPassword()
        throws MojoExecutionException
    {
        Server server = getServer();
        return server != null ? server.getPassword() : null;
    }

    /**
     * Find the (decrypted) server with id {@link #serverId} in Maven settings.xml, matching the id ignoring case
     * like the Maven 2 WagonManager did.
     */
    private Server getServer()
        throws MojoExecutionException
    {
        if (serverId == null || settings == null) {
            return null;
        }
        for (Server server : settings.getServers()) {
            if (serverId.equalsIgnoreCase(server.getId())) {
                SettingsDecryptionResult result = settingsDecrypter.decrypt(new DefaultSettingsDecryptionRequest(server));
                for (SettingsProblem problem : result.getProblems()) {
                    if (problem.getSeverity() != SettingsProblem.Severity.WARNING) {
                        throw new MojoExecutionException("Unable to decrypt server " + serverId + " from settings.xml: "
                                                         + problem.getMessage());
                    }
                }
                return result.getServer();
            }
        }
        return null;
    }

    protected void debug(String format, Object... args) {
        String message = args.length == 0 ? "[JBOSS] " + format : String.format("[JBOSS] " + format, args);
        getLog().debug(message);
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
