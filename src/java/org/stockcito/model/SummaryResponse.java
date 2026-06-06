package org.stockcito.model;

public class SummaryResponse {

    private String summary;
    private String model;

    public SummaryResponse(String summary, String model) {
        this.summary = summary;
        this.model = model;
    }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
}
