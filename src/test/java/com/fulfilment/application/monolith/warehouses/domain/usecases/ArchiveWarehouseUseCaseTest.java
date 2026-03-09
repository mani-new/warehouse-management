package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.adapters.database.DbWarehouse;
import com.fulfilment.application.monolith.warehouses.adapters.database.WarehouseRepository;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the Archive Warehouse use case.
 *
 * Covers basic archive operations and concurrent modification scenarios.
 */
@QuarkusTest
public class ArchiveWarehouseUseCaseTest {

  @Inject
  WarehouseRepository warehouseRepository;

  @Inject
  ArchiveWarehouseUseCase archiveWarehouseUseCase;

  @Inject
  EntityManager em;

  @BeforeEach
  @Transactional
  public void setup() {
    // Clean slate
    em.createQuery("DELETE FROM DbWarehouse").executeUpdate();
  }

  /**
   * Basic archive functionality
   */
  @Test
  @Transactional
  public void testArchiveWarehouseSuccessfully() {
    // Create a warehouse
    Warehouse warehouse = createWarehouse("ARCHIVE-TEST-001", "AMSTERDAM-001");

    // Archive it
    archiveWarehouseUseCase.archive(warehouse);

    // Verify it was archived
    Warehouse archived = warehouseRepository.findByBusinessUnitCode("ARCHIVE-TEST-001");
    assertNotNull(archived);
    assertNotNull(archived.archivedAt);
  }

  /**
   * Test archive with zero stock warehouse
   */
  @Test
  @Transactional
  public void testArchiveWarehouseWithZeroStock() {
    // Create warehouse with zero stock
    Warehouse warehouse = createWarehouse("ARCHIVE-ZERO-STOCK", "AMSTERDAM-001");
    warehouse.stock = 0;
    warehouseRepository.update(warehouse);

    // Archive it
    archiveWarehouseUseCase.archive(warehouse);

    // Verify archived
    Warehouse archived = warehouseRepository.findByBusinessUnitCode("ARCHIVE-ZERO-STOCK");
    assertNotNull(archived.archivedAt);
    assertEquals(0, archived.stock);
  }

  /**
   * Test archive with full capacity warehouse
   */
  @Test
  @Transactional
  public void testArchiveWarehouseWithFullCapacity() {
    // Create warehouse with full capacity
    Warehouse warehouse = createWarehouse("ARCHIVE-FULL-CAP", "AMSTERDAM-001");
    warehouse.stock = warehouse.capacity;
    warehouseRepository.update(warehouse);

    // Archive it
    archiveWarehouseUseCase.archive(warehouse);

    // Verify archived
    Warehouse archived = warehouseRepository.findByBusinessUnitCode("ARCHIVE-FULL-CAP");
    assertNotNull(archived.archivedAt);
    assertEquals(archived.capacity, archived.stock);
  }

  /**
   * Test archive sets timestamp correctly
   */
  @Test
  @Transactional
  public void testArchiveSetsArchivedAtTimestamp() {
    LocalDateTime beforeArchive = LocalDateTime.now().minusSeconds(1);

    // Create warehouse
    Warehouse warehouse = createWarehouse("ARCHIVE-TIMESTAMP", "AMSTERDAM-001");

    // Archive it
    archiveWarehouseUseCase.archive(warehouse);

    LocalDateTime afterArchive = LocalDateTime.now().plusSeconds(1);

    // Verify timestamp
    Warehouse archived = warehouseRepository.findByBusinessUnitCode("ARCHIVE-TIMESTAMP");
    assertNotNull(archived.archivedAt);
    assertTrue(archived.archivedAt.isAfter(beforeArchive));
    assertTrue(archived.archivedAt.isBefore(afterArchive));
  }

  /**
   * Test archive preserves all warehouse data
   */
  @Test
  @Transactional
  public void testArchivePreservesAllWarehouseData() {
    // Create warehouse with specific data
    Warehouse warehouse = createWarehouse("PRESERVE-DATA", "ZWOLLE-001");
    warehouse.capacity = 40;
    warehouse.stock = 20;
    warehouseRepository.update(warehouse);

    // Archive it
    archiveWarehouseUseCase.archive(warehouse);

    // Verify all data preserved
    Warehouse archived = warehouseRepository.findByBusinessUnitCode("PRESERVE-DATA");
    assertNotNull(archived.archivedAt);
    assertEquals("PRESERVE-DATA", archived.businessUnitCode);
    assertEquals("ZWOLLE-001", archived.location);
    assertEquals(40, archived.capacity);
    assertEquals(20, archived.stock);
    assertNotNull(archived.createdAt);
  }

  /**
   * Test archive multiple warehouses
   */
  @Test
  @Transactional
  public void testArchiveMultipleWarehouses() {
    // Create multiple warehouses
    Warehouse warehouse1 = createWarehouse("MULTI-ARCHIVE-1", "AMSTERDAM-001");
    Warehouse warehouse2 = createWarehouse("MULTI-ARCHIVE-2", "ZWOLLE-001");
    Warehouse warehouse3 = createWarehouse("MULTI-ARCHIVE-3", "TILBURG-001");

    // Archive all
    archiveWarehouseUseCase.archive(warehouse1);
    archiveWarehouseUseCase.archive(warehouse2);
    archiveWarehouseUseCase.archive(warehouse3);

    // Verify all archived
    assertNotNull(warehouseRepository.findByBusinessUnitCode("MULTI-ARCHIVE-1").archivedAt);
    assertNotNull(warehouseRepository.findByBusinessUnitCode("MULTI-ARCHIVE-2").archivedAt);
    assertNotNull(warehouseRepository.findByBusinessUnitCode("MULTI-ARCHIVE-3").archivedAt);
  }

  /**
   * Test archive with different locations
   */
  @Test
  @Transactional
  public void testArchiveWarehouseWithDifferentLocations() {
    String[] locations = {"AMSTERDAM-001", "ZWOLLE-001", "TILBURG-001", "HELMOND-001"};

    for (String location : locations) {
      String buc = "ARCHIVE-" + location.replace("-", "");
      Warehouse warehouse = createWarehouse(buc, location);
      archiveWarehouseUseCase.archive(warehouse);

      Warehouse archived = warehouseRepository.findByBusinessUnitCode(buc);
      assertNotNull(archived.archivedAt);
      assertEquals(location, archived.location);
    }
  }

  /**
   * Cannot archive non-existent warehouse
   */
  @Test
  @Transactional
  public void testCannotArchiveNonExistentWarehouse() {
    Warehouse warehouse = new Warehouse();
    warehouse.businessUnitCode = "NON-EXISTENT";

    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
      archiveWarehouseUseCase.archive(warehouse);
    });

    assertTrue(exception.getMessage().contains("does not exist"));
  }

  /**
   * Cannot archive already-archived warehouse
   */
  @Test
  @Transactional
  public void testCannotArchiveAlreadyArchivedWarehouse() {
    // Create and archive a warehouse
    Warehouse warehouse = createWarehouse("ARCHIVE-TEST-002", "ZWOLLE-001");
    archiveWarehouseUseCase.archive(warehouse);

    // Try to archive again
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
      archiveWarehouseUseCase.archive(warehouse);
    });

    assertTrue(exception.getMessage().contains("already archived"));
  }

  /**
   * Test archive with minimum capacity
   */
  @Test
  @Transactional
  public void testArchiveWarehouseWithMinimumCapacity() {
    Warehouse warehouse = createWarehouse("MIN-CAP-ARCHIVE", "AMSTERDAM-001");
    warehouse.capacity = 1;
    warehouse.stock = 0;
    warehouseRepository.update(warehouse);

    archiveWarehouseUseCase.archive(warehouse);

    Warehouse archived = warehouseRepository.findByBusinessUnitCode("MIN-CAP-ARCHIVE");
    assertNotNull(archived.archivedAt);
    assertEquals(1, archived.capacity);
  }

  /**
   * Concurrent archive operations on different warehouses
   */
  @Test
  public void testConcurrentArchiveDifferentWarehouses() throws InterruptedException {
    // Create multiple warehouses
    String[] businessUnitCodes = {"CONCURRENT-ARCHIVE-1", "CONCURRENT-ARCHIVE-2", "CONCURRENT-ARCHIVE-3"};

    for (String buc : businessUnitCodes) {
      createWarehouseInNewTransaction(buc, "AMSTERDAM-001");
    }

    ExecutorService executor = Executors.newFixedThreadPool(3);
    CountDownLatch startLatch = new CountDownLatch(1);
    CountDownLatch finishLatch = new CountDownLatch(3);

    AtomicBoolean[] successes = {new AtomicBoolean(false), new AtomicBoolean(false), new AtomicBoolean(false)};
    AtomicBoolean exceptionCaught = new AtomicBoolean(false);

    // Submit archive tasks
    for (int i = 0; i < businessUnitCodes.length; i++) {
      final int index = i;
      final String buc = businessUnitCodes[i];

      executor.submit(() -> {
        try {
          startLatch.await();
          archiveWarehouseInNewTransaction(buc);
          successes[index].set(true);
        } catch (Exception e) {
          exceptionCaught.set(true);
        } finally {
          finishLatch.countDown();
        }
      });
    }

    startLatch.countDown();
    finishLatch.await(10, TimeUnit.SECONDS);
    executor.shutdown();

    // Verify all succeeded
    assertFalse(exceptionCaught.get(), "No exceptions should be thrown");
    for (AtomicBoolean success : successes) {
      assertTrue(success.get(), "All archive operations should succeed");
    }

    // Verify all archived
    for (String buc : businessUnitCodes) {
      assertNotNull(warehouseRepository.findByBusinessUnitCode(buc).archivedAt);
    }
  }

  /**
   * Concurrent archive and stock update scenario.
   *
   * Scenario:
   * - Thread 1: Archives warehouse (sets archivedAt)
   * - Thread 2: Updates stock concurrently
   * - Expected: Data integrity is preserved — either the conflict is detected
   *             and an exception is thrown, or both changes are correctly applied.
   */
  @Test
  public void testConcurrentArchiveAndStockUpdateCausesOptimisticLockException() throws InterruptedException {
    // Setup: Create a warehouse
    String businessUnitCode = createWarehouseInNewTransaction("CONCURRENT-ARCHIVE-001", "AMSTERDAM-001");

    ExecutorService executor = Executors.newFixedThreadPool(2);
    CountDownLatch startLatch = new CountDownLatch(1);
    CountDownLatch finishLatch = new CountDownLatch(2);

    AtomicBoolean archiveSuccess = new AtomicBoolean(false);
    AtomicBoolean updateSuccess = new AtomicBoolean(false);
    AtomicBoolean exceptionCaught = new AtomicBoolean(false);

    // Thread 1: Archive warehouse
    executor.submit(() -> {
      try {
        startLatch.await(); // Synchronize start
        archiveWarehouseInNewTransaction(businessUnitCode);
        archiveSuccess.set(true);
      } catch (Exception e) {
        exceptionCaught.set(true);
      } finally {
        finishLatch.countDown();
      }
    });

    // Thread 2: Update stock concurrently
    executor.submit(() -> {
      try {
        startLatch.await(); // Synchronize start
        Thread.sleep(50);
        updateStockInNewTransaction(businessUnitCode, 75);
        updateSuccess.set(true);
      } catch (Exception e) {
        exceptionCaught.set(true);
      } finally {
        finishLatch.countDown();
      }
    });

    startLatch.countDown(); // Start both threads
    finishLatch.await(10, TimeUnit.SECONDS);
    executor.shutdown();

    // Verification: Check the final state
    Warehouse finalWarehouse = warehouseRepository.findByBusinessUnitCode(businessUnitCode);

    boolean bothChangesApplied = finalWarehouse.archivedAt != null && finalWarehouse.stock == 75;

    assertTrue(bothChangesApplied || exceptionCaught.get(),
        "Expected either both changes to be applied properly OR an exception to be thrown. " +
        "Instead, a lost update occurred: archivedAt=" + finalWarehouse.archivedAt +
        ", stock=" + finalWarehouse.stock);

    // Additional check: if no exception was caught, both changes should be applied
    if (!exceptionCaught.get()) {
      assertNotNull(finalWarehouse.archivedAt, "Archive timestamp should be set");
      assertEquals(75, finalWarehouse.stock, "Stock should be updated to 75");
    }

  }

  // Helper methods
  @Transactional(TxType.REQUIRES_NEW)
  Warehouse createWarehouse(String businessUnitCode, String location) {
    Warehouse warehouse = new Warehouse();
    warehouse.businessUnitCode = businessUnitCode;
    warehouse.location = location;
    warehouse.capacity = 100;
    warehouse.stock = 50;
    warehouse.createdAt = LocalDateTime.now();

    warehouseRepository.create(warehouse);
    return warehouse;
  }

  @Transactional(TxType.REQUIRES_NEW)
  String createWarehouseInNewTransaction(String businessUnitCode, String location) {
    createWarehouse(businessUnitCode, location);
    return businessUnitCode;
  }

  @Transactional(TxType.REQUIRES_NEW)
  void archiveWarehouseInNewTransaction(String businessUnitCode) {
    Warehouse warehouse = warehouseRepository.findByBusinessUnitCode(businessUnitCode);
    archiveWarehouseUseCase.archive(warehouse);
  }

  @Transactional(TxType.REQUIRES_NEW)
  void updateStockInNewTransaction(String businessUnitCode, int newStock) {
    Warehouse warehouse = warehouseRepository.findByBusinessUnitCode(businessUnitCode);
    em.refresh(warehouse);
    warehouse.stock = newStock;
    warehouseRepository.update(warehouse);
  }
}
