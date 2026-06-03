package org.stockcito.controller;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import org.stockcito.connection.ConexionMysql;
import org.stockcito.model.InventoryMovement;
import org.stockcito.model.InventorySummary;
import org.stockcito.model.MovementType;

public class ControllerInventoryMovement {

    private static final String BASE_SELECT = """
        SELECT m.*, p.name product_name, u.name user_name
        FROM inventory_movements m
        JOIN products p ON p.id=m.product_id
        JOIN users u ON u.id=m.user_id
        """;

    public List<InventoryMovement> getAll() throws SQLException {
        List<InventoryMovement> result = new ArrayList<>();
        ConexionMysql mysql = new ConexionMysql();
        try (Connection conn = mysql.open();
             PreparedStatement ps = conn.prepareStatement(BASE_SELECT + " ORDER BY m.movement_date DESC");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) result.add(fill(rs));
        } finally { mysql.close(); }
        return result;
    }

    public InventoryMovement getById(long id) throws SQLException {
        ConexionMysql mysql = new ConexionMysql();
        try (Connection conn = mysql.open(); PreparedStatement ps = conn.prepareStatement(BASE_SELECT + " WHERE m.id=?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? fill(rs) : null; }
        } finally { mysql.close(); }
    }

    public InventoryMovement save(InventoryMovement movement) throws SQLException {
        ConexionMysql mysql = new ConexionMysql();
        try (Connection conn = mysql.open()) {
            conn.setAutoCommit(false);
            try {
                movement.setId(insert(conn, movement));
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        } finally { mysql.close(); }
        return getById(movement.getId());
    }

    public long insert(Connection conn, InventoryMovement movement) throws SQLException {
        BigDecimal before = lockStock(conn, movement.getProductId());
        BigDecimal after = calculateAfter(before, movement.getMovementType(), movement.getQuantity());
        String sql = """
            INSERT INTO inventory_movements
            (product_id,user_id,movement_type,quantity,stock_before,stock_after,movement_date,reference_id,notes,original_text)
            VALUES (?,?,?,?,?,?,COALESCE(?,CURRENT_TIMESTAMP),?,?,?)
            """;
        long id;
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, movement.getProductId());
            ps.setLong(2, movement.getUserId());
            ps.setString(3, movement.getMovementType().name());
            ps.setBigDecimal(4, movement.getQuantity());
            ps.setBigDecimal(5, before);
            ps.setBigDecimal(6, after);
            ps.setString(7, blankToNull(movement.getMovementDate()));
            if (movement.getReferenceId() == null) ps.setNull(8, Types.BIGINT); else ps.setLong(8, movement.getReferenceId());
            ps.setString(9, movement.getNotes());
            ps.setString(10, movement.getOriginalText());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) { rs.next(); id = rs.getLong(1); }
        }
        updateStock(conn, movement.getProductId(), after);
        return id;
    }

    public InventoryMovement update(long id, InventoryMovement movement) throws SQLException {
        InventoryMovement current = getById(id);
        if (current == null) return null;
        if (current.getProductId() != movement.getProductId()) {
            throw new IllegalArgumentException("No se puede cambiar el producto de un movimiento");
        }
        ConexionMysql mysql = new ConexionMysql();
        try (Connection conn = mysql.open()) {
            conn.setAutoCommit(false);
            try {
                BigDecimal stock = lockStock(conn, current.getProductId());
                BigDecimal withoutCurrent = reverse(stock, current.getMovementType(), current.getQuantity());
                BigDecimal after = calculateAfter(withoutCurrent, movement.getMovementType(), movement.getQuantity());
                String sql = "UPDATE inventory_movements SET user_id=?,movement_type=?,quantity=?,stock_before=?,stock_after=?,notes=?,original_text=? WHERE id=?";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setLong(1, movement.getUserId());
                    ps.setString(2, movement.getMovementType().name());
                    ps.setBigDecimal(3, movement.getQuantity());
                    ps.setBigDecimal(4, withoutCurrent);
                    ps.setBigDecimal(5, after);
                    ps.setString(6, movement.getNotes());
                    ps.setString(7, movement.getOriginalText());
                    ps.setLong(8, id);
                    ps.executeUpdate();
                }
                updateStock(conn, current.getProductId(), after);
                conn.commit();
            } catch (Exception e) { conn.rollback(); throw e; }
        } finally { mysql.close(); }
        return getById(id);
    }

    public boolean delete(long id) throws SQLException {
        InventoryMovement current = getById(id);
        if (current == null) return false;
        ConexionMysql mysql = new ConexionMysql();
        try (Connection conn = mysql.open()) {
            conn.setAutoCommit(false);
            try {
                BigDecimal stock = lockStock(conn, current.getProductId());
                updateStock(conn, current.getProductId(), reverse(stock, current.getMovementType(), current.getQuantity()));
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM inventory_movements WHERE id=?")) {
                    ps.setLong(1, id);
                    ps.executeUpdate();
                }
                conn.commit();
                return true;
            } catch (Exception e) { conn.rollback(); throw e; }
        } finally { mysql.close(); }
    }

    public List<InventorySummary> getInventory() throws SQLException {
        String sql = "SELECT id,name,sku,current_stock,min_stock,unit_of_measure FROM products WHERE is_active=TRUE ORDER BY name";
        List<InventorySummary> result = new ArrayList<>();
        ConexionMysql mysql = new ConexionMysql();
        try (Connection conn = mysql.open(); PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                InventorySummary item = new InventorySummary();
                item.setProductId(rs.getLong("id"));
                item.setProductName(rs.getString("name"));
                item.setSku(rs.getString("sku"));
                item.setCurrentStock(rs.getBigDecimal("current_stock"));
                item.setMinimumStock(rs.getBigDecimal("min_stock"));
                item.setUnitOfMeasure(rs.getString("unit_of_measure"));
                item.setStatus(item.getCurrentStock().compareTo(item.getMinimumStock()) <= 0 ? "LOW_STOCK" : "OK");
                result.add(item);
            }
        } finally { mysql.close(); }
        return result;
    }

    private BigDecimal lockStock(Connection conn, long productId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT current_stock FROM products WHERE id=? AND is_active=TRUE FOR UPDATE")) {
            ps.setLong(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new IllegalArgumentException("Producto no encontrado");
                return rs.getBigDecimal(1);
            }
        }
    }

    private BigDecimal calculateAfter(BigDecimal before, MovementType type, BigDecimal quantity) {
        if (type == null || quantity == null || quantity.signum() <= 0) throw new IllegalArgumentException("Movimiento y cantidad valida son obligatorios");
        BigDecimal after = type == MovementType.ENTRY ? before.add(quantity) : before.subtract(quantity);
        if (after.signum() < 0) throw new IllegalArgumentException("Stock insuficiente");
        return after;
    }

    private BigDecimal reverse(BigDecimal stock, MovementType type, BigDecimal quantity) {
        return type == MovementType.ENTRY ? stock.subtract(quantity) : stock.add(quantity);
    }

    private void updateStock(Connection conn, long productId, BigDecimal stock) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("UPDATE products SET current_stock=? WHERE id=?")) {
            ps.setBigDecimal(1, stock);
            ps.setLong(2, productId);
            ps.executeUpdate();
        }
    }

    private InventoryMovement fill(ResultSet rs) throws SQLException {
        InventoryMovement m = new InventoryMovement();
        m.setId(rs.getLong("id")); m.setProductId(rs.getLong("product_id")); m.setProductName(rs.getString("product_name"));
        m.setUserId(rs.getLong("user_id")); m.setUserName(rs.getString("user_name"));
        m.setMovementType(MovementType.valueOf(rs.getString("movement_type"))); m.setQuantity(rs.getBigDecimal("quantity"));
        m.setStockBefore(rs.getBigDecimal("stock_before")); m.setStockAfter(rs.getBigDecimal("stock_after"));
        m.setMovementDate(String.valueOf(rs.getTimestamp("movement_date")));
        long ref = rs.getLong("reference_id"); m.setReferenceId(rs.wasNull() ? null : ref);
        m.setNotes(rs.getString("notes")); m.setOriginalText(rs.getString("original_text"));
        m.setCreatedAt(String.valueOf(rs.getTimestamp("created_at")));
        return m;
    }

    private String blankToNull(String value) { return value == null || value.isBlank() ? null : value; }
}
