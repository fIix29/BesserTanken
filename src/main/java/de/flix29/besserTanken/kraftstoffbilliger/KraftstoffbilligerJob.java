package de.flix29.besserTanken.kraftstoffbilliger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import de.flix29.BesserTanken;
import de.flix29.besserTanken.deserializer.*;
import de.flix29.besserTanken.model.kraftstoffbilliger.FuelStation;
import de.flix29.besserTanken.model.kraftstoffbilliger.FuelStationDetail;
import de.flix29.besserTanken.model.kraftstoffbilliger.FuelType;
import de.flix29.besserTanken.model.kraftstoffbilliger.requests.Endpoints;
import de.flix29.besserTanken.model.kraftstoffbilliger.requests.HTTPMethod;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static de.flix29.besserTanken.deserializer.CustomModelTypes.*;
import static de.flix29.besserTanken.model.kraftstoffbilliger.requests.Endpoints.*;
import static de.flix29.besserTanken.model.kraftstoffbilliger.requests.HTTPMethod.GET;
import static de.flix29.besserTanken.model.kraftstoffbilliger.requests.HTTPMethod.POST;

@Service
public class KraftstoffbilligerJob {

    private final Logger LOGGER = LoggerFactory.getLogger(KraftstoffbilligerJob.class);
    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new CustomLocalDateTimeDeserializer())
            .registerTypeAdapter(FuelType.class, new CustomFuelTypeDeserializer())
            .registerTypeAdapter(FuelStation.class, new CustomFuelStationDeserializer())
            .registerTypeAdapter(PRICE_LIST_TYPE, new CustomPriceDeserializer())
            .registerTypeAdapter(OPENING_TIMES_LIST_TYPE, new CustomOpeningTimeDeserializer())
            .setPrettyPrinting()
            .create();

    private HttpResponse<String> sendHttpGETRequestWithResponse(Endpoints endpoint) throws IOException, InterruptedException {
        return sendHttpRequestWithResponse(endpoint, GET, null);
    }

    private HttpResponse<String> sendHttpPOSTRequestWithResponse(Endpoints endpoint, Map<String, String> parameter) throws IOException, InterruptedException {
        return sendHttpRequestWithResponse(endpoint, POST, parameter);
    }

    private HttpResponse<String> sendHttpRequestWithResponse(Endpoints endpoint, HTTPMethod httpMethod, Map<String, String> parameter) throws IOException, InterruptedException {
        final var apiKey = BesserTanken.getSecrets().getOrDefault("bessertankenKey", "");
        var requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(endpoint.getUrl()))
                .header("apikey", apiKey);

        if (httpMethod == GET) {
            requestBuilder.GET();
        } else if (httpMethod == POST) {
            requestBuilder
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(createFormDataFromString(parameter)));
        }

        return HttpClient.newHttpClient().send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
    }

    public List<FuelType> getFuelTypes() throws IOException, InterruptedException {
        var response = sendHttpGETRequestWithResponse(TYPES_ENDPOINT);

        try {
            var jsonArray = gson.fromJson(response.body(), JsonObject.class).get("types").getAsJsonArray();
            return gson.fromJson(jsonArray, FUEL_TYPE_LIST_TYPE);
        } catch (Exception e) {
            LOGGER.error("Error while parsing fuel types json {}", response.body(), e);
            return Collections.emptyList();
        }
    }

    public List<FuelStation> getFuelStations(@NotNull FuelType fuelType,
                                             @NotNull double lat,
                                             @NotNull double lon,
                                             Integer radius) throws IOException, InterruptedException {
        var formData = new HashMap<>(Map.of(
                "type", String.valueOf(fuelType.getId()),
                "lat", String.valueOf(lat),
                "lon", String.valueOf(lon)));

        if (radius != null) {
            formData.put("radius", String.valueOf(radius));
        }

        var response = sendHttpPOSTRequestWithResponse(SEARCH_ENDPOINT, formData);
        try {
            var jsonArray = gson.fromJson(response.body(), JsonObject.class).get("results").getAsJsonArray();
            return gson.fromJson(jsonArray, FUEL_STATION_LIST_TYPE);
        } catch (Exception e) {
            LOGGER.error("Error while parsing fuel stations json {}", response.body(), e);
            return Collections.emptyList();
        }

    }

    public List<FuelStation> getFuelStationRoute(@NotNull int lat,
                                                 @NotNull int lon,
                                                 @NotNull int lat2,
                                                 @NotNull int lon2,
                                                 @NotNull int type,
                                                 String map) throws IOException, InterruptedException {
        var formData = new HashMap<>(Map.of(
                "lat", String.valueOf(lat),
                "lon", String.valueOf(lon),
                "lat2", String.valueOf(lat2),
                "lon2", String.valueOf(lon2),
                "type", String.valueOf(type)));

        if (map != null) {
            formData.put("map", map);
        }

        var response = sendHttpPOSTRequestWithResponse(ROUTING_ENDPOINT, formData);

        try {
            JsonArray jsonArray = gson.fromJson(response.body(), JsonObject.class).get("results").getAsJsonArray();
            return gson.fromJson(jsonArray, FUEL_STATION_LIST_TYPE);
        } catch (Exception e) {
            LOGGER.error("Error while parsing fuel stations json {}", response.body(), e);
            return Collections.emptyList();
        }
    }

    public FuelStationDetail getFuelStationDetails(@NotNull String id) throws IOException, InterruptedException {
        var formData = Map.of("id", id);

        var response = sendHttpPOSTRequestWithResponse(DETAILS_ENDPOINT, formData);

        try {
            var result = gson.fromJson(response.body(), JsonObject.class).get("result").getAsJsonArray().get(0);
            return gson.fromJson(result, FuelStationDetail.class);
        } catch (Exception e) {
            LOGGER.error("Error while parsing fuel station details json {}", response.body(), e);
            return null;
        }
    }

    private static String createFormDataFromString(Map<String, String> formData) {
        StringBuilder formBodyBuilder = new StringBuilder();
        formData.forEach((key, value) -> {
            if (!formBodyBuilder.isEmpty()) {
                formBodyBuilder.append("&");
            }
            formBodyBuilder.append(URLEncoder.encode(key, StandardCharsets.UTF_8));
            formBodyBuilder.append("=");
            formBodyBuilder.append(URLEncoder.encode(value, StandardCharsets.UTF_8));
        });
        return formBodyBuilder.toString();
    }
}
