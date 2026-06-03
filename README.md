# Merchant Service Portal (MSP)

## Project Overview
The Merchant Service Portal (MSP) is a comprehensive full-stack application designed to manage merchant services. The architecture consists of three main components:
1. A **Frontend** application built with Angular.
2. A **Backend** built with Java and Spring Boot to handle core business logic, API requests, security (JWT & MFA), and database interactions.
3. A **Python Sidecar** utilizing FastAPI and Machine Learning libraries (XGBoost, Prophet) to handle advanced analytics, forecasting, and churn prediction.

## Technology Stack Used
* **Frontend**: Angular (v21), Chart.js (for analytics visualization), jsPDF (for report generation), TypeScript.
* **Backend**: Java 17, Spring Boot (v4), Spring Security, JSON Web Tokens (JWT), Google Authenticator (TOTP for MFA), OpenPDF, Maven.
* **Data Science / Sidecar**: Python, FastAPI, Uvicorn, Pandas, scikit-learn, XGBoost, Prophet, SHAP, SQLAlchemy.
* **Database**: PostgreSQL.

## Prerequisites and Required Software
To run this project locally, ensure you have the following installed:
* **Java**: JDK 17
* **Node.js**: Node.js and npm (package manager configured for npm v11+)
* **Python**: Python 3.8+ and pip
* **Database**: PostgreSQL server running locally
* **Build Tool**: Maven (optional, as the project includes a Maven wrapper)

## Database Setup
1. Install and start your local PostgreSQL server running on the default port `5432`.
2. Create a new database named `msp_db`.
3. The application is configured to connect with the following default credentials. If your local setup differs, please update the `backend/src/main/resources/application.properties` file:
   ```properties
   spring.datasource.url=jdbc:postgresql://localhost:5432/msp_db
   spring.datasource.username=postgres
   spring.datasource.password=postgres

## Setup and Installation Steps
1. **Clone the Repository**:
   ```bash
   git clone <repository_url>

2. **Backend Setup**:
    - Navigate to the backend directory:
      ```bash
      cd backend
      ```
    - Build the project using Maven:
      ```bash
      ./mvnw clean install
      ```
    - Run the Spring Boot application:
      ```bash
      ./mvnw spring-boot:run
      ```

3. **Python Sidecar Setup**:
    - Navigate to the sidecar directory:
      ```bash
      cd python-sidecar
      ```
    - Create a virtual environment and activate it:
      ```bash
      python -m venv venv
      source venv/bin/activate  # On Windows: venv\Scripts\activate
      ```
    - Install the required Python packages:
      ```bash
      pip install -r requirements.txt
      ```
    - Run the FastAPI application:
      ```bash
      uvicorn main:app --reload --host 0.0.0.0 --port 8000
      ```
4. **Frontend Setup**:
    - Navigate to the frontend directory:
      ```bash
      cd frontend
      ```
    - Install the required Node.js packages:
      ```bash
      npm install
      ```
    - Run the Angular application:
      ```bash
      ng serve
      ```
