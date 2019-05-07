package com.sibilante.houseseats.client.model;

public class Show {

    private String id;

    private String name;

    public Show(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
