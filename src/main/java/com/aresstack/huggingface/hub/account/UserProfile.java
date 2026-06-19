package com.aresstack.huggingface.hub.account;

public final class UserProfile {

    private final String name;
    private final String fullName;
    private final String type;

    public UserProfile(String name, String fullName, String type) {
        this.name = name;
        this.fullName = fullName;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public String getFullName() {
        return fullName;
    }

    public String getType() {
        return type;
    }
}
