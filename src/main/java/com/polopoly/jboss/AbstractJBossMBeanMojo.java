package com.polopoly.jboss;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.StandardCharsets;
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

    /**
     * The ip to bind to. Defaults to localhost, but can be changed when needed,
     * for example use -Djboss.bindAddress=0.0.0.0 to bind to all interfaces.
     * @parameter default-value="localhost" expression="${jboss.bindAddress}"
     */
    protected String bindAddress;

    /**
     * The port for the rpc service.
     *
     * @parameter default-value="8090" expression="${jboss.rpcPort}"
     */
    protected String rpcPort;

    private volatile MBeanServerConnection _connection;

    public MBeanServerConnection connect(final boolean canBeStopped)
        throws MojoExecutionException
    {
        if (_connection != null) {
            return _connection;
        }

        info("Waiting to retrieve JBoss JMX MBean connection... ");
        InitialContext ctx = null;
        try {
            ctx = getInitialContext();
        } catch (MojoExecutionException e) {
            e.printStackTrace();
            throw e;
        }

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
                if (!canBeStopped && isNamingPortFree()) {
                    warn("JBoss does not seems to be up anymore.");
                    break;
                }
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
            new Socket(getAddress(), new Integer(namingPort));
        } catch (IOException e) {
            return false;
        }

        return true;
    }

    /**
     * Determine whether <code>rpcPort</code> is free.
     * @return
     */
    protected boolean isRpcPortRunning()
    {
        try {
            final URL url = new URL(String.format("http://%s:%s/internal/running", getAddress(), rpcPort));
            final HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            try {
                //final byte[] postDataBytes = "".getBytes(StandardCharsets.UTF_8);
                //conn.setRequestMethod("POST");
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Accept", "application/json");
                conn.setRequestProperty("Accept-Charset", "UTF-8");
                //conn.setRequestProperty("Content-Length", String.valueOf(postDataBytes.length));
                conn.setRequestProperty("User-Agent", "jboss-maven-plugin");
                /*
                conn.setDoOutput(true);

                try (OutputStream output = conn.getOutputStream()) {
                    output.write(postDataBytes);
                }
                */

                final int status = conn.getResponseCode();
                info("status " + status);
                if (status != 200) {
                    return false;
                }

                final String output = getResponseContent(conn);
                info(output);
                return output.contains("OK");
            } finally {
                conn.disconnect();
            }
        } catch (IOException e) {
            return false;
        }
    }

    private String getResponseContent(final HttpURLConnection conn) throws IOException {
        try (Reader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            final StringBuilder sb = new StringBuilder();
            for (int c; (c = in.read()) >= 0; ) {
                sb.append((char) c);
            }
            return sb.toString();
        }
    }

    private String getAddress() {
        if ("0.0.0.0".equals(bindAddress)) {
            return "localhost";
        }
        return bindAddress;
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
        env.put(Context.PROVIDER_URL, getAddress() + ":" + namingPort);
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

    protected boolean isWindows() {
        final String osName = System.getProperty("os.name");
        return osName.startsWith("Windows");
    }
}
