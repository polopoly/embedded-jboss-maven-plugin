package com.polopoly.jboss;

public class ArtifactData {

    public String groupId;
    public String artifactId;
    public String version;
    public String classifier;
    public String type = "jar";

    @Override
    public String toString() {
        return "ArtifactData{" +
                "groupId='" + groupId + '\'' +
                ", artifactId='" + artifactId + '\'' +
                ", version='" + version + '\'' +
                ", classifier='" + classifier + '\'' +
                ", type='" + type + '\'' +
                '}';
    }
}
