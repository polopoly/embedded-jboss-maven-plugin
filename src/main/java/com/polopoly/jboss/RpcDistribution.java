package com.polopoly.jboss;

public class RpcDistribution extends ArtifactData {

    public RpcDistribution() {
    }

    public RpcDistribution(String groupId, String artifactId, String version, String type) {
        this.artifactId = artifactId;
        this.version = version;
        this.groupId = groupId;
        this.type = type;
    }
}
