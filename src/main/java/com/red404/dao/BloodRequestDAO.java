package com.red404.dao;

import com.red404.model.BloodRequest;
import com.red404.util.DBConnection;
import com.red404.util.HaversineUtil;

import java.sql.*;
import java.util.*;

/**
 * BloodRequestDAO
 *
 * DSA used:
 *  - PriorityQueue → getRequestsForDonor() returns requests sorted by urgency (CRITICAL > HIGH > NORMAL)
 *  - Haversine     → distance stored at request-creation time
 */
public class BloodRequestDAO {

    // Urgency weight for PriorityQueue comparison
    private static int urgencyWeight(String urgency) {
        return switch (urgency) {
            case "CRITICAL" -> 3;
            case "HIGH"     -> 2;
            default         -> 1;
        };
    }

    // ── Create Request ───────────────────────────────────────────────
    public boolean createRequest(BloodRequest req, double donorLat, double donorLon) throws SQLException {
        String sql = "INSERT INTO blood_requests (hospital_id,donor_id,blood_group,units_needed,urgency,status,message,distance_km) "
                   + "VALUES (?,?,?,?,?,?,?,?)";
        // Fetch hospital coords to compute distance
        String hSql = "SELECT latitude,longitude FROM hospitals WHERE id=?";
        double dist = 0;
        try (Connection con = DBConnection.getConnection()) {
            PreparedStatement hps = con.prepareStatement(hSql);
            hps.setInt(1, req.getHospitalId());
            ResultSet hrs = hps.executeQuery();
            if (hrs.next()) {
                dist = HaversineUtil.distance(hrs.getDouble("latitude"), hrs.getDouble("longitude"), donorLat, donorLon);
            }
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, req.getHospitalId());
            if (req.getDonorId() != null) ps.setInt(2, req.getDonorId()); else ps.setNull(2, Types.INTEGER);
            ps.setString(3, req.getBloodGroup());
            ps.setInt(4, req.getUnitsNeeded());
            ps.setString(5, req.getUrgency());
            ps.setString(6, "PENDING");
            ps.setString(7, req.getMessage());
            ps.setDouble(8, dist);
            int rows = ps.executeUpdate();
            if (rows > 0) {
                ResultSet rk = ps.getGeneratedKeys();
                if (rk.next()) req.setId(rk.getInt(1));
            }
            return rows > 0;
        }
    }

    // ── Get requests directed at a donor (sorted by urgency via PriorityQueue) ──
    public List<BloodRequest> getRequestsForDonor(int donorId) throws SQLException {
        String sql = "SELECT br.*, h.name AS hospital_name FROM blood_requests br "
                   + "JOIN hospitals h ON br.hospital_id = h.id "
                   + "WHERE br.donor_id=? AND br.status='PENDING' ORDER BY br.requested_at DESC";

        // PriorityQueue: CRITICAL first
        PriorityQueue<BloodRequest> pq = new PriorityQueue<>(
            Comparator.comparingInt((BloodRequest r) -> urgencyWeight(r.getUrgency())).reversed()
        );

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, donorId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) pq.add(mapRow(rs));
        }
        return new ArrayList<>(pq);
    }

    // ── Get all requests by a hospital ───────────────────────────────
    public List<BloodRequest> getRequestsByHospital(int hospitalId) throws SQLException {
        String sql = "SELECT br.*, h.name AS hospital_name, d.name AS donor_name "
                   + "FROM blood_requests br "
                   + "JOIN hospitals h ON br.hospital_id = h.id "
                   + "LEFT JOIN donors d ON br.donor_id = d.id "
                   + "WHERE br.hospital_id=? ORDER BY br.requested_at DESC";
        List<BloodRequest> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, hospitalId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    // ── Donor responds to request ─────────────────────────────────────
    public boolean respondToRequest(int requestId, String status) throws SQLException {
        String sql = "UPDATE blood_requests SET status=?, responded_at=NOW() WHERE id=?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, requestId);
            return ps.executeUpdate() > 0;
        }
    }

    // ── Stats for donor dashboard ─────────────────────────────────────
    public Map<String, Integer> getDonorStats(int donorId) throws SQLException {
        Map<String, Integer> stats = new LinkedHashMap<>();
        String sql = "SELECT status, COUNT(*) as cnt FROM blood_requests WHERE donor_id=? GROUP BY status";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, donorId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) stats.put(rs.getString("status"), rs.getInt("cnt"));
        }
        return stats;
    }

    // ── Stats for hospital dashboard ──────────────────────────────────
    public Map<String, Integer> getHospitalStats(int hospitalId) throws SQLException {
        Map<String, Integer> stats = new LinkedHashMap<>();
        String sql = "SELECT status, COUNT(*) as cnt FROM blood_requests WHERE hospital_id=? GROUP BY status";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, hospitalId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) stats.put(rs.getString("status"), rs.getInt("cnt"));
        }
        return stats;
    }

    private BloodRequest mapRow(ResultSet rs) throws SQLException {
        BloodRequest r = new BloodRequest();
        r.setId(rs.getInt("id"));
        r.setHospitalId(rs.getInt("hospital_id"));
        r.setBloodGroup(rs.getString("blood_group"));
        r.setUnitsNeeded(rs.getInt("units_needed"));
        r.setUrgency(rs.getString("urgency"));
        r.setStatus(rs.getString("status"));
        r.setMessage(rs.getString("message"));
        r.setDistanceKm(rs.getDouble("distance_km"));
        r.setRequestedAt(rs.getTimestamp("requested_at"));
        r.setRespondedAt(rs.getTimestamp("responded_at"));
        try { r.setHospitalName(rs.getString("hospital_name")); } catch (SQLException ignored) {}
        try { r.setDonorName(rs.getString("donor_name")); } catch (SQLException ignored) {}
        Object did = rs.getObject("donor_id");
        if (did != null) r.setDonorId((Integer) did);
        return r;
    }
}