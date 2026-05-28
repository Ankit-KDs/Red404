package com.red404.model;

import java.sql.Timestamp;

public class BloodRequest {
    private int id;
    private int hospitalId;
    private String hospitalName;   // joined field
    private Integer donorId;
    private String donorName;      // joined field
    private String bloodGroup;
    private int unitsNeeded;
    private String urgency;        // CRITICAL / HIGH / NORMAL
    private String status;         // PENDING / ACCEPTED / REJECTED / FULFILLED / EXPIRED
    private String message;
    private Double distanceKm;
    private Timestamp requestedAt;
    private Timestamp respondedAt;
    private String donorPhone;

    public BloodRequest() {}

    // --- Getters & Setters ---
    public int getId()                       { return id; }
    public void setId(int id)                { this.id = id; }

    public int getHospitalId()               { return hospitalId; }
    public void setHospitalId(int hid)       { this.hospitalId = hid; }

    public String getHospitalName()          { return hospitalName; }
    public void setHospitalName(String hn)   { this.hospitalName = hn; }

    public Integer getDonorId()              { return donorId; }
    public void setDonorId(Integer did)      { this.donorId = did; }

    public String getDonorPhone()            { return donorPhone; }
    public void setDonorPhone(String donorPhone) { this.donorPhone = donorPhone; }

    public String getDonorName()             { return donorName; }
    public void setDonorName(String dn)      { this.donorName = dn; }

    public String getBloodGroup()            { return bloodGroup; }
    public void setBloodGroup(String bg)     { this.bloodGroup = bg; }

    public int getUnitsNeeded()              { return unitsNeeded; }
    public void setUnitsNeeded(int u)        { this.unitsNeeded = u; }

    public String getUrgency()               { return urgency; }
    public void setUrgency(String urgency)   { this.urgency = urgency; }

    public String getStatus()                { return status; }
    public void setStatus(String status)     { this.status = status; }

    public String getMessage()               { return message; }
    public void setMessage(String msg)       { this.message = msg; }

    public Double getDistanceKm()            { return distanceKm; }
    public void setDistanceKm(Double d)      { this.distanceKm = d; }

    public Timestamp getRequestedAt()        { return requestedAt; }
    public void setRequestedAt(Timestamp t)  { this.requestedAt = t; }

    public Timestamp getRespondedAt()        { return respondedAt; }
    public void setRespondedAt(Timestamp t)  { this.respondedAt = t; }
}