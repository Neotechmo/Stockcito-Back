package org.stockcito.controller;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import org.stockcito.connection.ConexionMysql;
import org.stockcito.model.Product;

public class ControllerProduct {

    private static final String BASE_SELECT = """
            SELECT p.*, c.name AS category_name, s.name AS supplier_name
            FROM products p
            INNER JOIN categories c ON c.id = p.category_id
            LEFT JOIN suppliers s ON s.id = p.supplier_id
            """;

    public List<Product> getAll() throws SQLException {
        String sql = BASE_SELECT + " WHERE p.is_active = TRUE ORDER BY p.name";
        List<Product> products = new ArrayList<>();
        ConexionMysql connMysql = new ConexionMysql();

        try (Connection conn = connMysql.open();
             PreparedStatement pstm = conn.prepareStatement(sql);
             ResultSet rs = pstm.executeQuery()) {
            while (rs.next()) {
                products.add(fill(rs));
            }
        } finally {
            connMysql.close();
        }

        return products;
    }

    public Product getById(long id) throws SQLException {
        String sql = BASE_SELECT + " WHERE p.id = ? AND p.is_active = TRUE";
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

    public Product save(Product product) throws SQLException {
        String sql = """
                INSERT INTO products
                (category_id, supplier_id, name, sku, description, unit_of_measure, min_stock, current_stock, unit_price, barcode)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        ConexionMysql connMysql = new ConexionMysql();

        try (Connection conn = connMysql.open();
             PreparedStatement pstm = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            setFields(pstm, product);
            pstm.executeUpdate();

            try (ResultSet rs = pstm.getGeneratedKeys()) {
                if (rs.next()) {
                    product.setId(rs.getLong(1));
                }
            }
        } finally {
            connMysql.close();
        }

        return getById(product.getId());
    }

    public Product update(long id, Product product) throws SQLException {
        String sql = """
                UPDATE products
                SET category_id = ?, supplier_id = ?, name = ?, sku = ?, description = ?,
                    unit_of_measure = ?, min_stock = ?, current_stock = ?, unit_price = ?,
                    barcode = ?, is_active = ?
                WHERE id = ?
                """;
        ConexionMysql connMysql = new ConexionMysql();

        try (Connection conn = connMysql.open();
             PreparedStatement pstm = conn.prepareStatement(sql)) {
            setFields(pstm, product);
            pstm.setBoolean(11, product.isIsActive());
            pstm.setLong(12, id);
            pstm.executeUpdate();
        } finally {
            connMysql.close();
        }

        return getById(id);
    }

    public boolean delete(long id) throws SQLException {
        String sql = "UPDATE products SET is_active = FALSE WHERE id = ?";
        ConexionMysql connMysql = new ConexionMysql();

        try (Connection conn = connMysql.open();
             PreparedStatement pstm = conn.prepareStatement(sql)) {
            pstm.setLong(1, id);
            return pstm.executeUpdate() > 0;
        } finally {
            connMysql.close();
        }
    }

    private void setFields(PreparedStatement pstm, Product product) throws SQLException {
        pstm.setLong(1, product.getCategoryId());
        if (product.getSupplierId() == null) {
            pstm.setNull(2, Types.BIGINT);
        } else {
            pstm.setLong(2, product.getSupplierId());
        }
        pstm.setString(3, product.getName());
        pstm.setString(4, blankToNull(product.getSku()));
        pstm.setString(5, product.getDescription());
        pstm.setString(6, product.getUnitOfMeasure());
        pstm.setBigDecimal(7, product.getMinStock());
        pstm.setBigDecimal(8, product.getCurrentStock());
        pstm.setBigDecimal(9, product.getUnitPrice());
        pstm.setString(10, product.getBarcode());
    }

    private Product fill(ResultSet rs) throws SQLException {
        Product product = new Product();
        product.setId(rs.getLong("id"));
        product.setCategoryId(rs.getLong("category_id"));
        product.setCategoryName(rs.getString("category_name"));

        long supplierId = rs.getLong("supplier_id");
        product.setSupplierId(rs.wasNull() ? null : supplierId);
        product.setSupplierName(rs.getString("supplier_name"));

        product.setName(rs.getString("name"));
        product.setSku(rs.getString("sku"));
        product.setDescription(rs.getString("description"));
        product.setUnitOfMeasure(rs.getString("unit_of_measure"));
        product.setMinStock(rs.getBigDecimal("min_stock"));
        product.setCurrentStock(rs.getBigDecimal("current_stock"));
        product.setUnitPrice(rs.getBigDecimal("unit_price"));
        product.setBarcode(rs.getString("barcode"));
        product.setIsActive(rs.getBoolean("is_active"));
        product.setCreatedAt(String.valueOf(rs.getTimestamp("created_at")));
        product.setUpdatedAt(String.valueOf(rs.getTimestamp("updated_at")));
        return product;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
