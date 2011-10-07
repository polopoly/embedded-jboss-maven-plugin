package com.polopoly.jboss;

/**
 * Created by bitter on 2011-10-07
 */
public class ArtifactData {

    public String groupId;
    public String artifactId;
    public String version;
    public String packaging;

    @Override
    public String toString() {
        return "ArtifactData{" +
                "groupId='" + groupId + '\'' +
                ", artifactId='" + artifactId + '\'' +
                ", version='" + version + '\'' +
                ", packaging='" + packaging + '\'' +
                '}';
    }
}
