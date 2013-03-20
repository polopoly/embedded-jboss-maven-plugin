package com.polopoly.jboss;

public class JBossDistribution extends ArtifactData {

    public static final JBossDistribution DEFAULT_JBOSS_DISTRIUTION =
            new JBossDistribution("com.polopoly.jboss", "slim-jboss-4.0.5", "1.5", "zip");

    public JBossDistribution() {
    }

    public JBossDistribution(String groupId, String artifactId, String version, String type) {
        this.artifactId = artifactId;
        this.version = version;
        this.groupId = groupId;
        this.type = type;
    }
}
