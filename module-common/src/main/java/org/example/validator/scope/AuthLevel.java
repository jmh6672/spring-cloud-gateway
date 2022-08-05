package org.example.validator.scope;

public enum AuthLevel implements CommonScope<String> {
    SUPER_ADMIN("superadmin"),  // 최상위 관리자
    ADMIN("admin"),  // 서비스 관리자
    MANAGER("usermanager"),  // 상담 관리자
    USER("user")  // 일반 사용자
    ;

    String value;

    AuthLevel(String value) {
        this.value = value;
    }

    @Override
    public String getValue() {
        return value;
    }
}