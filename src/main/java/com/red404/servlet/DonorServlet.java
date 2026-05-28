package com.red404.servlet;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Map;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.google.gson.Gson;
import com.red404.dao.BloodRequestDAO;
import com.red404.dao.DonorDAO;
import com.red404.model.Donor;

@WebServlet("/api/donor/*")
public class DonorServlet extends HttpServlet {

    private final DonorDAO        donorDAO   = new DonorDAO();
    private final BloodRequestDAO requestDAO = new BloodRequestDAO();
    private final Gson            gson       = new Gson();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        res.setContentType("application/json");
        if (!authorized(req, res)) return;
        int donorId = (int) req.getSession().getAttribute("userId");
        String path = req.getPathInfo();
        try {
            switch (path) {
                case "/profile"  -> json(res, 200, donorDAO.findById(donorId));
                case "/requests" -> json(res, 200, requestDAO.getRequestsForDonor(donorId));
                case "/stats"    -> {
                    Map<String, Integer> stats = requestDAO.getDonorStats(donorId);
                    int totalDonations = donorDAO.countDonationsByDonor(donorId);
                    stats.put("TOTAL_FULFILLED", totalDonations);
                    json(res, 200, stats);
                }
                default          -> error(res, 404, "Not found");
            }
        } catch (Exception e) {
            error(res, 500, e.getMessage());
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse res) throws IOException {
        res.setContentType("application/json");
        if (!authorized(req, res)) return;
        int donorId = (int) req.getSession().getAttribute("userId");
        String path = req.getPathInfo();
        try (BufferedReader reader = req.getReader()) {
            Map<String, Object> body = gson.fromJson(reader, Map.class);
            if ("/profile".equals(path)) {
                Donor d = donorDAO.findById(donorId);
                d.setName((String) body.getOrDefault("name", d.getName()));
                d.setPhone((String) body.getOrDefault("phone", d.getPhone()));
                d.setAddress((String) body.getOrDefault("address", d.getAddress()));
                if (body.containsKey("latitude"))  d.setLatitude(((Number) body.get("latitude")).doubleValue());
                if (body.containsKey("longitude")) d.setLongitude(((Number) body.get("longitude")).doubleValue());
                if (body.containsKey("available")) d.setAvailable((Boolean) body.get("available"));
                boolean ok = donorDAO.updateProfile(d);
                req.getSession().setAttribute("userName", d.getName());
                json(res, ok ? 200 : 500, ok ? Map.of("name", d.getName()) : Map.of("error", "Update failed"));
            } else if ("/password".equals(path)) {
                Donor d = donorDAO.findById(donorId);
                String current = hashPassword((String) body.get("currentPassword"));
                if (!current.equals(d.getPassword())) { error(res, 401, "Wrong current password"); return; }
                String newHash = hashPassword((String) body.get("newPassword"));
                boolean ok = donorDAO.updatePassword(donorId, newHash);
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
        String path = req.getPathInfo();
        try (BufferedReader reader = req.getReader()) {
            Map<String, Object> body = gson.fromJson(reader, Map.class);
            if ("/requests/respond".equals(path)) {
                int requestId = ((Number) body.get("requestId")).intValue();
                String status = (String) body.get("status");
                boolean ok = requestDAO.respondToRequest(requestId, status);
                json(res, ok ? 200 : 500, ok ? Map.of("message", "Response recorded") : Map.of("error", "Failed"));
            } else {
                error(res, 404, "Not found");
            }
        } catch (Exception e) {
            error(res, 500, e.getMessage());
        }
    }

    private boolean authorized(HttpServletRequest req, HttpServletResponse res) throws IOException {
        HttpSession session = req.getSession(false);
        if (session == null || !"DONOR".equals(session.getAttribute("userType"))) {
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