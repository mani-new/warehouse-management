package com.fulfilment.application.monolith.stores;

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

/**
 * Integration tests for Store REST endpoints.
 *
 * Tests store CRUD operations, validation, and event synchronization.
 */
@QuarkusTest
public class StoreResourceTest {

  @Inject
  EntityManager em;

  @BeforeEach
  @Transactional
  public void setup() {
    // Only clean up test data, preserve initial stores
    em.createQuery("DELETE FROM Store s WHERE s.name LIKE 'TEST-%'").executeUpdate();
  }

  // ==================== List Stores Tests ====================

//  @Test
//  public void testListAllStores() {
//    given()
//        .when()
//        .get("/store")
//        .then()
//        .statusCode(200)
//        .body("size()", greaterThanOrEqualTo(1));
//  }

  @Test
  public void testListStoresReturnsValidData() {
    given()
        .when()
        .get("/store")
        .then()
        .statusCode(200)
        .body("name", notNullValue());
  }

  // ==================== Get Store Tests ====================

  @Test
  public void testGetStoreByID() {
    // Create a test store first
    Long storeId = given()
        .contentType(ContentType.JSON)
        .body("{\"name\": \"TEST-GET-STORE\", \"quantityProductsInStock\": 15}")
        .when()
        .post("/store")
        .then()
        .statusCode(201)
        .extract()
        .jsonPath()
        .getLong("id");

    given()
        .when()
        .get("/store/" + storeId)
        .then()
        .statusCode(200)
        .body("name", equalTo("TEST-GET-STORE"))
        .body("quantityProductsInStock", equalTo(15));
  }

  @Test
  public void testGetNonExistentStore() {
    given()
        .when()
        .get("/store/99999")
        .then()
        .statusCode(404);
  }

  // ==================== Create Store Tests ====================

  @Test
  public void testCreateStoreSuccessfully() {
    given()
        .contentType(ContentType.JSON)
        .body("{\"name\": \"TEST-NEW-STORE\", \"quantityProductsInStock\": 15}")
        .when()
        .post("/store")
        .then()
        .statusCode(201)
        .body("name", equalTo("TEST-NEW-STORE"))
        .body("quantityProductsInStock", equalTo(15));
  }

  @Test
  public void testCreateStoreWithMinimalQuantity() {
    given()
        .contentType(ContentType.JSON)
        .body("{\"name\": \"TEST-MIN-QTY\", \"quantityProductsInStock\": 0}")
        .when()
        .post("/store")
        .then()
        .statusCode(201)
        .body("quantityProductsInStock", equalTo(0));
  }

  @Test
  public void testCreateStoreWithLargeQuantity() {
    given()
        .contentType(ContentType.JSON)
        .body("{\"name\": \"TEST-LARGE-QTY\", \"quantityProductsInStock\": 10000}")
        .when()
        .post("/store")
        .then()
        .statusCode(201)
        .body("quantityProductsInStock", equalTo(10000));
  }

  @Test
  public void testCreateMultipleStoresSequentially() {
    // Create first store
    given()
        .contentType(ContentType.JSON)
        .body("{\"name\": \"TEST-SEQ-1\", \"quantityProductsInStock\": 5}")
        .when()
        .post("/store")
        .then()
        .statusCode(201);

    // Create second store
    given()
        .contentType(ContentType.JSON)
        .body("{\"name\": \"TEST-SEQ-2\", \"quantityProductsInStock\": 10}")
        .when()
        .post("/store")
        .then()
        .statusCode(201);

    // Verify both exist
    given()
        .when()
        .get("/store")
        .then()
        .statusCode(200)
        .body("name", hasItems("TEST-SEQ-1", "TEST-SEQ-2"));
  }

  // ==================== Update Store Tests ====================

  @Test
  public void testUpdateStoreSuccessfully() {
    // Create a store
    Long storeId = given()
        .contentType(ContentType.JSON)
        .body("{\"name\": \"TEST-UPDATE-1\", \"quantityProductsInStock\": 10}")
        .when()
        .post("/store")
        .then()
        .statusCode(201)
        .extract()
        .jsonPath()
        .getLong("id");

    // Update it
    given()
        .contentType(ContentType.JSON)
        .body("{\"name\": \"TEST-UPDATE-1\", \"quantityProductsInStock\": 25}")
        .when()
        .put("/store/" + storeId)
        .then()
        .statusCode(200)
        .body("quantityProductsInStock", equalTo(25));
  }

  @Test
  public void testUpdateStoreQuantity() {
    // Create a store
    Long storeId = given()
        .contentType(ContentType.JSON)
        .body("{\"name\": \"TEST-UPDATE-2\", \"quantityProductsInStock\": 5}")
        .when()
        .post("/store")
        .then()
        .statusCode(201)
        .extract()
        .jsonPath()
        .getLong("id");

    given()
        .contentType(ContentType.JSON)
        .body("{\"name\": \"TEST-UPDATE-2\", \"quantityProductsInStock\": 50}")
        .when()
        .put("/store/" + storeId)
        .then()
        .statusCode(200)
        .body("quantityProductsInStock", equalTo(50));
  }

  @Test
  public void testUpdateStoreToZeroQuantity() {
    // Create a store
    Long storeId = given()
        .contentType(ContentType.JSON)
        .body("{\"name\": \"TEST-UPDATE-3\", \"quantityProductsInStock\": 10}")
        .when()
        .post("/store")
        .then()
        .statusCode(201)
        .extract()
        .jsonPath()
        .getLong("id");

    given()
        .contentType(ContentType.JSON)
        .body("{\"name\": \"TEST-UPDATE-3\", \"quantityProductsInStock\": 0}")
        .when()
        .put("/store/" + storeId)
        .then()
        .statusCode(200)
        .body("quantityProductsInStock", equalTo(0));
  }

  @Test
  public void testUpdateNonExistentStore() {
    given()
        .contentType(ContentType.JSON)
        .body("{\"name\": \"TEST-NONEXISTENT\", \"quantityProductsInStock\": 5}")
        .when()
        .put("/store/99999")
        .then()
        .statusCode(404);
  }

  // ==================== Delete Store Tests ====================

  @Test
  public void testDeleteStoreSuccessfully() {
    // Create a store to delete
    Long storeId = given()
        .contentType(ContentType.JSON)
        .body("{\"name\": \"TEST-DELETE\", \"quantityProductsInStock\": 5}")
        .when()
        .post("/store")
        .then()
        .statusCode(201)
        .extract()
        .jsonPath()
        .getLong("id");

    // Delete it
    given()
        .when()
        .delete("/store/" + storeId)
        .then()
        .statusCode(204);

    // Verify deletion
    given()
        .when()
        .get("/store/" + storeId)
        .then()
        .statusCode(404);
  }

  @Test
  public void testDeleteNonExistentStore() {
    given()
        .when()
        .delete("/store/99999")
        .then()
        .statusCode(404);
  }

  // ==================== Data Consistency Tests ====================

  @Test
  public void testStoreNamePreserved() {
    given()
        .contentType(ContentType.JSON)
        .body("{\"name\": \"TEST-PRESERVE-NAME\", \"quantityProductsInStock\": 7}")
        .when()
        .post("/store")
        .then()
        .statusCode(201)
        .body("name", equalTo("TEST-PRESERVE-NAME"));
  }

//  @Test
//  public void testInitialStoresExist() {
//    // Verify that stores exist
//    given()
//        .when()
//        .get("/store")
//        .then()
//        .statusCode(200)
//        .body("size()", greaterThanOrEqualTo(1));
//  }
}

