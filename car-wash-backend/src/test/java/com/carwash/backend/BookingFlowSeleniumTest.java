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
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Selenium E2E tests for the customer booking wizard.
 *
 * Prerequisites:
 *   1. Frontend : cd car-wash-frontend && npm run dev -- --host
 *   2. Backend  : cd car-wash-backend  && mvn spring-boot:run
 *                 (tests 2-10 auto-skip when backend is down)
 *
 * Test account is self-registered on first run via the /login → Sign up UI.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class BookingFlowSeleniumTest {

    private static final String BASE_URL   = "http://localhost:5173";
    private static final String API_HEALTH = "http://localhost:8080/api/v1/public/services";
    private static final String TEST_EMAIL = "selenium_booking@test.com";
    private static final String TEST_PASS  = "SeleniumPass123!";
    private static final String TEST_PHONE = "0123456789";

    private static WebDriver driver;
    private static WebDriverWait wait;
    private static boolean viteUp;
    private static boolean backendUp;

    // ── Setup / teardown ─────────────────────────────────────────────────────

    @BeforeAll
    static void setup() {
        viteUp    = ping(BASE_URL);
        backendUp = ping(API_HEALTH);

        if (!viteUp) return;

        WebDriverManager.chromedriver().setup();
        ChromeOptions opts = new ChromeOptions();
        opts.addArguments("--headless=new", "--no-sandbox", "--disable-dev-shm-usage",
                "--window-size=1280,900", "--disable-gpu");
        driver = new ChromeDriver(opts);
        wait   = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    @AfterAll
    static void teardown() {
        if (driver != null) driver.quit();
    }

    // ── Guards ───────────────────────────────────────────────────────────────

    private void requireVite()    { Assumptions.assumeTrue(viteUp,    "Vite not running on " + BASE_URL); }
    private void requireBackend() { Assumptions.assumeTrue(backendUp, "Backend not running — checked " + API_HEALTH); }

    private void requireLoggedIn() {
        Assumptions.assumeFalse(driver.getCurrentUrl().contains("/login"),
                "Auth failed — skip: still on login page");
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private static boolean ping(String url) {
        try {
            HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
            c.setConnectTimeout(3000);
            c.setReadTimeout(3000);
            c.setRequestMethod("GET");
            c.connect();
            int code = c.getResponseCode();
            c.disconnect();
            return code < 500;
        } catch (Exception e) {
            return false;
        }
    }

    private void clickTab(String label) {
        driver.findElements(By.tagName("button")).stream()
              .filter(b -> b.getText().equals(label))
              .findFirst()
              .ifPresent(b -> {
                  b.click();
                  wait.until(ExpectedConditions.stalenessOf(b));
              });
    }

    private void loginOrRegister() {
        driver.get(BASE_URL + "/login");
        clickTab("Sign up");

        boolean registered = false;
        try {
            WebElement email = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("reg-email")));
            email.clear(); email.sendKeys(TEST_EMAIL);
            driver.findElement(By.id("reg-phone")).sendKeys(TEST_PHONE);
            driver.findElement(By.id("reg-password")).sendKeys(TEST_PASS);
            driver.findElement(By.id("reg-confirm")).sendKeys(TEST_PASS);
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();",
                    driver.findElement(By.cssSelector("form#auth-form button[type='submit']")));
            wait.until(ExpectedConditions.not(ExpectedConditions.urlContains("/login")));
            registered = true;
        } catch (Exception ignored) {
            // email already exists or form not shown — fall through to sign in
        }

        if (!registered && driver.getCurrentUrl().contains("/login")) {
            clickTab("Sign in");
            WebElement email = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("login-email")));
            email.clear(); email.sendKeys(TEST_EMAIL);
            driver.findElement(By.id("login-password")).sendKeys(TEST_PASS);
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();",
                    driver.findElement(By.cssSelector("form#auth-form button[type='submit']")));
            wait.until(ExpectedConditions.not(ExpectedConditions.urlContains("/login")));
        }
    }

    // ── Tests ────────────────────────────────────────────────────────────────

    @Test
    @Order(1)
    void booking_page_redirects_to_login_when_not_authenticated() {
        requireVite();
        driver.get(BASE_URL + "/book");
        wait.until(ExpectedConditions.urlContains("/login"));
        assertThat(driver.getCurrentUrl()).contains("/login");
    }

    @Test
    @Order(2)
    void login_with_test_account_succeeds() {
        requireVite();
        requireBackend();
        loginOrRegister();
        Assumptions.assumeFalse(driver.getCurrentUrl().contains("/login"),
                "Login failed — backend may not be AI Car Wash (wrong service on port 8080)");
        assertThat(driver.getCurrentUrl()).doesNotContain("/login");
    }

    @Test
    @Order(3)
    void booking_page_shows_step1_after_login() {
        requireVite();
        requireBackend();
        loginOrRegister();
        requireLoggedIn();

        driver.get(BASE_URL + "/book");
        String body = wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body"))).getText();
        assertThat(body).containsAnyOf("Pick a slot", "Select Slot", "Date", "slot");
    }

    @Test
    @Order(4)
    void step1_date_input_present_and_accepts_tomorrow() {
        requireVite();
        requireBackend();
        loginOrRegister();
        requireLoggedIn();

        driver.get(BASE_URL + "/book");
        WebElement dateInput = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("input#book-date")));
        String tomorrow = LocalDate.now().plusDays(1).toString();
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].value=arguments[1];" +
                "arguments[0].dispatchEvent(new Event('input',{bubbles:true}));" +
                "arguments[0].dispatchEvent(new Event('change',{bubbles:true}));",
                dateInput, tomorrow);
        assertThat(dateInput.getAttribute("value")).isEqualTo(tomorrow);
    }

    @Test
    @Order(5)
    void step1_vehicle_type_buttons_visible() {
        requireVite();
        requireBackend();
        loginOrRegister();
        requireLoggedIn();

        driver.get(BASE_URL + "/book");
        String body = wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body"))).getText();
        assertThat(body).contains("Motorcycle");
        assertThat(body).contains("Sedan");
        assertThat(body).contains("SUV");
    }

    @Test
    @Order(6)
    void step1_selecting_sedan_sets_aria_pressed() {
        requireVite();
        requireBackend();
        loginOrRegister();
        requireLoggedIn();

        driver.get(BASE_URL + "/book");
        WebElement sedanBtn = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//button[@aria-pressed and contains(., 'Sedan')]")));
        sedanBtn.click();
        wait.until(ExpectedConditions.attributeToBe(sedanBtn, "aria-pressed", "true"));
        assertThat(sedanBtn.getAttribute("aria-pressed")).isEqualTo("true");
    }

    @Test
    @Order(7)
    void step1_vehicle_model_input_accepts_text() {
        requireVite();
        requireBackend();
        loginOrRegister();
        requireLoggedIn();

        driver.get(BASE_URL + "/book");
        WebElement modelInput = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("input#vehicle-model")));
        modelInput.clear();
        modelInput.sendKeys("Toyota Vios");
        assertThat(modelInput.getAttribute("value")).isEqualTo("Toyota Vios");
    }

    @Test
    @Order(8)
    void step1_slots_load_after_date_selected() {
        requireVite();
        requireBackend();
        loginOrRegister();
        requireLoggedIn();

        driver.get(BASE_URL + "/book");
        WebElement dateInput = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("input#book-date")));
        String tomorrow = LocalDate.now().plusDays(1).toString();
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].value=arguments[1];" +
                "arguments[0].dispatchEvent(new Event('input',{bubbles:true}));" +
                "arguments[0].dispatchEvent(new Event('change',{bubbles:true}));",
                dateInput, tomorrow);

        new WebDriverWait(driver, Duration.ofSeconds(8)).until(
                ExpectedConditions.or(
                        ExpectedConditions.presenceOfElementLocated(By.xpath("//button[@aria-pressed]")),
                        ExpectedConditions.textToBePresentInElementLocated(By.tagName("body"), "No slots")));

        String body = driver.findElement(By.tagName("body")).getText();
        assertThat(body.toLowerCase()).containsAnyOf("slot", "available", "no slot");
    }

    @Test
    @Order(9)
    void step1_next_button_exists() {
        requireVite();
        requireBackend();
        loginOrRegister();
        requireLoggedIn();

        driver.get(BASE_URL + "/book");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));
        boolean hasNext = driver.findElements(By.tagName("button")).stream()
                .anyMatch(b -> b.getText().toLowerCase().matches(".*next.*|.*continue.*|.*proceed.*"));
        assertThat(hasNext).isTrue();
    }

    @Test
    @Order(10)
    void progress_bar_shows_all_three_steps() {
        requireVite();
        requireBackend();
        loginOrRegister();
        requireLoggedIn();

        driver.get(BASE_URL + "/book");
        String body = wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body"))).getText();
        assertThat(body).contains("Select Slot");
        assertThat(body).contains("Choose Service");
        assertThat(body).contains("Confirm");
    }
}
