# AML Compliance Management System

## Overview

The **AML Compliance Management System** is a web-based, multi-tenant platform designed to help financial institutions monitor transactions, detect potentially suspicious financial activity, investigate alerts, manage compliance cases, and prepare Suspicious Activity Reports (SAR) / Suspicious Transaction Reports (STR).

The system provides an end-to-end AML compliance workflow:

```text
Transaction Data
       ↓
Excel Batch Upload
       ↓
Data Validation
       ↓
AML Rule Engine
       ↓
Suspicious Activity Alerts
       ↓
Case Assignment
       ↓
Compliance Investigation
       ↓
SAR/STR or False Positive
       ↓
Audit & Compliance Reporting
```

The platform uses a **rule-based transaction monitoring approach**. Financial institutions upload transaction batches in a standardized Excel format, after which the AML Rule Engine evaluates transactions against configurable AML typology rules. Matching transactions generate alerts with defined severity levels for further investigation.

The system follows a **multi-tenant architecture**, allowing multiple financial institutions to operate independently on the same platform while keeping their transaction, alert, case, and user data isolated from other institutions.

### Key Users

The platform supports three primary roles:

* **AML System Admin** — Manages financial institution onboarding, AML rule configuration, platform governance, and system-wide reporting.
* **Bank Admin** — Manages transaction uploads, Compliance Officers, alert monitoring, case assignments, and institution-level reporting.
* **Compliance Officer** — Investigates assigned alerts and cases, reviews historical transaction activity, records investigation findings, and files SAR/STR reports when suspicious activity is confirmed.

### Core Capabilities

* Secure role-based authentication and access control
* Multi-tenant financial institution management
* Excel-based transaction batch ingestion
* Transaction schema and data validation
* Configurable AML Rule Engine
* Rule-based suspicious transaction detection
* Alert generation and severity classification
* Alert filtering and monitoring
* Case creation and assignment
* Compliance Officer investigation workflow
* Historical transaction and counterparty analysis
* Linked-account investigation
* Investigation notes and decision rationale
* Case escalation
* SAR/STR generation
* False-positive case closure
* Immutable audit trails
* Institutional and system-wide compliance reports
* PDF and Excel report exports
* In-platform and email notifications

### AML Detection

The Rule Engine supports configurable AML typologies and transaction-monitoring conditions such as:

* Structuring / Smurfing
* Transaction velocity
* Round-amount flagging
* Geographic risk
* PEP-related transaction patterns
* Other configurable AML typologies

Rules can be created, updated, activated, paused, and reactivated by the AML System Admin. Rule changes maintain version history, while previously generated alerts remain unaffected when a rule is paused.

### Investigation & Case Management

When suspicious transactions generate alerts, Bank Admins can assign related alerts to Compliance Officers as formal investigation cases.

Compliance Officers can review:

* Triggered AML rules
* Transaction details
* Historical transactions
* Originator and beneficiary information
* Counterparty details
* Linked accounts
* Existing customer risk ratings
* Previous case records

Investigators can document observations, evidence references, and decision rationales through an append-only case audit trail.

### Regulatory Output

After completing an investigation, a Compliance Officer can either:

**Confirm suspicious activity**

```text
Investigation
     ↓
SAR/STR Filing
     ↓
Regulatory Narrative
     ↓
Typology Classification
     ↓
Submission
     ↓
Structured SAR/STR PDF
     ↓
Case Closed – SAR Filed
```

or determine that the alert was a **false positive**:

```text
Investigation
     ↓
False Positive Rationale
     ↓
Case Closed – No Action
```

Filed SAR/STR records remain permanently linked to their originating cases.

### Audit & Compliance

The system maintains an immutable audit trail covering authentication, case assignments, status transitions, investigation notes, escalations, SAR/STR activities, and case closure events.

Transaction data, alerts, cases, and audit records are required to be retained for a minimum of five years according to the SRS requirements.

### Regulatory Alignment

The system is designed with reference to:

* FATF Recommendations
* FinCEN SAR Guidelines
* India FIU-IND STR reporting requirements, where applicable
* Basel AML Index risk assessment methodology
* GDPR / DPDP Act data-handling obligations

### Current Scope

The current version focuses on **batch-based transaction monitoring and compliance management**.

The following are intentionally outside the current scope:

* Real-time/API transaction ingestion
* Direct core-banking integration
* External PEP/sanctions database integration
* Direct electronic submission to FIU/FinCEN portals
* Mobile application
* Machine-learning-based anomaly detection
* Senior Compliance Officer maker-checker workflow
* Multi-currency FX conversion and cross-currency risk scoring

The current implementation therefore focuses on a controlled workflow of:

**Transaction Ingestion → Rule-Based Monitoring → Alerting → Investigation → Case Resolution → SAR/STR & Reporting.**
