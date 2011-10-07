package com.polopoly.jboss;

/**
 * Created by bitter on 2011-10-06
 */
public class JBossDistribution extends ArtifactData {

    public static final JBossDistribution DEFAULT_JBOSS_DISTRIUTION =
            new JBossDistribution("com.polopoly.jboss", "slim-jboss-4.0.5", "1.0", "zip");

    public JBossDistribution(String groupId, String artifactId, String version, String packaging) {
        this.artifactId = artifactId;
        this.version = version;
        this.groupId = groupId;
        this.packaging = packaging;
    }
}
