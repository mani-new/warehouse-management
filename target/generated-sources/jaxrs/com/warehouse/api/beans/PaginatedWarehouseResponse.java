package com.warehouse.api.beans;

import java.util.List;

public class PaginatedWarehouseResponse {
    private List<Warehouse> content;
    private int totalElements;
    private int totalPages;
    private int page;
    private int pageSize;
    private boolean hasNext;
    private boolean hasPrevious;

    public PaginatedWarehouseResponse() {}

    public PaginatedWarehouseResponse(List<Warehouse> content, int totalElements, int totalPages, int page, int pageSize, boolean hasNext, boolean hasPrevious) {
        this.content = content;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.page = page;
        this.pageSize = pageSize;
        this.hasNext = hasNext;
        this.hasPrevious = hasPrevious;
    }

    public List<Warehouse> getContent() {
        return content;
    }

    public void setContent(List<Warehouse> content) {
        this.content = content;
    }

    public int getTotalElements() {
        return totalElements;
    }

    public void setTotalElements(int totalElements) {
        this.totalElements = totalElements;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public boolean isHasNext() {
        return hasNext;
    }

    public void setHasNext(boolean hasNext) {
        this.hasNext = hasNext;
    }

    public boolean isHasPrevious() {
        return hasPrevious;
    }

    public void setHasPrevious(boolean hasPrevious) {
        this.hasPrevious = hasPrevious;
    }
}
