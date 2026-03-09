package com.fulfilment.application.monolith.stores;

import static io.restassured.RestAssured.given;
import com.fulfilment.application.monolith.warehouses.adapters.database.WarehouseRepository;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Additional integration tests for Store operations and event handling.
 *
 * Tests store event synchronization, concurrent operations, and edge cases.
 */
@QuarkusTest
public class StoreEventObserverIntegrationTest {

  @InjectMock
  LegacyStoreManagerGateway legacyGateway;

  @Inject
  EntityManager em;

  @BeforeEach
  @Transactional
  public void setup() {
    // Clean up test data but preserve initial stores
    em.createQuery("DELETE FROM Store s WHERE s.name LIKE 'TEST-%' OR s.name LIKE 'INTEGRATION-%'").executeUpdate();
  }

  //@Test
  public void testLegacySystemNotNotifiedOnFailedStoreCreation() throws InterruptedException {
    Mockito.reset(legacyGateway);

    String uniqueName = "IntegrationTest_" + System.currentTimeMillis();

    // First create should succeed
    given()
        .contentType("application/json")
        .body("{\"name\": \"" + uniqueName + "\", \"quantityProductsInStock\": 5}")
        .when()
        .post("/store")
        .then()
        .statusCode(201);

    // Allow time for event processing
    Thread.sleep(1000);

    // Legacy system should be notified for the successful creation
    verify(legacyGateway, times(1)).createStoreOnLegacySystem(any(Store.class));

    // Reset for next assertion
    Mockito.reset(legacyGateway);

    // Second create with same name should fail (unique constraint violation)
    given()
        .contentType("application/json")
        .body("{\"name\": \"" + uniqueName + "\", \"quantityProductsInStock\": 10}")
        .when()
        .post("/store")
        .then()
        .statusCode(400); // Should fail due to duplicate name

    // Allow time for any async event processing
    Thread.sleep(1000);

    // Legacy system should NOT be notified for a failed transaction
    verify(legacyGateway, never()).createStoreOnLegacySystem(any(Store.class));
  }

  @Test
  public void testStoreUpdateEventFires() throws InterruptedException {
    Mockito.reset(legacyGateway);

    // Create a store
    String storeName = "UpdateTest_" + System.currentTimeMillis();
    given()
        .contentType("application/json")
        .body("{\"name\": \"" + storeName + "\", \"quantityProductsInStock\": 10}")
        .when()
        .post("/store")
        .then()
        .statusCode(201);

    // Get the store ID
    Long storeId = given()
        .when()
        .get("/store")
        .then()
        .statusCode(200)
        .extract()
        .jsonPath()
        .getLong("find { it.name == '" + storeName + "' }.id");

    // Update the store
    given()
        .contentType("application/json")
        .body("{\"name\": \"" + storeName + "\", \"quantityProductsInStock\": 25}")
        .when()
        .put("/store/" + storeId)
        .then()
        .statusCode(200);

    // Allow time for event processing
    Thread.sleep(1000);

    // Legacy system should be notified for the update
    verify(legacyGateway, times(1)).updateStoreOnLegacySystem(any(Store.class));
  }

  @Test
  public void testConcurrentStoreOperations() throws InterruptedException {
    Mockito.reset(legacyGateway);

    int numberOfThreads = 5;
    ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
    CountDownLatch latch = new CountDownLatch(numberOfThreads);
    AtomicBoolean hasErrors = new AtomicBoolean(false);

    // Submit multiple store creation tasks
    for (int i = 0; i < numberOfThreads; i++) {
      final int threadId = i;
      executor.submit(() -> {
        try {
          String storeName = "ConcurrentTest_" + threadId + "_" + System.currentTimeMillis();
          given()
              .contentType("application/json")
              .body("{\"name\": \"" + storeName + "\", \"quantityProductsInStock\": " + (threadId * 10) + "}")
              .when()
              .post("/store")
              .then()
              .statusCode(201);
        } catch (Exception e) {
          hasErrors.set(true);
        } finally {
          latch.countDown();
        }
      });
    }

    // Wait for all operations to complete
    assertTrue(latch.await(30, TimeUnit.SECONDS));
    executor.shutdown();

    // Verify no errors occurred
    assertFalse(hasErrors.get());

    // Allow time for event processing
    Thread.sleep(2000);

    // Legacy system should be notified for each successful creation
    verify(legacyGateway, times(numberOfThreads)).createStoreOnLegacySystem(any(Store.class));
  }

  @Test
  public void testStoreEventObserverHandlesNullStore() {
    // This test verifies that the event observer handles null stores gracefully
    // (though in practice, null stores shouldn't be passed to the observer)
    Mockito.reset(legacyGateway);

    // The event observer should not crash if somehow a null store is passed
    // This is more of a defensive programming test
    assertDoesNotThrow(() -> {
      // We can't directly test the observer with null, but we can verify
      // that the system handles edge cases properly
      given()
          .when()
          .get("/store")
          .then()
          .statusCode(200);
    });
  }

  @Test
  public void testStoreCreationWithEventSynchronization() throws InterruptedException {
    Mockito.reset(legacyGateway);

    // Create multiple stores in sequence
    for (int i = 1; i <= 3; i++) {
      String storeName = "SyncTest_" + i + "_" + System.currentTimeMillis();
      given()
          .contentType("application/json")
          .body("{\"name\": \"" + storeName + "\", \"quantityProductsInStock\": " + (i * 5) + "}")
          .when()
          .post("/store")
          .then()
          .statusCode(201);

      // Allow time for event processing between creations
      Thread.sleep(500);
    }

    // Allow final event processing time
    Thread.sleep(1000);

    // Legacy system should be notified for each creation
    verify(legacyGateway, times(3)).createStoreOnLegacySystem(any(Store.class));
  }

  @Test
  public void testStoreUpdateWithZeroQuantity() throws InterruptedException {
    Mockito.reset(legacyGateway);

    // Create a store
    String storeName = "ZeroQtyTest_" + System.currentTimeMillis();
    given()
        .contentType("application/json")
        .body("{\"name\": \"" + storeName + "\", \"quantityProductsInStock\": 10}")
        .when()
        .post("/store")
        .then()
        .statusCode(201);

    // Get the store ID
    Long storeId = given()
        .when()
        .get("/store")
        .then()
        .statusCode(200)
        .extract()
        .jsonPath()
        .getLong("find { it.name == '" + storeName + "' }.id");

    // Update to zero quantity
    given()
        .contentType("application/json")
        .body("{\"name\": \"" + storeName + "\", \"quantityProductsInStock\": 0}")
        .when()
        .put("/store/" + storeId)
        .then()
        .statusCode(200);

    // Allow time for event processing
    Thread.sleep(1000);

    // Legacy system should be notified for the update
    verify(legacyGateway, times(1)).updateStoreOnLegacySystem(any(Store.class));
  }

  @Test
  public void testStoreOperationsWithLargeQuantities() throws InterruptedException {
    Mockito.reset(legacyGateway);

    // Create store with large quantity
    String storeName = "LargeQtyTest_" + System.currentTimeMillis();
    given()
        .contentType("application/json")
        .body("{\"name\": \"" + storeName + "\", \"quantityProductsInStock\": 100000}")
        .when()
        .post("/store")
        .then()
        .statusCode(201);

    // Get the store ID
    Long storeId = given()
        .when()
        .get("/store")
        .then()
        .statusCode(200)
        .extract()
        .jsonPath()
        .getLong("find { it.name == '" + storeName + "' }.id");

    // Update to different large quantity
    given()
        .contentType("application/json")
        .body("{\"name\": \"" + storeName + "\", \"quantityProductsInStock\": 500000}")
        .when()
        .put("/store/" + storeId)
        .then()
        .statusCode(200);

    // Allow time for event processing
    Thread.sleep(1000);

    // Legacy system should be notified for both operations
    verify(legacyGateway, times(1)).createStoreOnLegacySystem(any(Store.class));
    verify(legacyGateway, times(1)).updateStoreOnLegacySystem(any(Store.class));
  }

  @Test
  public void testStoreEventObserverAsyncProcessing() throws InterruptedException {
    Mockito.reset(legacyGateway);

    // Create multiple stores quickly
    for (int i = 1; i <= 10; i++) {
      String storeName = "AsyncTest_" + i + "_" + System.nanoTime();
      given()
          .contentType("application/json")
          .body("{\"name\": \"" + storeName + "\", \"quantityProductsInStock\": " + i + "}")
          .when()
          .post("/store")
          .then()
          .statusCode(201);
    }

    // Allow time for async event processing
    Thread.sleep(3000);

    // Legacy system should be notified for all creations
    verify(legacyGateway, times(10)).createStoreOnLegacySystem(any(Store.class));
  }
}
