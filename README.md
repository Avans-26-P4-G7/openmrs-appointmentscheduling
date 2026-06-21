Appointment Scheduling Module
==========================
[![Build Status](https://travis-ci.org/openmrs/openmrs-module-appointmentscheduling.svg?branch=master)](https://travis-ci.org/openmrs/openmrs-module-appointmentscheduling)

## Overview

The Appointment Scheduling Module is for scheduling patient appointments and managing provider schedules. This module also allows for managing the patient queue in a clinic. This README contains information primarily pertinent to developers. If you are a user looking for user related instruction, navigate to the [Wiki page section](#wiki) of this documentation.

<br>

## File Tree

* api/			- This folder contains all Appointment Scheduling API java and test files.
* omod/			- This folder contains all of the module's java and test files.
* .gitattributes	- Lists git attributes that were changed from default.
* .gitignore		- Lists files to be ignored when pushing to git.
* .travis.yml		- Configures Travis CI for automated testing.
* LICENSE.txt		- OpenMRS license agreement.
* OpenMRSFormatter.xml	- OpenMRS formatting file.
* README.md		- Describes the Appointment Scheduling module.
* pom.xml		- Used for building the project with maven.

<br>

## Build Instructions

If your module's file tree is set up correctly [(see section above)](#file-tree), building and packaging the app is a simple process thanks to maven. Navigate to the module's root directory, and run the command `mvn package`. This will package the application according to maven's typical package instructions. The packaged module will be available in /omod/target

<br>

## Wiki

The wiki page for the Appointment Scheduling module contains information more pertinent to users. To view this information, navigate to the following [link](https://wiki.openmrs.org/display/docs/Appointment+Scheduling+Module).



# Beveiligingsbeleid en procedures

Dit document beschrijft het beveiligingsbeleid, de deployment-procedure en het secrets-beheer voor het project van Avans 26 P4 G7. Het dient als aantoonbare compliance-documentatie voor een auditor (ISO 27001 / NEN 7510 controls 8.4, 8.5, 8.8, 8.9, 8.16, 8.25, 8.28, 8.29, 8.31, 8.32, 8.33).

---

## 1. Branch-policy

### 1.1 Beschermde branches

De repository kent drie beschermde branches, elk met eigen branch protection rules:

| Branch | Doel |
|---|---|
| `development` | Actieve ontwikkeling door teamleden |
| `test` | Integratietesten en QA |
| `production` | Productieomgeving, alleen stabiele releases |

### 1.2 Regels voor alle beschermde branches

Directe pushes naar `development`, `test` of `production` zijn geblokkeerd. Wijzigingen mogen uitsluitend via een Pull Request (PR) worden samengevoegd. Voor elke PR gelden de volgende verplichtingen:

- Minimaal een goedkeuring van een andere teamgenoot (geen self-review).
- Alle verplichte status checks moeten slagen voordat de PR gemerged mag worden. De volgende checks zijn vereist:
  - **Build & Test** (GitHub Actions): de applicatie moet succesvol bouwen en alle geautomatiseerde tests moeten slagen.
  - **CodeQL Security Scan** (GitHub Actions): er mogen geen nieuwe kritieke of hoge kwetsbaarheden worden geintroduceerd.
  - **Dependency Review** (GitHub Actions): PR's die afhankelijkheden introduceren met een CVSS-score van 7 of hoger worden automatisch geblokkeerd.
- De branch moet up-to-date zijn met de doelbranch voordat er gemerged mag worden.

### 1.3 Verantwoordelijkheden

Ieder teamlid is verantwoordelijk voor het aanmaken van feature branches vanuit `development`. Merges naar `test` en `production` worden gecoordineerd door de teamleden met reviewer-rechten.

---

## 2. Deployment-procedure

### 2.1 Omgevingen (OTAP)

Het project maakt gebruik van drie gescheiden GitHub Environments:

| Omgeving | Branch | Approval vereist |
|---|---|---|
| `development` | `development` | Nee |
| `test` | `test` | Nee |
| `production` | `production` | Ja (zie 2.2) |

### 2.2 Approval-gate voor productie

Deployments naar de `production`-omgeving vereisen expliciete goedkeuring van een aangewezen reviewer voordat de workflow verder mag gaan. De volgende regels zijn van kracht:

- Verplichte reviewers zijn ingesteld op de `production`-omgeving in GitHub (Settings > Environments > production > Required reviewers).
- Self-review is uitgeschakeld: de persoon die de deployment triggert kan zichzelf niet goedkeuren.
- Administrators mogen de protection rules bypassen, maar dit dient alleen in noodgevallen te gebeuren en wordt geregistreerd in het Audit Log.

### 2.3 Stappen voor een release naar productie

1. Maak een PR aan van `test` naar `production`.
2. Wacht tot alle status checks (Build & Test, CodeQL, Dependency Review) zijn geslaagd.
3. Vraag goedkeuring aan een verplichte reviewer.
4. Na goedkeuring wordt de merge en de deployment automatisch uitgevoerd door de CI/CD-pipeline.

---

## 3. Secrets-beheer

### 3.1 Principe van scheiding

Secrets worden nooit gedeeld tussen omgevingen. Elke omgeving heeft eigen, gescheiden secrets die uitsluitend beschikbaar zijn voor workflows die op die omgeving draaien.

| Secret | Omgeving |
|---|---|
| `DB_URL` | `development` |
| `DB_URL` | `test` |
| `DB_URL` | `production` |

### 3.2 Opslaan en beheren van secrets

- Secrets worden uitsluitend opgeslagen als GitHub Environment Secrets (Settings > Secrets and variables > Actions > Environment secrets).
- Secrets worden nooit in broncode, configuratiebestanden of commentaar opgenomen.
- Secrets worden nooit gelogd in workflow-output.
- Bij vermoeden van een lek wordt het betreffende secret onmiddellijk geroteerd en wordt het GitHub Secret Scanning-systeem geraadpleegd.

### 3.3 Secret Scanning

GitHub Secret Scanning is actief op de repository. GitHub detecteert per ongeluk gecommitte secrets (zoals API-sleutels en wachtwoorden) en stuurt een melding. Historische commits zijn eveneens gescand.

---

## 4. Toegangsbeheer en authenticatie

### 4.1 Multi-factor authenticatie (MFA)

MFA is verplicht voor alle leden van de GitHub-organisatie Avans 26 P4 G7. Alleen veilige tweede-factorvarianten zijn toegestaan: authenticator-apps, passkeys, security keys en de GitHub Mobile-app. SMS is uitdrukkelijk uitgesloten. Leden die geen MFA hebben ingesteld, krijgen geen toegang tot de organisatieresources.

### 4.2 Toegang tot omgevingen

- Toegang tot de GitHub-organisatie en repositories is beperkt tot actieve teamleden.
- Deployment naar `production` vereist daarnaast expliciete goedkeuring van een aangewezen reviewer (zie sectie 2.2).

---

## 5. Kwetsbaarheidsbeheer

### 5.1 Statische code-analyse (SAST)

CodeQL wordt automatisch uitgevoerd bij elke push en elke PR. Gevonden kwetsbaarheden worden gerapporteerd in het Security-tabblad van de repository. Een PR mag niet worden gemerged zolang de CodeQL-check niet is geslaagd.

### 5.2 Software Composition Analysis (SCA)

Dependabot Alerts is actief. Dependabot detecteert kwetsbaarheden in third-party dependencies en maakt automatisch PR's aan met een voorgestelde update. Deze PR's vereisen handmatige review en goedkeuring door een teamlid voordat ze worden gemerged.

### 5.3 Dependency Review

De Dependency Review Action (`.github/workflows/dependency-review.yml`) blokkeert automatisch PR's die nieuwe afhankelijkheden introduceren met een CVSS-score van 7 of hoger. Dit geldt voor alle branches.

### 5.4 Software Bill of Materials (SBOM)

De CycloneDX Maven-plugin genereert bij elke CI-run een actuele SBOM als artifact. De SBOM biedt een volledige inventaris van alle gebruikte dependencies en hun versies. De SBOM wordt momenteel niet automatisch geanalyseerd op nieuwe CVEs; dit is een bekend restrisico.

---

## 6. Audit Log

Het GitHub Organisation Audit Log registreert alle organisatieacties: wie heeft wat gedaan en wanneer. Het log is exporteerbaar en wordt aan het einde van het project geexporteerd als aantoonbaar bewijs. Het log wordt niet actief gemonitord op afwijkingen; dit is een bekend restrisico.

---

## 7. Restrisico-overzicht

| Control | Restrisico |
|---|---|
| 8.8 SAST | CodeQL dekt niet alle kwetsbaarheidstypen. Aanvullend Snyk is ingericht. |
| 8.8 SCA | Dependabot maakt automatisch PR's aan maar mergt niet zelf. Handmatige review vereist. |
| 8.8 Dependency Review | Controleer of de workflow PR's daadwerkelijk blokkeert bij CVSS >= 7. |
| 8.8 SBOM | SBOM wordt gegenereerd maar nog niet automatisch geanalyseerd op nieuwe CVEs. |
| 8.16 Audit Log | Het log wordt niet actief gemonitord op afwijkingen. |

