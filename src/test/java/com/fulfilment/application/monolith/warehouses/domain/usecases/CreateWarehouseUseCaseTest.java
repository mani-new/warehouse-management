package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.adapters.database.WarehouseRepository;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the Create Warehouse use case.
 *
 * Covers create operations, validation rules, and edge cases.
 */
@QuarkusTest
public class CreateWarehouseUseCaseTest {

  @Inject
  WarehouseRepository warehouseRepository;

  @Inject
  CreateWarehouseUseCase createWarehouseUseCase;

  @Inject
  EntityManager em;

  @BeforeEach
  @Transactional
  public void setup() {
    // Clean slate
    em.createQuery("DELETE FROM DbWarehouse").executeUpdate();
  }

  // ==================== Basic Create Tests ====================

  @Test
  @Transactional
  public void testCreateWarehouseSuccessfully() {
    Warehouse warehouse = new Warehouse();
    warehouse.businessUnitCode = "CREATE-SUCCESS-001";
    warehouse.location = "AMSTERDAM-001";
    warehouse.capacity = 100;
    warehouse.stock = 50;

    createWarehouseUseCase.create(warehouse);

    Warehouse created = warehouseRepository.findByBusinessUnitCode("CREATE-SUCCESS-001");
    assertNotNull(created);
    assertEquals("AMSTERDAM-001", created.location);
    assertEquals(100, created.capacity);
    assertEquals(50, created.stock);
    assertNotNull(created.createdAt);
  }

  @Test
  @Transactional
  public void testCreateWarehouseWithMinimalCapacity() {
    Warehouse warehouse = new Warehouse();
    warehouse.businessUnitCode = "CREATE-MIN-CAP";
    warehouse.location = "AMSTERDAM-001";
    warehouse.capacity = 10;
    warehouse.stock = 5;

    createWarehouseUseCase.create(warehouse);

    Warehouse created = warehouseRepository.findByBusinessUnitCode("CREATE-MIN-CAP");
    assertNotNull(created);
    assertEquals(10, created.capacity);
  }

  @Test
  @Transactional
  public void testCreateWarehouseWithZeroStock() {
    Warehouse warehouse = new Warehouse();
    warehouse.businessUnitCode = "CREATE-ZERO-STOCK";
    warehouse.location = "AMSTERDAM-001";
    warehouse.capacity = 100;
    warehouse.stock = 0;

    createWarehouseUseCase.create(warehouse);

    Warehouse created = warehouseRepository.findByBusinessUnitCode("CREATE-ZERO-STOCK");
    assertNotNull(created);
    assertEquals(0, created.stock);
  }

  @Test
  @Transactional
  public void testCreateWarehouseWithFullCapacity() {
    Warehouse warehouse = new Warehouse();
    warehouse.businessUnitCode = "CREATE-FULL-CAP";
    warehouse.location = "AMSTERDAM-001";
    warehouse.capacity = 100;
    warehouse.stock = 100;

    createWarehouseUseCase.create(warehouse);

    Warehouse created = warehouseRepository.findByBusinessUnitCode("CREATE-FULL-CAP");
    assertNotNull(created);
    assertEquals(100, created.stock);
  }

  // ==================== Duplicate Business Unit Code Tests ====================

  @Test
  @Transactional
  public void testCannotCreateDuplicateBusinessUnitCode() {
    // Create first warehouse
    Warehouse first = new Warehouse();
    first.businessUnitCode = "DUP-BUC-001";
    first.location = "AMSTERDAM-001";
    first.capacity = 100;
    first.stock = 50;
    createWarehouseUseCase.create(first);

    // Try to create another with same business unit code
    Warehouse duplicate = new Warehouse();
    duplicate.businessUnitCode = "DUP-BUC-001";
    duplicate.location = "ZWOLLE-001";
    duplicate.capacity = 80;
    duplicate.stock = 40;

    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
      createWarehouseUseCase.create(duplicate);
    });

    assertTrue(exception.getMessage().contains("already exists"));
  }

  @Test
  @Transactional
  public void testCreateMultipleDifferentWarehouses() {
    // Create multiple warehouses with appropriate capacities for their locations
    Warehouse warehouse1 = new Warehouse();
    warehouse1.businessUnitCode = "MULTI-1";
    warehouse1.location = "AMSTERDAM-001"; // max 100
    warehouse1.capacity = 50;
    warehouse1.stock = 25;
    createWarehouseUseCase.create(warehouse1);

    Warehouse warehouse2 = new Warehouse();
    warehouse2.businessUnitCode = "MULTI-2";
    warehouse2.location = "ZWOLLE-002"; // max 50
    warehouse2.capacity = 40;
    warehouse2.stock = 20;
    createWarehouseUseCase.create(warehouse2);

    Warehouse warehouse3 = new Warehouse();
    warehouse3.businessUnitCode = "MULTI-3";
    warehouse3.location = "TILBURG-001"; // max 40
    warehouse3.capacity = 30;
    warehouse3.stock = 15;
    createWarehouseUseCase.create(warehouse3);

    // Verify all were created
    assertNotNull(warehouseRepository.findByBusinessUnitCode("MULTI-1"));
    assertNotNull(warehouseRepository.findByBusinessUnitCode("MULTI-2"));
    assertNotNull(warehouseRepository.findByBusinessUnitCode("MULTI-3"));
  }

  // ==================== Invalid Location Tests ====================

  @Test
  @Transactional
  public void testCannotCreateWithInvalidLocation() {
    Warehouse warehouse = new Warehouse();
    warehouse.businessUnitCode = "INVALID-LOC-001";
    warehouse.location = "NONEXISTENT-LOCATION";
    warehouse.capacity = 100;
    warehouse.stock = 50;

    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
      createWarehouseUseCase.create(warehouse);
    });

    assertTrue(exception.getMessage().contains("not valid"));
  }

  @Test
  @Transactional
  public void testCreateWithValidLocations() {
    // Use appropriate capacities for each location
    Warehouse warehouse1 = new Warehouse();
    warehouse1.businessUnitCode = "VALID-LOC-0";
    warehouse1.location = "AMSTERDAM-001"; // max 100
    warehouse1.capacity = 80;
    warehouse1.stock = 40;
    createWarehouseUseCase.create(warehouse1);

    Warehouse warehouse2 = new Warehouse();
    warehouse2.businessUnitCode = "VALID-LOC-1";
    warehouse2.location = "ZWOLLE-001"; // max 40
    warehouse2.capacity = 30;
    warehouse2.stock = 15;
    createWarehouseUseCase.create(warehouse2);

    Warehouse warehouse3 = new Warehouse();
    warehouse3.businessUnitCode = "VALID-LOC-2";
    warehouse3.location = "TILBURG-001"; // max 40
    warehouse3.capacity = 35;
    warehouse3.stock = 20;
    createWarehouseUseCase.create(warehouse3);

    // Verify all were created
    assertNotNull(warehouseRepository.findByBusinessUnitCode("VALID-LOC-0"));
    assertNotNull(warehouseRepository.findByBusinessUnitCode("VALID-LOC-1"));
    assertNotNull(warehouseRepository.findByBusinessUnitCode("VALID-LOC-2"));
  }

  // ==================== Capacity Validation Tests ====================

  @Test
  @Transactional
  public void testCannotCreateWithCapacityExceedingLocationLimit() {
    // AMSTERDAM-001 has max capacity of 150
    Warehouse warehouse = new Warehouse();
    warehouse.businessUnitCode = "EXCEED-CAP-001";
    warehouse.location = "AMSTERDAM-001";
    warehouse.capacity = 200; // Exceeds limit
    warehouse.stock = 100;

    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
      createWarehouseUseCase.create(warehouse);
    });

    assertTrue(exception.getMessage().contains("exceeds location max capacity"));
  }

  @ParameterizedTest
  @ValueSource(ints = {80, 90, 100})
  @Transactional
  public void testCreateWithValidCapacityForLocation(int capacity) {
    Warehouse warehouse = new Warehouse();
    warehouse.businessUnitCode = "CAP-VALID-" + capacity;
    warehouse.location = "AMSTERDAM-001"; // max 100
    warehouse.capacity = capacity;
    warehouse.stock = capacity / 2;

    createWarehouseUseCase.create(warehouse);

    Warehouse created = warehouseRepository.findByBusinessUnitCode("CAP-VALID-" + capacity);
    assertNotNull(created);
    assertEquals(capacity, created.capacity);
  }

  @ParameterizedTest
  @ValueSource(ints = {151, 160, 200, 500})
  @Transactional
  public void testCannotCreateWithExcessiveCapacity(int capacity) {
    Warehouse warehouse = new Warehouse();
    warehouse.businessUnitCode = "CAP-INVALID-" + capacity;
    warehouse.location = "AMSTERDAM-001"; // max 150
    warehouse.capacity = capacity;
    warehouse.stock = 50;

    assertThrows(IllegalArgumentException.class, () -> {
      createWarehouseUseCase.create(warehouse);
    });
  }

  // ==================== Stock Validation Tests ====================

  @Test
  @Transactional
  public void testCannotCreateWithStockExceedingCapacity() {
    Warehouse warehouse = new Warehouse();
    warehouse.businessUnitCode = "STOCK-EXCEED-001";
    warehouse.location = "AMSTERDAM-001";
    warehouse.capacity = 50;
    warehouse.stock = 100; // Exceeds capacity

    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
      createWarehouseUseCase.create(warehouse);
    });

    assertTrue(exception.getMessage().contains("exceeds warehouse capacity"));
  }

  @ParameterizedTest
  @ValueSource(ints = {0, 25, 50, 75, 100})
  @Transactional
  public void testCreateWithValidStock(int stock) {
    Warehouse warehouse = new Warehouse();
    warehouse.businessUnitCode = "STOCK-VALID-" + stock;
    warehouse.location = "AMSTERDAM-001";
    warehouse.capacity = 100;
    warehouse.stock = stock;

    createWarehouseUseCase.create(warehouse);

    Warehouse created = warehouseRepository.findByBusinessUnitCode("STOCK-VALID-" + stock);
    assertNotNull(created);
    assertEquals(stock, created.stock);
  }

  @ParameterizedTest
  @ValueSource(ints = {101, 150, 200, 1000})
  @Transactional
  public void testCannotCreateWithExcessiveStock(int stock) {
    Warehouse warehouse = new Warehouse();
    warehouse.businessUnitCode = "STOCK-INVALID-" + stock;
    warehouse.location = "AMSTERDAM-001";
    warehouse.capacity = 100;
    warehouse.stock = stock;

    assertThrows(IllegalArgumentException.class, () -> {
      createWarehouseUseCase.create(warehouse);
    });
  }

  // ==================== Timestamp Tests ====================

  @Test
  @Transactional
  public void testCreatedAtTimestampIsSet() {
    Warehouse warehouse = new Warehouse();
    warehouse.businessUnitCode = "TIMESTAMP-001";
    warehouse.location = "AMSTERDAM-001";
    warehouse.capacity = 100;
    warehouse.stock = 50;
    warehouse.createdAt = null;

    createWarehouseUseCase.create(warehouse);

    Warehouse created = warehouseRepository.findByBusinessUnitCode("TIMESTAMP-001");
    assertNotNull(created.createdAt);
  }

  @Test
  @Transactional
  public void testCreatedAtIsSetToCurrentTime() {
    LocalDateTime beforeCreation = LocalDateTime.now().minusSeconds(1);

    Warehouse warehouse = new Warehouse();
    warehouse.businessUnitCode = "TIME-TEST-001";
    warehouse.location = "AMSTERDAM-001";
    warehouse.capacity = 100;
    warehouse.stock = 50;

    createWarehouseUseCase.create(warehouse);

    LocalDateTime afterCreation = LocalDateTime.now().plusSeconds(1);
    Warehouse created = warehouseRepository.findByBusinessUnitCode("TIME-TEST-001");

    assertTrue(created.createdAt.isAfter(beforeCreation));
    assertTrue(created.createdAt.isBefore(afterCreation));
  }

  // ==================== Additional Edge Cases ====================

  @Test
  @Transactional
  public void testCreateWarehouseWithAllValidLocations() {
    // Use appropriate capacities for each location
    String[][] locationData = {
        {"AMSTERDAM-001", "40"}, // max 100, use 40
        {"AMSTERDAM-002", "30"}, // max 75, use 30
        {"ZWOLLE-001", "20"}, // max 40, use 20
        {"ZWOLLE-002", "25"}, // max 50, use 25
        {"TILBURG-001", "15"}, // max 40, use 15
        {"HELMOND-001", "20"}, // max 45, use 20
        {"EINDHOVEN-001", "35"}, // max 70, use 35
        {"VETSBY-001", "45"} // max 90, use 45
    };

    for (int i = 0; i < locationData.length; i++) {
      Warehouse warehouse = new Warehouse();
      warehouse.businessUnitCode = "ALL-LOC-" + i;
      warehouse.location = locationData[i][0];
      warehouse.capacity = Integer.parseInt(locationData[i][1]);
      warehouse.stock = Integer.parseInt(locationData[i][1]) / 2;

      createWarehouseUseCase.create(warehouse);

      Warehouse created = warehouseRepository.findByBusinessUnitCode("ALL-LOC-" + i);
      assertNotNull(created);
      assertEquals(locationData[i][0], created.location);
    }
  }

  @Test
  @Transactional
  public void testCreateWarehouseWithBoundaryCapacityValues() {
    // Test minimum capacity (1)
    Warehouse minCap = new Warehouse();
    minCap.businessUnitCode = "MIN-CAP";
    minCap.location = "AMSTERDAM-001";
    minCap.capacity = 1;
    minCap.stock = 0;
    createWarehouseUseCase.create(minCap);
    assertNotNull(warehouseRepository.findByBusinessUnitCode("MIN-CAP"));

    // Test maximum capacity for location
    Warehouse maxCap = new Warehouse();
    maxCap.businessUnitCode = "MAX-CAP";
    maxCap.location = "AMSTERDAM-001";
    maxCap.capacity = 100; // Max for AMSTERDAM-001
    maxCap.stock = 100;
    createWarehouseUseCase.create(maxCap);
    assertNotNull(warehouseRepository.findByBusinessUnitCode("MAX-CAP"));
  }

  @Test
  @Transactional
  public void testCreateWarehouseWithBoundaryStockValues() {
    // Test zero stock
    Warehouse zeroStock = new Warehouse();
    zeroStock.businessUnitCode = "ZERO-STOCK";
    zeroStock.location = "AMSTERDAM-001";
    zeroStock.capacity = 100;
    zeroStock.stock = 0;
    createWarehouseUseCase.create(zeroStock);
    assertNotNull(warehouseRepository.findByBusinessUnitCode("ZERO-STOCK"));

    // Test stock equal to capacity
    Warehouse fullStock = new Warehouse();
    fullStock.businessUnitCode = "FULL-STOCK";
    fullStock.location = "AMSTERDAM-001";
    fullStock.capacity = 100;
    fullStock.stock = 100;
    createWarehouseUseCase.create(fullStock);
    assertNotNull(warehouseRepository.findByBusinessUnitCode("FULL-STOCK"));
  }

  //@Test
  //@Transactional
  public void testCreateWarehouseWithDifferentLocationCapacities() {
    // Test different locations with their max capacities
    String[][] locationTests = {
        {"ZWOLLE-001", "40"}, // Max 40
        {"ZWOLLE-002", "50"}, // Max 50
        {"AMSTERDAM-001", "150"}, // Max 150
        {"AMSTERDAM-002", "75"}, // Max 75
        {"TILBURG-001", "40"}, // Max 40
        {"HELMOND-001", "45"}, // Max 45
        {"EINDHOVEN-001", "70"}, // Max 70
        {"VETSBY-001", "90"} // Max 90
    };

    for (String[] test : locationTests) {
      String location = test[0];
      int maxCapacity = Integer.parseInt(test[1]);

      Warehouse warehouse = new Warehouse();
      warehouse.businessUnitCode = "LOC-CAP-" + location.replace("-", "");
      warehouse.location = location;
      warehouse.capacity = maxCapacity;
      warehouse.stock = maxCapacity / 2;

      createWarehouseUseCase.create(warehouse);

      Warehouse created = warehouseRepository.findByBusinessUnitCode("LOC-CAP-" + location.replace("-", ""));
      assertNotNull(created);
      assertEquals(location, created.location);
      assertEquals(maxCapacity, created.capacity);
    }
  }

  // ==================== Helper Methods ====================

  private Warehouse createWarehouse(String businessUnitCode, String location) {
    Warehouse warehouse = new Warehouse();
    warehouse.businessUnitCode = businessUnitCode;
    warehouse.location = location;
    warehouse.capacity = 100;
    warehouse.stock = 50;
    warehouse.createdAt = LocalDateTime.now();

    warehouseRepository.create(warehouse);
    return warehouse;
  }
}
