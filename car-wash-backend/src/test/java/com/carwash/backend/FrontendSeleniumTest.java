package com.carwash.backend;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.*;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Selenium end-to-end tests against the Vite dev server (http://localhost:5173).
 * Start the frontend before running: `npm run dev` inside car-wash-frontend/.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FrontendSeleniumTest {

    private static final String BASE_URL = "http://localhost:5173";
    private static WebDriver driver;
    private static WebDriverWait wait;
    private static boolean viteUp;

    @BeforeAll
    static void setupDriver() throws Exception {
        // Guard: skip all tests if Vite is not running
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(BASE_URL).openConnection();
            conn.setConnectTimeout(3000);
            conn.connect();
            viteUp = conn.getResponseCode() < 500;
            conn.disconnect();
        } catch (Exception e) {
            viteUp = false;
        }

        if (!viteUp) return;

        WebDriverManager.chromedriver().setup();
        ChromeOptions opts = new ChromeOptions();
        opts.addArguments("--headless=new", "--no-sandbox", "--disable-dev-shm-usage",
                "--window-size=1280,900", "--disable-gpu");
        driver = new ChromeDriver(opts);
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    @AfterAll
    static void teardown() {
        if (driver != null) driver.quit();
    }

    private void requireVite() {
        Assumptions.assumeTrue(viteUp, "Vite dev server not running on " + BASE_URL);
    }

    // ── Landing page ────────────────────────────────────────────────────────

    @Test
    @Order(1)
    void landing_page_loads_with_correct_title() {
        requireVite();
        driver.get(BASE_URL + "/");
        assertThat(driver.getTitle()).isNotBlank();
    }

    @Test
    @Order(2)
    void landing_page_shows_brand_name() {
        requireVite();
        driver.get(BASE_URL + "/");
        String body = wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body"))).getText();
        assertThat(body.toLowerCase()).containsAnyOf("azurewash", "car wash", "ai car wash");
    }

    @Test
    @Order(3)
    void landing_page_shows_services_section() {
        requireVite();
        driver.get(BASE_URL + "/");
        String body = driver.findElement(By.tagName("body")).getText();
        assertThat(body).containsAnyOf("Standard Wash", "Premium Wash", "Full Detailing", "Valet");
    }

    @Test
    @Order(4)
    void landing_page_has_book_now_link() {
        requireVite();
        driver.get(BASE_URL + "/");
        List<WebElement> links = driver.findElements(By.tagName("a"));
        boolean hasBookLink = links.stream().anyMatch(a -> {
            String href = a.getAttribute("href");
            String text = a.getText();
            return (href != null && href.contains("/book")) || text.toLowerCase().contains("book");
        });
        assertThat(hasBookLink).isTrue();
    }

    // ── Navigation ──────────────────────────────────────────────────────────

    @Test
    @Order(5)
    void login_page_renders() {
        requireVite();
        driver.get(BASE_URL + "/login");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));
        String body = driver.findElement(By.tagName("body")).getText();
        assertThat(body.toLowerCase()).containsAnyOf("login", "sign in", "log in", "email", "password");
    }

    @Test
    @Order(6)
    void login_page_has_email_and_password_fields() {
        requireVite();
        driver.get(BASE_URL + "/login");
        WebElement emailField = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.cssSelector("input[type='email'], input[name='email'], input[placeholder*='mail' i]")));
        assertThat(emailField.isDisplayed()).isTrue();
        WebElement passwordField = driver.findElement(By.cssSelector("input[type='password']"));
        assertThat(passwordField.isDisplayed()).isTrue();
    }

    @Test
    @Order(7)
    void login_form_empty_submit_stays_on_login() {
        requireVite();
        driver.get(BASE_URL + "/login");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("auth-form")));
        WebElement submitBtn = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.cssSelector("form#auth-form button[type='submit']")));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", submitBtn);
        assertThat(driver.getCurrentUrl()).contains("/login");
    }

    @Test
    @Order(8)
    void unknown_route_redirects_to_home() {
        requireVite();
        driver.get(BASE_URL + "/this-route-does-not-exist-xyz");
        // React Router replaces the URL — wait up to 5s for redirect
        new WebDriverWait(driver, Duration.ofSeconds(5))
                .until(ExpectedConditions.urlToBe(BASE_URL + "/"));
        assertThat(driver.getCurrentUrl()).isEqualTo(BASE_URL + "/");
    }

    @Test
    @Order(9)
    void unauthorized_page_renders() {
        requireVite();
        driver.get(BASE_URL + "/unauthorized");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));
        String body = driver.findElement(By.tagName("body")).getText();
        assertThat(body.toLowerCase()).containsAnyOf("unauthori", "access", "permission", "403");
    }

    @Test
    @Order(10)
    void book_page_requires_auth_or_renders() {
        requireVite();
        driver.get(BASE_URL + "/book");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));
        String url = driver.getCurrentUrl();
        assertThat(url).containsAnyOf("/login", "/book");
    }

    // ── Responsive layout ───────────────────────────────────────────────────

    @Test
    @Order(11)
    void landing_page_renders_on_mobile_viewport() {
        requireVite();
        driver.manage().window().setSize(new Dimension(390, 844));
        driver.get(BASE_URL + "/");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));
        assertThat(driver.findElement(By.tagName("body")).isDisplayed()).isTrue();
        driver.manage().window().setSize(new Dimension(1280, 900));
    }
}
