package org.example.validator.scope;


public enum AccessAuth implements CommonScope<String> {
    ALL("all"),
    GROUP("group"),
    FLOW("flow"),
    QUEUE("queue")
    ;

    String value;

    AccessAuth(String value) {
        this.value = value;
    }

    @Override
    public String getValue() {
        return value;
    }
}