package com.polopoly.jboss;

public class AdmDistribution extends ArtifactData {

    public AdmDistribution() {
    }

    public AdmDistribution(final String groupId,
                           final String artifactId,
                           final String version,
                           final String type,
                           final String classifier) {
        this.artifactId = artifactId;
        this.version = version;
        this.groupId = groupId;
        this.type = type;
        this.classifier = classifier;
    }
}
