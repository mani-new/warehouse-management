
package com.warehouse.api.beans;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "content",
    "totalElements",
    "totalPages",
    "page",
    "pageSize",
    "hasNext",
    "hasPrevious"
})
@Generated("jsonschema2pojo")
public class PaginatedWarehouseResponse {

    @JsonProperty("content")
    private List<Warehouse> content = new ArrayList<Warehouse>();
    @JsonProperty("totalElements")
    private Integer totalElements;
    @JsonProperty("totalPages")
    private Integer totalPages;
    @JsonProperty("page")
    private Integer page;
    @JsonProperty("pageSize")
    private Integer pageSize;
    @JsonProperty("hasNext")
    private Boolean hasNext;
    @JsonProperty("hasPrevious")
    private Boolean hasPrevious;

    @JsonProperty("content")
    public List<Warehouse> getContent() {
        return content;
    }

    @JsonProperty("content")
    public void setContent(List<Warehouse> content) {
        this.content = content;
    }

    @JsonProperty("totalElements")
    public Integer getTotalElements() {
        return totalElements;
    }

    @JsonProperty("totalElements")
    public void setTotalElements(Integer totalElements) {
        this.totalElements = totalElements;
    }

    @JsonProperty("totalPages")
    public Integer getTotalPages() {
        return totalPages;
    }

    @JsonProperty("totalPages")
    public void setTotalPages(Integer totalPages) {
        this.totalPages = totalPages;
    }

    @JsonProperty("page")
    public Integer getPage() {
        return page;
    }

    @JsonProperty("page")
    public void setPage(Integer page) {
        this.page = page;
    }

    @JsonProperty("pageSize")
    public Integer getPageSize() {
        return pageSize;
    }

    @JsonProperty("pageSize")
    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    @JsonProperty("hasNext")
    public Boolean getHasNext() {
        return hasNext;
    }

    @JsonProperty("hasNext")
    public void setHasNext(Boolean hasNext) {
        this.hasNext = hasNext;
    }

    @JsonProperty("hasPrevious")
    public Boolean getHasPrevious() {
        return hasPrevious;
    }

    @JsonProperty("hasPrevious")
    public void setHasPrevious(Boolean hasPrevious) {
        this.hasPrevious = hasPrevious;
    }

}
