package org.stockcito.model;

import java.math.BigDecimal;

public class ImportItem {

    private long id;
    private long importJobId;
    private Long productId;
    private String productName;
    private MovementType movementType;
    private BigDecimal quantity;
    private String unitOfMeasure;
    private BigDecimal confidence;
    private String rawLine;
    private ImportItemStatus status;
    private Long duplicateOf;
    private String createdAt;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getImportJobId() { return importJobId; }
    public void setImportJobId(long importJobId) { this.importJobId = importJobId; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public MovementType getMovementType() { return movementType; }
    public void setMovementType(MovementType movementType) { this.movementType = movementType; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public String getUnitOfMeasure() { return unitOfMeasure; }
    public void setUnitOfMeasure(String unitOfMeasure) { this.unitOfMeasure = unitOfMeasure; }
    public BigDecimal getConfidence() { return confidence; }
    public void setConfidence(BigDecimal confidence) { this.confidence = confidence; }
    public String getRawLine() { return rawLine; }
    public void setRawLine(String rawLine) { this.rawLine = rawLine; }
    public ImportItemStatus getStatus() { return status; }
    public void setStatus(ImportItemStatus status) { this.status = status; }
    public Long getDuplicateOf() { return duplicateOf; }
    public void setDuplicateOf(Long duplicateOf) { this.duplicateOf = duplicateOf; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
