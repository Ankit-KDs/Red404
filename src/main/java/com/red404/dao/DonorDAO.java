package com.red404.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.red404.model.Donor;
import com.red404.util.DBConnection;
import com.red404.util.HaversineUtil;

/**
 * DonorDAO — CRUD + zone-based retrieval for donors.
 *
 * DSA used:
 *  - Zone/Grid HashMap → zone_key column for O(1) bucket retrieval
 *  - Binary Search     → getDonorsByBloodGroup returns sorted list; caller can binarySearch
 */
public class DonorDAO {

    // ── Register ────────────────────────────────────────────────────
    public boolean register(Donor d) throws SQLException {
        String sql = "INSERT INTO donors (name,age,dob,blood_group,phone,email,address,password,latitude,longitude,zone_key) "
                   + "VALUES (?,?,?,?,?,?,?,?,?,?,?)";
        String zoneKey = HaversineUtil.zoneKey(d.getLatitude(), d.getLongitude());
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, d.getName());
            ps.setInt(2, d.getAge());
            ps.setDate(3, d.getDob());
            ps.setString(4, d.getBloodGroup());
            ps.setString(5, d.getPhone());
            ps.setString(6, d.getEmail());
            ps.setString(7, d.getAddress());
            ps.setString(8, d.getPassword());
            ps.setDouble(9, d.getLatitude());
            ps.setDouble(10, d.getLongitude());
            ps.setString(11, zoneKey);
            int rows = ps.executeUpdate();
            if (rows > 0) {
                ResultSet rs = ps.getGeneratedKeys();
                if (rs.next()) d.setId(rs.getInt(1));
            }
            return rows > 0;
        }
    }

    // ── Login (by email) ─────────────────────────────────────────────
    public Donor findByEmail(String email) throws SQLException {
        String sql = "SELECT * FROM donors WHERE email = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        }
        return null;
    }

    // ── Get by ID ────────────────────────────────────────────────────
    public Donor findById(int id) throws SQLException {
        String sql = "SELECT * FROM donors WHERE id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        }
        return null;
    }

    // ── Update Profile ───────────────────────────────────────────────
    public boolean updateProfile(Donor d) throws SQLException {
        String sql = "UPDATE donors SET name=?, phone=?, address=?, latitude=?, longitude=?, zone_key=?, is_available=? WHERE id=?";
        String zoneKey = HaversineUtil.zoneKey(d.getLatitude(), d.getLongitude());
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, d.getName());
            ps.setString(2, d.getPhone());
            ps.setString(3, d.getAddress());
            ps.setDouble(4, d.getLatitude());
            ps.setDouble(5, d.getLongitude());
            ps.setString(6, zoneKey);
            ps.setBoolean(7, d.isAvailable());
            ps.setInt(8, d.getId());
            return ps.executeUpdate() > 0;
        }
    }

    // ── Update Password ──────────────────────────────────────────────
    public boolean updatePassword(int id, String hashedPassword) throws SQLException {
        String sql = "UPDATE donors SET password=? WHERE id=?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, hashedPassword);
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        }
    }

    // ── Zone-based donor search (DSA: Grid Hashing) ──────────────────
    /**
     * Returns available donors with matching blood group in neighbouring zones.
     * Uses Haversine to filter precisely within radiusKm.
     * Sorted by distance ASC (natural ordering for PriorityQueue on caller side).
     */
    // public List<Donor> findNearbyDonors(String bloodGroup, double hospitalLat,
    //                                      double hospitalLon, double radiusKm) throws SQLException {
    //     String[] zones = HaversineUtil.neighbouringZones(hospitalLat, hospitalLon);
    //     StringBuilder inClause = new StringBuilder();
    //     for (int i = 0; i < zones.length; i++) {
    //         inClause.append(i == 0 ? "?" : ",?");
    //     }
    //     String sql = "SELECT * FROM donors WHERE blood_group=? AND is_available=TRUE AND zone_key IN (" + inClause + ")";
    //     List<Donor> result = new ArrayList<>();
    //     try (Connection con = DBConnection.getConnection();
    //          PreparedStatement ps = con.prepareStatement(sql)) {
    //         ps.setString(1, bloodGroup);
    //         for (int i = 0; i < zones.length; i++) ps.setString(i + 2, zones[i]);
    //         ResultSet rs = ps.executeQuery();
    //         while (rs.next()) {
    //             Donor d = mapRow(rs);
    //             double dist = HaversineUtil.distance(hospitalLat, hospitalLon, d.getLatitude(), d.getLongitude());
    //             if (dist <= radiusKm) {
    //                 result.add(d);
    //             }
    //         }
    //     }
    //     // Sort by distance (DSA: Comparable sort)
    //     result.sort((a, b) -> Double.compare(
    //         HaversineUtil.distance(hospitalLat, hospitalLon, a.getLatitude(), a.getLongitude()),
    //         HaversineUtil.distance(hospitalLat, hospitalLon, b.getLatitude(), b.getLongitude())
    //     ));
    //     return result;
    // }

    // ── Zone-based donor search (DSA: Dynamic Grid Hashing & Optional Filter) ──
    public List<Donor> findNearbyDonors(String bloodGroup, double hospitalLat,
                                        double hospitalLon, double radiusKm) throws SQLException {
        
        // Dynamic grid radius fetch matching the requested boundary
        List<String> zones = HaversineUtil.neighbouringZones(hospitalLat, hospitalLon, radiusKm);
        
        StringBuilder inClause = new StringBuilder();
        for (int i = 0; i < zones.size(); i++) {
            inClause.append(i == 0 ? "?" : ",?");
        }
        
        // Check if blood group filtering is actually required ("All Types" handler)
        boolean filterBg = (bloodGroup != null && !bloodGroup.trim().isEmpty());
        
        String sql = "SELECT * FROM donors WHERE is_available=TRUE " 
                + (filterBg ? "AND blood_group=? " : "") 
                + "AND zone_key IN (" + inClause + ")";
                
        List<Donor> result = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
            PreparedStatement ps = con.prepareStatement(sql)) {
            
            int paramIdx = 1;
            // Set blood group parameter only if it's explicitly specified
            if (filterBg) {
                ps.setString(paramIdx++, bloodGroup);
            }
            
            // Populate the dynamic IN clause markers
            for (String zone : zones) {
                ps.setString(paramIdx++, zone);
            }
            
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Donor d = mapRow(rs);
                double dist = HaversineUtil.distance(hospitalLat, hospitalLon, d.getLatitude(), d.getLongitude());
                if (dist <= radiusKm) {
                    result.add(d);
                }
            }
        }
        
        // Sort by real-world distance (DSA: Comparable sort)
        result.sort((a, b) -> Double.compare(
            HaversineUtil.distance(hospitalLat, hospitalLon, a.getLatitude(), a.getLongitude()),
            HaversineUtil.distance(hospitalLat, hospitalLon, b.getLatitude(), b.getLongitude())
        ));
        return result;
    }

    

    // ── All Donors (for hospital dashboard overview) ─────────────────
    public List<Donor> getAllDonors() throws SQLException {
        String sql = "SELECT * FROM donors ORDER BY name";
        List<Donor> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    // ── Donor Stats ──────────────────────────────────────────────────
    public int countDonationsByDonor(int donorId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM blood_requests WHERE donor_id=? AND status='FULFILLED'";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, donorId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        }
        return 0;
    }

    // ── Map ResultSet row to Donor ───────────────────────────────────
    private Donor mapRow(ResultSet rs) throws SQLException {
        Donor d = new Donor();
        d.setId(rs.getInt("id"));
        d.setName(rs.getString("name"));
        d.setAge(rs.getInt("age"));
        d.setDob(rs.getDate("dob"));
        d.setBloodGroup(rs.getString("blood_group"));
        d.setPhone(rs.getString("phone"));
        d.setEmail(rs.getString("email"));
        d.setAddress(rs.getString("address"));
        d.setPassword(rs.getString("password"));
        d.setLatitude(rs.getDouble("latitude"));
        d.setLongitude(rs.getDouble("longitude"));
        d.setZoneKey(rs.getString("zone_key"));
        d.setAvailable(rs.getBoolean("is_available"));
        d.setLastDonated(rs.getDate("last_donated"));
        d.setCreatedAt(rs.getTimestamp("created_at"));
        return d;
    }
}