# App Risk Auditor: Scoring Rubric

All scoring runs on-device and offline. Every point comes from a named signal, and every signal produces a plain-language reason shown to the user. Weights live in one file (`RiskWeights.kt`).

## 1. Bands
| Score | Band | Meaning shown to the user |
|---|---|---|
| 0-24 | Low | Nothing here looks unusual |
| 25-49 | Medium | Worth a look if you don't use the app much |
| 50-74 | High | Powerful access. Review it |
| 75-100 | Critical | Can do serious harm. Review it now |

## 2. Signal weights (points)
| Category | Signal | Points |
|---|---|---|
| **Special access** (cap 45) | Accessibility service **enabled** / declared only | 30 / 10 |
| | Device admin **active** | 25 |
| | Notification listener **enabled** | 20 |
| | Screen overlay granted (with a strong signal) / otherwise | 20 / 8 |
| | Install-unknown-apps declared (sideloaded) / otherwise | 15 / 5 |
| | Usage access granted / declared only | 10 / 5 |
| **SMS** (cap 25) | READ_SMS or RECEIVE_SMS granted (each) / SEND_SMS | 15 / 10 |
| **Contacts and call logs** (cap 15) | READ_CALL_LOG / READ_CONTACTS | 10 / 8 |
| **Camera and microphone** (cap 10) | CAMERA, RECORD_AUDIO (each) | 6 |
| **Location** (cap 12) | Fine / background | 5 / 8 |
| **Phone and identity** (cap 8) | READ_PHONE_STATE, READ_PHONE_NUMBERS / CALL_PHONE | 3 / 4 |
| **Storage and media** | MANAGE_EXTERNAL_STORAGE / each media permission (cap 8) | 12 / 3 |
| **Background** (cap 8) | Boot, foreground service, ignore battery optimisations | 3 / 2 / 4 |
| **Network** | INTERNET | 0 alone, used only in combinations |

Rules applied to the weights:
- A dangerous permission that is **declared but not granted** counts at **25%**.
- All plain runtime-permission categories together contribute at most **35** points.
- An app with **no strong signal** is capped at **49 (Medium)**. Strong signals: accessibility enabled, device admin active (non-allowlisted), notification listener enabled (non-system app), any combination rule, a context mismatch, or a sideloaded app with sensitive granted access.
- Default Messages and Phone apps get no points for the SMS and call permissions they must have.

## 3. Multipliers
| Condition | Effect |
|---|---|
| Installer unknown or sideloaded | x 1.25 |
| System preinstalled app | x 0.5 |
| Well-known publisher installed from Play Store or preinstalled | x 0.6 |
| Play Store or other store | x 1.0 |

## 4. Combination rules
| Rule | Bonus |
|---|---|
| Accessibility (enabled or declared) + screen overlay | +20 |
| SMS read + internet + boot persistence + sideloaded | +25 |
| SMS read + notification listener enabled (one-time-passcode capture) | +15 |
| Device admin active on an app that is not a known security or MDM tool | +20 |
| Install-unknown-apps + sideloaded | +10 |

## 5. Context mismatch
If the app name or package suggests a simple utility (torch, flashlight, calculator, wallpaper, keyboard, QR, scanner, compass, ringtone) and it holds SMS, contacts or call log, microphone, or accessibility access: **+20**, or **+25** for SMS or accessibility.

## 6. Final score and explanations
Score = min(100, round(total)). Reasons are sorted by points. Every High or Critical app shows at least three reasons in plain language, so the user can see why it was flagged and what to do (open app settings, uninstall, or switch off the special access).
