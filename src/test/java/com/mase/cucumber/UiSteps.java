package com.mase.cucumber;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.cucumber.spring.ScenarioScope;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

@Component
@ScenarioScope
class UiSteps {

    @LocalServerPort
    private int port;

    private WebDriver driver;
    private WebDriverWait wait;

    @Before
    void setUp() {
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--window-size=1400,900");
        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(12));
    }

    @After
    void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }

    private String baseUrl() {
        return "http://localhost:" + port + "/";
    }

    @Given("I open the storefront")
    public void iOpenTheStorefront() {
        driver.get(baseUrl());
        clearBrowserState();
        driver.navigate().refresh();
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".product-card")));
    }

    @Then("I should see product categories")
    public void iShouldSeeProductCategories() {
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".category-chip")));
        List<WebElement> categories = driver.findElements(By.cssSelector(".category-chip"));
        assertTrue(categories.size() > 0, "Expected at least one category chip");
    }

    @Then("I should see at least one product card")
    public void iShouldSeeAtLeastOneProductCard() {
        List<WebElement> cards = driver.findElements(By.cssSelector(".product-card"));
        assertTrue(cards.size() > 0, "Expected at least one product card");
    }

    @When("I add the first product to the cart")
    public void iAddTheFirstProductToTheCart() {
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".js-add-to-cart")));
        WebElement addButton = driver.findElements(By.cssSelector(".js-add-to-cart")).get(0);
        addButton.click();
    }

    @Then("the cart count should be {string}")
    public void theCartCountShouldBe(String expectedCount) {
        wait.until(ExpectedConditions.textToBe(By.id("cart-sidebar-count"), expectedCount));
        String actual = driver.findElement(By.id("cart-sidebar-count")).getText();
        assertEquals(expectedCount, actual);
    }

    @And("the cart should list {int} item")
    public void theCartShouldListItems(int expectedCount) {
        wait.until(webDriver -> webDriver.findElements(By.cssSelector("#cart-items .cart-item")).size() == expectedCount);
        int actual = driver.findElements(By.cssSelector("#cart-items .cart-item")).size();
        assertEquals(expectedCount, actual);
    }

    @When("I log in as admin")
    public void iLogInAsAdmin() {
        WebElement loginButton = driver.findElement(By.cssSelector(".js-auth-action"));
        loginButton.click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("login-username")));
        driver.findElement(By.id("login-username")).sendKeys("admin");
        driver.findElement(By.id("login-password")).sendKeys("admin");
        driver.findElement(By.cssSelector("#login-form button[type='submit']")).click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".js-admin-toggle")));
    }

    @And("I open the admin products modal")
    public void iOpenTheAdminProductsModal() {
        WebElement adminButton = driver.findElement(By.cssSelector(".js-admin-toggle"));
        adminButton.click();
        wait.until(ExpectedConditions.attributeContains(By.id("adminProductsModal"), "class", "show"));
    }

    @Then("I should see the admin products table")
    public void iShouldSeeTheAdminProductsTable() {
        wait.until(ExpectedConditions.not(ExpectedConditions.attributeContains(
            By.id("admin-products-table"), "class", "d-none")));
        wait.until(webDriver -> webDriver.findElements(By.cssSelector("#admin-products-table-body tr")).size() > 0);
        int rows = driver.findElements(By.cssSelector("#admin-products-table-body tr")).size();
        assertTrue(rows > 0, "Expected admin products table to have rows");
    }
    
    private void clearBrowserState() {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("window.localStorage.clear(); window.sessionStorage.clear();");
    }
}
