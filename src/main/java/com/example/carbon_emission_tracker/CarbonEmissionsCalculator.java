package com.example.carbon_emission_tracker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class CarbonEmissionsCalculator {

    private static final String TOMTOM_API_KEY = "0TQGxzg4nBU5qP4Du9YkaQAkKJ1OvNnd";
    
    private static final double CO2_EMISSION_PER_KM_CAR = 0.21; // CO2 emissions per km for a car (in kg)
    private static final double CO2_EMISSION_PER_KM_BUS = 0.06; // CO2 emissions per km for a bus (in kg)
    private static final double CO2_EMISSION_PER_KM_TRAIN = 0.02; // CO2 emissions per km for a train (in kg)
    private static final double CO2_EMISSION_PER_KM_ELECTRIC_CAR = 0.05; // CO2 emissions per km for an electric car (in kg)
    private static final double CO2_EMISSION_PER_KM_BICYCLE = 0.00; // CO2 emissions per km for a bicycle (in kg)
    private static final double CO2_EMISSION_PER_KM_MOTORCYCLE = 0.15; // CO2 emissions per km for a motorcycle (in kg)
    private static final double CO2_EMISSION_PER_KM_VAN = 0.25; // CO2 emissions per km for a van (in kg)
    private static final double CO2_EMISSION_PER_KM_HYBRID_VEHICLE = 0.12; // CO2 emissions per km for a hybrid vehicle (in kg)
    private static final double CO2_EMISSION_PER_KM_AUTO_RICKSHAW = 0.10; // CO2 emissions per km for an auto rickshaw (in kg)
    private static final double CO2_EMISSION_PER_KM_CNG_VEHICLE = 0.08; // CO2 emissions per km for a CNG vehicle (in kg)

    public static void main(String[] args) {
    Scanner scanner = new Scanner(System.in);
    List<Trip> trips = new ArrayList<>();

    while (true) {
        System.out.println("Enter trip details:");

        System.out.print("Enter the start address: ");
        String startAddress = scanner.nextLine();

        System.out.print("Enter the end address: ");
        String endAddress = scanner.nextLine();

        System.out.print("Enter the type of vehicle (car, bus, train, electric car, bicycle, motorcycle, van, hybrid vehicle, auto rickshaw, CNG vehicle): ");
        String vehicleType = scanner.nextLine().toUpperCase();

        trips.add(new Trip(startAddress, endAddress, vehicleType));

        System.out.print("Do you want to add another trip? (yes/no): ");
        String response = scanner.nextLine();
        if (!response.equalsIgnoreCase("yes")) {
            break;
        }
    }

    double totalEmissions = 0;

    for (Trip trip : trips) {
        try {
            double[] startCoordinates = getCoordinates(trip.getStartAddress());
            double[] endCoordinates = getCoordinates(trip.getEndAddress());

            if (startCoordinates != null && endCoordinates != null) {
                double distance = getDistance(startCoordinates, endCoordinates);
                if (distance >= 0) {
                    double emissions = calculateEmissions(trip.getVehicleType(), distance);
                    totalEmissions += emissions;
                    System.out.printf("Trip from %s to %s with vehicle type %s has a distance of %.2f km and emissions of %.2f kg CO2.%n",
                            trip.getStartAddress(), trip.getEndAddress(), trip.getVehicleType(), distance, emissions);
                } else {
                    System.out.println("Failed to calculate distance for trip: " + trip);
                }
            } else {
                System.out.println("Failed to get coordinates for trip: " + trip);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    System.out.printf("Total carbon emissions for the day: %.2f kg CO2%n", totalEmissions);

    // Call the personalized suggestions method with both totalEmissions and trips
    givePersonalizedSuggestions(totalEmissions, trips);

    scanner.close();
}

    private static OkHttpClient createClientWithTimeouts() {
        return new OkHttpClient.Builder()
                .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .build();
    }

    public static double[] getCoordinates(String address) throws IOException {
        OkHttpClient client = createClientWithTimeouts();
        String url = "https://api.tomtom.com/search/2/geocode/" + address.replace(" ", "%20") + ".json?key=" + TOMTOM_API_KEY;

        Request request = new Request.Builder().url(url).build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                System.out.println("Request failed with status code: " + response.code());
                return null;
            }
            String responseBody = response.body().string();
            return parseCoordinates(responseBody);
        }
    }

    private static double[] parseCoordinates(String responseBody) {
        JsonObject jsonObject = JsonParser.parseString(responseBody).getAsJsonObject();
        JsonObject firstResult = jsonObject.getAsJsonArray("results").get(0).getAsJsonObject();
        JsonObject position = firstResult.getAsJsonObject("position");

        double latitude = position.get("lat").getAsDouble();
        double longitude = position.get("lon").getAsDouble();

        return new double[]{latitude, longitude};
    }

    public static double getDistance(double[] start, double[] end) throws IOException {
        OkHttpClient client = createClientWithTimeouts();
        String url = "https://api.tomtom.com/routing/1/calculateRoute/" +
                start[0] + "," + start[1] + ":" + end[0] + "," + end[1] + "/json?key=" + TOMTOM_API_KEY;

        Request request = new Request.Builder().url(url).build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                System.out.println("Request failed with status code: " + response.code());
                return -1;
            }

            String responseBody = response.body().string();
            return parseDistance(responseBody);
        }
    }

    private static double parseDistance(String responseBody) {
        JsonObject jsonObject = JsonParser.parseString(responseBody).getAsJsonObject();
        JsonObject routes = jsonObject.getAsJsonArray("routes").get(0).getAsJsonObject();
        JsonObject summary = routes.getAsJsonObject("summary");
        double distanceInMeters = summary.get("lengthInMeters").getAsDouble();
        return distanceInMeters / 1000.0; // Convert to kilometers
    }

    static double calculateEmissions(String vehicleType, double distance) {
        double emissionsPerKm = 0;

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
                System.out.println("Unknown vehicle type: " + vehicleType);
                return 0;
        }

        return emissionsPerKm * distance;
    }

    static void givePersonalizedSuggestions(double totalEmissions, List<Trip> trips) {
        List<String> suggestions = new ArrayList<>();
        List<String> personalizedSuggestions = new ArrayList<>();
    
        // General suggestions
        suggestions.add("Consider carpooling to reduce emissions.");
        suggestions.add("Use public transportation more often.");
        suggestions.add("Switch to a more fuel-efficient vehicle.");
        suggestions.add("Try walking or cycling for shorter trips.");
        suggestions.add("Combine multiple errands into one trip to reduce travel.");
        suggestions.add("Plan your routes to avoid traffic and reduce idling.");
        suggestions.add("Keep your vehicle well-maintained to improve fuel efficiency.");
        suggestions.add("Consider working from home to reduce commuting.");
        suggestions.add("Use electric or hybrid vehicles to lower your carbon footprint.");
        suggestions.add("Choose eco-friendly driving habits, like smooth acceleration and braking.");
    
        // Check user vehicle types and provide tailored suggestions
        boolean usesPublicTransport = trips.stream().anyMatch(trip -> trip.getVehicleType().equals("bus") || trip.getVehicleType().equals("train"));
        boolean usesEcoFriendlyVehicles = trips.stream().anyMatch(trip -> trip.getVehicleType().equals("electric car") || trip.getVehicleType().equals("hybrid vehicle") || trip.getVehicleType().equals("bicycle"));
        boolean usesEfficientVehicles = trips.stream().anyMatch(trip -> trip.getVehicleType().equals("car") || trip.getVehicleType().equals("van") || trip.getVehicleType().equals("motorcycle"));
    
        // Provide specific suggestions based on usage
        if (usesPublicTransport) {
            personalizedSuggestions.add("Great job using public transportation! Consider optimizing your travel by reducing unnecessary trips.");
        }
        if (usesEcoFriendlyVehicles) {
            personalizedSuggestions.add("You're doing well with eco-friendly vehicles. You might explore options like solar panels at home to further reduce your carbon footprint.");
        }
        if (usesEfficientVehicles) {
            personalizedSuggestions.add("You use efficient vehicles. Keep up with regular maintenance to ensure maximum fuel efficiency.");
        }
        if (!usesPublicTransport && !usesEcoFriendlyVehicles && !usesEfficientVehicles) {
            personalizedSuggestions.add("Consider incorporating more eco-friendly transportation options into your routine, like using a bicycle or an electric car.");
        }
    
        // Add general suggestions if none are personalized
        if (personalizedSuggestions.isEmpty()) {
            personalizedSuggestions.addAll(suggestions);
        }
    
        // Display personalized suggestions
        System.out.println("Personalized suggestions to reduce your carbon emissions:");
        for (int i = 0; i < Math.min(3, personalizedSuggestions.size()); i++) {
            System.out.println("- " + personalizedSuggestions.get(i));
        }
    }
    

    static class Trip {
        public final String startAddress;
        public final String endAddress;
        public final String vehicleType;

        public Trip(String startAddress, String endAddress, String vehicleType) {
            this.startAddress = startAddress;
            this.endAddress = endAddress;
            this.vehicleType = vehicleType;
        }

        public String getStartAddress() {
            return startAddress;
        }

        public String getEndAddress() {
            return endAddress;
        }

        public String getVehicleType() {
            return vehicleType;
        }
    }
}
