---
name: Bug report
about: Create a report to help us improve
title: ''
labels: bug:unconfirmed
assignees: ''

---

# How to contribute

In order to help diagnose and fix your issue, we often need your "debug information". It's easy to save this information and attach it to a GitHub issue:

  * Start UMS.
  * Make sure you are looking at the "old" settings, which looks like this:
    <img width="711" height="173" alt="Image" src="https://github.com/user-attachments/assets/644118e8-7571-49f7-8442-aa895f9d7cab" />
  * Go to the `Logs` tab.
  * Select `Trace` from the `Log Level` dropdown at the bottom (or set `log_level = TRACE` in `UMS.conf`).
  * Reproduce the problem.
  * Click `Pack debug files` on the lower left.
  * Click `Zip selected files`.
  * Save the zip file to a location you will remember.
  * Attach the zip file to the GitHub issue.

Additionally, if the problem occurred in the browser, it can be useful to know about any browser console errors, so please paste them into the issue as well.

## Thank you for your participation
