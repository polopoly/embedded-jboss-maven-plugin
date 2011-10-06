package com.polopoly.jboss;

/**
 * Created by bitter on 2011-10-06
 */
public class JBossArtifact {

    String artifactId;
    String groupId;
    String version;

    @Override
    public String toString() {
        return "JBossArtifact{" +
                "artifactId='" + artifactId + '\'' +
                ", groupId='" + groupId + '\'' +
                ", version='" + version + '\'' +
                '}';
    }
}
