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
import java.util.Base64;

public class SeleniumReportGenerator {

    public static void main(String[] args) {
        Path repoRoot = Path.of("").toAbsolutePath();
        Path jsonPath = repoRoot.resolve("username.json");
        Path reportDir = repoRoot.resolve("docs");
        // Always recreate docs directory to ensure clean overwrite
        try {
            if (!Files.exists(reportDir)) Files.createDirectories(reportDir);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        String username = readUsername(jsonPath);
        if (username == null) {
            System.err.println("username not found in " + jsonPath.toString());
            return;
        }

        // Setup ChromeDriver
        WebDriverManager.chromedriver().setup();

        ChromeOptions options = new ChromeOptions();
        // Headless mode (works with modern Chrome)
        options.addArguments("--headless=new"); // use new headless mode if available; fallback works on many runners
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--disable-gpu");
        // use a user-agent to avoid uncommon detection
        options.addArguments("--user-agent=Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120 Safari/537.36");

        WebDriver driver = null;
        try {
            driver = new ChromeDriver(options);
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));

            // 1) Navigate to Salesforce login
            driver.get("https://login.salesforce.com/");

            // Wait / locate username field by id "username"
            WebElement usernameField = driver.findElement(By.id("username"));

            // Step 1: fill username
            usernameField.sendKeys(username);
            // Take screenshot step1.png
            Path step1 = reportDir.resolve("step1.png");
            takeElementOrViewportScreenshot(driver, step1.toFile());

            // Step 2: clear username and take screenshot
            usernameField.clear();
            Path step2 = reportDir.resolve("step2.png");
            takeElementOrViewportScreenshot(driver, step2.toFile());

            // generate HTML report
            Path reportHtml = reportDir.resolve("report.html");
            System.out.println("Working directory: " + repoRoot); //
            System.out.println("Writing report to: " + reportDir.toAbsolutePath()); //
            generateHtmlReport(reportHtml, step1.getFileName().toString(), step2.getFileName().toString());
            System.out.println("Working directory: " + repoRoot); //
            System.out.println("Writing report to: " + reportDir.toAbsolutePath()); //
            System.out.println("Report generated: " + reportHtml.toAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (driver != null) {
                try { driver.quit(); } catch (Exception ignored) {}
            }
        }
    }

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

    private static void takeElementOrViewportScreenshot(WebDriver driver, File outFile) throws IOException {
        // Try full page/viewport screenshot
        if (driver instanceof TakesScreenshot) {
            TakesScreenshot ts = (TakesScreenshot) driver;
            byte[] bytes = ts.getScreenshotAs(OutputType.BYTES);
            Files.write(outFile.toPath(), bytes);
        } else {
            throw new UnsupportedOperationException("Driver does not support screenshots");
        }
    }

    private static void generateHtmlReport(Path target, String step1Name, String step2Name) throws IOException {
        String html = """
                <!doctype html>
                <html lang="en">
                <head>
                  <meta charset="utf-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1">
                  <title>Test Report</title>
                  <style>
                    body { font-family: Arial, sans-serif; padding: 18px; }
                    table { border-collapse: collapse; width: 100%; max-width: 900px; }
                    th, td { border: 1px solid #ccc; padding: 8px; text-align: left; vertical-align: top; }
                    th { background: #f2f2f2; }
                    img.sshot { max-width: 320px; height: auto; border: 1px solid #999; }
                  </style>
                </head>
                <body>
                <h1>Automated Test Report</h1>
                <table>
                  <thead>
                    <tr>
                      <th>No.</th>
                      <th>Steps</th>
                      <th>Screenshot</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr>
                      <td>1.</td>
                      <td>Fill value in username field.</td>
                      <td><img class="sshot" src="%s" alt="step1" /></td>
                    </tr>
                    <tr>
                      <td>2.</td>
                      <td>Empty value in username field.</td>
                      <td><img class="sshot" src="%s" alt="step2" /></td>
                    </tr>
                    <tr>
                      <td>3.</td>
                      <td>Report generated and saved to repository.</td>
                      <td>report: %s</td>
                    </tr>
                  </tbody>
                </table>
                </body>
                </html>
                """;
        String finalHtml = String.format(html, step1Name, step2Name, target.getFileName().toString());
        Files.writeString(target, finalHtml);
    }
}
