Feature: E-commerce REST API integration tests

  Background:
    # Set the base URL and obtain an admin token for protected endpoints.
    * url baseUrl
    * def loginResult = callonce read('classpath:com/mase/karate/login.feature')
    * def adminToken = loginResult.accessToken
    * def adminAuth = 'Bearer ' + adminToken

  Scenario: Get all products returns seeded data
    Given path '/api/ecommerce/v1/products'
    When method get
    Then status 200
    And assert response.length > 0

  Scenario: Admin can create a new product
    Given path '/api/ecommerce/v1/products'
    And header Authorization = adminAuth
    And request { name: 'Lamp', category: 'Accessories', price: 19.99, description: 'Best lamp' }
    When method post
    Then status 200
    And match response.name == 'Karate Lamp'
    And match response.category == 'Accessories'

  Scenario: Create cart and add a product
    Given path '/api/ecommerce/v1/carts'
    When method post
    Then status 200
    And match response.cartId == '#string'
    * def cartId = response.cartId

    Given path '/api/ecommerce/v1/carts', cartId, 'products', 1
    When method post
    Then status 200
    And match response.productIds contains 1

    Given path '/api/ecommerce/v1/carts', cartId
    When method get
    Then status 200
    And match response.productIds contains 1

  Scenario: Admin can list all carts
    Given path '/api/ecommerce/v1/carts'
    And header Authorization = adminAuth
    When method get
    Then status 200
