@ui
Feature: WebCBD storefront UI

  Scenario: View product catalog
    Given I open the storefront
    Then I should see product categories
    And I should see at least one product card

  Scenario: Add a product to the cart
    Given I open the storefront
    When I add the first product to the cart
    Then the cart count should be "1 item"
    And the cart should list 1 item

  Scenario: Admin login opens product data
    Given I open the storefront
    When I log in as admin
    And I open the admin products modal
    Then I should see the admin products table
