package org.stockcito.controller;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import org.stockcito.connection.ConexionMysql;
import org.stockcito.model.Category;

public class ControllerCategory {

    public List<Category> getAll() throws SQLException {
        String sql = "SELECT * FROM categories WHERE is_active = TRUE ORDER BY name";
        List<Category> categories = new ArrayList<>();
        ConexionMysql connMysql = new ConexionMysql();

        try (Connection conn = connMysql.open();
             PreparedStatement pstm = conn.prepareStatement(sql);
             ResultSet rs = pstm.executeQuery()) {
            while (rs.next()) {
                categories.add(fill(rs));
            }
        } finally {
            connMysql.close();
        }

        return categories;
    }

    public Category getById(long id) throws SQLException {
        String sql = "SELECT * FROM categories WHERE id = ? AND is_active = TRUE";
        ConexionMysql connMysql = new ConexionMysql();

        try (Connection conn = connMysql.open();
             PreparedStatement pstm = conn.prepareStatement(sql)) {
            pstm.setLong(1, id);
            try (ResultSet rs = pstm.executeQuery()) {
                if (rs.next()) {
                    return fill(rs);
                }
            }
        } finally {
            connMysql.close();
        }

        return null;
    }

    public Category save(Category category) throws SQLException {
        ConexionMysql connMysql = new ConexionMysql();

        try (Connection conn = connMysql.open()) {
            conn.setAutoCommit(false);
            try {
                Category existing = getByName(conn, category.getName());
                if (existing != null) {
                    if (existing.isIsActive()) {
                        throw new IllegalArgumentException("Ya existe una categoria activa con ese nombre");
                    }
                    reactivate(conn, existing.getId(), category);
                    category.setId(existing.getId());
                } else {
                    insert(conn, category);
                }
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        } finally {
            connMysql.close();
        }

        return getById(category.getId());
    }

    private void insert(Connection conn, Category category) throws SQLException {
        String sql = "INSERT INTO categories (name, description) VALUES (?, ?)";
        try (PreparedStatement pstm = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstm.setString(1, category.getName().trim());
            pstm.setString(2, category.getDescription());
            pstm.executeUpdate();
            try (ResultSet rs = pstm.getGeneratedKeys()) {
                if (rs.next()) category.setId(rs.getLong(1));
            }
        }
    }

    private void reactivate(Connection conn, long id, Category category) throws SQLException {
        String sql = "UPDATE categories SET name = ?, description = ?, is_active = TRUE WHERE id = ?";
        try (PreparedStatement pstm = conn.prepareStatement(sql)) {
            pstm.setString(1, category.getName().trim());
            pstm.setString(2, category.getDescription());
            pstm.setLong(3, id);
            pstm.executeUpdate();
        }
    }

    private Category getByName(Connection conn, String name) throws SQLException {
        String sql = "SELECT * FROM categories WHERE LOWER(name) = LOWER(?) FOR UPDATE";
        try (PreparedStatement pstm = conn.prepareStatement(sql)) {
            pstm.setString(1, name.trim());
            try (ResultSet rs = pstm.executeQuery()) {
                return rs.next() ? fill(rs) : null;
            }
        }
    }

    public Category update(long id, Category category) throws SQLException {
        if (getById(id) == null) {
            return null;
        }

        String sql = "UPDATE categories SET name = ?, description = ? WHERE id = ? AND is_active = TRUE";
        ConexionMysql connMysql = new ConexionMysql();

        try (Connection conn = connMysql.open();
             PreparedStatement pstm = conn.prepareStatement(sql)) {
            pstm.setString(1, category.getName());
            pstm.setString(2, category.getDescription());
            pstm.setLong(3, id);
            pstm.executeUpdate();
        } finally {
            connMysql.close();
        }

        return getById(id);
    }

    public boolean delete(long id) throws SQLException {
        String sql = "UPDATE categories SET is_active = FALSE WHERE id = ?";
        ConexionMysql connMysql = new ConexionMysql();

        try (Connection conn = connMysql.open();
             PreparedStatement pstm = conn.prepareStatement(sql)) {
            pstm.setLong(1, id);
            return pstm.executeUpdate() > 0;
        } finally {
            connMysql.close();
        }
    }

    private Category fill(ResultSet rs) throws SQLException {
        Category category = new Category();
        category.setId(rs.getLong("id"));
        category.setName(rs.getString("name"));
        category.setDescription(rs.getString("description"));
        category.setIsActive(rs.getBoolean("is_active"));
        category.setCreatedAt(String.valueOf(rs.getTimestamp("created_at")));
        category.setUpdatedAt(String.valueOf(rs.getTimestamp("updated_at")));
        return category;
    }
}
