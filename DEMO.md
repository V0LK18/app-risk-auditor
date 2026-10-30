# 3-Minute Demo Script: App Risk Auditor

## Before you go on (2 minutes of prep)
- APK installed on the organiser's device. Open the app once and let the first scan finish.
- Know which 2 or 3 apps on that device score highest. Open the home screen and note them.
- Keep **Include system apps** off.
- Flight mode on is fine. Everything works offline.

## 0:00 to 0:30 Setup (the problem)
"Banking trojans don't need exotic exploits. They abuse accessibility, overlays, and notification access. These are special settings most people never check. This app answers three questions: which apps can do the most harm, why, and what can I do right now."

Show the home screen: total apps scanned, the device summary line (accessibility, device admin and notification access counts).

## 0:30 to 1:15 Q1: Which apps can do the most harm?
- Point at the **Top 3 riskiest apps** strip.
- Scroll the sorted list. Show the colour bands (Low, Medium, High, Critical).
- "The list is ranked by a 0 to 100 score. Nothing leaves the phone."

## 1:15 to 2:15 Q2: Why is it flagged?
- Tap the highest-scoring app on the test kit.
- Show **Why flagged**: "These reasons are in plain language, sorted by how much each adds."
- Show the **Special access** panel: "Switched ON and only requested look different. ON is far worse."
- Open a category (SMS, for example): "Granted versus declared, with each permission's protection level."
- If a combination rule or context mismatch fires, call it out: "A flashlight app should not read texts."

## 2:15 to 2:45 Q3: What can I do right now?
- Scroll to **What you can do now**.
- Tap **Open app settings** to show it lands on the right page, then come back.
- Point at Uninstall, and at the shortcuts (accessibility, notification access, device admin). "If an app is a device admin it can't be uninstalled until that is switched off, and the app tells you so."

## 2:45 to 3:00 Close
"Offline, on-device, no root, no data leaves the phone, and every score is explained by the rubric. Weights and combination rules are in RUBRIC.md."

## If something goes wrong
- **App list empty or slow:** toggle Include system apps off and on to rescan.
- **Top app looks wrong:** open the next one. Every flagged app has a Why flagged panel.
- **App crashes:** reopen it. Scans are fast, and the second launch usually works.
- **A judge asks how the score works:** open RUBRIC.md, point at the weights table and the combination rules.
