package me.flashyreese.mods.greenlight.feature;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

final class JsonCopies {
    private static final Gson GSON = new Gson();

    private JsonCopies() {
    }

    static JsonObject copy(JsonObject json) {
        return GSON.fromJson(GSON.toJson(json), JsonObject.class);
    }
}
