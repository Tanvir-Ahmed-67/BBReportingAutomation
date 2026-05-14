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

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main {
    public static void main(String[] args) {
        // Setup WebDriver for Chrome
        WebDriverManager.chromedriver().setup();

        ChromeOptions options = new ChromeOptions();
        // options.addArguments("--headless"); // Uncomment if you want to run without
        // browser UI

        WebDriver driver = new ChromeDriver(options);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));

        try {
            // Navigate to the website
            String url = "https://exp.bb.org.bd/ords/f?p=117:LOGIN:0:";
            System.out.println("Navigating to: " + url);
            driver.get(url);

            // Wait for Username field to be visible
            WebElement usernameField = wait
                    .until(ExpectedConditions.visibilityOfElementLocated(By.id("P101_USERNAME")));
            WebElement passwordField = driver.findElement(By.id("P101_PASSWORD"));
            WebElement loginButton = driver.findElement(By.xpath("//a[contains(text(), 'Login')]"));

            // Enter credentials
            System.out.println("Entering credentials...");
            usernameField.sendKeys("agrani");
            passwordField.sendKeys("Dgmfrd#654213");

            // Click login
            System.out.println("Clicking Login button...");
            loginButton.click();

            // Wait for a successful login indicator (e.g., a logo or logout link)
            // For now, we'll just wait for the URL to change or a specific element on the
            // dashboard
            wait.until(ExpectedConditions.not(ExpectedConditions.urlContains("LOGIN")));

            System.out.println("Login successful! Current URL: " + driver.getCurrentUrl());

            // Click on "Wage Earner's Entry" button
            System.out.println("Searching for 'Wage Earner's Entry' button...");
            try {
                WebElement wageEarnerButton = wait
                        .until(ExpectedConditions.elementToBeClickable(By.xpath("//a[contains(., 'Wage Earner')]")));
                wageEarnerButton.click();
                System.out.println("Clicked Wage Earner's Entry. Current URL: " + driver.getCurrentUrl());
            } catch (Exception e) {
                System.err.println("Could not find button by exact text. Listing all links on the page:");
                driver.findElements(By.tagName("a")).forEach(link -> {
                    String text = link.getText().trim();
                    if (!text.isEmpty()) {
                        System.out.println("Link found: [" + text + "]");
                    }
                });
                throw e;
            }

            // Wait for the form to load
            Thread.sleep(5000);

            // Read all data from CSV
            List<Map<String, String>> allData = readCSVData("test.csv");
            System.out.println("Total records to process: " + allData.size());

            for (int i = 0; i < allData.size(); i++) {
                Map<String, String> csvData = allData.get(i);
                System.out.println("\nProcessing record " + (i + 1) + " of " + allData.size());

                // 1) Inremst Id dropdown field: select value - 111000
                System.out.println("Selecting Inremst Id with value: 111000");
                WebElement inremstDropdown = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(
                        "//label[contains(.,'Inremst Id')]/following-sibling::div/select | //select[contains(@id, 'INREMST_ID')]")));
                new Select(inremstDropdown).selectByValue("112000");

                // 2) Transaction Date input field: read from CSV and convert format
                String rawDate = csvData.get("dated");
                System.out.println("Entering Transaction Date: " + rawDate);
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

                // 3) Currency Code dropdown: read from CSV (currency)
                System.out.println("Selecting Currency Code: " + csvData.get("currency"));
                WebElement currencyDropdown = driver.findElement(By.xpath(
                        "//label[contains(.,'Currency Code')]/following-sibling::div/select | //select[contains(@id, 'CURRENCY_CODE')]"));
                new Select(currencyDropdown).selectByVisibleText(csvData.get("currency"));

                // 4) Country Code dropdown: read from CSV (country_name)
                System.out.println("Selecting Country Code: " +
                        csvData.get("country_name"));
                WebElement countryDropdown = driver.findElement(By.xpath(
                        "//label[contains(.,'Country Code')]/following-sibling::div/select | //select[contains(@id, 'COUNTRY_CODE')]"));
                new Select(countryDropdown).selectByVisibleText(csvData.get("country_name"));

                // 5) FC Amount input field: read from CSV (amount_usd)
                String amountValue = csvData.get("amount_usd");
                if (amountValue != null) {
                    amountValue = amountValue.replaceAll("[^0-9.]", "");
                }
                System.out.println("Entering FC Amount: " + amountValue);
                WebElement amountField = wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath(
                        "//input[contains(@id, 'P8_FCAMOUNT')] | //input[contains(@name, 'P8_FCAMOUNT')] | //label[contains(.,'FC Amount')]/..//input")));

                // Focus and set value
                ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", amountField);
                Thread.sleep(500);
                amountField.clear();
                amountField.sendKeys(amountValue);

                // Force set via JS to be sure
                ((JavascriptExecutor) driver).executeScript(
                        "arguments[0].value = arguments[1];" +
                                "arguments[0].dispatchEvent(new Event('input', { bubbles: true }));" +
                                "arguments[0].dispatchEvent(new Event('change', { bubbles: true }));" +
                                "arguments[0].dispatchEvent(new Event('blur', { bubbles: true }));",
                        amountField, amountValue);

                // Click Create button
                System.out.println("Clicking Create button...");
                WebElement createButton = driver.findElement(By.xpath(
                        "//button[contains(.,'Create')] | //button[contains(@id, 'CREATE')] | //a[contains(.,'Create')]"));
                createButton.click();

                // Wait for success message "Action Processed."
                System.out.println("Waiting for success message 'Action Processed.'...");
                wait.until(ExpectedConditions
                        .visibilityOfElementLocated(By.xpath("//*[contains(text(), 'Action Processed.')]")));
                System.out.println("Record processed successfully.");

                // Short sleep between records
                Thread.sleep(2000);
            }

            System.out.println("\nTotal records successfully entered: " + allData.size());
            System.out.println("All records processed. Logging out...");
            try {
                WebElement logoutButton = wait.until(ExpectedConditions.elementToBeClickable(
                        By.xpath("//a[contains(text(), 'Logout')] | //button[contains(text(), 'Logout')]")));
                logoutButton.click();
                System.out.println("Logged out successfully.");
            } catch (Exception e) {
                System.err.println("Could not find logout button: " + e.getMessage());
            }

            // Optional: Wait for a few seconds to see the result
            Thread.sleep(5000);

        } catch (Exception e) {
            System.err.println("An error occurred during automation: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Close the browser
            System.out.println("Closing browser...");
            driver.quit();
        }
    }

    private static List<Map<String, String>> readCSVData(String filePath) throws IOException {
        List<Map<String, String>> allData = new ArrayList<>();
        try (Reader reader = new FileReader(filePath);
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