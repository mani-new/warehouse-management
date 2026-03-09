package com.fulfilment.application.monolith.location;

import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class LocationGateway implements LocationResolver {

  private static final List<Location> locations = new ArrayList<>();

  static {
    locations.add(new Location("ZWOLLE-001", 1, 40));
    locations.add(new Location("ZWOLLE-002", 2, 50));
    locations.add(new Location("AMSTERDAM-001", 5, 100));
    locations.add(new Location("AMSTERDAM-002", 3, 75));
    locations.add(new Location("TILBURG-001", 1, 40));
    locations.add(new Location("HELMOND-001", 1, 45));
    locations.add(new Location("EINDHOVEN-001", 2, 70));
    locations.add(new Location("VETSBY-001", 1, 90));
  }

  @Override
  public Location resolveByIdentifier(String identifier) {
    // Validate input: null and empty string checks
    if (identifier == null || identifier.isBlank()) {
      throw new IllegalArgumentException("Location identifier cannot be null or empty");
    }

    // Trim and normalize the identifier for case-insensitive comparison
    String normalizedIdentifier = identifier.trim();

    // Search through the predefined locations
    Location location = locations.stream()
        .filter(loc -> loc.identifier().equalsIgnoreCase(normalizedIdentifier))
        .findFirst()
        .orElse(null);

    // Log warning if location not found for debugging purposes
    if (location == null) {
      System.err.println("Warning: Location with identifier '" + identifier +
          "' not found. Available locations: " +
          locations.stream().map(Location::identifier).toList());
    }

    return location;
  }
}
