# 🚀 NeoBank Engine – Spring Boot Banking Backend

A secure NeoBank backend engine built with Java Spring Boot, supporting account creation, deposits, withdrawals, transfers, PDF statements, notifications, and admin features.

---

## ✨ FEATURES

### 🔐 Authentication (JWT)
- Register
- Login
- Token based authentication
- Roles (USER, ADMIN)

---

### 💳 Banking Operations
- Create Account
- Deposit money
- Withdraw money
- Send money (Transfers)
- View balance
- View transactions
- Mini statement
- Full statement

---

### 📄 PDF Statement Generation

Includes:
- Logo
- Watermark
- QR verification
- Summary
- Opening/Closing balance
- Timestamp
- Reference
- Transaction list

---

### 🔔 Notification Engine (with email support)

Triggers notifications for:
- Deposit
- Withdraw
- Transfer:
  - Sender notification
  - Receiver notification

User features:
- Fetch notifications
- Mark as read
- Unread count

Optional:
- Email alerts (requires mail config)

---

### 🛡 Administrator Module
Admins can:
- View all users
- View all accounts
- Freeze account
- Unfreeze account
- Manage roles *(future update)*

---

## 🧑‍💻 Tech Stack

| Layer | Technology |
|---|---|
| Backend | Spring Boot |
| Database | MySQL |
| ORM | JPA / Hibernate |
| Security | Spring Security + JWT |
| PDF | iText |
| QR Code | ZXing |
| Mail | JavaMail |

---

## 📌 Prerequisites
- Java 21+
- Maven
- MySQL 8+
- IntelliJ or VS Code
- Internet

---

## 📦 Download / Clone
```bash
git clone https://github.com/Prem-004/NeoBank-Engine.git
```

---

## ⚙ Database Setup

```sql
CREATE DATABASE neo_bank_engine;
```

---

## Configure DB in `application.properties`

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/neo_bank_engine
spring.datasource.username=root
spring.datasource.password=YOUR_DB_PASSWORD
spring.jpa.hibernate.ddl-auto=update
```

---

▶ Run Application
-mvn clean install
-mvn spring-boot:run
-Backend starts at:
-http://localhost:8080

---

## 🔐 API Endpoints Summary

### Public
| Method | Endpoint           |
| ------ | ------------------ |
| POST   | /api/auth/register |
| POST   | /api/auth/login    |

---

### User (JWT required)
| Method | Endpoint                    |
| ------ | --------------------------- |
| POST   | /api/accounts/create        |
| GET    | /api/accounts/{id}/balance  |
| POST   | /api/accounts/{id}/deposit  |
| POST   | /api/accounts/{id}/withdraw |
| POST   | /api/accounts/transfer      |

---

### Notifications
| Method | Endpoint                        |
| ------ | ------------------------------- |
| GET    | /api/notifications              |
| GET    | /api/notifications/unread-count |
| POST   | /api/notifications/{id}/read    |

---

### PDF Statement
| Method | Endpoint                         |
| ------ | -------------------------------- |
| GET    | /api/accounts/{id}/statement/pdf |

---

### Admin (ROLE_ADMIN)
| Method | Endpoint                          |
| ------ | --------------------------------- |
| GET    | /api/admin/users                  |
| GET    | /api/admin/accounts               |
| POST   | /api/admin/accounts/{id}/freeze   |
| POST   | /api/admin/accounts/{id}/unfreeze |

---

## 👨‍💻 Roles
| Role  | Usage           |
| ----- | --------------- |
| USER  | normal banking  |
| ADMIN | admin endpoints |

---

## 📁 Project Structure
```
controller/
service/
entity/
repository/
dto/
security/
exception/
config/
```

---

## 🧩 Implemented Features (by days)
| Day | Task                           |
| --- | ------------------------------ |
| 1–3 | basic auth, JWT                |
| 4   | accounts                       |
| 5   | deposits / withdraw            |
| 6   | transfers                      |
| 7   | PDF export                     |
| 8   | PDF improvements               |
| 9   | QR code + watermark            |
| 10  | RBAC                           |
| 11  | notifications                  |
| 12  | mark read & unread count       |
| 13  | admin APIs                     |
| 14  | dashboard DTO                  |
| 15  | freeze/unfreeze                |
| 16  | GitHub deployment *(this day)* |

---

## 📎 README Purpose
- Created as part of Day-16 task  
- Documentation  
- GitHub readiness  
- Future-proof developer explanation  

---

## 🧪 Testing (Postman)
- Register User  
- Login (get token)  
- Create account  
- Deposit  
- Withdraw  
- Transfer  
- Notification list  
- PDF statement  
- Admin features  

---

## 📝 Default Data Behavior
- Account defaults to ACTIVE  
- Withdraw only if balance sufficient  
- Transfer sends notification to both parties  
- PDF returned as binary  

---

## 🔧 Optional Email Setup
```properties
spring.mail.host=smtp.gmail.com
spring.mail.username=you@gmail.com
spring.mail.password=APP_PASSWORD
spring.mail.port=587
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
```

---

## ⭐ Future Enhancements
- UI (React / Angular)  
- Mobile App (Flutter)  
- UPI/Payments  
- SMS alert  
- Graph dashboard  
- Docker deployment  
- Kubernetes  
- Microservices version  

---

## 💡 Learning Outcomes
By doing this project, you learned:
- Spring Boot architecture  
- JWT security system  
- Banking logic  
- PDF generation  
- Notifications  
- Admin modules  
- GitHub workflow  

---

## 👨‍💻 Developer
Prem Prasath  
Final Year IT  
Tamil Nadu, India 🇮🇳  

GitHub: https://github.com/Prem-004  

---

## 📃 License
This project is open for:
- learning  
- portfolio  
- personal use

