# EcoQuest (ClimateOS v2) 🌍

**A local-first, AI-driven action engine for measurable climate impact.**

This isn't a carbon calculator. It’s a mission optimization engine. We use Gemini integration combined with an offline-first Room Database state engine to find the highest-ROI behavior changes that fit your lifestyle with the lowest friction.

## The Architecture

![Architecture Ecosystem]
```text
           [ Jetpack Compose UI ] (Reactive State)
                     |
            (ViewModel / Flow)
                     |
         [ Local Room Database ] <===> [ Offline Sync & State Recovery ]
                     |
[ Gemini API (Generative AI Coach) ]  (Remote Generation when online)
```

## Resilience Engineering

We built EcoQuest differently from most hackathon projects. Every action is local-first, meaning the application stays alive even when network access drops or the Gemini API responds unpredictably.

Key Features:
* **Crash Telemetry:** Added error recovery on all network calls.
* **Schema Validation:** Graceful parsing and recovery from malformed Gemini responses using strict try/catch blocks.
* **Deterministic Flow:** You never wait for a prompt to "think" — generation is asynchronous with background message states.
* **Immediate Local State:** Actions instantly save to Room DB so data is never lost.

## Design Philosophy

**Clarity > Decoration**
**Impact First > Points Second**

We removed gamified visual noise to focus the user on real-world impact. The application features a clean, high-contrast, edge-to-edge layout that highlights CO₂ savings and future modeling.

*   No fake points.
*   No unnecessary tabs.
*   Pure action.

## Demo Video

[Link to 2-minute Demo Video - TBD]
