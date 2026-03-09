package com.fulfilment.application.monolith.warehouses.adapters.database;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class WarehouseRepository implements WarehouseStore, PanacheRepository<DbWarehouse> {

  @Override
  public List<Warehouse> getAll() {
    return this.listAll().stream().map(DbWarehouse::toWarehouse).toList();
  }

  @Override
  public void create(Warehouse warehouse) {
    DbWarehouse dbWarehouse = new DbWarehouse();
    dbWarehouse.businessUnitCode = warehouse.businessUnitCode;
    dbWarehouse.location = warehouse.location;
    dbWarehouse.capacity = warehouse.capacity;
    dbWarehouse.stock = warehouse.stock;
    dbWarehouse.createdAt = warehouse.createdAt;
    dbWarehouse.archivedAt = warehouse.archivedAt;
    
    this.persist(dbWarehouse);
  }

  @Override
  public void update(Warehouse warehouse) {
    getEntityManager().createQuery(
      "UPDATE DbWarehouse w SET w.location = :loc, w.capacity = :cap, " +
      "w.stock = :stock, w.archivedAt = :archived WHERE w.businessUnitCode = :code")
      .setParameter("loc", warehouse.location)
      .setParameter("cap", warehouse.capacity)
      .setParameter("stock", warehouse.stock)
      .setParameter("archived", warehouse.archivedAt)
      .setParameter("code", warehouse.businessUnitCode)
      .executeUpdate();

    // Clear persistence context to see updates in subsequent queries
    getEntityManager().flush();
    getEntityManager().clear();
  }

  @Override
  public void remove(Warehouse warehouse) {
    delete("businessUnitCode", warehouse.businessUnitCode);
    getEntityManager().flush();
    getEntityManager().clear();
  }

  @Override
  public Warehouse findByBusinessUnitCode(String buCode) {
    DbWarehouse dbWarehouse = find("businessUnitCode", buCode).firstResult();
    return dbWarehouse != null ? dbWarehouse.toWarehouse() : null;
  }

  public PanacheQuery<DbWarehouse> searchWarehouses(String location, Integer minCapacity, Integer maxCapacity,
      String sortBy, String sortOrder, int page, int pageSize) {
    StringBuilder query = new StringBuilder("archivedAt is null");
    List<Object> params = new ArrayList<>();

    if (location != null && !location.trim().isEmpty()) {
      query.append(" and location = ?").append(params.size() + 1);
      params.add(location);
    }
    if (minCapacity != null) {
      query.append(" and capacity >= ?").append(params.size() + 1);
      params.add(minCapacity);
    }
    if (maxCapacity != null) {
      query.append(" and capacity <= ?").append(params.size() + 1);
      params.add(maxCapacity);
    }

    String orderBy = "createdAt";
    if ("capacity".equals(sortBy)) {
      orderBy = "capacity";
    }
    String order = "asc".equalsIgnoreCase(sortOrder) ? "asc" : "desc";
    query.append(" order by ").append(orderBy).append(" ").append(order);

    return find(query.toString(), params.toArray()).page(page, pageSize);
  }
}
