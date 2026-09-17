package com.bazarfx.util;

import java.util.UUID;

/** Central place for generating unique IDs, kept small so it's easy to swap the strategy later. */
public class IdGenerator {
    public static String newId() {
        return UUID.randomUUID().toString();
    }
}
