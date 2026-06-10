package org.stockcito.controller;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.stockcito.connection.ConexionMysql;
import org.stockcito.model.ImportItem;
import org.stockcito.model.ImportItemStatus;
import org.stockcito.model.ImportJob;
import org.stockcito.model.ImportStatus;
import org.stockcito.model.InventoryMovement;
import org.stockcito.model.MovementType;

public class ControllerImportJob {

    private static final Logger LOGGER = Logger.getLogger(ControllerImportJob.class.getName());
    private static final String FILE_HASH_UNIQUE_INDEX = "idx_ij_file_hash";

    private static final String JOB_SELECT = """
        SELECT j.*, u.name user_name FROM import_jobs j JOIN users u ON u.id=j.user_id
        """;

    public List<ImportJob> getAll() throws SQLException {
        List<ImportJob> result = new ArrayList<>();
        ConexionMysql mysql = new ConexionMysql();
        try (Connection conn = mysql.open();
             PreparedStatement ps = conn.prepareStatement(JOB_SELECT + " ORDER BY j.created_at DESC");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) result.add(fillJob(rs, false));
        } finally { mysql.close(); }
        return result;
    }

    public ImportJob getById(long id) throws SQLException {
        ConexionMysql mysql = new ConexionMysql();
        try (Connection conn = mysql.open(); PreparedStatement ps = conn.prepareStatement(JOB_SELECT + " WHERE j.id=?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? fillJob(rs, true) : null; }
        } finally { mysql.close(); }
    }

    public ImportJob preview(ImportJob job) throws SQLException {
        if (job.getItems() == null || job.getItems().isEmpty()) throw new IllegalArgumentException("La importacion debe contener items");
        String sql = """
            INSERT INTO import_jobs(user_id,source_type,source_filename,file_hash,status,raw_text,ai_model,notes)
            VALUES(?,?,?,?, 'PREVIEW',?,?,?)
            """;
        ConexionMysql mysql = new ConexionMysql();
        try (Connection conn = mysql.open()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                releaseCancelledFileHash(conn, job.getFileHash());
                ps.setLong(1, job.getUserId()); ps.setString(2, defaultSource(job.getSourceType()));
                ps.setString(3, job.getSourceFilename()); ps.setString(4, job.getFileHash());
                ps.setString(5, job.getRawText()); ps.setString(6, job.getAiModel()); ps.setString(7, job.getNotes());
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) { rs.next(); job.setId(rs.getLong(1)); }
                for (ImportItem item : job.getItems()) insertItem(conn, job.getId(), item);
                conn.commit();
            } catch (SQLIntegrityConstraintViolationException e) {
                conn.rollback();
                if (isDuplicateFileHash(e)) {
                    logDuplicateImport(job, e);
                    throw new DuplicateImportException(job.getFileHash(), job.getUserId(), e);
                }
                throw e;
            } catch (SQLException e) {
                conn.rollback();
                if (isDuplicateFileHash(e)) {
                    logDuplicateImport(job, e);
                    throw new DuplicateImportException(job.getFileHash(), job.getUserId(), e);
                }
                throw e;
            } catch (Exception e) { conn.rollback(); throw e; }
        } finally { mysql.close(); }
        return getById(job.getId());
    }

    public ImportJob confirm(long id) throws SQLException {
        ImportJob job = getById(id);
        if (job == null) return null;
        if (job.getStatus() != ImportStatus.PREVIEW) throw new IllegalArgumentException("Solo se pueden confirmar importaciones en PREVIEW");
        ConexionMysql mysql = new ConexionMysql();
        try (Connection conn = mysql.open()) {
            conn.setAutoCommit(false);
            try {
                ControllerInventoryMovement movements = new ControllerInventoryMovement();
                for (ImportItem item : job.getItems()) {
                    if (item.getStatus() == ImportItemStatus.VALID) {
                        if (item.getProductId() == null) throw new IllegalArgumentException("Todos los items confirmables requieren productId");
                        InventoryMovement movement = new InventoryMovement();
                        movement.setProductId(item.getProductId()); movement.setUserId(job.getUserId());
                        movement.setMovementType(item.getMovementType() == null ? MovementType.ENTRY : item.getMovementType());
                        movement.setQuantity(item.getQuantity()); movement.setReferenceId(job.getId());
                        movement.setOriginalText(item.getRawLine()); movement.setNotes("Importacion #" + job.getId());
                        movements.insert(conn, movement);
                    }
                }
                try (PreparedStatement ps = conn.prepareStatement("UPDATE import_jobs SET status='CONFIRMED',confirmed_at=CURRENT_TIMESTAMP WHERE id=?")) {
                    ps.setLong(1, id); ps.executeUpdate();
                }
                conn.commit();
            } catch (Exception e) { conn.rollback(); throw e; }
        } finally { mysql.close(); }
        return getById(id);
    }

    public ImportJob cancel(long id) throws SQLException {
        ImportJob job = getById(id);
        if (job == null) return null;
        if (job.getStatus() == ImportStatus.CONFIRMED) throw new IllegalArgumentException("Una importacion confirmada no se puede cancelar");
        ConexionMysql mysql = new ConexionMysql();
        String sql = """
            UPDATE import_jobs
            SET status='CANCELLED',
                file_hash=CASE
                    WHEN file_hash IS NULL OR file_hash='' THEN file_hash
                    WHEN file_hash LIKE 'cancelled-%' THEN file_hash
                    ELSE ?
                END
            WHERE id=?
            """;
        try (Connection conn = mysql.open(); PreparedStatement ps = conn.prepareStatement(sql)) {
            String releasedHash = cancelledFileHash(id);
            ps.setString(1, releasedHash);
            ps.setLong(2, id);
            ps.executeUpdate();
            LOGGER.info("Importacion cancelada: id=" + id
                    + ", userId=" + job.getUserId()
                    + ", fileHashOriginal=" + safe(job.getFileHash())
                    + ", fileHashLiberado=" + releasedHash);
        } finally { mysql.close(); }
        return getById(id);
    }

    private void insertItem(Connection conn, long jobId, ImportItem item) throws SQLException {
        if (item.getQuantity() == null || item.getQuantity().signum() <= 0 || item.getProductName() == null || item.getProductName().isBlank()) {
            throw new IllegalArgumentException("Cada item requiere productName y quantity positiva");
        }
        String sql = """
            INSERT INTO import_items(import_job_id,product_id,product_name,movement_type,quantity,unit_of_measure,confidence,raw_line,status)
            VALUES(?,?,?,?,?,?,?,?,?)
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, jobId);
            if (item.getProductId() == null) ps.setNull(2, Types.BIGINT); else ps.setLong(2, item.getProductId());
            ps.setString(3, item.getProductName());
            ps.setString(4, (item.getMovementType() == null ? MovementType.ENTRY : item.getMovementType()).name());
            ps.setBigDecimal(5, item.getQuantity()); ps.setString(6, item.getUnitOfMeasure());
            ps.setBigDecimal(7, item.getConfidence()); ps.setString(8, item.getRawLine());
            ps.setString(9, (item.getStatus() == null ? classify(item) : item.getStatus()).name());
            ps.executeUpdate();
        }
    }

    private ImportItemStatus classify(ImportItem item) {
        if (item.getProductId() == null) return ImportItemStatus.ERROR;
        if (item.getConfidence() != null && item.getConfidence().doubleValue() < .75) return ImportItemStatus.WARNING;
        return ImportItemStatus.VALID;
    }

    private List<ImportItem> getItems(long jobId) throws SQLException {
        List<ImportItem> result = new ArrayList<>();
        ConexionMysql mysql = new ConexionMysql();
        try (Connection conn = mysql.open(); PreparedStatement ps = conn.prepareStatement("SELECT * FROM import_items WHERE import_job_id=? ORDER BY id")) {
            ps.setLong(1, jobId);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) result.add(fillItem(rs)); }
        } finally { mysql.close(); }
        return result;
    }

    private ImportJob fillJob(ResultSet rs, boolean includeItems) throws SQLException {
        ImportJob j = new ImportJob();
        j.setId(rs.getLong("id")); j.setUserId(rs.getLong("user_id")); j.setUserName(rs.getString("user_name"));
        j.setSourceType(rs.getString("source_type")); j.setSourceFilename(rs.getString("source_filename"));
        j.setFileHash(rs.getString("file_hash")); j.setStatus(ImportStatus.valueOf(rs.getString("status")));
        j.setRawText(rs.getString("raw_text")); j.setAiModel(rs.getString("ai_model")); j.setNotes(rs.getString("notes"));
        j.setCreatedAt(String.valueOf(rs.getTimestamp("created_at"))); j.setConfirmedAt(String.valueOf(rs.getTimestamp("confirmed_at")));
        j.setUpdatedAt(String.valueOf(rs.getTimestamp("updated_at")));
        if (includeItems) j.setItems(getItems(j.getId()));
        return j;
    }

    private ImportItem fillItem(ResultSet rs) throws SQLException {
        ImportItem i = new ImportItem();
        i.setId(rs.getLong("id")); i.setImportJobId(rs.getLong("import_job_id"));
        long product = rs.getLong("product_id"); i.setProductId(rs.wasNull() ? null : product);
        i.setProductName(rs.getString("product_name")); i.setMovementType(MovementType.valueOf(rs.getString("movement_type")));
        i.setQuantity(rs.getBigDecimal("quantity")); i.setUnitOfMeasure(rs.getString("unit_of_measure"));
        i.setConfidence(rs.getBigDecimal("confidence")); i.setRawLine(rs.getString("raw_line"));
        i.setStatus(ImportItemStatus.valueOf(rs.getString("status")));
        long duplicate = rs.getLong("duplicate_of"); i.setDuplicateOf(rs.wasNull() ? null : duplicate);
        i.setCreatedAt(String.valueOf(rs.getTimestamp("created_at")));
        return i;
    }

    private String defaultSource(String source) { return source == null || source.isBlank() ? "MANUAL" : source.toUpperCase(); }

    private void releaseCancelledFileHash(Connection conn, String fileHash) throws SQLException {
        if (fileHash == null || fileHash.isBlank()) return;

        String selectSql = "SELECT id,user_id FROM import_jobs WHERE file_hash=? AND status='CANCELLED'";
        try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
            ps.setString(1, fileHash);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return;
                long cancelledId = rs.getLong("id");
                long cancelledUserId = rs.getLong("user_id");
                String releasedHash = cancelledFileHash(cancelledId);
                try (PreparedStatement update = conn.prepareStatement("UPDATE import_jobs SET file_hash=? WHERE id=?")) {
                    update.setString(1, releasedHash);
                    update.setLong(2, cancelledId);
                    update.executeUpdate();
                }
                LOGGER.info("Hash de importacion cancelada liberado antes de reimportar: id="
                        + cancelledId
                        + ", userId=" + cancelledUserId
                        + ", fileHashOriginal=" + safe(fileHash)
                        + ", fileHashLiberado=" + releasedHash);
            }
        }
    }

    private String cancelledFileHash(long id) {
        return "cancelled-" + id;
    }

    private boolean isDuplicateFileHash(SQLException e) {
        String message = e.getMessage();
        return message != null
                && message.contains(FILE_HASH_UNIQUE_INDEX)
                && (e instanceof SQLIntegrityConstraintViolationException || message.contains("Duplicate entry"));
    }

    private void logDuplicateImport(ImportJob job, SQLException e) {
        LOGGER.warning("Importacion rechazada por duplicado: fileHash="
                + safe(job.getFileHash())
                + ", userId=" + job.getUserId()
                + ", motivo=" + e.getMessage());
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "<sin-file-hash>" : value;
    }
}
