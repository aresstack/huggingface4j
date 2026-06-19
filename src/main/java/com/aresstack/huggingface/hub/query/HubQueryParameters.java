package com.aresstack.huggingface.hub.query;

public final class HubQueryParameters {

    private final StringBuilder query = new StringBuilder();

    public HubQueryParameters set(String name, String value) {
        return add(name, value);
    }

    public HubQueryParameters add(String name, String value) {
        if (name == null || value == null || name.trim().isEmpty() || value.trim().isEmpty()) {
            return this;
        }
        if (query.length() > 0) {
            query.append('&');
        }
        query.append(HubUrlBuilder.encode(name.trim()));
        query.append('=');
        query.append(HubUrlBuilder.encode(value.trim()));
        return this;
    }

    public HubQueryParameters setNumber(String name, Integer value) {
        return value == null ? this : add(name, String.valueOf(value));
    }

    public boolean isEmpty() {
        return query.length() == 0;
    }

    public String toQueryString() {
        return query.toString();
    }
}
