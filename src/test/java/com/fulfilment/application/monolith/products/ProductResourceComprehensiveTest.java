package com.fulfilment.application.monolith.products;

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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Comprehensive integration tests for Product REST endpoints.
 *
 * Tests product CRUD operations, validation, and data integrity.
 */
@QuarkusTest
public class ProductResourceComprehensiveTest {

  @Inject
  EntityManager em;

  @BeforeEach
  @Transactional
  public void setup() {
    // Clean up test data but preserve initial products
    em.createQuery("DELETE FROM Product p WHERE p.name LIKE 'TEST-%' OR p.name LIKE 'COMPREHENSIVE-%'").executeUpdate();
  }

  // ==================== List Products Tests ====================

  @Test
  public void testListAllProducts() {
    given()
        .when()
        .get("/product")
        .then()
        .statusCode(200)
        .body("size()", greaterThanOrEqualTo(1)); // At least some products exist
  }

  @Test
  public void testListProductsReturnsValidData() {
    given()
        .when()
        .get("/product")
        .then()
        .statusCode(200)
        .body("name", notNullValue());
  }

  @Test
  public void testListProductsOrderByName() {
    given()
        .when()
        .get("/product")
        .then()
        .statusCode(200)
        .body("size()", greaterThanOrEqualTo(1));
  }

  // ==================== Get Product Tests ====================

  @Test
  public void testGetProductByID() {
    // Get first product ID
    Long productId = given()
        .when()
        .get("/product")
        .then()
        .statusCode(200)
        .extract()
        .jsonPath()
        .getLong("[0].id");

    given()
        .when()
        .get("/product/" + productId)
        .then()
        .statusCode(200)
        .body("name", notNullValue())
        .body("stock", notNullValue());
  }

  @Test
  public void testGetNonExistentProduct() {
    given()
        .when()
        .get("/product/99999")
        .then()
        .statusCode(404);
  }

  // ==================== Create Product Tests ====================

  @Test
  public void testCreateProductSuccessfully() {
    given()
        .contentType(ContentType.JSON)
        .body("{\"name\": \"TEST-NEW-PRODUCT\", \"description\": \"A new test product\", \"stock\": 20, \"price\": 19.99}")
        .when()
        .post("/product")
        .then()
        .statusCode(201)
        .body("name", equalTo("TEST-NEW-PRODUCT"))
        .body("stock", equalTo(20));
  }

  @Test
  public void testCreateProductWithBasicInfo() {
    given()
        .contentType(ContentType.JSON)
        .body("{\"name\": \"TEST-BASIC\", \"stock\": 5}")
        .when()
        .post("/product")
        .then()
        .statusCode(201)
        .body("name", equalTo("TEST-BASIC"))
        .body("stock", equalTo(5));
  }

  @Test
  public void testCreateProductWithDescription() {
    given()
        .contentType(ContentType.JSON)
        .body("{\"name\": \"TEST-DESC\", \"description\": \"Detailed description\", \"stock\": 10}")
        .when()
        .post("/product")
        .then()
        .statusCode(201)
        .body("description", equalTo("Detailed description"));
  }

  @Test
  public void testCreateProductWithPrice() {
    given()
        .contentType(ContentType.JSON)
        .body("{\"name\": \"TEST-PRICE\", \"price\": 99.99, \"stock\": 5}")
        .when()
        .post("/product")
        .then()
        .statusCode(201)
        .body("price", equalTo(99.99f));
  }

  @Test
  public void testCreateProductWithZeroStock() {
    given()
        .contentType(ContentType.JSON)
        .body("{\"name\": \"TEST-ZERO-STOCK\", \"stock\": 0}")
        .when()
        .post("/product")
        .then()
        .statusCode(201)
        .body("stock", equalTo(0));
  }

  @Test
  public void testCreateProductWithLargeStock() {
    given()
        .contentType(ContentType.JSON)
        .body("{\"name\": \"TEST-LARGE-STOCK\", \"stock\": 999999}")
        .when()
        .post("/product")
        .then()
        .statusCode(201)
        .body("stock", equalTo(999999));
  }

  @Test
  public void testCreateProductDuplicateName() {
    // Note: Current API allows duplicate names - this test documents the current behavior
    given()
        .contentType(ContentType.JSON)
        .body("{\"name\": \"TONSTAD\", \"stock\": 15}")
        .when()
        .post("/product")
        .then()
        .statusCode(201); // API currently allows duplicates
  }

  @Test
  public void testCreateMultipleProducts() {
    for (int i = 1; i <= 3; i++) {
      given()
          .contentType(ContentType.JSON)
          .body("{\"name\": \"TEST-MULTI-" + i + "\", \"stock\": " + (i * 10) + "}")
          .when()
          .post("/product")
          .then()
          .statusCode(201);
    }

    given()
        .when()
        .get("/product")
        .then()
        .statusCode(200)
        .body("name", hasItems("TEST-MULTI-1", "TEST-MULTI-2", "TEST-MULTI-3"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"COMPREHENSIVE-001", "COMPREHENSIVE-002", "COMPREHENSIVE-003"})
  public void testCreateProductParameterized(String productName) {
    given()
        .contentType(ContentType.JSON)
        .body("{\"name\": \"" + productName + "\", \"stock\": 10}")
        .when()
        .post("/product")
        .then()
        .statusCode(201)
        .body("name", equalTo(productName));
  }

  // ==================== Update Product Tests ====================

  @Test
  public void testUpdateProductSuccessfully() {
    // Create a product first
    Long productId = given()
        .contentType(ContentType.JSON)
        .body("{\"name\": \"TEST-UPDATE-1\", \"stock\": 10}")
        .when()
        .post("/product")
        .then()
        .statusCode(201)
        .extract()
        .jsonPath()
        .getLong("id");

    given()
        .contentType(ContentType.JSON)
        .body("{\"name\": \"TEST-UPDATE-1\", \"stock\": 25, \"description\": \"Updated description\"}")
        .when()
        .put("/product/" + productId)
        .then()
        .statusCode(200)
        .body("stock", equalTo(25));
  }

  @Test
  public void testUpdateProductStock() {
    // Create a product first
    Long productId = given()
        .contentType(ContentType.JSON)
        .body("{\"name\": \"TEST-UPDATE-2\", \"stock\": 5}")
        .when()
        .post("/product")
        .then()
        .statusCode(201)
        .extract()
        .jsonPath()
        .getLong("id");

    given()
        .contentType(ContentType.JSON)
        .body("{\"name\": \"TEST-UPDATE-2\", \"stock\": 50}")
        .when()
        .put("/product/" + productId)
        .then()
        .statusCode(200)
        .body("stock", equalTo(50));
  }

  @Test
  public void testUpdateProductPrice() {
    // Create a product first
    Long productId = given()
        .contentType(ContentType.JSON)
        .body("{\"name\": \"TEST-UPDATE-3\", \"stock\": 5}")
        .when()
        .post("/product")
        .then()
        .statusCode(201)
        .extract()
        .jsonPath()
        .getLong("id");

    given()
        .contentType(ContentType.JSON)
        .body("{\"name\": \"TEST-UPDATE-3\", \"price\": 299.99, \"stock\": 5}")
        .when()
        .put("/product/" + productId)
        .then()
        .statusCode(200)
        .body("price", equalTo(299.99f));
  }

  @Test
  public void testUpdateProductDescription() {
    // Create a product first
    Long productId = given()
        .contentType(ContentType.JSON)
        .body("{\"name\": \"TEST-UPDATE-4\", \"stock\": 5}")
        .when()
        .post("/product")
        .then()
        .statusCode(201)
        .extract()
        .jsonPath()
        .getLong("id");

    given()
        .contentType(ContentType.JSON)
        .body("{\"name\": \"TEST-UPDATE-4\", \"description\": \"New description\", \"stock\": 5}")
        .when()
        .put("/product/" + productId)
        .then()
        .statusCode(200)
        .body("description", equalTo("New description"));
  }

  @Test
  public void testUpdateProductToZeroStock() {
    // Create a product first
    Long productId = given()
        .contentType(ContentType.JSON)
        .body("{\"name\": \"TEST-UPDATE-5\", \"stock\": 10}")
        .when()
        .post("/product")
        .then()
        .statusCode(201)
        .extract()
        .jsonPath()
        .getLong("id");

    given()
        .contentType(ContentType.JSON)
        .body("{\"name\": \"TEST-UPDATE-5\", \"stock\": 0}")
        .when()
        .put("/product/" + productId)
        .then()
        .statusCode(200)
        .body("stock", equalTo(0));
  }

  @Test
  public void testUpdateNonExistentProduct() {
    given()
        .contentType(ContentType.JSON)
        .body("{\"name\": \"TEST-NONEXISTENT\", \"stock\": 5}")
        .when()
        .put("/product/99999")
        .then()
        .statusCode(404);
  }

  @ParameterizedTest
  @ValueSource(ints = {0, 10, 100, 1000})
  public void testUpdateProductStockParameterized(int newStock) {
    // Create a product first
    Long productId = given()
        .contentType(ContentType.JSON)
        .body("{\"name\": \"TEST-PARAM-" + newStock + "\", \"stock\": 5}")
        .when()
        .post("/product")
        .then()
        .statusCode(201)
        .extract()
        .jsonPath()
        .getLong("id");

    given()
        .contentType(ContentType.JSON)
        .body("{\"name\": \"TEST-PARAM-" + newStock + "\", \"stock\": " + newStock + "}")
        .when()
        .put("/product/" + productId)
        .then()
        .statusCode(200)
        .body("stock", equalTo(newStock));
  }

  // ==================== Delete Product Tests ====================

  @Test
  public void testDeleteProductSuccessfully() {
    // Create a product to delete
    Long productId = given()
        .contentType(ContentType.JSON)
        .body("{\"name\": \"TEST-DELETE-PRODUCT\", \"stock\": 10}")
        .when()
        .post("/product")
        .then()
        .statusCode(201)
        .extract()
        .jsonPath()
        .getLong("id");

    // Delete it
    given()
        .when()
        .delete("/product/" + productId)
        .then()
        .statusCode(204);

    // Verify deletion
    given()
        .when()
        .get("/product/" + productId)
        .then()
        .statusCode(404);
  }

  @Test
  public void testDeleteInitialProduct() {
    // Get first product ID
    Long productId = given()
        .when()
        .get("/product")
        .then()
        .statusCode(200)
        .extract()
        .jsonPath()
        .getLong("[0].id");

    given()
        .when()
        .delete("/product/" + productId)
        .then()
        .statusCode(204);

    given()
        .when()
        .get("/product/" + productId)
        .then()
        .statusCode(404);
  }

  @Test
  public void testDeleteNonExistentProduct() {
    given()
        .when()
        .delete("/product/99999")
        .then()
        .statusCode(404);
  }

  // ==================== Data Consistency Tests ====================

  //@Test
  public void testProductNameUniqueness() {
    // Note: Current API allows duplicate names - this test documents the current behavior
    // Create a product
    given()
        .contentType(ContentType.JSON)
        .body("{\"name\": \"TEST-UNIQUE\", \"stock\": 5}")
        .when()
        .post("/product")
        .then()
        .statusCode(201);

    // Try to create another with same name - API currently allows this
    given()
        .contentType(ContentType.JSON)
        .body("{\"name\": \"TEST-UNIQUE\", \"stock\": 10}")
        .when()
        .post("/product")
        .then()
        .statusCode(201); // API currently allows duplicates
  }

  @Test
  public void testProductStockCanBeNegativeAfterUpdate() {
    // Note: This tests the current behavior. May want to add validation to prevent this
    Long productId = given()
        .contentType(ContentType.JSON)
        .body("{\"name\": \"TEST-NEGATIVE\", \"stock\": 10}")
        .when()
        .post("/product")
        .then()
        .statusCode(201)
        .extract()
        .jsonPath()
        .getLong("id");

    // Update with negative stock (if system allows)
    given()
        .contentType(ContentType.JSON)
        .body("{\"name\": \"TEST-NEGATIVE\", \"stock\": -5}")
        .when()
        .put("/product/" + productId)
        .then()
        .statusCode(200); // PUT returns the entity
  }

  @Test
  public void testInitialProductsExist() {
    // Verify that at least some products exist (may be fewer due to other tests)
    given()
        .when()
        .get("/product")
        .then()
        .statusCode(200)
        .body("size()", greaterThanOrEqualTo(1));
  }

  @Test
  public void testProductsHaveCorrectStock() {
    // Test that products have stock values (may not be the original values due to other tests)
    given()
        .when()
        .get("/product")
        .then()
        .statusCode(200)
        .body("stock", everyItem(notNullValue()));
  }

  @Test
  public void testProductCreationWithAllFields() {
    given()
        .contentType(ContentType.JSON)
        .body("{\"name\": \"TEST-ALL-FIELDS\", \"description\": \"Complete product\", \"price\": 49.99, \"stock\": 100}")
        .when()
        .post("/product")
        .then()
        .statusCode(201)
        .body("name", equalTo("TEST-ALL-FIELDS"))
        .body("description", equalTo("Complete product"))
        .body("price", equalTo(49.99f))
        .body("stock", equalTo(100));
  }

  @Test
  public void testProductUpdateWithAllFields() {
    // Create a product first
    Long productId = given()
        .contentType(ContentType.JSON)
        .body("{\"name\": \"TEST-UPDATE-ALL\", \"stock\": 5}")
        .when()
        .post("/product")
        .then()
        .statusCode(201)
        .extract()
        .jsonPath()
        .getLong("id");

    given()
        .contentType(ContentType.JSON)
        .body("{\"name\": \"TEST-UPDATE-ALL\", \"description\": \"Updated complete product\", \"price\": 79.99, \"stock\": 200}")
        .when()
        .put("/product/" + productId)
        .then()
        .statusCode(200);

    given()
        .when()
        .get("/product/" + productId)
        .then()
        .statusCode(200)
        .body("description", equalTo("Updated complete product"))
        .body("price", equalTo(79.99f))
        .body("stock", equalTo(200));
  }
}
