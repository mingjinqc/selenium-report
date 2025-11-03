# Selenium Test + GitHub Actions Report Example

What this does
- On every push to `main`, a GitHub Actions workflow runs a headless Selenium script which:
  - reads `username.json`
  - navigates to `https://login.salesforce.com/`
  - types the username -> saves `docs/step1.png`
  - clears the username -> saves `docs/step2.png`
  - writes `docs/report.html`
- The workflow commits the `docs` files back to `main`, replacing previous ones.

Files to edit
- `username.json` — put the username value here.

Run locally
1. Install Java 17+ and Maven.
2. Ensure Chrome or Chromium is installed locally.
3. From repo root run:
   ```bash
   mvn -B exec:java -Dexec.mainClass=com.example.SeleniumReportGenerator
