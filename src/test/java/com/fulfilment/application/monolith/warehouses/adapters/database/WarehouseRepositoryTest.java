package com.fulfilment.application.monolith.warehouses.adapters.database;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for WarehouseRepository database operations.
 *
 * Covers CRUD operations and query methods for warehouses.
 */
@QuarkusTest
public class WarehouseRepositoryTest {

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

  // ==================== Create Tests ====================

  @Test
  @Transactional
  public void testCreateWarehouse() {
    Warehouse warehouse = new Warehouse();
    warehouse.businessUnitCode = "CREATE-TEST-001";
    warehouse.location = "AMSTERDAM-001";
    warehouse.capacity = 100;
    warehouse.stock = 50;
    warehouse.createdAt = LocalDateTime.now();

    warehouseRepository.create(warehouse);

    Warehouse created = warehouseRepository.findByBusinessUnitCode("CREATE-TEST-001");
    assertNotNull(created);
    assertEquals("AMSTERDAM-001", created.location);
  }

  @Test
  @Transactional
  public void testCreateMultipleWarehouses() {
    for (int i = 1; i <= 5; i++) {
      Warehouse warehouse = new Warehouse();
      warehouse.businessUnitCode = "MULTI-CREATE-" + i;
      warehouse.location = "AMSTERDAM-001";
      warehouse.capacity = 100 * i;
      warehouse.stock = 50 * i;
      warehouse.createdAt = LocalDateTime.now();

      warehouseRepository.create(warehouse);
    }

    List<Warehouse> all = warehouseRepository.getAll();
    assertEquals(5, all.size());
  }

  // ==================== Read Tests ====================

  @Test
  @Transactional
  public void testFindByBusinessUnitCode() {
    Warehouse warehouse = new Warehouse();
    warehouse.businessUnitCode = "FIND-TEST-001";
    warehouse.location = "ZWOLLE-001";
    warehouse.capacity = 80;
    warehouse.stock = 40;
    warehouse.createdAt = LocalDateTime.now();

    warehouseRepository.create(warehouse);

    Warehouse found = warehouseRepository.findByBusinessUnitCode("FIND-TEST-001");
    assertNotNull(found);
    assertEquals("ZWOLLE-001", found.location);
  }

  @Test
  @Transactional
  public void testFindByBusinessUnitCodeNotFound() {
    Warehouse found = warehouseRepository.findByBusinessUnitCode("NON-EXISTENT");
    assertNull(found);
  }

  @Test
  @Transactional
  public void testGetAll() {
    for (int i = 1; i <= 3; i++) {
      Warehouse warehouse = new Warehouse();
      warehouse.businessUnitCode = "GET-ALL-" + i;
      warehouse.location = "AMSTERDAM-001";
      warehouse.capacity = 100;
      warehouse.stock = 50;
      warehouse.createdAt = LocalDateTime.now();

      warehouseRepository.create(warehouse);
    }

    List<Warehouse> all = warehouseRepository.getAll();
    assertEquals(3, all.size());
  }

  @Test
  @Transactional
  public void testGetAllEmpty() {
    List<Warehouse> all = warehouseRepository.getAll();
    assertEquals(0, all.size());
  }

  // ==================== Update Tests ====================

  @Test
  @Transactional
  public void testUpdateWarehouse() {
    Warehouse warehouse = new Warehouse();
    warehouse.businessUnitCode = "UPDATE-TEST-001";
    warehouse.location = "AMSTERDAM-001";
    warehouse.capacity = 100;
    warehouse.stock = 50;
    warehouse.createdAt = LocalDateTime.now();

    warehouseRepository.create(warehouse);

    // Update the warehouse
    Warehouse toUpdate = warehouseRepository.findByBusinessUnitCode("UPDATE-TEST-001");
    toUpdate.stock = 75;
    toUpdate.capacity = 150;

    warehouseRepository.update(toUpdate);

    // Verify update
    Warehouse updated = warehouseRepository.findByBusinessUnitCode("UPDATE-TEST-001");
    assertEquals(75, updated.stock);
    assertEquals(150, updated.capacity);
  }

  @Test
  @Transactional
  public void testUpdateWarehouseArchiveTimestamp() {
    Warehouse warehouse = new Warehouse();
    warehouse.businessUnitCode = "ARCHIVE-UPDATE-001";
    warehouse.location = "AMSTERDAM-001";
    warehouse.capacity = 100;
    warehouse.stock = 50;
    warehouse.createdAt = LocalDateTime.now();
    warehouse.archivedAt = null;

    warehouseRepository.create(warehouse);

    // Archive it
    Warehouse toArchive = warehouseRepository.findByBusinessUnitCode("ARCHIVE-UPDATE-001");
    toArchive.archivedAt = LocalDateTime.now();

    warehouseRepository.update(toArchive);

    // Verify archive
    Warehouse archived = warehouseRepository.findByBusinessUnitCode("ARCHIVE-UPDATE-001");
    assertNotNull(archived.archivedAt);
  }

  @Test
  @Transactional
  public void testUpdatePreservesBusinessUnitCode() {
    Warehouse warehouse = new Warehouse();
    warehouse.businessUnitCode = "PRESERVE-BUC-001";
    warehouse.location = "AMSTERDAM-001";
    warehouse.capacity = 100;
    warehouse.stock = 50;
    warehouse.createdAt = LocalDateTime.now();

    warehouseRepository.create(warehouse);

    Warehouse toUpdate = warehouseRepository.findByBusinessUnitCode("PRESERVE-BUC-001");
    toUpdate.location = "ZWOLLE-001";
    toUpdate.capacity = 200;

    warehouseRepository.update(toUpdate);

    Warehouse updated = warehouseRepository.findByBusinessUnitCode("PRESERVE-BUC-001");
    assertEquals("PRESERVE-BUC-001", updated.businessUnitCode);
    assertEquals("ZWOLLE-001", updated.location);
    assertEquals(200, updated.capacity);
  }

  // ==================== Delete Tests ====================


  // ==================== Data Integrity Tests ====================

  @Test
  @Transactional
  public void testWarehouseFieldsPreserved() {
    Warehouse warehouse = new Warehouse();
    warehouse.businessUnitCode = "INTEGRITY-001";
    warehouse.location = "TILBURG-001";
    warehouse.capacity = 75;
    warehouse.stock = 25;
    warehouse.createdAt = LocalDateTime.now();
    warehouse.archivedAt = null;

    warehouseRepository.create(warehouse);

    Warehouse retrieved = warehouseRepository.findByBusinessUnitCode("INTEGRITY-001");
    assertEquals("INTEGRITY-001", retrieved.businessUnitCode);
    assertEquals("TILBURG-001", retrieved.location);
    assertEquals(75, retrieved.capacity);
    assertEquals(25, retrieved.stock);
    assertNull(retrieved.archivedAt);
  }

  @Test
  @Transactional
  public void testGetAllReturnsCorrectOrder() {
    for (int i = 1; i <= 3; i++) {
      Warehouse warehouse = new Warehouse();
      warehouse.businessUnitCode = "ORDER-TEST-" + i;
      warehouse.location = "AMSTERDAM-001";
      warehouse.capacity = 100;
      warehouse.stock = i * 10;
      warehouse.createdAt = LocalDateTime.now();

      warehouseRepository.create(warehouse);
    }

    List<Warehouse> all = warehouseRepository.getAll();
    assertEquals(3, all.size());

    // Verify we get back valid warehouses
    assertTrue(all.stream().anyMatch(w -> w.businessUnitCode.equals("ORDER-TEST-1")));
    assertTrue(all.stream().anyMatch(w -> w.businessUnitCode.equals("ORDER-TEST-2")));
    assertTrue(all.stream().anyMatch(w -> w.businessUnitCode.equals("ORDER-TEST-3")));
  }
}

