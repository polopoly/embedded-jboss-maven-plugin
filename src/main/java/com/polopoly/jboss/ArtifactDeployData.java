package com.polopoly.jboss;

import java.util.StringJoiner;

/**
 * ArtifactDeployData
 *
 * @author mnova
 */
public class ArtifactDeployData extends ArtifactData {

    public String name;

    @Override
    public String toString() {
        return new StringJoiner(", ", ArtifactDeployData.class.getSimpleName() + "[", "]")
                .add("name='" + name + "'")
                .add("groupId='" + groupId + "'")
                .add("artifactId='" + artifactId + "'")
                .add("version='" + version + "'")
                .add("classifier='" + classifier + "'")
                .add("type='" + type + "'")
                .toString();
    }
}
