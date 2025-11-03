package com.example;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

public class SeleniumReportGenerator {

    public static void main(String[] args) {
        Path repoRoot = Path.of("").toAbsolutePath();
        Path jsonPath = repoRoot.resolve("username.json");
        Path reportDir = repoRoot.resolve("docs");

        // Ensure docs folder exists
        try {
            if (!Files.exists(reportDir)) Files.createDirectories(reportDir);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        String username = readUsername(jsonPath);
        if (username == null) {
            System.err.println("❌ username not found in " + jsonPath.toString());
            return;
        }

        // Setup ChromeDriver
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--disable-gpu");
        options.addArguments("--user-agent=Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120 Safari/537.36");

        WebDriver driver = null;
        boolean isFilled = false;
        boolean isCleared = false;

        try {
            driver = new ChromeDriver(options);
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));

            // Step 1: Navigate
            driver.get("https://login.salesforce.com/");
            WebElement usernameField = driver.findElement(By.id("username"));

            // Step 2: Fill username
            usernameField.sendKeys(username);
            String filledValue = usernameField.getAttribute("value");
            isFilled = filledValue.equals(username);
            System.out.println("✅ Verification 1 - Field filled correctly: " + isFilled);

            // Take screenshot for Step 1
            Path step1 = reportDir.resolve("step1.png");
            takeScreenshot(driver, step1.toFile());

            // Step 3: Clear username
            usernameField.clear();
            String clearedValue = usernameField.getAttribute("value");
            isCleared = clearedValue.isEmpty();
            System.out.println("✅ Verification 2 - Field cleared correctly: " + isCleared);

            // Take screenshot for Step 2
            Path step2 = reportDir.resolve("step2.png");
            takeScreenshot(driver, step2.toFile());

            // Step 4: Generate HTML report
            Path reportHtml = reportDir.resolve("report.html");
            generateHtmlReport(reportHtml, step1.getFileName().toString(), step2.getFileName().toString(), isFilled, isCleared);

            System.out.println("📄 Report generated: " + reportHtml.toAbsolutePath());

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (driver != null) {
                try { driver.quit(); } catch (Exception ignored) {}
            }
        }
    }

    // Read username from JSON file
    private static String readUsername(Path jsonPath) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(jsonPath.toFile());
            if (root.has("username")) return root.get("username").asText();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // Capture viewport screenshot
    private static void takeScreenshot(WebDriver driver, File outFile) throws IOException {
        if (driver instanceof TakesScreenshot ts) {
            byte[] bytes = ts.getScreenshotAs(OutputType.BYTES);
            Files.write(outFile.toPath(), bytes);
        } else {
            throw new UnsupportedOperationException("Driver does not support screenshots");
        }
    }

    // Generate HTML report with pass/fail status
    private static void generateHtmlReport(Path target, String step1Name, String step2Name, boolean filledOk, boolean clearedOk) throws IOException {
        String html = """
            <!doctype html>
            <html lang="en">
            <head>
              <meta charset="utf-8">
              <meta name="viewport" content="width=device-width, initial-scale=1">
              <title>Test Report</title>
              <style>
                body { font-family: Arial, sans-serif; padding: 18px; }
                table { border-collapse: collapse; width: 100%%; max-width: 900px; }
                th, td { border: 1px solid #ccc; padding: 8px; text-align: left; vertical-align: top; }
                th { background: #f2f2f2; }
                .pass { color: green; font-weight: bold; }
                .fail { color: red; font-weight: bold; }
                img.sshot { max-width: 320px; height: auto; border: 1px solid #999; }
              </style>
            </head>
            <body>
            <h1>Automated Selenium Test Report</h1>
            <table>
              <thead>
                <tr><th>No.</th><th>Step</th><th>Result</th><th>Screenshot</th></tr>
              </thead>
              <tbody>
                <tr>
                  <td>1.</td>
                  <td>Fill value in username field.</td>
                  <td class="%s">%s</td>
                  <td><img class="sshot" src="%s" alt="step1" /></td>
                </tr>
                <tr>
                  <td>2.</td>
                  <td>Empty value in username field.</td>
                  <td class="%s">%s</td>
                  <td><img class="sshot" src="%s" alt="step2" /></td>
                </tr>
              </tbody>
            </table>
            <p>Generated automatically by <b>SeleniumReportGenerator</b>.</p>
            </body>
            </html>
            """;

        String finalHtml = String.format(
            html,
            filledOk ? "pass" : "fail", filledOk ? "PASS" : "FAIL", step1Name,
            clearedOk ? "pass" : "fail", clearedOk ? "PASS" : "FAIL", step2Name
        );

        Files.writeString(target, finalHtml,
            java.nio.file.StandardOpenOption.CREATE,
            java.nio.file.StandardOpenOption.TRUNCATE_EXISTING);
    }
}
