package com.tev.riderapp.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tev.riderapp.model.Location;

@Service
public class GoogleMapsService {
    
    @Value("${google.maps.api.key:YOUR_API_KEY}")
    private String apiKey;
    
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    public Location geocodePostcode(String postcode) {
        try {
            System.out.println("Geocoding postcode: " + postcode);
            System.out.println("Using API key: " + (apiKey != null ? apiKey.substring(0, 10) + "..." : "null"));
            
            String url = String.format(
                "https://maps.googleapis.com/maps/api/geocode/json?address=%s&key=%s",
                postcode, apiKey
            );
            
            String response = restTemplate.getForObject(url, String.class);
            System.out.println("Google Maps API Response: " + response);
            
            JsonNode root = objectMapper.readTree(response);
            String status = root.get("status").asText();
            
            System.out.println("API Status: " + status);
            
            if (status.equals("OK")) {
                JsonNode result = root.get("results").get(0);
                JsonNode geometry = result.get("geometry").get("location");
                
                double lat = geometry.get("lat").asDouble();
                double lng = geometry.get("lng").asDouble();
                String address = result.get("formatted_address").asText();
                
                Location location = new Location();
                location.setLatitude(lat);
                location.setLongitude(lng);
                location.setAddress(address);
                location.setPostcode(postcode);
                
                System.out.println("Successfully geocoded: " + address);
                return location;
            } else {
                System.err.println("Geocoding failed with status: " + status);
                if (root.has("error_message")) {
                    System.err.println("Error message: " + root.get("error_message").asText());
                }
            }
        } catch (Exception e) {
            System.err.println("Exception during geocoding: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }
    
    public double calculateDistance(Location pickup, Location dropoff) {
        try {
            String url = String.format(
                "https://maps.googleapis.com/maps/api/distancematrix/json?origins=%f,%f&destinations=%f,%f&units=metric&key=%s",
                pickup.getLatitude(), pickup.getLongitude(),
                dropoff.getLatitude(), dropoff.getLongitude(),
                apiKey
            );
            
            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(response);
            
            if (root.get("status").asText().equals("OK")) {
                JsonNode element = root.get("rows").get(0).get("elements").get(0);
                if (element.get("status").asText().equals("OK")) {
                    int distanceInMeters = element.get("distance").get("value").asInt();
                    return distanceInMeters / 1000.0;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0.0;
    }
}