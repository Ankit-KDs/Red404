package com.red404.model;

import java.sql.Timestamp;

public class Hospital {
    private int id;
    private String name;
    private String registrationId;
    private String phone;
    private String email;
    private String adminName;
    private String adminPhone;
    private String adminEmail;
    private String address;
    private String password;
    private double latitude;
    private double longitude;
    private String zoneKey;
    private Timestamp createdAt;

    public Hospital() {}

    public Hospital(String name, String registrationId, String phone, String email,
                    String adminName, String adminPhone, String adminEmail,
                    String address, String password, double latitude, double longitude) {
        this.name = name;
        this.registrationId = registrationId;
        this.phone = phone;
        this.email = email;
        this.adminName = adminName;
        this.adminPhone = adminPhone;
        this.adminEmail = adminEmail;
        this.address = address;
        this.password = password;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    // --- Getters & Setters ---
    public int getId()                      { return id; }
    public void setId(int id)               { this.id = id; }

    public String getName()                 { return name; }
    public void setName(String n)           { this.name = n; }

    public String getRegistrationId()       { return registrationId; }
    public void setRegistrationId(String r) { this.registrationId = r; }

    public String getPhone()                { return phone; }
    public void setPhone(String p)          { this.phone = p; }

    public String getEmail()                { return email; }
    public void setEmail(String e)          { this.email = e; }

    public String getAdminName()            { return adminName; }
    public void setAdminName(String an)     { this.adminName = an; }

    public String getAdminPhone()           { return adminPhone; }
    public void setAdminPhone(String ap)    { this.adminPhone = ap; }

    public String getAdminEmail()           { return adminEmail; }
    public void setAdminEmail(String ae)    { this.adminEmail = ae; }

    public String getAddress()              { return address; }
    public void setAddress(String addr)     { this.address = addr; }

    public String getPassword()             { return password; }
    public void setPassword(String pw)      { this.password = pw; }

    public double getLatitude()             { return latitude; }
    public void setLatitude(double lat)     { this.latitude = lat; }

    public double getLongitude()            { return longitude; }
    public void setLongitude(double lon)    { this.longitude = lon; }

    public String getZoneKey()              { return zoneKey; }
    public void setZoneKey(String zk)       { this.zoneKey = zk; }

    public Timestamp getCreatedAt()         { return createdAt; }
    public void setCreatedAt(Timestamp t)   { this.createdAt = t; }
}