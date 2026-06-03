package org.stockcito.controller;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import org.mindrot.jbcrypt.BCrypt;
import org.stockcito.connection.ConexionMysql;
import org.stockcito.model.User;
import org.stockcito.model.UserRole;

public class ControllerUser {

    public List<User> getAll() throws SQLException {
        String sql = "SELECT * FROM users WHERE is_active = TRUE ORDER BY name";
        List<User> users = new ArrayList<>();
        ConexionMysql mysql = new ConexionMysql();
        try (Connection conn = mysql.open();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) users.add(fill(rs));
        } finally {
            mysql.close();
        }
        return users;
    }

    public User getById(long id) throws SQLException {
        return find("SELECT * FROM users WHERE id = ? AND is_active = TRUE", id);
    }

    public User getByEmail(String email) throws SQLException {
        String sql = "SELECT * FROM users WHERE LOWER(email) = LOWER(?) AND is_active = TRUE";
        ConexionMysql mysql = new ConexionMysql();
        try (Connection conn = mysql.open(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? fill(rs) : null;
            }
        } finally {
            mysql.close();
        }
    }

    public User save(User user) throws SQLException {
        if (getByEmail(user.getEmail()) != null) throw new IllegalArgumentException("El email ya esta registrado");
        String sql = "INSERT INTO users (name, email, password_hash, role) VALUES (?, LOWER(?), ?, ?)";
        ConexionMysql mysql = new ConexionMysql();
        try (Connection conn = mysql.open();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.getName().trim());
            ps.setString(2, user.getEmail().trim());
            ps.setString(3, BCrypt.hashpw(user.getPassword(), BCrypt.gensalt(12)));
            ps.setString(4, (user.getRole() == null ? UserRole.OPERADOR : user.getRole()).name());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) user.setId(rs.getLong(1));
            }
        } finally {
            mysql.close();
        }
        return getById(user.getId());
    }

    public User update(long id, User user) throws SQLException {
        User current = getById(id);
        if (current == null) return null;
        User duplicate = getByEmail(user.getEmail());
        if (duplicate != null && duplicate.getId() != id) throw new IllegalArgumentException("El email ya esta registrado");

        boolean changePassword = user.getPassword() != null && !user.getPassword().isBlank();
        String sql = changePassword
                ? "UPDATE users SET name=?, email=LOWER(?), role=?, is_active=?, password_hash=? WHERE id=?"
                : "UPDATE users SET name=?, email=LOWER(?), role=?, is_active=? WHERE id=?";
        ConexionMysql mysql = new ConexionMysql();
        try (Connection conn = mysql.open(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.getName().trim());
            ps.setString(2, user.getEmail().trim());
            ps.setString(3, (user.getRole() == null ? current.getRole() : user.getRole()).name());
            ps.setBoolean(4, current.isIsActive());
            if (changePassword) {
                ps.setString(5, BCrypt.hashpw(user.getPassword(), BCrypt.gensalt(12)));
                ps.setLong(6, id);
            } else {
                ps.setLong(5, id);
            }
            ps.executeUpdate();
        } finally {
            mysql.close();
        }
        return getById(id);
    }

    public boolean delete(long id) throws SQLException {
        ConexionMysql mysql = new ConexionMysql();
        try (Connection conn = mysql.open();
             PreparedStatement ps = conn.prepareStatement("UPDATE users SET is_active=FALSE WHERE id=? AND is_active=TRUE")) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } finally {
            mysql.close();
        }
    }

    public User authenticate(String email, String password) throws SQLException {
        User user = getByEmail(email);
        return user != null && BCrypt.checkpw(password, user.getPasswordHash()) ? user : null;
    }

    private User find(String sql, long id) throws SQLException {
        ConexionMysql mysql = new ConexionMysql();
        try (Connection conn = mysql.open(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? fill(rs) : null;
            }
        } finally {
            mysql.close();
        }
    }

    private User fill(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getLong("id"));
        user.setName(rs.getString("name"));
        user.setEmail(rs.getString("email"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setRole(UserRole.valueOf(rs.getString("role")));
        user.setIsActive(rs.getBoolean("is_active"));
        user.setCreatedAt(String.valueOf(rs.getTimestamp("created_at")));
        user.setUpdatedAt(String.valueOf(rs.getTimestamp("updated_at")));
        return user;
    }
}
