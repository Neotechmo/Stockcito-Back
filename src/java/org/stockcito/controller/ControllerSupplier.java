package org.stockcito.controller;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import org.stockcito.connection.ConexionMysql;
import org.stockcito.model.Supplier;

public class ControllerSupplier {

    public List<Supplier> getAll() throws SQLException {
        String sql = "SELECT * FROM suppliers WHERE is_active = TRUE ORDER BY name";
        List<Supplier> suppliers = new ArrayList<>();
        ConexionMysql connMysql = new ConexionMysql();

        try (Connection conn = connMysql.open();
             PreparedStatement pstm = conn.prepareStatement(sql);
             ResultSet rs = pstm.executeQuery()) {
            while (rs.next()) {
                suppliers.add(fill(rs));
            }
        } finally {
            connMysql.close();
        }

        return suppliers;
    }

    public Supplier getById(long id) throws SQLException {
        String sql = "SELECT * FROM suppliers WHERE id = ? AND is_active = TRUE";
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

    public Supplier save(Supplier supplier) throws SQLException {
        String sql = "INSERT INTO suppliers (name, contact_name, email, phone, address, notes) VALUES (?, ?, ?, ?, ?, ?)";
        ConexionMysql connMysql = new ConexionMysql();

        try (Connection conn = connMysql.open();
             PreparedStatement pstm = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            setFields(pstm, supplier);
            pstm.executeUpdate();

            try (ResultSet rs = pstm.getGeneratedKeys()) {
                if (rs.next()) {
                    supplier.setId(rs.getLong(1));
                }
            }
        } finally {
            connMysql.close();
        }

        return getById(supplier.getId());
    }

    public Supplier update(long id, Supplier supplier) throws SQLException {
        if (getById(id) == null) {
            return null;
        }

        String sql = "UPDATE suppliers SET name = ?, contact_name = ?, email = ?, phone = ?, address = ?, notes = ? WHERE id = ? AND is_active = TRUE";
        ConexionMysql connMysql = new ConexionMysql();

        try (Connection conn = connMysql.open();
             PreparedStatement pstm = conn.prepareStatement(sql)) {
            setFields(pstm, supplier);
            pstm.setLong(7, id);
            pstm.executeUpdate();
        } finally {
            connMysql.close();
        }

        return getById(id);
    }

    public boolean delete(long id) throws SQLException {
        String sql = "UPDATE suppliers SET is_active = FALSE WHERE id = ?";
        ConexionMysql connMysql = new ConexionMysql();

        try (Connection conn = connMysql.open();
             PreparedStatement pstm = conn.prepareStatement(sql)) {
            pstm.setLong(1, id);
            return pstm.executeUpdate() > 0;
        } finally {
            connMysql.close();
        }
    }

    private void setFields(PreparedStatement pstm, Supplier supplier) throws SQLException {
        pstm.setString(1, supplier.getName());
        pstm.setString(2, supplier.getContactName());
        pstm.setString(3, supplier.getEmail());
        pstm.setString(4, supplier.getPhone());
        pstm.setString(5, supplier.getAddress());
        pstm.setString(6, supplier.getNotes());
    }

    private Supplier fill(ResultSet rs) throws SQLException {
        Supplier supplier = new Supplier();
        supplier.setId(rs.getLong("id"));
        supplier.setName(rs.getString("name"));
        supplier.setContactName(rs.getString("contact_name"));
        supplier.setEmail(rs.getString("email"));
        supplier.setPhone(rs.getString("phone"));
        supplier.setAddress(rs.getString("address"));
        supplier.setNotes(rs.getString("notes"));
        supplier.setIsActive(rs.getBoolean("is_active"));
        supplier.setCreatedAt(String.valueOf(rs.getTimestamp("created_at")));
        supplier.setUpdatedAt(String.valueOf(rs.getTimestamp("updated_at")));
        return supplier;
    }
}
