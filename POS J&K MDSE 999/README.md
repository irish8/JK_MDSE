J&K MDSE Store (999) Point of Sale

 Run it without installing anything

1. Open this project folder in File Explorer.
2. Double-click `index.html`. It opens the POS in your default web browser.
3. Sign in with "admin" / "Admin999!".

No Maven, Java, Node.js, server, or database install is needed for this offline version. Add products (including product pictures) in "Products", then create cashier accounts in "Team".

Included features

- Admin and cashier sign-in with role-specific screens and sign out
- Admin user creation, editing, role changes, and activation/deactivation
- Product search, pictures, add/edit/remove, stock tracking, and low-stock alerts
- Cart quantity controls, fixed/percentage discounts, payment validation, and change
- Stock reduction when a sale is completed
- Printable receipts, searchable transaction history, and cashier/date filters
- Daily, weekly, monthly, and yearly sales summaries
- Responsive layout for desktop and smaller screens

 Important storage note

This no-install version uses **HTML, CSS, and JavaScript** and keeps users, products, images, and sales in the browser’s local storage. Data stays in that browser profile on that computer; it is not shared with other computers or browser profiles. Avoid clearing the browser’s site data if you need to keep your records. The login and role controls are intended for a single-computer store/demo; because there is no server, they are not suitable for protecting sensitive data from someone with access to the computer.

 MySQL database and Java edition

The project also retains the Java/Spring Boot source and paste-ready MySQL database schema for a server-connected edition:

- [Database schema](database/jk_mdse_pos.sql)
- [ERD source](ERD.md)
- [Editable draw.io ERD](POS-ERD.drawio)
- [ERD picture](POS-ERD.svg)

The browser-only `index.html` does **not** connect to MySQL. Browsers cannot connect directly to MySQL safely, so using MySQL requires the Java backend plus a running MySQL service. The offline version is the one to open when you want to run the POS without installing software.
