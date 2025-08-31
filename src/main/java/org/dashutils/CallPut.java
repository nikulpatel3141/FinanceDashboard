package org.dashutils;

public enum CallPut {
    CALL,
    PUT;

    public static CallPut fromString(String s) {
        s = s.toUpperCase();
        return switch (s) {
            case "CALL" -> CALL;
            case "PUT" -> PUT;
            default -> throw new RuntimeException("Cannot parse " + s + " into CallPut");
        };
    }
}
