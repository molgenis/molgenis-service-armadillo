package org.molgenis.armadillo.controller;

public class ProfileDefinition {
    public String getProfileName() {
        return profileName;
    }

    public void setProfileName(String profileName) {
        this.profileName = profileName;
    }

    public String getProfileImage() {
        return profileImage;
    }

    public void setProfileImage(String profileImage) {
        this.profileImage = profileImage;
    }

    public String getProfilePort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    private String profileName;
    private String profileImage;
    private String port;


}
