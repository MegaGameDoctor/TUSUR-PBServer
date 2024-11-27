package com.mayakplay.aclf.cloud.util;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * @author mayakplay
 * @version 0.0.1
 * @since 21.07.2019.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class JsonUtils {

    private static final Gson gson = new Gson();

    public static <T> T toObject(String jsonString, Class<T> type) {
        try {
            return gson.fromJson(jsonString, type);
        } catch (JsonSyntaxException e) {
            return null;
        }
    }

    public static String toJson(Object object) {
        return gson.toJson(object);
    }

}