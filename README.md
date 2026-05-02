# Smart Society Entry Management System

## 📋 Project Overview

A full-featured Java desktop application for managing residential society gate entry/exit operations. Built with JavaFX for the UI, MS SQL Server for persistence, and pure Java SE for business logic — **no frameworks** (Spring, Hibernate, etc.) are used.

### Key Features
- **Role-based Authentication** — Admin, Guard, Resident dashboards
- **Visitor Pre-Approval** — Residents create approvals with QR codes
- **QR Code Scanning** — Guards verify visitors via webcam or manual input
- **Entry/Exit Tracking** — Real-time occupancy monitoring
- **Access Rules** — Admin defines visitor time windows and duration limits
- **Violation Management** — Automatic overstay detection and admin actions
- **Notifications** — In-app notifications for arrivals, approvals, and violations

### Architecture
```
┌─────────────────────────────────────────┐
│          UI Layer (JavaFX/FXML)         │
│    Login │ Resident │ Guard │ Admin     │
├─────────────────────────────────────────┤
│        Business Logic Layer             │
│  LoginController │ ApprovalController   │
│  GateController  │ AdminController      │
│  QRCodeService   │ NotificationService  │
├─────────────────────────────────────────┤
│          Data Access Layer (JDBC)       │
│  UserDAO │ ApprovalDAO │ EntryLogDAO    │
│  AccessRuleDAO │ ViolationDAO           │
├─────────────────────────────────────────┤
│       MS SQL Server Database            │
└─────────────────────────────────────────┘
```

### Design Patterns
| Pattern | Type | Usage |
|---------|------|-------|
| Singleton | GoF | `DatabaseConnection`, `QRCodeService` |
| Factory Method | GoF | `UserSessionFactory` — creates sessions by role |
| Observer | GoF | `NotificationService`, `OccupancyDashboard` |
| Controller | GRASP | All business controllers handle system events |
| Information Expert | GRASP | DAOs/Repositories own their domain data |
| Creator | GRASP | Controllers create domain objects |

---

## 🛠️ Prerequisites

1. **Java JDK 23.0.1** — [Download](https://adoptium.net/)
2. **JavaFX SDK 21.0.11** — [Download](https://openjfx.io/)
3. **MS SQL Server** — [Download Express](https://www.microsoft.com/en-us/sql-server/sql-server-downloads)
4. **SQL Server Management Studio (SSMS)** — [Download](https://learn.microsoft.com/en-us/sql/ssms/download-sql-server-management-studio-ssms)

---

## 📦 Required Libraries (place in `lib/` folder)

| Library | Download |
|---------|----------|
| `mssql-jdbc-12.4.2.jre11.jar` | [Maven Central](https://repo1.maven.org/maven2/com/microsoft/sqlserver/mssql-jdbc/12.4.2.jre11/) |
| `webcam-capture-0.3.12.jar` | [GitHub](https://github.com/sarxos/webcam-capture/releases) |
| `bridj-0.7.0.jar` | Included with webcam-capture |
| `slf4j-api-1.7.36.jar` | [Maven Central](https://repo1.maven.org/maven2/org/slf4j/slf4j-api/1.7.36/) |
| `slf4j-simple-1.7.36.jar` | [Maven Central](https://repo1.maven.org/maven2/org/slf4j/slf4j-simple/1.7.36/) |
| `core-3.5.3.jar` (ZXing) | [Maven Central](https://repo1.maven.org/maven2/com/google/zxing/core/3.5.3/) |
| `javase-3.5.3.jar` (ZXing) | [Maven Central](https://repo1.maven.org/maven2/com/google/zxing/javase/3.5.3/) |

> **Note**: Webcam/ZXing libraries are optional. Without them, QR scanning falls back to manual text input.

---

## 🗄️ Database Setup

1. Open **SQL Server Management Studio (SSMS)**
2. Connect to your SQL Server instance
3. Open and execute `database/schema.sql`
4. Update connection settings in `src/com/smartsociety/config/DatabaseConnection.java`:
   ```java
   private static final String SERVER   = "localhost";
   private static final String PORT     = "1433";
   private static final String DATABASE = "SmartSocietyDB";
   private static final String USER     = "sa";
   private static final String PASSWORD = "your_password_here";
   ```

### Default Test Accounts
| Username | Password | Role |
|----------|----------|------|
| admin | password123 | Admin |
| guard1 | password123 | Guard |
| guard2 | password123 | Guard |
| resident1 | password123 | Resident |
| resident2 | password123 | Resident |
| resident3 | password123 | Resident |

---

## 🚀 Running the Application

### Option 1: Using the batch script
```batch
run.bat
```

### Option 2: Command line
```bash
# Set JavaFX path
set PATH_TO_FX=C:\path\to\javafx-sdk-21\lib

# Compile
javac -d out -cp "lib/*" --module-path "%PATH_TO_FX%" --add-modules javafx.controls,javafx.fxml src/com/smartsociety/**/*.java

# Copy resources
xcopy /s /y resources\* out\

# Run
java -cp "out;lib/*" --module-path "%PATH_TO_FX%" --add-modules javafx.controls,javafx.fxml com.smartsociety.Main
```

### Option 3: IntelliJ IDEA
1. Open the project folder
2. Mark `src/` as Sources Root
3. Mark `resources/` as Resources Root
4. Add all JARs from `lib/` to project libraries
5. Add JavaFX SDK to project libraries
6. Set VM options: `--module-path "path/to/javafx/lib" --add-modules javafx.controls,javafx.fxml`
7. Run `com.smartsociety.Main`

---

## 📁 Project Structure

```
SmartSocietyEntryMS/
├── src/com/smartsociety/
│   ├── Main.java                          # Entry point
│   ├── config/DatabaseConnection.java     # Singleton DB
│   ├── model/                             # Domain objects
│   ├── factory/UserSessionFactory.java    # Factory pattern
│   ├── dao/                               # Data Access Layer
│   ├── controller/                        # Business Logic
│   ├── service/                           # Shared services
│   └── ui/                                # JavaFX UI controllers
├── resources/fxml/                        # FXML layouts
├── resources/css/glassmorphism.css        # Theme
├── database/schema.sql                    # DB script
├── lib/                                   # External JARs
├── run.bat                                # Build & run script
└── README.md
```

---

## 🧪 Testing Guide

1. **Login** — Test with `admin`, `guard1`, `resident1` (password: `password123`)
2. **Create Approval** — Login as `resident1`, go to Create Approval tab, fill form
3. **View QR** — Note the generated QR code string
4. **Guard Verify** — Login as `guard1`, paste QR code in scan field, click Verify
5. **Register Entry** — Click Register Entry after verification
6. **Register Exit** — Select active entry, click Register Exit
7. **Access Rules** — Login as `admin`, manage rules in Access Rules tab
8. **Occupancy** — Admin can see real-time occupancy and detect overstays
9. **Violations** — Admin can review violations and take actions

---

## 📄 License

This project is for educational purposes — SDA Course, Semester 4.
