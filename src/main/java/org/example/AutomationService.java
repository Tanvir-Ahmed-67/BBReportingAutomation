package org.example;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class AutomationService {

    private Consumer<String> outputConsumer;
    private volatile boolean shouldStop = false; // Flag to signal stopping
    private WebDriver driver; // Keep a reference to the driver

    public AutomationService(Consumer<String> outputConsumer) {
        this.outputConsumer = outputConsumer;
    }

    public void stop() {
        this.shouldStop = true;
        log("Stop signal received. Attempting to shut down automation gracefully...");
        if (driver != null) {
            driver.quit(); // Immediately quit the driver if it exists
        }
    }

    private void log(String message) {
        if (outputConsumer != null) {
            outputConsumer.accept(message);
        }
        System.out.println(message); // Keep console output for debugging
    }

    private void logError(String message, Exception e) {
        if (outputConsumer != null) {
            outputConsumer.accept("ERROR: " + message + " - " + e.getMessage());
        }
        System.err.println("ERROR: " + message);
        e.printStackTrace(); // Keep stack trace in console for debugging
    }

    public void runAutomation(File csvFile) {
        shouldStop = false; // Reset stop flag for a new run
        log("Starting automation process...");

        try {
            // Setup WebDriver for Chrome
            WebDriverManager.chromedriver().setup();

            ChromeOptions options = new ChromeOptions();
            // options.addArguments("--headless"); // Uncomment if you want to run without
            // browser UI

            driver = new ChromeDriver(options); // Assign to instance variable
            // INCREASED WAIT TIMEOUT TO 10 SECONDS to prevent TimeoutException
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10)); 

            // Check for stop signal before starting navigation
            if (shouldStop) return;

            // Navigate to the website
            String url = "https://exp.bb.org.bd/ords/f?p=117:LOGIN:0:";
            log("Navigating to: " + url);
            driver.get(url);

            // Check for stop signal
            if (shouldStop) return;

            // Wait for Username field to be visible
            WebElement usernameField = wait
                    .until(ExpectedConditions.visibilityOfElementLocated(By.id("P101_USERNAME")));
            WebElement passwordField = driver.findElement(By.id("P101_PASSWORD"));
            WebElement loginButton = driver.findElement(By.xpath("//a[contains(text(), 'Login')]"));

            // Enter credentials
            log("Entering credentials...");
            usernameField.sendKeys("agrani");
            passwordField.sendKeys("Dgmfrd#654213");

            // Check for stop signal
            if (shouldStop) return;

            // Click login
            log("Clicking Login button...");
            loginButton.click();

            // Wait for a successful login indicator (e.g., a logo or logout link)
            wait.until(ExpectedConditions.not(ExpectedConditions.urlContains("LOGIN")));

            log("Login successful! Current URL: " + driver.getCurrentUrl());

            // Check for stop signal
            if (shouldStop) return;

            // Click on "Wage Earner's Entry" button
            log("Searching for 'Wage Earner's Entry' button...");
            try {
                WebElement wageEarnerButton = wait
                        .until(ExpectedConditions.elementToBeClickable(By.xpath("//a[contains(., 'Wage Earner')]")));
                wageEarnerButton.click();
                log("Clicked Wage Earner's Entry. Current URL: " + driver.getCurrentUrl());
            } catch (Exception e) {
                logError("Could not find 'Wage Earner's Entry' button. Listing all links on the page:", e);
                driver.findElements(By.tagName("a")).forEach(link -> {
                    String text = link.getText().trim();
                    if (!text.isEmpty()) {
                        log("Link found: [" + text + "]");
                    }
                });
                throw e;
            }

            // Check for stop signal
            if (shouldStop) return;

            // Wait for the form to load
            Thread.sleep(3000);

            // Read all data from CSV
            List<Map<String, String>> allData = readCSVData(csvFile);
            log("Total records to process: " + allData.size());

            for (int i = 0; i < allData.size(); i++) {
                // Check for stop signal before processing each record
                if (shouldStop) {
                    log("Automation stopped by user before processing record " + (i + 1));
                    return;
                }

                Map<String, String> csvData = allData.get(i);
                log("\nProcessing record " + (i + 1) + " of " + allData.size());

                // 1) Inremst Id dropdown field: select value - 111000
                log("Selecting Inremst Id with value: 111000");
                WebElement inremstDropdown = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(
                        "//label[contains(.,'Inremst Id')]/following-sibling::div/select | //select[contains(@id, 'INREMST_ID')]")));
                new Select(inremstDropdown).selectByValue("112000");

                // 2) Transaction Date input field: read from CSV and convert format
                String rawDate = csvData.get("dated");
                log("Entering Transaction Date: " + rawDate);
                WebElement dateField = wait.until(ExpectedConditions.elementToBeClickable(By.xpath(
                        "//label[contains(.,'Transaction Date')]/following-sibling::div/input | //input[contains(@id, 'TRANSACTION_DATE')]")));
                dateField.clear();
                dateField.sendKeys(rawDate);

                // Fallback using JS if value is still empty
                if (dateField.getAttribute("value").isEmpty()) {
                    ((JavascriptExecutor) driver).executeScript("arguments[0].value = arguments[1];", dateField,
                            rawDate);
                    ((JavascriptExecutor) driver).executeScript("arguments[0].dispatchEvent(new Event('change'));",
                            dateField);
                }

                // Check for stop signal
                if (shouldStop) {
                    log("Automation stopped by user during record " + (i + 1));
                    return;
                }

                // 3) Currency Code dropdown: read from CSV (currency)
                log("Selecting Currency Code: " + csvData.get("currency"));
                WebElement currencyDropdown = driver.findElement(By.xpath(
                        "//label[contains(.,'Currency Code')]/following-sibling::div/select | //select[contains(@id, 'CURRENCY_CODE')]"));
                new Select(currencyDropdown).selectByVisibleText(csvData.get("currency"));

                // 4) Country Code dropdown: read from CSV (country_name)
                log("Selecting Country Code: " +
                        csvData.get("country_name"));
                WebElement countryDropdown = driver.findElement(By.xpath(
                        "//label[contains(.,'Country Code')]/following-sibling::div/select | //select[contains(@id, 'COUNTRY_CODE')]"));
                new Select(countryDropdown).selectByVisibleText(csvData.get("country_name"));

                // 5) FC Amount input field: read from CSV (amount_usd)
                String amountValue = csvData.get("amount_usd");
                if (amountValue != null) {
                    amountValue = amountValue.replaceAll("[^0-9.]", "");
                }
                log("Entering FC Amount: " + amountValue);
                WebElement amountField = wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath(
                        "//input[contains(@id, 'P8_FCAMOUNT')] | //input[contains(@name, 'P8_FCAMOUNT')] | //label[contains(.,'FC Amount')]/..//input")));

                // Focus and set value
                ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", amountField);
                Thread.sleep(300);
                amountField.clear();
                amountField.sendKeys(amountValue);

                // Force set via JS to be sure
                ((JavascriptExecutor) driver).executeScript(
                        "arguments[0].value = arguments[1];" +
                                "arguments[0].dispatchEvent(new Event('input', { bubbles: true }));" +
                                "arguments[0].dispatchEvent(new Event('change', { bubbles: true }));" +
                                "arguments[0].dispatchEvent(new Event('blur', { bubbles: true }));",
                        amountField, amountValue);

                // Check for stop signal
                if (shouldStop) {
                    log("Automation stopped by user during record " + (i + 1));
                    return;
                }

                // Click Create button
                log("Clicking Create button...");
                WebElement createButton = driver.findElement(By.xpath(
                        "//button[contains(.,'Create')] | //button[contains(@id, 'CREATE')] | //a[contains(.,'Create')]"));
                createButton.click();

                // Wait for success message "Action Processed."
                log("Waiting for success message 'Action Processed.'...");
                wait.until(ExpectedConditions
                        .visibilityOfElementLocated(By.xpath("//*[contains(text(), 'Action Processed.')]")));
                log("Record processed successfully.");

                // Short sleep between records
                Thread.sleep(1000);
            }

            log("\nTotal records successfully entered: " + allData.size());
            log("All records processed. Logging out...");
            try {
                WebElement logoutButton = wait.until(ExpectedConditions.elementToBeClickable(
                        By.xpath("//a[contains(text(), 'Logout')] | //button[contains(text(), 'Logout')]")));
                logoutButton.click();
                log("Logged out successfully.");
            } catch (Exception e) {
                logError("Could not find logout button:", e);
            }

            // Optional: Wait for a few seconds to see the result
            Thread.sleep(2000);

        } catch (Exception e) {
            if (shouldStop) {
                log("Automation was stopped by user.");
            } else {
                logError("An error occurred during automation:", e);
            }
        } finally {
            // Close the browser
            if (driver != null) {
                log("Closing browser...");
                driver.quit();
            }
            log("Automation process finished.");
        }
    }

    private List<Map<String, String>> readCSVData(File csvFile) throws IOException {
        List<Map<String, String>> allData = new ArrayList<>();
        if (!csvFile.exists()) {
            throw new IOException("The file '" + csvFile.getAbsolutePath() + "' was not found!");
        }
        try (Reader reader = new FileReader(csvFile);
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT
                     .withFirstRecordAsHeader()
                     .withIgnoreHeaderCase()
                     .withTrim())) {

            for (CSVRecord record : csvParser) {
                Map<String, String> data = new HashMap<>();
                for (String header : csvParser.getHeaderNames()) {
                    data.put(header.toLowerCase().trim(), record.get(header));
                }
                allData.add(data);
            }
        }
        return allData;
    }
}
