# Personal Budget Tracker

A full-stack web application designed to help users take control of their personal finances by tracking income, expenses, budgets, savings goals, and wallets seamlessly.

## Tech Stack
- **Backend**: Spring Boot 3.2.x (Java 17+)
- **Database**: MySQL 8.x with Spring Data JPA / Hibernate
- **Security**: Spring Security + JWT (JSON Web Tokens)
- **Frontend**: HTML5, CSS3, Vanilla JavaScript
- **Charts**: Chart.js 4.x

## Features Implemented
1. **User Authentication**: Secure token-based sessions, automated default setup upon registration.
2. **Interactive Dashboard**: Total metrics, monthly comparisons, visualizations, and financial health score.
3. **Transaction Management**: Full CRUD, advanced filtering, receipt file uploads, and voice entry.
4. **Budget Management**: Category limits with progress tracking and visual alerts.
5. **Savings Goals**: Goal tracking with one-click funding.
6. **Multiple Wallets**: Support for diverse wallets and inter-wallet transfers.
7. **Analytics**: Trend analysis and calendar view for transactions.
8. **Reports & Data Export**: PDF, CSV, and Excel exports.
9. **Customization**: Dark mode, currency selection, and custom categories.
10. **Admin Panel**: Role-based access control, platform statistics, and user management.

## Setup Instructions
1. Clone the repository.
2. Ensure you have MySQL running and create a database named `budget_tracker`.
3. Rename `application-example.properties` to `application.properties` and add your database credentials and Gmail SMTP details.
4. Build and run the backend using Maven: `mvn spring-boot:run`.
5. Access the app at `http://localhost:8080`.

## Default Admin Credentials
When you run the application for the first time, an admin user is automatically created:
- **Email**: `admin@budgettracker.com`
- **Password**: `Admin@123`
