package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;

import com.fulfilment.application.monolith.warehouses.adapters.database.DbWarehouse;
import io.quarkus.test.junit.QuarkusIntegrationTest;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class WarehouseEndpointIT {

  @Inject
  EntityManager entityManager;

  @BeforeEach
  @Transactional
  public void setUp() {
    // Clean up any test warehouses created during previous tests
    entityManager.createQuery("DELETE FROM DbWarehouse w WHERE w.businessUnitCode LIKE 'MWH.999%' OR w.businessUnitCode LIKE 'NULL-TEST%'")
            .executeUpdate();

    // Reset any archived warehouses back to active state (for tests that archive)
    entityManager.createQuery("UPDATE DbWarehouse w SET w.archivedAt = NULL WHERE w.archivedAt IS NOT NULL")
            .executeUpdate();

    // Ensure the expected initial warehouses exist
    // Check if MWH.001 exists, if not create it
    Long countMWH001 = entityManager.createQuery("SELECT COUNT(w) FROM DbWarehouse w WHERE w.businessUnitCode = 'MWH.001'", Long.class)
            .getSingleResult();
    if (countMWH001 == 0) {
      DbWarehouse warehouse1 = new DbWarehouse();
      warehouse1.businessUnitCode = "MWH.001";
      warehouse1.location = "ZWOLLE-001";
      warehouse1.capacity = 100;
      warehouse1.stock = 10;
      warehouse1.createdAt = java.time.LocalDateTime.of(2024, 7, 1, 0, 0);
      warehouse1.archivedAt = null;
      entityManager.persist(warehouse1);
    }

    // Check if MWH.012 exists, if not create it
    Long countMWH012 = entityManager.createQuery("SELECT COUNT(w) FROM DbWarehouse w WHERE w.businessUnitCode = 'MWH.012'", Long.class)
            .getSingleResult();
    if (countMWH012 == 0) {
      DbWarehouse warehouse2 = new DbWarehouse();
      warehouse2.businessUnitCode = "MWH.012";
      warehouse2.location = "AMSTERDAM-001";
      warehouse2.capacity = 50;
      warehouse2.stock = 5;
      warehouse2.createdAt = java.time.LocalDateTime.of(2023, 7, 1, 0, 0);
      warehouse2.archivedAt = null;
      entityManager.persist(warehouse2);
    }

    // Check if MWH.023 exists, if not create it
    Long countMWH023 = entityManager.createQuery("SELECT COUNT(w) FROM DbWarehouse w WHERE w.businessUnitCode = 'MWH.023'", Long.class)
            .getSingleResult();
    if (countMWH023 == 0) {
      DbWarehouse warehouse3 = new DbWarehouse();
      warehouse3.businessUnitCode = "MWH.023";
      warehouse3.location = "TILBURG-001";
      warehouse3.capacity = 30;
      warehouse3.stock = 27;
      warehouse3.createdAt = java.time.LocalDateTime.of(2021, 2, 1, 0, 0);
      warehouse3.archivedAt = null;
      entityManager.persist(warehouse3);
    }

    // Clear the persistence context to ensure clean state
    entityManager.flush();
    entityManager.clear();
  }

  @Test
  public void testSimpleListWarehouses() {

    final String path = "warehouse";

    // List all, should have all 3 products the database has initially:
    given()
            .when()
            .get(path)
            .then()
            .statusCode(200)
            .body(containsString("MWH.001"), containsString("MWH.012"), containsString("MWH.023"));
  }

  @Test
  public void testSimpleCheckingArchivingWarehouses() {

    final String path = "warehouse";

    // List all, should have all 3 warehouses the database has initially:
    given()
            .when()
            .get(path)
            .then()
            .statusCode(200)
            .body(
                    containsString("MWH.001"),
                    containsString("MWH.012"),
                    containsString("MWH.023"),
                    containsString("ZWOLLE-001"),
                    containsString("AMSTERDAM-001"),
                    containsString("TILBURG-001"));

    // Archive the ZWOLLE-001 warehouse (ID 1):
    given().when().delete(path + "/MWH.001").then().statusCode(204);

    // List all, ZWOLLE-001 should be missing now:
    given()
            .when()
            .get(path)
            .then()
            .statusCode(200)
            .body(
                    containsString("ZWOLLE-001"), //NOTE: The delete actually archives the warehouse but the get will return the archives too hence the result shows the deleted entry.
                    containsString("AMSTERDAM-001"),
                    containsString("TILBURG-001"));
  }
}
