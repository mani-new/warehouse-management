package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import com.fulfilment.application.monolith.warehouses.adapters.database.WarehouseRepository;
import com.fulfilment.application.monolith.warehouses.domain.ports.ArchiveWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.CreateWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.ReplaceWarehouseOperation;
import com.warehouse.api.WarehouseResource;
import com.warehouse.api.beans.Warehouse;
import com.warehouse.api.beans.PaginatedWarehouseResponse;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.WebApplicationException;
import java.util.List;
import com.fulfilment.application.monolith.warehouses.adapters.database.DbWarehouse;

@RequestScoped
public class WarehouseResourceImpl implements WarehouseResource {

  @Inject WarehouseRepository warehouseRepository;
  @Inject CreateWarehouseOperation createWarehouseOperation;
  @Inject ArchiveWarehouseOperation archiveWarehouseOperation;
  @Inject ReplaceWarehouseOperation replaceWarehouseOperation;

  @Override
  public List<Warehouse> listAllWarehousesUnits() {
    return warehouseRepository.getAll().stream().map(this::toWarehouseResponse).toList();
  }

  @Override
  @Transactional
  public Warehouse createANewWarehouseUnit(@NotNull Warehouse data) {
    // Convert API model to domain model
    var domainWarehouse = new com.fulfilment.application.monolith.warehouses.domain.models.Warehouse();
    domainWarehouse.businessUnitCode = data.getBusinessUnitCode();
    domainWarehouse.location = data.getLocation();
    domainWarehouse.capacity = data.getCapacity();
    domainWarehouse.stock = data.getStock() != null ? data.getStock() : 0;

    try {
      // Create warehouse through use case (includes validations)
      createWarehouseOperation.create(domainWarehouse);
      
      // Return the created warehouse
      return toWarehouseResponse(domainWarehouse);
    } catch (IllegalArgumentException e) {
      throw new WebApplicationException(e.getMessage(), 400);
    }
  }

  @Override
  public Warehouse getAWarehouseUnitByID(String id) {
    // Find warehouse by business unit code
    var domainWarehouse = warehouseRepository.findByBusinessUnitCode(id);
    
    if (domainWarehouse == null) {
      throw new WebApplicationException("Warehouse with business unit code '" + id + "' not found", 404);
    }
    
    return toWarehouseResponse(domainWarehouse);
  }

  @Override
  @Transactional
  public void archiveAWarehouseUnitByID(String id) {
    // Find warehouse by business unit code
    var domainWarehouse = warehouseRepository.findByBusinessUnitCode(id);

    if (domainWarehouse == null) {
      throw new WebApplicationException("Warehouse with business unit code '" + id + "' not found", 404);
    }

    try {
      // Archive warehouse through use case (includes validations)
      archiveWarehouseOperation.archive(domainWarehouse);
    } catch (IllegalArgumentException e) {
      throw new WebApplicationException(e.getMessage(), 400);
    }
  }

  @Override
  @Transactional
  public Warehouse replaceTheCurrentActiveWarehouse(
      String businessUnitCode, @NotNull Warehouse data) {
    // Convert API model to domain model
    var domainWarehouse = new com.fulfilment.application.monolith.warehouses.domain.models.Warehouse();
    domainWarehouse.businessUnitCode = businessUnitCode; // Use businessUnitCode from path
    domainWarehouse.location = data.getLocation();
    domainWarehouse.capacity = data.getCapacity();
    domainWarehouse.stock = data.getStock() != null ? data.getStock() : 0;

    try {
      // Replace warehouse through use case (includes validations)
      replaceWarehouseOperation.replace(domainWarehouse);

      // Return the updated warehouse
      var updated = warehouseRepository.findByBusinessUnitCode(businessUnitCode);
      return toWarehouseResponse(updated);
    } catch (IllegalArgumentException e) {
      throw new WebApplicationException(e.getMessage(), 400);
    }
  }

  @Override
  @jakarta.ws.rs.GET
  @jakarta.ws.rs.Path("search")
  @jakarta.ws.rs.Produces("application/json")
  public PaginatedWarehouseResponse searchAndFilterWarehouses(@jakarta.ws.rs.QueryParam("location") String location,
      @jakarta.ws.rs.QueryParam("minCapacity") java.math.BigInteger minCapacity,
      @jakarta.ws.rs.QueryParam("maxCapacity") java.math.BigInteger maxCapacity,
      @jakarta.ws.rs.QueryParam("sortBy") String sortBy,
      @jakarta.ws.rs.QueryParam("sortOrder") String sortOrder,
      @jakarta.ws.rs.QueryParam("page") java.math.BigInteger page,
      @jakarta.ws.rs.QueryParam("pageSize") java.math.BigInteger pageSize) {
    // Convert BigInteger to int, with defaults
    int pageInt = page != null ? page.intValue() : 0;
    int pageSizeInt = pageSize != null ? pageSize.intValue() : 10;
    if (pageSizeInt > 100) pageSizeInt = 100;
    if (sortBy == null) sortBy = "createdAt";
    if (sortOrder == null) sortOrder = "asc";

    // Perform search
    var panachePage = warehouseRepository.searchWarehouses(location, minCapacity != null ? minCapacity.intValue() : null, maxCapacity != null ? maxCapacity.intValue() : null, sortBy, sortOrder, pageInt, pageSizeInt);
    var warehouses = panachePage.list().stream().map(DbWarehouse::toWarehouse).map(this::toWarehouseResponse).toList();

    // Build response
    var response = new PaginatedWarehouseResponse();
    response.setContent(warehouses);
    response.setTotalElements((int) panachePage.count());
    response.setTotalPages(panachePage.pageCount());
    response.setPage(pageInt);
    response.setPageSize(pageSizeInt);
    response.setHasNext(panachePage.hasNextPage());
    response.setHasPrevious(panachePage.hasPreviousPage());

    return response;
  }

  private Warehouse toWarehouseResponse(
      com.fulfilment.application.monolith.warehouses.domain.models.Warehouse warehouse) {
    var response = new Warehouse();
    response.setBusinessUnitCode(warehouse.businessUnitCode);
    response.setLocation(warehouse.location);
    response.setCapacity(warehouse.capacity);
    response.setStock(warehouse.stock);

    return response;
  }
}
