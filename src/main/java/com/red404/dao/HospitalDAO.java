package com.red404.dao;

import com.red404.model.Hospital;
import com.red404.util.DBConnection;
import com.red404.util.HaversineUtil;

import java.sql.*;

public class HospitalDAO {

    public boolean register(Hospital h) throws SQLException {
        String sql = "INSERT INTO hospitals (name,registration_id,phone,email,admin_name,admin_phone,admin_email,address,password,latitude,longitude,zone_key) "
                   + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";
        String zoneKey = HaversineUtil.zoneKey(h.getLatitude(), h.getLongitude());
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, h.getName());
            ps.setString(2, h.getRegistrationId());
            ps.setString(3, h.getPhone());
            ps.setString(4, h.getEmail());
            ps.setString(5, h.getAdminName());
            ps.setString(6, h.getAdminPhone());
            ps.setString(7, h.getAdminEmail());
            ps.setString(8, h.getAddress());
            ps.setString(9, h.getPassword());
            ps.setDouble(10, h.getLatitude());
            ps.setDouble(11, h.getLongitude());
            ps.setString(12, zoneKey);
            int rows = ps.executeUpdate();
            if (rows > 0) {
                ResultSet rs = ps.getGeneratedKeys();
                if (rs.next()) h.setId(rs.getInt(1));
            }
            return rows > 0;
        }
    }

    public Hospital findByEmail(String email) throws SQLException {
        String sql = "SELECT * FROM hospitals WHERE email = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        }
        return null;
    }

    public Hospital findById(int id) throws SQLException {
        String sql = "SELECT * FROM hospitals WHERE id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        }
        return null;
    }

    public boolean updateProfile(Hospital h) throws SQLException {
        String sql = "UPDATE hospitals SET name=?, phone=?, address=?, admin_name=?, admin_phone=?, admin_email=?, latitude=?, longitude=?, zone_key=? WHERE id=?";
        String zoneKey = HaversineUtil.zoneKey(h.getLatitude(), h.getLongitude());
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, h.getName());
            ps.setString(2, h.getPhone());
            ps.setString(3, h.getAddress());
            ps.setString(4, h.getAdminName());
            ps.setString(5, h.getAdminPhone());
            ps.setString(6, h.getAdminEmail());
            ps.setDouble(7, h.getLatitude());
            ps.setDouble(8, h.getLongitude());
            ps.setString(9, zoneKey);
            ps.setInt(10, h.getId());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean updatePassword(int id, String hashedPassword) throws SQLException {
        String sql = "UPDATE hospitals SET password=? WHERE id=?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, hashedPassword);
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        }
    }

    private Hospital mapRow(ResultSet rs) throws SQLException {
        Hospital h = new Hospital();
        h.setId(rs.getInt("id"));
        h.setName(rs.getString("name"));
        h.setRegistrationId(rs.getString("registration_id"));
        h.setPhone(rs.getString("phone"));
        h.setEmail(rs.getString("email"));
        h.setAdminName(rs.getString("admin_name"));
        h.setAdminPhone(rs.getString("admin_phone"));
        h.setAdminEmail(rs.getString("admin_email"));
        h.setAddress(rs.getString("address"));
        h.setPassword(rs.getString("password"));
        h.setLatitude(rs.getDouble("latitude"));
        h.setLongitude(rs.getDouble("longitude"));
        h.setZoneKey(rs.getString("zone_key"));
        h.setCreatedAt(rs.getTimestamp("created_at"));
        return h;
    }
}