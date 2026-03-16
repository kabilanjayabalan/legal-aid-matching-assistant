package com.legalaid.backend.dto;

public class CitizenProfileUpdateDTO {

    private String fullName;
    private String contactInfo;
    private String location;
    private Double latitude;
    private Double longitude;

    public String getFullName() {
        return fullName;
    }

    public String getContactInfo() {
        return contactInfo;
    }

    public String getLocation() {
        return location;
    }

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public void setContactInfo(String contactInfo) {
        this.contactInfo = contactInfo;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }
}
