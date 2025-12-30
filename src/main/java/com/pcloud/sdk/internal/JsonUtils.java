package com.pcloud.sdk.internal;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonObject;
import com.pcloud.sdk.ApiClient;
import com.pcloud.sdk.RemoteFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Utility class for parsing RemoteFile instance compatible with the pCloud sdk.
 */
public class JsonUtils {
    private static final String PCLOUD_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss Z";
    private static final DateFormat DATE_FORMAT;
    
    static {
        DATE_FORMAT = new SimpleDateFormat(PCLOUD_DATE_FORMAT, Locale.US);
        DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    private JsonUtils() {
        // Utility class - prevent instantiation
    }

    /**
     * Parses a JSON object containing file metadata into a RealRemoteFile instance.
     *
     * @param jsonObject The JSON object containing the file metadata
     * @param apiClient The ApiClient instance to associate with the file
     * @return A RealRemoteFile instance populated with the metadata
     * @throws IOException if parsing fails
     */
    private static RemoteFile parseFile(JsonObject jsonObject, ApiClient apiClient) throws IOException {
        if (jsonObject == null) {
            throw new IllegalArgumentException("JSON object cannot be null");
        }

        // Create a new Gson instance with the provided ApiClient for this specific conversion
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(RealRemoteFile.class, new RealRemoteFile.InstanceCreator(apiClient))
                .registerTypeAdapter(Date.class, (JsonDeserializer<Date>) (json, typeOfT, context) -> {
                    try {
                        return DATE_FORMAT.parse(json.getAsString());
                    } catch (ParseException e) {
                        throw new UncheckedIOException(new IOException(e));
                    }
                })
                .create();

        return gson.fromJson(jsonObject, RealRemoteFile.class);
    }

    /**
     * Extracts the first file from a JSON response that contains a "metadata" array.
     * This is useful for upload responses where the file is in the "metadata" array.
     *
     * @param jsonObject The JSON object containing the API response
     * @param apiClient The ApiClient instance to associate with the file
     * @return A RealRemoteFile instance for the first file in the metadata array
     * @throws IOException if parsing fails
     */
    public static RemoteFile buildFile(JsonObject jsonObject, ApiClient apiClient) throws IOException {
        if (jsonObject == null || !jsonObject.has("metadata")) {
            throw new IOException("Invalid response: missing 'metadata' field");
        }

        var metadataArray = jsonObject.getAsJsonArray("metadata");
        if (metadataArray.isEmpty()) {
            throw new IOException("No file metadata found in response");
        }

        JsonObject fileJson = metadataArray.get(0).getAsJsonObject();
        try {
            return parseFile(fileJson, apiClient);
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }
}
