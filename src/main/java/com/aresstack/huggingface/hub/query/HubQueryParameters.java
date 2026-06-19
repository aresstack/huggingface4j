package com.aresstack.huggingface.hub.query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class HubQueryParameters {

    private final List<Entry> entries = new ArrayList<Entry>();

    public HubQueryParameters set(String name, String value) {
        remove(name);
        return add(name, value);
    }

    public HubQueryParameters add(String name, String value) {
        if (name == null || value == null || name.trim().isEmpty() || value.trim().isEmpty()) {
            return this;
        }
        entries.add(new Entry(name.trim(), value.trim()));
        return this;
    }

    public HubQueryParameters setNumber(String name, Integer value) {
        remove(name);
        return value == null ? this : add(name, String.valueOf(value));
    }

    public void remove(String name) {
        for (int index = entries.size() - 1; index >= 0; index--) {
            if (entries.get(index).name.equals(name)) {
                entries.remove(index);
            }
        }
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    public List<Entry> entries() {
        return Collections.unmodifiableList(entries);
    }

    public String toQueryString() {
        StringBuilder builder = new StringBuilder();
        for (Entry entry : entries) {
            if (builder.length() > 0) {
                builder.append('&');
            }
            builder.append(HubUrlBuilder.encode(entry.getName()));
            builder.append('=');
            builder.append(HubUrlBuilder.encode(entry.getValue()));
        }
        return builder.toString();
    }

    public static final class Entry {
        private final String name;
        private final String value;

        private Entry(String name, String value) {
            this.name = name;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public String getValue() {
            return value;
        }
    }
}
