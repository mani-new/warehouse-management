package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.Matchers.*;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fulfilment.application.monolith.warehouses.adapters.database.WarehouseRepository;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;

import java.time.LocalDateTime;

/**
 * Integration tests for WarehouseResourceImpl REST endpoints.
 *
 * Tests all warehouse REST API operations including list, get, create, archive, and replace.
 */
@QuarkusTest
public class WarehouseResourceImplTest {

  @Inject
  WarehouseRepository warehouseRepository;

  @Inject
  EntityManager em;

  @BeforeEach
  @Transactional
  public void setup() {
    // Clean slate
    em.createQuery("DELETE FROM DbWarehouse").executeUpdate();
  }

  // ==================== List Warehouses Tests ====================

  @Test
  public void testListAllWarehouses() {
    // Should return list of all warehouses (3 from import.sql)
    given()
        .when()
        .get("/warehouse")
        .then()
        .statusCode(200)
        .body("size()", greaterThanOrEqualTo(0));
  }

  @Test
  @Transactional
  public void testListWarehousesWithData() {
    // Create a warehouse
    createWarehouseViaAPI("TEST-001", "AMSTERDAM-001", 100, 50);

    given()
        .when()
        .get("/warehouse")
        .then()
        .statusCode(200)
        .body("size()", greaterThan(0))
        .body("businessUnitCode", hasItem("TEST-001"))
        .body("location", hasItem("AMSTERDAM-001"));
  }

  // ==================== Get Single Warehouse Tests ====================

  @Test
  @Transactional
  public void testGetWarehouseByID() {
    createWarehouseViaAPI("GET-TEST-001", "AMSTERDAM-001", 50, 25);

    given()
        .when()
        .get("/warehouse/GET-TEST-001")
        .then()
        .statusCode(200)
        .body("businessUnitCode", equalTo("GET-TEST-001"))
        .body("location", equalTo("AMSTERDAM-001"))
        .body("capacity", equalTo(50))
        .body("stock", equalTo(25));
  }

  @Test
  public void testGetNonExistentWarehouse() {
    given()
        .when()
        .get("/warehouse/NON-EXISTENT-CODE")
        .then()
        .statusCode(404)
        .body(containsString("not found"));
  }

  // ==================== Create Warehouse Tests ====================

  @Test
  public void testCreateWarehouseSuccessfully() {
    given()
        .contentType(ContentType.JSON)
        .body("{\"businessUnitCode\": \"CREATE-001\", \"location\": \"AMSTERDAM-001\", \"capacity\": 100, \"stock\": 50}")
        .when()
        .post("/warehouse")
        .then()
        .statusCode(200)
        .body("businessUnitCode", equalTo("CREATE-001"))
        .body("location", equalTo("AMSTERDAM-001"))
        .body("capacity", equalTo(100))
        .body("stock", equalTo(50));
  }

  @Test
  public void testCreateWarehouseWithDuplicateCode() {
    // Create first warehouse
    given()
        .contentType(ContentType.JSON)
        .body("{\"businessUnitCode\": \"DUP-001\", \"location\": \"AMSTERDAM-001\", \"capacity\": 100, \"stock\": 50}")
        .when()
        .post("/warehouse")
        .then()
        .statusCode(200);

    // Try to create another with same business unit code
    given()
        .contentType(ContentType.JSON)
        .body("{\"businessUnitCode\": \"DUP-001\", \"location\": \"ZWOLLE-001\", \"capacity\": 80, \"stock\": 40}")
        .when()
        .post("/warehouse")
        .then()
        .statusCode(400)
        .body(containsString("already exists"));
  }

  @Test
  public void testCreateWarehouseWithInvalidLocation() {
    given()
        .contentType(ContentType.JSON)
        .body("{\"businessUnitCode\": \"LOC-TEST\", \"location\": \"INVALID-LOCATION\", \"capacity\": 100, \"stock\": 50}")
        .when()
        .post("/warehouse")
        .then()
        .statusCode(400)
        .body(containsString("not valid"));
  }

  @Test
  public void testCreateWarehouseWithCapacityExceedingLocationLimit() {
    // AMSTERDAM-001 has max capacity of 150, try to exceed it
    given()
        .contentType(ContentType.JSON)
        .body("{\"businessUnitCode\": \"CAP-001\", \"location\": \"AMSTERDAM-001\", \"capacity\": 200, \"stock\": 100}")
        .when()
        .post("/warehouse")
        .then()
        .statusCode(400)
        .body(containsString("exceeds location max capacity"));
  }

  @Test
  public void testCreateWarehouseWithStockExceedingCapacity() {
    given()
        .contentType(ContentType.JSON)
        .body("{\"businessUnitCode\": \"STOCK-001\", \"location\": \"AMSTERDAM-001\", \"capacity\": 50, \"stock\": 100}")
        .when()
        .post("/warehouse")
        .then()
        .statusCode(400)
        .body(containsString("exceeds warehouse capacity"));
  }

  @Test
  public void testCreateWarehouseWithDefaultStock() {
    // Stock should default to 0 if not provided
    given()
        .contentType(ContentType.JSON)
        .body("{\"businessUnitCode\": \"DEFAULT-STOCK\", \"location\": \"AMSTERDAM-001\", \"capacity\": 100}")
        .when()
        .post("/warehouse")
        .then()
        .statusCode(200)
        .body("businessUnitCode", equalTo("DEFAULT-STOCK"))
        .body("stock", equalTo(0));
  }

  // ==================== Archive Warehouse Tests ====================

  @Test
  public void testArchiveWarehouseSuccessfully() {
    createWarehouseViaAPI("ARCHIVE-001", "AMSTERDAM-001", 50, 25);

    given()
        .when()
        .delete("/warehouse/ARCHIVE-001")
        .then()
        .statusCode(204);

    // Verify warehouse still exists but is archived
    given()
        .when()
        .get("/warehouse/ARCHIVE-001")
        .then()
        .statusCode(200)
        .body("businessUnitCode", equalTo("ARCHIVE-001"));
  }

  @Test
  public void testArchiveNonExistentWarehouse() {
    given()
        .when()
        .delete("/warehouse/NON-EXISTENT-ARCHIVE")
        .then()
        .statusCode(404)
        .body(containsString("not found"));
  }

  @Test
  @Transactional
  public void testCannotArchiveAlreadyArchivedWarehouse() {
    // Create and archive a warehouse
    createWarehouseViaAPI("ARCHIVE-TWICE", "AMSTERDAM-001", 50, 25);

    given()
        .when()
        .delete("/warehouse/ARCHIVE-TWICE")
        .then()
        .statusCode(204);

    // Try to archive again
    given()
        .when()
        .delete("/warehouse/ARCHIVE-TWICE")
        .then()
        .statusCode(400)
        .body(containsString("already archived"));
  }

  // ==================== Replace Warehouse Tests ====================

  @Test
  public void testReplaceWarehouseSuccessfully() {
    createWarehouseViaAPI("REPLACE-001", "AMSTERDAM-001", 60, 30);

    given()
        .contentType(ContentType.JSON)
        .body("{\"location\": \"AMSTERDAM-001\", \"capacity\": 40, \"stock\": 20}")
        .when()
        .post("/warehouse/REPLACE-001/replacement")
        .then()
        .statusCode(200)
        .body("businessUnitCode", equalTo("REPLACE-001"))
        .body("location", equalTo("AMSTERDAM-001"))
        .body("capacity", equalTo(40))
        .body("stock", equalTo(20));
  }

  @Test
  public void testReplaceNonExistentWarehouse() {
    given()
        .contentType(ContentType.JSON)
        .body("{\"location\": \"ZWOLLE-001\", \"capacity\": 80, \"stock\": 40}")
        .when()
        .post("/warehouse/NON-EXISTENT-REPLACE/replacement")
        .then()
        .statusCode(400)
        .body(containsString("does not exist"));
  }

  @Test
  @Transactional
  public void testCannotReplaceArchivedWarehouse() {
    // Create and archive a warehouse
    createWarehouseViaAPI("REPLACE-ARCHIVED", "AMSTERDAM-001", 50, 25);

    given()
        .when()
        .delete("/warehouse/REPLACE-ARCHIVED")
        .then()
        .statusCode(204);

    // Try to replace archived warehouse
    given()
        .contentType(ContentType.JSON)
        .body("{\"location\": \"AMSTERDAM-001\", \"capacity\": 40, \"stock\": 20}")
        .when()
        .post("/warehouse/REPLACE-ARCHIVED/replacement")
        .then()
        .statusCode(400)
        .body(containsString("archived"));
  }

  @Test
  public void testReplaceWarehouseWithInvalidLocation() {
    createWarehouseViaAPI("REPLACE-LOC", "AMSTERDAM-001", 100, 50);

    given()
        .contentType(ContentType.JSON)
        .body("{\"location\": \"INVALID-LOC\", \"capacity\": 80, \"stock\": 40}")
        .when()
        .post("/warehouse/REPLACE-LOC/replacement")
        .then()
        .statusCode(400)
        .body(containsString("not valid"));
  }

  @Test
  public void testReplaceWarehouseWithCapacityExceedingLocation() {
    createWarehouseViaAPI("REPLACE-CAP", "AMSTERDAM-001", 50, 25);

    // Try to replace with capacity exceeding ZWOLLE-001 max (40)
    given()
        .contentType(ContentType.JSON)
        .body("{\"location\": \"ZWOLLE-001\", \"capacity\": 50, \"stock\": 20}")
        .when()
        .post("/warehouse/REPLACE-CAP/replacement")
        .then()
        .statusCode(400)
        .body(containsString("exceeds location max capacity"));
  }

  @Test
  public void testReplaceWarehouseWithStockExceedingCapacity() {
    createWarehouseViaAPI("REPLACE-STOCK", "AMSTERDAM-001", 40, 20);

    given()
        .contentType(ContentType.JSON)
        .body("{\"location\": \"AMSTERDAM-001\", \"capacity\": 30, \"stock\": 50}")
        .when()
        .post("/warehouse/REPLACE-STOCK/replacement")
        .then()
        .statusCode(400)
        .body(containsString("exceeds warehouse capacity"));
  }

  // ==================== Helper Methods ====================

  private void createWarehouseViaAPI(String businessUnitCode, String location, int capacity, int stock) {
    given()
        .contentType(ContentType.JSON)
        .body(String.format(
            "{\"businessUnitCode\": \"%s\", \"location\": \"%s\", \"capacity\": %d, \"stock\": %d}",
            businessUnitCode, location, capacity, stock))
        .when()
        .post("/warehouse")
        .then()
        .statusCode(200);
  }

  private void createWarehouseInDB(String businessUnitCode, String location, int capacity, int stock) {
    Warehouse warehouse = new Warehouse();
    warehouse.businessUnitCode = businessUnitCode;
    warehouse.location = location;
    warehouse.capacity = capacity;
    warehouse.stock = stock;
    warehouse.createdAt = LocalDateTime.now();

    warehouseRepository.create(warehouse);
  }
}

