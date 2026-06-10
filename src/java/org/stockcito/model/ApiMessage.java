package org.stockcito.model;

public class ApiMessage {

    private String message;
    private String code;

    public ApiMessage(String message) {
        this.message = message;
    }

    public ApiMessage(String message, String code) {
        this.message = message;
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
