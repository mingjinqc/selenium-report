# Selenium Test + GitHub Actions Report Example

## What This Does
On every edit and commit to `main` that changes `username.json`,  a GitHub Actions workflow automatically runs a headless Selenium test that:

1. Reads the username from `username.json`
2. Opens `https://login.salesforce.com/` in a headless Chrome browser
3. Enters the username into the username field → saves as `docs/step1.png`
4. Clears the username field → saves as `docs/step2.png`
5. Generates a test report → saves as `docs/report.html`
6. Commits and pushes these files (`report.html`, `step1.png`, `step2.png`) back to the `main` branch, replacing any previous ones

---

## Files to Edit
- **`username.json`** — put the username value here, e.g.:
  ```json
  {
    "username": "testuser@example.com"
  }

---

## How to Try It Yourself
1. Fork this repository.
2. Edit `username.json` with any new username value.
3. Commit the change to the `main` branch.
4. After the GitHub Action finishes, open the generated test report:
   - https://<your-username>.github.io/<your-repo-name>/report.html
     (Make sure GitHub Pages is enabled on the main branch → /docs folder.)

✅ Tip: Every time you commit a new `username.json`, the report and screenshots in `/docs` will be automatically updated.
