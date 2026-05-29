package com.hackerprank.editor;

public class JavaCompletionRequest {
    private String code = "";
    private int lineNumber = 1;
    private int column = 1;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code == null ? "" : code;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(int lineNumber) {
        this.lineNumber = Math.max(1, lineNumber);
    }

    public int getColumn() {
        return column;
    }

    public void setColumn(int column) {
        this.column = Math.max(1, column);
    }
}
