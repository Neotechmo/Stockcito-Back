package org.stockcito.controller;

public class DuplicateImportException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public static final String CODE = "IMPORT_ALREADY_EXISTS";

    private final String fileHash;
    private final long userId;

    public DuplicateImportException(String fileHash, long userId, Throwable cause) {
        super("Este archivo ya fue importado previamente", cause);
        this.fileHash = fileHash;
        this.userId = userId;
    }

    public String getCode() {
        return CODE;
    }

    public String getFileHash() {
        return fileHash;
    }

    public long getUserId() {
        return userId;
    }
}
