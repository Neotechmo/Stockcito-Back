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
import org.stockcito.model.PurchaseRequest;
import org.stockcito.model.PurchaseStatus;

public class ControllerPurchaseRequest {

    private static final String BASE_SELECT = """
        SELECT pr.*, p.name product_name, requester.name requested_by_name, approver.name approved_by_name
        FROM purchase_requests pr
        JOIN products p ON p.id=pr.product_id
        JOIN users requester ON requester.id=pr.requested_by
        LEFT JOIN users approver ON approver.id=pr.approved_by
        """;

    public List<PurchaseRequest> getAll(PurchaseStatus status) throws SQLException {
        String sql = BASE_SELECT + (status == null ? "" : " WHERE pr.status=?") + " ORDER BY pr.requested_at DESC";
        List<PurchaseRequest> result = new ArrayList<>();
        ConexionMysql mysql = new ConexionMysql();
        try (Connection conn = mysql.open(); PreparedStatement ps = conn.prepareStatement(sql)) {
            if (status != null) ps.setString(1, status.name());
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) result.add(fill(rs)); }
        } finally { mysql.close(); }
        return result;
    }

    public PurchaseRequest getById(long id) throws SQLException {
        ConexionMysql mysql = new ConexionMysql();
        try (Connection conn = mysql.open(); PreparedStatement ps = conn.prepareStatement(BASE_SELECT + " WHERE pr.id=?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? fill(rs) : null; }
        } finally { mysql.close(); }
    }

    public PurchaseRequest save(PurchaseRequest request) throws SQLException {
        String sql = "INSERT INTO purchase_requests(product_id,requested_by,approved_by,quantity,status,notes) VALUES(?,?,?,?,?,?)";
        ConexionMysql mysql = new ConexionMysql();
        try (Connection conn = mysql.open(); PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            setFields(ps, request);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) { if (rs.next()) request.setId(rs.getLong(1)); }
        } finally { mysql.close(); }
        return getById(request.getId());
    }

    public PurchaseRequest update(long id, PurchaseRequest request) throws SQLException {
        PurchaseRequest current = getById(id);
        if (current == null) return null;
        if (request.getStatus() == null) request.setStatus(current.getStatus());
        String sql = "UPDATE purchase_requests SET product_id=?,requested_by=?,approved_by=?,quantity=?,status=?,notes=? WHERE id=?";
        ConexionMysql mysql = new ConexionMysql();
        try (Connection conn = mysql.open(); PreparedStatement ps = conn.prepareStatement(sql)) {
            setFields(ps, request); ps.setLong(7, id); ps.executeUpdate();
        } finally { mysql.close(); }
        return getById(id);
    }

    public PurchaseRequest updateStatus(long id, PurchaseStatus status, Long approvedBy) throws SQLException {
        if (getById(id) == null) return null;
        String sql = "UPDATE purchase_requests SET status=?,approved_by=COALESCE(?,approved_by) WHERE id=?";
        ConexionMysql mysql = new ConexionMysql();
        try (Connection conn = mysql.open(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            if (approvedBy == null) ps.setNull(2, Types.BIGINT); else ps.setLong(2, approvedBy);
            ps.setLong(3, id); ps.executeUpdate();
        } finally { mysql.close(); }
        return getById(id);
    }

    public boolean delete(long id) throws SQLException {
        ConexionMysql mysql = new ConexionMysql();
        try (Connection conn = mysql.open(); PreparedStatement ps = conn.prepareStatement("DELETE FROM purchase_requests WHERE id=?")) {
            ps.setLong(1, id); return ps.executeUpdate() > 0;
        } finally { mysql.close(); }
    }

    private void setFields(PreparedStatement ps, PurchaseRequest r) throws SQLException {
        ps.setLong(1, r.getProductId()); ps.setLong(2, r.getRequestedBy());
        if (r.getApprovedBy() == null) ps.setNull(3, Types.BIGINT); else ps.setLong(3, r.getApprovedBy());
        ps.setBigDecimal(4, r.getQuantity());
        ps.setString(5, (r.getStatus() == null ? PurchaseStatus.PENDING : r.getStatus()).name());
        ps.setString(6, r.getNotes());
    }

    private PurchaseRequest fill(ResultSet rs) throws SQLException {
        PurchaseRequest r = new PurchaseRequest();
        r.setId(rs.getLong("id")); r.setProductId(rs.getLong("product_id")); r.setProductName(rs.getString("product_name"));
        r.setRequestedBy(rs.getLong("requested_by")); r.setRequestedByName(rs.getString("requested_by_name"));
        long approved = rs.getLong("approved_by"); r.setApprovedBy(rs.wasNull() ? null : approved);
        r.setApprovedByName(rs.getString("approved_by_name")); r.setQuantity(rs.getBigDecimal("quantity"));
        r.setStatus(PurchaseStatus.valueOf(rs.getString("status"))); r.setNotes(rs.getString("notes"));
        r.setRequestedAt(String.valueOf(rs.getTimestamp("requested_at"))); r.setUpdatedAt(String.valueOf(rs.getTimestamp("updated_at")));
        return r;
    }
}
