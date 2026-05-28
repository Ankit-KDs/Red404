package com.red404.servlet;

import java.io.BufferedReader;
import java.io.IOException;
import java.sql.Date;
import java.util.Map;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.google.gson.Gson;
import com.red404.dao.DonorDAO;
import com.red404.dao.HospitalDAO;
import com.red404.model.Donor;
import com.red404.model.Hospital;

@WebServlet("/api/auth/*")
public class AuthServlet extends HttpServlet {

    private final DonorDAO    donorDAO    = new DonorDAO();
    private final HospitalDAO hospitalDAO = new HospitalDAO();
    private final Gson        gson        = new Gson();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res)
            throws IOException {
        res.setContentType("application/json");
        res.setCharacterEncoding("UTF-8");
        String path = req.getPathInfo();

        try (BufferedReader reader = req.getReader()) {
            Map<String, Object> body = gson.fromJson(reader, Map.class);

            switch (path) {
                case "/donor/register"    -> donorRegister(body, req, res);
                case "/hospital/register" -> hospitalRegister(body, req, res);
                case "/donor/login"       -> donorLogin(body, req, res);
                case "/hospital/login"    -> hospitalLogin(body, req, res);
                case "/logout"            -> logout(req, res);
                default                   -> error(res, 404, "Unknown endpoint");
            }
        } catch (Exception e) {
            error(res, 500, e.getMessage());
        }
    }

    private void donorRegister(Map<String, Object> body, HttpServletRequest req, HttpServletResponse res)
            throws Exception {
        Donor d = new Donor();
        d.setName((String) body.get("name"));
        d.setAge(((Number) body.get("age")).intValue());
        d.setDob(Date.valueOf((String) body.get("dob")));
        d.setBloodGroup((String) body.get("bloodGroup"));
        d.setPhone((String) body.get("phone"));
        d.setEmail((String) body.get("email"));
        d.setAddress((String) body.get("address"));
        d.setLatitude(((Number) body.get("latitude")).doubleValue());
        d.setLongitude(((Number) body.get("longitude")).doubleValue());
        d.setPassword(hashPassword((String) body.get("password")));

        if (donorDAO.findByEmail(d.getEmail()) != null) {
            error(res, 409, "Email already registered"); return;
        }

        boolean ok = donorDAO.register(d);
        if (ok) {
            HttpSession session = req.getSession(true);
            session.setAttribute("userId",   d.getId());
            session.setAttribute("userType", "DONOR");
            session.setAttribute("userName", d.getName());
            json(res, 201, Map.of("id", d.getId(), "name", d.getName(), "type", "DONOR"));
        } else {
            error(res, 500, "Registration failed");
        }
    }

    private void hospitalRegister(Map<String, Object> body, HttpServletRequest req, HttpServletResponse res)
            throws Exception {
        Hospital h = new Hospital();
        h.setName((String) body.get("name"));
        h.setRegistrationId((String) body.get("registrationId"));
        h.setPhone((String) body.get("phone"));
        h.setEmail((String) body.get("email"));
        h.setAdminName((String) body.get("adminName"));
        h.setAdminPhone((String) body.get("adminPhone"));
        h.setAdminEmail((String) body.get("adminEmail"));
        h.setAddress((String) body.get("address"));
        h.setLatitude(((Number) body.get("latitude")).doubleValue());
        h.setLongitude(((Number) body.get("longitude")).doubleValue());
        h.setPassword(hashPassword((String) body.get("password")));

        if (hospitalDAO.findByEmail(h.getEmail()) != null) {
            error(res, 409, "Email already registered"); return;
        }

        boolean ok = hospitalDAO.register(h);
        if (ok) {
            HttpSession session = req.getSession(true);
            session.setAttribute("userId",   h.getId());
            session.setAttribute("userType", "HOSPITAL");
            session.setAttribute("userName", h.getName());
            json(res, 201, Map.of("id", h.getId(), "name", h.getName(), "type", "HOSPITAL"));
        } else {
            error(res, 500, "Registration failed");
        }
    }

    private void donorLogin(Map<String, Object> body, HttpServletRequest req, HttpServletResponse res)
            throws Exception {
        String email    = (String) body.get("email");
        String password = (String) body.get("password");
        Donor d = donorDAO.findByEmail(email);
        if (d == null || !checkPassword(password, d.getPassword())) {
            error(res, 401, "Invalid credentials"); return;
        }
        HttpSession session = req.getSession(true);
        session.setAttribute("userId",   d.getId());
        session.setAttribute("userType", "DONOR");
        session.setAttribute("userName", d.getName());
        json(res, 200, Map.of("id", d.getId(), "name", d.getName(), "type", "DONOR"));
    }

    private void hospitalLogin(Map<String, Object> body, HttpServletRequest req, HttpServletResponse res)
            throws Exception {
        String email    = (String) body.get("email");
        String password = (String) body.get("password");
        Hospital h = hospitalDAO.findByEmail(email);
        if (h == null || !checkPassword(password, h.getPassword())) {
            error(res, 401, "Invalid credentials"); return;
        }
        HttpSession session = req.getSession(true);
        session.setAttribute("userId",   h.getId());
        session.setAttribute("userType", "HOSPITAL");
        session.setAttribute("userName", h.getName());
        json(res, 200, Map.of("id", h.getId(), "name", h.getName(), "type", "HOSPITAL"));
    }

    private void logout(HttpServletRequest req, HttpServletResponse res) throws IOException {
        HttpSession session = req.getSession(false);
        if (session != null) session.invalidate();
        json(res, 200, Map.of("message", "Logged out"));
    }

    private String hashPassword(String plain) throws Exception {
        java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
        byte[] hash = md.digest(plain.getBytes("UTF-8"));
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    private boolean checkPassword(String plain, String stored) throws Exception {
        return hashPassword(plain).equals(stored);
    }

    private void json(HttpServletResponse res, int status, Object obj) throws IOException {
        res.setStatus(status);
        res.getWriter().write(gson.toJson(obj));
    }

    private void error(HttpServletResponse res, int status, String msg) throws IOException {
        json(res, status, Map.of("error", msg));
    }
}