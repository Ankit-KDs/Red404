package com.red404.servlet;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.google.gson.Gson;
import com.red404.dao.BloodRequestDAO;
import com.red404.dao.DonorDAO;
import com.red404.dao.HospitalDAO;
import com.red404.model.BloodRequest;
import com.red404.model.Donor;
import com.red404.model.Hospital;
import com.red404.util.HaversineUtil;

@WebServlet("/api/hospital/*")
public class HospitalServlet extends HttpServlet {

    private final HospitalDAO     hospitalDAO = new HospitalDAO();
    private final DonorDAO        donorDAO    = new DonorDAO();
    private final BloodRequestDAO requestDAO  = new BloodRequestDAO();
    private final Gson            gson        = new Gson();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        res.setContentType("application/json");
        if (!authorized(req, res)) return;
        int hospitalId = (int) req.getSession().getAttribute("userId");
        String path = req.getPathInfo();
        try {
            Hospital h = hospitalDAO.findById(hospitalId);
            switch (path) {
                case "/profile"       -> json(res, 200, h);
                case "/requests"      -> json(res, 200, requestDAO.getRequestsByHospital(hospitalId));
                case "/stats"         -> json(res, 200, requestDAO.getHospitalStats(hospitalId));
                case "/donors"        -> {
                    List<Donor> donors = donorDAO.getAllDonors();
                    var result = donors.stream().map(d -> Map.of(
                        "id",         d.getId(),
                        "name",       d.getName(),
                        "bloodGroup", d.getBloodGroup(),
                        "phone",      d.getPhone(),
                        "address",    d.getAddress(),
                        "available",  d.isAvailable(),
                        "distanceKm", Math.round(HaversineUtil.distance(
                                        h.getLatitude(), h.getLongitude(),
                                        d.getLatitude(), d.getLongitude()) * 10.0) / 10.0
                    )).toList();
                    json(res, 200, result);
                }
                case "/donors/nearby" -> {
                    String bg     = req.getParameter("bg");
                    double radius = Double.parseDouble(
                        req.getParameter("radius") != null ? req.getParameter("radius") : "10");
                    List<Donor> nearby = donorDAO.findNearbyDonors(
                        bg, h.getLatitude(), h.getLongitude(), radius);
                    var result = nearby.stream().map(d -> Map.of(
                        "id",         d.getId(),
                        "name",       d.getName(),
                        "bloodGroup", d.getBloodGroup(),
                        "phone",      d.getPhone(),
                        "address",    d.getAddress(),
                        "available",  d.isAvailable(),
                        "distanceKm", Math.round(HaversineUtil.distance(
                                        h.getLatitude(), h.getLongitude(),
                                        d.getLatitude(), d.getLongitude()) * 10.0) / 10.0
                    )).toList();
                    json(res, 200, result);
                }
                default               -> error(res, 404, "Not found");
            }
        } catch (Exception e) {
            error(res, 500, e.getMessage());
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse res) throws IOException {
        res.setContentType("application/json");
        if (!authorized(req, res)) return;
        int hospitalId = (int) req.getSession().getAttribute("userId");
        String path = req.getPathInfo();
        try (BufferedReader reader = req.getReader()) {
            Map<String, Object> body = gson.fromJson(reader, Map.class);
            if ("/profile".equals(path)) {
                Hospital h = hospitalDAO.findById(hospitalId);
                h.setName((String) body.getOrDefault("name", h.getName()));
                h.setPhone((String) body.getOrDefault("phone", h.getPhone()));
                h.setAddress((String) body.getOrDefault("address", h.getAddress()));
                h.setAdminName((String) body.getOrDefault("adminName", h.getAdminName()));
                h.setAdminPhone((String) body.getOrDefault("adminPhone", h.getAdminPhone()));
                h.setAdminEmail((String) body.getOrDefault("adminEmail", h.getAdminEmail()));
                if (body.containsKey("latitude"))  h.setLatitude(((Number) body.get("latitude")).doubleValue());
                if (body.containsKey("longitude")) h.setLongitude(((Number) body.get("longitude")).doubleValue());
                boolean ok = hospitalDAO.updateProfile(h);
                req.getSession().setAttribute("userName", h.getName());
                json(res, ok ? 200 : 500, ok ? Map.of("name", h.getName()) : Map.of("error", "Update failed"));
            } else if ("/password".equals(path)) {
                Hospital h = hospitalDAO.findById(hospitalId);
                String current = hashPassword((String) body.get("currentPassword"));
                if (!current.equals(h.getPassword())) { error(res, 401, "Wrong current password"); return; }
                String newHash = hashPassword((String) body.get("newPassword"));
                boolean ok = hospitalDAO.updatePassword(hospitalId, newHash);
                json(res, ok ? 200 : 500, ok ? Map.of("message", "Password updated") : Map.of("error", "Failed"));
            } else {
                error(res, 404, "Not found");
            }
        } catch (Exception e) {
            error(res, 500, e.getMessage());
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
        res.setContentType("application/json");
        if (!authorized(req, res)) return;
        int hospitalId = (int) req.getSession().getAttribute("userId");
        String path = req.getPathInfo();
        try (BufferedReader reader = req.getReader()) {
            Map<String, Object> body = gson.fromJson(reader, Map.class);
            if ("/request".equals(path)) {
                int donorId = ((Number) body.get("donorId")).intValue();
                Donor donor = donorDAO.findById(donorId);
                BloodRequest rq = new BloodRequest();
                rq.setHospitalId(hospitalId);
                rq.setDonorId(donorId);
                rq.setBloodGroup((String) body.get("bloodGroup"));
                rq.setUnitsNeeded(((Number) body.getOrDefault("unitsNeeded", 1)).intValue());
                rq.setUrgency((String) body.getOrDefault("urgency", "NORMAL"));
                rq.setMessage((String) body.getOrDefault("message", ""));
                boolean ok = requestDAO.createRequest(rq, donor.getLatitude(), donor.getLongitude());
                json(res, ok ? 201 : 500, ok ? Map.of("requestId", rq.getId()) : Map.of("error", "Failed"));
            } else {
                error(res, 404, "Not found");
            }
        } catch (Exception e) {
            error(res, 500, e.getMessage());
        }
    }

    private boolean authorized(HttpServletRequest req, HttpServletResponse res) throws IOException {
        HttpSession session = req.getSession(false);
        if (session == null || !"HOSPITAL".equals(session.getAttribute("userType"))) {
            error(res, 401, "Unauthorized"); return false;
        }
        return true;
    }

    private String hashPassword(String plain) throws Exception {
        java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
        byte[] hash = md.digest(plain.getBytes("UTF-8"));
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    private void json(HttpServletResponse res, int status, Object obj) throws IOException {
        res.setStatus(status);
        res.getWriter().write(gson.toJson(obj));
    }

    private void error(HttpServletResponse res, int status, String msg) throws IOException {
        json(res, status, Map.of("error", msg));
    }
}