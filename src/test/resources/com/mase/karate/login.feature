Feature: Admin login helper

  Scenario: Login as admin and return access token
    # Use the configured base URL.
    Given url baseUrl
    And path '/api/ecommerce/v1/auth/login'
    And request { username: 'admin', password: 'admin' }
    When method post
    Then status 200
    And match response.accessToken == '#string'
    * def accessToken = response.accessToken
