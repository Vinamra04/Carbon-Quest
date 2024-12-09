package com.example.carbon_emission_tracker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

@RestController
@RequestMapping("/api/carbon")
public class CarbonEmissionsController {

    private static final String TOMTOM_API_KEY = "0TQGxzg4nBU5qP4Du9YkaQAkKJ1OvNnd";

    // Define emission constants
    private static final double CO2_EMISSION_PER_KM_CAR = 0.21;
    private static final double CO2_EMISSION_PER_KM_BUS = 0.06;
    private static final double CO2_EMISSION_PER_KM_TRAIN = 0.02;
    private static final double CO2_EMISSION_PER_KM_ELECTRIC_CAR = 0.05;
    private static final double CO2_EMISSION_PER_KM_BICYCLE = 0.00;
    private static final double CO2_EMISSION_PER_KM_MOTORCYCLE = 0.15;
    private static final double CO2_EMISSION_PER_KM_VAN = 0.25;
    private static final double CO2_EMISSION_PER_KM_HYBRID_VEHICLE = 0.12;
    private static final double CO2_EMISSION_PER_KM_AUTO_RICKSHAW = 0.10;
    private static final double CO2_EMISSION_PER_KM_CNG_VEHICLE = 0.08;

    @PostMapping("/calculate")
    public String calculateEmissions(@RequestBody List<Trip> trips) {
        double totalEmissions = 0;
        StringBuilder responseBuilder = new StringBuilder();

        for (Trip trip : trips) {
            try {
                double[] startCoordinates = getCoordinates(trip.getStartAddress());
                double[] endCoordinates = getCoordinates(trip.getEndAddress());

                if (startCoordinates != null && endCoordinates != null) {
                    double distance = getDistance(startCoordinates, endCoordinates);
                    if (distance >= 0) {
                        double emissions = calculateEmissions(trip.getVehicleType(), distance);
                        totalEmissions += emissions;
                        responseBuilder.append(String.format(
                                "Trip from %s to %s with vehicle type %s has a distance of %.2f km and emissions of %.2f kg CO2.%n",
                                trip.getStartAddress(), trip.getEndAddress(), trip.getVehicleType(), distance, emissions));
                    } else {
                        responseBuilder.append("Failed to calculate distance for trip: ").append(trip).append("\n");
                    }
                } else {
                    responseBuilder.append("Failed to get coordinates for trip: ").append(trip).append("\n");
                }
            } catch (IOException e) {
                e.printStackTrace();
                responseBuilder.append("Error during the trip calculation.").append("\n");
            }
        }

        responseBuilder.append(String.format("Total carbon emissions: %.2f kg CO2%n", totalEmissions));
        responseBuilder.append(givePersonalizedSuggestions(totalEmissions, trips));
        return responseBuilder.toString();
    }

    // OkHttp client with timeouts
    private OkHttpClient createClientWithTimeouts() {
        return new OkHttpClient.Builder()
                .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .build();
    }

    // Fetch coordinates from the TomTom API
    private double[] getCoordinates(String address) throws IOException {
        OkHttpClient client = createClientWithTimeouts();
        String url = "https://api.tomtom.com/search/2/geocode/" + address.replace(" ", "%20") + ".json?key=" + TOMTOM_API_KEY;

        Request request = new Request.Builder().url(url).build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                return null;
            }
            String responseBody = response.body().string();
            return parseCoordinates(responseBody);
        }
    }

    // Parse the coordinates from the TomTom API response
    private double[] parseCoordinates(String responseBody) {
        JsonObject jsonObject = JsonParser.parseString(responseBody).getAsJsonObject();
        JsonObject firstResult = jsonObject.getAsJsonArray("results").get(0).getAsJsonObject();
        JsonObject position = firstResult.getAsJsonObject("position");

        double latitude = position.get("lat").getAsDouble();
        double longitude = position.get("lon").getAsDouble();
        return new double[]{latitude, longitude};
    }

    // Fetch distance from the TomTom API
    private double getDistance(double[] start, double[] end) throws IOException {
        OkHttpClient client = createClientWithTimeouts();
        String url = "https://api.tomtom.com/routing/1/calculateRoute/" +
                start[0] + "," + start[1] + ":" + end[0] + "," + end[1] + "/json?key=" + TOMTOM_API_KEY;

        Request request = new Request.Builder().url(url).build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                return -1;
            }
            String responseBody = response.body().string();
            return parseDistance(responseBody);
        }
    }

    // Parse the distance from the TomTom API response
    private double parseDistance(String responseBody) {
        JsonObject jsonObject = JsonParser.parseString(responseBody).getAsJsonObject();
        JsonObject routes = jsonObject.getAsJsonArray("routes").get(0).getAsJsonObject();
        JsonObject summary = routes.getAsJsonObject("summary");
        double distanceInMeters = summary.get("lengthInMeters").getAsDouble();
        return distanceInMeters / 1000.0; // Convert meters to kilometers
    }

    // Calculate emissions for each trip
    private double calculateEmissions(String vehicleType, double distance) {
        double emissionsPerKm;
        switch (vehicleType) {
            case "car":
                emissionsPerKm = CO2_EMISSION_PER_KM_CAR;
                break;
            case "bus":
                emissionsPerKm = CO2_EMISSION_PER_KM_BUS;
                break;
            case "train":
                emissionsPerKm = CO2_EMISSION_PER_KM_TRAIN;
                break;
            case "electric car":
                emissionsPerKm = CO2_EMISSION_PER_KM_ELECTRIC_CAR;
                break;
            case "bicycle":
                emissionsPerKm = CO2_EMISSION_PER_KM_BICYCLE;
                break;
            case "motorcycle":
                emissionsPerKm = CO2_EMISSION_PER_KM_MOTORCYCLE;
                break;
            case "van":
                emissionsPerKm = CO2_EMISSION_PER_KM_VAN;
                break;
            case "hybrid vehicle":
                emissionsPerKm = CO2_EMISSION_PER_KM_HYBRID_VEHICLE;
                break;
            case "auto rickshaw":
                emissionsPerKm = CO2_EMISSION_PER_KM_AUTO_RICKSHAW;
                break;
            case "cng vehicle":
                emissionsPerKm = CO2_EMISSION_PER_KM_CNG_VEHICLE;
                break;
            default:
                return 0;
        }
        return emissionsPerKm * distance;
    }
    
    private String givePersonalizedSuggestions(double totalEmissions, List<Trip> trips) {
        List<String> personalizedSuggestions = new ArrayList<>();
        boolean usesPublicTransport = trips.stream().anyMatch(trip -> trip.getVehicleType().equals("bus") || trip.getVehicleType().equals("train"));
        boolean usesEcoFriendlyVehicles = trips.stream().anyMatch(trip -> trip.getVehicleType().equals("electric car") || trip.getVehicleType().equals("hybrid vehicle") || trip.getVehicleType().equals("bicycle"));

        if (usesPublicTransport) {
            personalizedSuggestions.add("Great job using public transportation!");
        }
        if (usesEcoFriendlyVehicles) {
            personalizedSuggestions.add("You're doing well with eco-friendly vehicles.");
        }

        if (personalizedSuggestions.isEmpty()) {
            personalizedSuggestions.add("Consider using eco-friendly transport methods, like bicycles or electric vehicles.");
        }

        StringBuilder suggestions = new StringBuilder("Personalized suggestions:\n");
        personalizedSuggestions.forEach(suggestion -> suggestions.append("- ").append(suggestion).append("\n"));

        return suggestions.toString();
    }

    // Trip class to represent each trip object
    public static class Trip {
        private String startAddress;
        private String endAddress;
        private String vehicleType;

        // Getters and Setters
        public String getStartAddress() {
            return startAddress;
        }

        public void setStartAddress(String startAddress) {
            this.startAddress = startAddress;
        }

        public String getEndAddress() {
            return endAddress;
        }

        public void setEndAddress(String endAddress) {
            this.endAddress = endAddress;
        }

        public String getVehicleType() {
            return vehicleType;
        }

        public void setVehicleType(String vehicleType) {
            this.vehicleType = vehicleType;
        }
    }
}
