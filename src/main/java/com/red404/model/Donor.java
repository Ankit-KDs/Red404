package com.red404.model;

import java.sql.Date;
import java.sql.Timestamp;

public class Donor {
    private int id;
    private String name;
    private int age;
    private Date dob;
    private String bloodGroup;
    private String phone;
    private String email;
    private String address;
    private String password;
    private double latitude;
    private double longitude;
    private String zoneKey;
    private boolean available;
    private Date lastDonated;
    private Timestamp createdAt;

    public Donor() {}

    public Donor(String name, int age, Date dob, String bloodGroup,
                 String phone, String email, String address, String password,
                 double latitude, double longitude) {
        this.name = name;
        this.age = age;
        this.dob = dob;
        this.bloodGroup = bloodGroup;
        this.phone = phone;
        this.email = email;
        this.address = address;
        this.password = password;
        this.latitude = latitude;
        this.longitude = longitude;
        this.available = true;
    }

    // --- Getters & Setters ---
    public int getId()                   { return id; }
    public void setId(int id)            { this.id = id; }

    public String getName()              { return name; }
    public void setName(String name)     { this.name = name; }

    public int getAge()                  { return age; }
    public void setAge(int age)          { this.age = age; }

    public Date getDob()                 { return dob; }
    public void setDob(Date dob)         { this.dob = dob; }

    public String getBloodGroup()        { return bloodGroup; }
    public void setBloodGroup(String bg) { this.bloodGroup = bg; }

    public String getPhone()             { return phone; }
    public void setPhone(String phone)   { this.phone = phone; }

    public String getEmail()             { return email; }
    public void setEmail(String email)   { this.email = email; }

    public String getAddress()           { return address; }
    public void setAddress(String addr)  { this.address = addr; }

    public String getPassword()          { return password; }
    public void setPassword(String pw)   { this.password = pw; }

    public double getLatitude()          { return latitude; }
    public void setLatitude(double lat)  { this.latitude = lat; }

    public double getLongitude()         { return longitude; }
    public void setLongitude(double lon) { this.longitude = lon; }

    public String getZoneKey()           { return zoneKey; }
    public void setZoneKey(String zk)    { this.zoneKey = zk; }

    public boolean isAvailable()         { return available; }
    public void setAvailable(boolean av) { this.available = av; }

    public Date getLastDonated()         { return lastDonated; }
    public void setLastDonated(Date ld)  { this.lastDonated = ld; }

    public Timestamp getCreatedAt()      { return createdAt; }
    public void setCreatedAt(Timestamp t){ this.createdAt = t; }
}