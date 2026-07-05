package com.waste.wastemanagement.e2e;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.waste.wastemanagement.repository.UserRepository;
import com.waste.wastemanagement.model.AppUser;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "spring.security.oauth2.client.registration.google.client-id=mock-client-id",
        "spring.security.oauth2.client.registration.google.client-secret=mock-client-secret"
    }
)
public class UserWorkflowE2ETest {

    @LocalServerPort
    private int port;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private WebDriver driver;
    private WebDriverWait wait;

    @BeforeEach
    public void setUp() {
        ensureDummyUsersExist();

        ChromeOptions options = new ChromeOptions();
        // options.addArguments("--headless=new"); // Commented out so you can watch the browser execute live!
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--remote-allow-origins=*");
        
        // Grant geolocation permission automatically in Chrome
        java.util.Map<String, Object> prefs = new java.util.HashMap<>();
        prefs.put("profile.default_content_setting_values.geolocation", 1); // 1 = Allow
        options.setExperimentalOption("prefs", prefs);

        // Configure logging preferences to capture browser console logs
        org.openqa.selenium.logging.LoggingPreferences logPrefs = new org.openqa.selenium.logging.LoggingPreferences();
        logPrefs.enable(org.openqa.selenium.logging.LogType.BROWSER, java.util.logging.Level.ALL);
        options.setCapability("goog:loggingPrefs", logPrefs);
        
        driver = new ChromeDriver(options);
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    private void ensureDummyUsersExist() {
        recreateUser("admin", "admin123", "ADMIN");
        recreateUser("worker1", "worker123", "WORKER");
        recreateUser("user1", "user123", "USER");
    }

    private void recreateUser(String username, String password, String role) {
        AppUser existing = userRepository.findByUsername(username);
        if (existing != null) {
            userRepository.delete(existing);
        }
        userRepository.save(new AppUser(null, username, passwordEncoder.encode(password), role));
    }

    @AfterEach
    public void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }

    private void jsClick(WebElement element) {
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
    }

    @Test
    public void testUserRegistrationAndLoginFlow() {
        String baseUrl = "http://localhost:" + port;
        String testUser = "e2e_user_" + System.currentTimeMillis();
        String testPassword = "securePassword123";

        try {
            // 1. Go to register page
            driver.get(baseUrl + "/register");
            System.out.println("CURRENT URL: " + driver.getCurrentUrl());
            System.out.println("PAGE TITLE: " + driver.getTitle());
            Thread.sleep(2000); // Pause to let the user see the page

            // 2. Fill in registration form
            wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("input[placeholder='Username']")));
            WebElement usernameInput = driver.findElement(By.cssSelector("input[placeholder='Username']"));
            usernameInput.clear();
            usernameInput.sendKeys(testUser);
            Thread.sleep(1000); // Pause to see typing

            WebElement passwordInput = driver.findElement(By.cssSelector("input[placeholder='Password']"));
            passwordInput.clear();
            passwordInput.sendKeys(testPassword);
            Thread.sleep(1000); // Pause to see typing

            // Click the Citizen (USER) role button to be explicit (though default is USER)
            WebElement citizenButton = driver.findElement(By.xpath("//button[contains(text(), 'Citizen')]"));
            jsClick(citizenButton);
            Thread.sleep(1000); // Pause to see click

            // Submit form
            WebElement registerButton = driver.findElement(By.xpath("//button[contains(text(), 'Register')]"));
            jsClick(registerButton);

            // 3. Verify redirected to login page after registration
            wait.until(ExpectedConditions.urlContains("/login"));
            assertTrue(driver.getCurrentUrl().endsWith("/login"), "Should redirect to login page after registration");
            Thread.sleep(2000); // Pause to let the user see the login page

            // 4. Fill login credentials
            wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("input[placeholder='Username or Email']")));
            WebElement loginUsernameInput = driver.findElement(By.cssSelector("input[placeholder='Username or Email']"));
            loginUsernameInput.clear();
            loginUsernameInput.sendKeys(testUser);
            Thread.sleep(1000);

            WebElement loginPasswordInput = driver.findElement(By.cssSelector("input[placeholder='Password']"));
            loginPasswordInput.clear();
            loginPasswordInput.sendKeys(testPassword);
            Thread.sleep(1000);

            WebElement loginButton = driver.findElement(By.xpath("//button[contains(text(), 'Login')]"));
            jsClick(loginButton);

            // 5. Verify successful login redirects to homepage (CitizenView)
            wait.until(ExpectedConditions.urlToBe(baseUrl + "/"));
            Thread.sleep(2000); // Pause to let the user see the homepage
            
            // Assert header is present
            wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//h1[contains(text(), 'Report a Waste Issue')]")));
            WebElement header = driver.findElement(By.xpath("//h1[contains(text(), 'Report a Waste Issue')]"));
            assertTrue(header.getText().contains("Report a Waste Issue"), "Header should indicate we are on the complaint submission page");
        } catch (Exception e) {
            System.out.println("TEST FAILED. CURRENT URL: " + driver.getCurrentUrl());
            System.out.println("PAGE SOURCE:\n" + driver.getPageSource());
            try {
                System.out.println("BROWSER LOGS:");
                driver.manage().logs().get("browser").forEach(System.out::println);
            } catch (Exception logEx) {
                System.out.println("Failed to fetch browser logs: " + logEx.getMessage());
            }
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testRoleBasedPageSecurity() {
        String baseUrl = "http://localhost:" + port;

        try {
            // 1. Log in as Worker
            driver.get(baseUrl + "/login");
            wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("input[placeholder='Username or Email']")));
            
            WebElement userIn = driver.findElement(By.cssSelector("input[placeholder='Username or Email']"));
            userIn.clear();
            userIn.sendKeys("worker1");
            
            WebElement passIn = driver.findElement(By.cssSelector("input[placeholder='Password']"));
            passIn.clear();
            passIn.sendKeys("worker123");
            Thread.sleep(1000);
            
            WebElement loginButton = driver.findElement(By.xpath("//button[contains(text(), 'Login')]"));
            jsClick(loginButton);

            // 2. Verify redirected to worker dashboard
            wait.until(ExpectedConditions.urlToBe(baseUrl + "/worker"));
            assertTrue(driver.getCurrentUrl().endsWith("/worker"), "Worker should land on /worker");
            Thread.sleep(2000);

            // 3. Try to access /admin page directly (Should be blocked by Spring Security 403)
            driver.get(baseUrl + "/admin");
            Thread.sleep(2000);
            
            // Assert that the page source indicates a 403 Forbidden or Access Denied
            String pageSource = driver.getPageSource().toLowerCase();
            assertTrue(pageSource.contains("forbidden") || pageSource.contains("403") || pageSource.contains("access denied"),
                    "Worker should be blocked from accessing /admin with a 403 Forbidden response");

            // 4. Clear cookies to log out the worker, then log in as Admin
            driver.manage().deleteAllCookies();
            driver.get(baseUrl + "/login");
            wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("input[placeholder='Username or Email']")));
            
            WebElement adminIn = driver.findElement(By.cssSelector("input[placeholder='Username or Email']"));
            adminIn.clear();
            adminIn.sendKeys("admin");
            
            WebElement adminPass = driver.findElement(By.cssSelector("input[placeholder='Password']"));
            adminPass.clear();
            adminPass.sendKeys("admin123");
            Thread.sleep(1000);
            
            WebElement loginButtonAdmin = driver.findElement(By.xpath("//button[contains(text(), 'Login')]"));
            jsClick(loginButtonAdmin);

            // 5. Verify Admin can successfully load /admin
            wait.until(ExpectedConditions.urlToBe(baseUrl + "/admin"));
            assertTrue(driver.getCurrentUrl().endsWith("/admin"), "Admin should be able to view /admin");
            Thread.sleep(2000);
        } catch (Exception e) {
            System.out.println("TEST ROLE SECURITY FAILED. CURRENT URL: " + driver.getCurrentUrl());
            System.out.println("PAGE SOURCE:\n" + driver.getPageSource());
            try {
                System.out.println("BROWSER LOGS:");
                driver.manage().logs().get("browser").forEach(System.out::println);
            } catch (Exception logEx) {
                System.out.println("Failed to fetch browser logs: " + logEx.getMessage());
            }
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testEndToEndSystemWorkflow() {
        String baseUrl = "http://localhost:" + port;
        String uniqueIssue = "E2E Test Waste Complaint: " + System.currentTimeMillis();

        try {
            // ==========================================
            // Step 1: Citizen (User) logs in and files a complaint with GPS location
            // ==========================================
            driver.get(baseUrl + "/login");
            wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("input[placeholder='Username or Email']")));
            
            WebElement userIn = driver.findElement(By.cssSelector("input[placeholder='Username or Email']"));
            userIn.clear();
            userIn.sendKeys("user1");
            
            WebElement passIn = driver.findElement(By.cssSelector("input[placeholder='Password']"));
            passIn.clear();
            passIn.sendKeys("user123");
            
            WebElement loginButton = driver.findElement(By.xpath("//button[contains(text(), 'Login')]"));
            jsClick(loginButton);

            // Wait for Citizen homepage (/)
            wait.until(ExpectedConditions.urlToBe(baseUrl + "/"));
            wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("input[placeholder='Full name']")));
            Thread.sleep(2000);

            // Fill out complaint details
            WebElement fullNameInput = driver.findElement(By.cssSelector("input[placeholder='Full name']"));
            fullNameInput.clear();
            fullNameInput.sendKeys("Test User");
            
            // Click GPS location button on user page
            WebElement gpsButton = driver.findElement(By.cssSelector("button[title='Use my GPS location']"));
            jsClick(gpsButton);
            Thread.sleep(2000); // Wait for geolocation retrieval

            // Wait until location field is populated by GPS
            WebElement locationInput = driver.findElement(By.cssSelector("input[placeholder='Type address or click the map']"));
            wait.until(d -> !locationInput.getAttribute("value").isEmpty());
            System.out.println("RETRIEVED GPS LOCATION: " + locationInput.getAttribute("value"));

            WebElement issueArea = driver.findElement(By.tagName("textarea"));
            issueArea.clear();
            issueArea.sendKeys(uniqueIssue);
            Thread.sleep(2000);

            // Click submit
            WebElement submitBtn = driver.findElement(By.xpath("//button[contains(text(), 'Submit Complaint')]"));
            jsClick(submitBtn);

            // Verify success banner is shown
            wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//div[contains(text(), 'submitted successfully')]")));
            Thread.sleep(2500);

            // Log out
            WebElement logoutBtn = driver.findElement(By.xpath("//button[contains(text(), 'Logout')]"));
            jsClick(logoutBtn);
            wait.until(ExpectedConditions.urlContains("/login"));
            Thread.sleep(1500);

            // ==========================================
            // Step 2: Worker logs in to set location (GPS tracking) and logs out
            // ==========================================
            driver.manage().deleteAllCookies();
            driver.get(baseUrl + "/login");
            wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("input[placeholder='Username or Email']")));
            
            WebElement workerIn = driver.findElement(By.cssSelector("input[placeholder='Username or Email']"));
            workerIn.clear();
            workerIn.sendKeys("worker1");
            
            WebElement workerPass = driver.findElement(By.cssSelector("input[placeholder='Password']"));
            workerPass.clear();
            workerPass.sendKeys("worker123");
            
            WebElement loginBtnWorker = driver.findElement(By.xpath("//button[contains(text(), 'Login')]"));
            jsClick(loginBtnWorker);

            // Wait for Worker dashboard (/worker)
            wait.until(ExpectedConditions.urlToBe(baseUrl + "/worker"));
            Thread.sleep(2500);

            // Click "Start GPS Tracking"
            WebElement startGpsBtn = driver.findElement(By.xpath("//button[contains(text(), 'Start GPS Tracking')]"));
            jsClick(startGpsBtn);
            Thread.sleep(3000); // Wait for position tracking post request

            // Log out
            WebElement logoutBtnWorker = driver.findElement(By.xpath("//button[contains(text(), 'Logout')]"));
            jsClick(logoutBtnWorker);
            wait.until(ExpectedConditions.urlContains("/login"));
            Thread.sleep(1500);

            // ==========================================
            // Step 3: Admin logs in and assigns complaint to worker1
            // ==========================================
            driver.manage().deleteAllCookies();
            driver.get(baseUrl + "/login");
            wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("input[placeholder='Username or Email']")));
            
            WebElement adminIn = driver.findElement(By.cssSelector("input[placeholder='Username or Email']"));
            adminIn.clear();
            adminIn.sendKeys("admin");
            
            WebElement adminPass = driver.findElement(By.cssSelector("input[placeholder='Password']"));
            adminPass.clear();
            adminPass.sendKeys("admin123");
            
            WebElement loginBtnAdmin = driver.findElement(By.xpath("//button[contains(text(), 'Login')]"));
            jsClick(loginBtnAdmin);

            // Wait for Admin dashboard (/admin)
            wait.until(ExpectedConditions.urlToBe(baseUrl + "/admin"));
            Thread.sleep(2500);

            // Locate select element in the table row containing the unique issue description
            WebElement selectElement = wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.xpath("//tr[contains(., '" + uniqueIssue + "')]//select")
            ));
            
            // Assign to worker1
            Select workerSelect = new Select(selectElement);
            workerSelect.selectByValue("worker1");
            Thread.sleep(3000); // Wait for the assignment API call and refresh

            // Log out
            WebElement logoutBtnAdmin = driver.findElement(By.xpath("//button[contains(text(), 'Logout')]"));
            jsClick(logoutBtnAdmin);
            wait.until(ExpectedConditions.urlContains("/login"));
            Thread.sleep(1500);

            // ==========================================
            // Step 4: Worker logs in and completes/marks task as cleaned
            // ==========================================
            driver.manage().deleteAllCookies();
            driver.get(baseUrl + "/login");
            wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("input[placeholder='Username or Email']")));
            
            WebElement workerIn2 = driver.findElement(By.cssSelector("input[placeholder='Username or Email']"));
            workerIn2.clear();
            workerIn2.sendKeys("worker1");
            
            WebElement workerPass2 = driver.findElement(By.cssSelector("input[placeholder='Password']"));
            workerPass2.clear();
            workerPass2.sendKeys("worker123");
            
            WebElement loginBtnWorker2 = driver.findElement(By.xpath("//button[contains(text(), 'Login')]"));
            jsClick(loginBtnWorker2);

            // Wait for Worker dashboard (/worker)
            wait.until(ExpectedConditions.urlToBe(baseUrl + "/worker"));
            Thread.sleep(2500);

            // Find the Mark Cleaned button in the card containing our unique issue
            WebElement cleanButton = wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.xpath("//div[contains(., '" + uniqueIssue + "')]//button[contains(text(), 'Mark Cleaned')]")
            ));
            jsClick(cleanButton);
            
            // Wait for completion status to show as "Done"
            wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.xpath("//div[contains(., '" + uniqueIssue + "')]//span[contains(text(), 'Done')]")
            ));
            Thread.sleep(2500);

            // Log out
            WebElement logoutBtnWorker2 = driver.findElement(By.xpath("//button[contains(text(), 'Logout')]"));
            jsClick(logoutBtnWorker2);
            wait.until(ExpectedConditions.urlContains("/login"));
            Thread.sleep(1500);

            // ==========================================
            // Step 5: Admin logs in again and verifies task is CLEANED
            // ==========================================
            driver.manage().deleteAllCookies();
            driver.get(baseUrl + "/login");
            wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("input[placeholder='Username or Email']")));
            
            WebElement adminIn2 = driver.findElement(By.cssSelector("input[placeholder='Username or Email']"));
            adminIn2.clear();
            adminIn2.sendKeys("admin");
            
            WebElement adminPass2 = driver.findElement(By.cssSelector("input[placeholder='Password']"));
            adminPass2.clear();
            adminPass2.sendKeys("admin123");
            
            WebElement loginBtnAdmin2 = driver.findElement(By.xpath("//button[contains(text(), 'Login')]"));
            jsClick(loginBtnAdmin2);

            // Wait for Admin dashboard (/admin)
            wait.until(ExpectedConditions.urlToBe(baseUrl + "/admin"));
            Thread.sleep(2500);

            // Find the status badge for our unique issue row
            WebElement statusBadge = wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.xpath("//tr[contains(., '" + uniqueIssue + "')]//span[contains(@class, 'rounded-full')]")
            ));
            
            // Verify that the status text is indeed "CLEANED"
            assertTrue(statusBadge.getText().contains("CLEANED"), "Task status should be CLEANED in Admin Dashboard");
            Thread.sleep(2500);
            
            // Log out
            WebElement logoutBtnAdmin2 = driver.findElement(By.xpath("//button[contains(text(), 'Logout')]"));
            jsClick(logoutBtnAdmin2);
        } catch (Exception e) {
            System.out.println("E2E WORKFLOW TEST FAILED. CURRENT URL: " + driver.getCurrentUrl());
            System.out.println("PAGE SOURCE:\n" + driver.getPageSource());
            try {
                System.out.println("BROWSER LOGS:");
                driver.manage().logs().get("browser").forEach(System.out::println);
            } catch (Exception logEx) {
                System.out.println("Failed to fetch browser logs: " + logEx.getMessage());
            }
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new RuntimeException(e);
        }
    }
}
