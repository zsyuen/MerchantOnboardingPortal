# Merchant Onboarding Portal

## Project Overview
The Merchant Onboarding Portal is a full-stack application designed to manage the merchant onboarding process. It enables merchants to submit applications and allows bank officers to log in, view/manage applications, manage admins and configure facial recognition thresholds.

## Technology Stack Used
- **Frontend:** Angular 20, TypeScript, WebGL/FaMediaPipe (for face detection/landmarks), QRCode.
- **Backend:** Java 21, Spring Boot 3.5.5 (Spring Web, Spring Data JPA, Validation).
- **Database:** PostgreSQL.
- **Additional Tools:** SMTP4Dev (for local mock email delivery).

## Prerequisites and Required Software
- **Node.js & npm**
- **Angular CLI** (`npm install -g @angular/cli`)
- **Java Development Kit (JDK) 21**
- **Maven** (or use the provided `mvnw` wrapper in the backend directory)
- **PostgreSQL** (running on default port 5432)
- **SMTP4Dev** (running locally on port 25)
- **Authenticator App** (e.g., Google Authenticator, Microsoft Authenticator, Authy) on your mobile device for Bank Officer Two-Factor Authentication (2FA) login.

## Database Setup
1. Ensure your local PostgreSQL server is running.
2. Create a new database named `merchantdb`.
3. The backend expects the following credentials by default in `application.properties`:
   *(Update `merchant-portal-backend/src/main/resources/application.properties` if your credentials differ).*
4. The database tables will be automatically generated upon starting the backend application, as Hibernate is configured to `update` the schema.

## Setup and Installation Steps

### Backend
1. Open a terminal and navigate to the `merchant-portal-backend` directory.
2. Download dependencies and build the project:
   ```bash
   ./mvnw clean install
   ```

### Frontend
1. Open a terminal and navigate to the `merchant-portal-frontend` directory.
2. Install the necessary Node modules:
   ```bash
   npm install
   ```

## How to Run the Application
1. Ensure your **PostgreSQL** database and **SMTP4Dev** are actively running.
2. Start the **backend** application:
   ```bash
   cd merchant-portal-backend
   ./mvnw spring-boot:run
   ```
   *The backend will run on `http://localhost:8080`.*
3. Start the **frontend** development server:
   ```bash
   cd merchant-portal-frontend
   npm start
   ```
4. Access the web interface by navigating to `http://localhost:4200/` in your browser.

## Important Notes & Known Limitations
- **Face Landmark Models:** The frontend utilizes facial recognition models stored in `public/assets/models/`. These paths must not be altered, or the `face-api.js` integration will fail.
- **Email Server:** SMTP4Dev must be running on port 25 to capture system emails. Otherwise, backend registration/notification flows might encounter connection errors.