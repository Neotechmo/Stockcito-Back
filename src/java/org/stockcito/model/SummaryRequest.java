package org.stockcito.model;

public class SummaryRequest {

    private Integer daysBack;
    private Integer lowStockLimit;
    private Integer highExitLimit;
    private Integer slowMovementDays;
    private Integer slowMovementLimit;
    private Integer maxSentences;
    private String language;

    public Integer getDaysBack() { return daysBack; }
    public void setDaysBack(Integer daysBack) { this.daysBack = daysBack; }
    public Integer getLowStockLimit() { return lowStockLimit; }
    public void setLowStockLimit(Integer lowStockLimit) { this.lowStockLimit = lowStockLimit; }
    public Integer getHighExitLimit() { return highExitLimit; }
    public void setHighExitLimit(Integer highExitLimit) { this.highExitLimit = highExitLimit; }
    public Integer getSlowMovementDays() { return slowMovementDays; }
    public void setSlowMovementDays(Integer slowMovementDays) { this.slowMovementDays = slowMovementDays; }
    public Integer getSlowMovementLimit() { return slowMovementLimit; }
    public void setSlowMovementLimit(Integer slowMovementLimit) { this.slowMovementLimit = slowMovementLimit; }
    public Integer getMaxSentences() { return maxSentences; }
    public void setMaxSentences(Integer maxSentences) { this.maxSentences = maxSentences; }
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
}
