-- ============================================================
-- Smart Society Entry Management System
-- Database Schema for Microsoft SQL Server
-- ============================================================

-- Create database
IF NOT EXISTS (SELECT name FROM sys.databases WHERE name = 'SmartSocietyDB')
BEGIN
    CREATE DATABASE SmartSocietyDB;
END
GO

USE SmartSocietyDB;
GO

-- ============================================================
-- 1. Users Table
-- Stores Admin, Guard, and Resident accounts
-- ============================================================
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'Users')
BEGIN
    CREATE TABLE Users (
        user_id        INT IDENTITY(1,1) PRIMARY KEY,
        username       VARCHAR(50)  NOT NULL UNIQUE,
        password_hash  VARCHAR(255) NOT NULL,
        role           VARCHAR(20)  NOT NULL CHECK (role IN ('ADMIN', 'GUARD', 'RESIDENT')),
        full_name      VARCHAR(100) NOT NULL,
        unit_number    VARCHAR(20)  NULL,        -- Only for residents
        contact        VARCHAR(50)  NULL,
        qr_code        VARCHAR(255) NULL,        -- Resident QR code for gate entry
        status         VARCHAR(20)  DEFAULT 'OUTSIDE' CHECK (status IN ('INSIDE', 'OUTSIDE')),
        created_at     DATETIME     DEFAULT GETDATE()
    );
END
GO

-- ============================================================
-- 2. Approvals Table
-- Visitor pre-approvals created by residents
-- ============================================================
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'Approvals')
BEGIN
    CREATE TABLE Approvals (
        approval_id       INT IDENTITY(1,1) PRIMARY KEY,
        resident_id       INT          NOT NULL,
        visitor_name      VARCHAR(100) NOT NULL,
        visitor_contact   VARCHAR(50)  NULL,
        category          VARCHAR(50)  NOT NULL,   -- e.g., Guest, Delivery, Service
        purpose           VARCHAR(255) NULL,
        visit_date        DATE         NOT NULL,
        time_window_start TIME         NOT NULL,
        time_window_end   TIME         NOT NULL,
        duration_minutes  INT          DEFAULT 60,
        qr_code           VARCHAR(255) NULL,
        status            VARCHAR(20)  DEFAULT 'PENDING'
                          CHECK (status IN ('PENDING','APPROVED','ENTERED','COMPLETED','CANCELLED','REJECTED')),
        created_at        DATETIME     DEFAULT GETDATE(),
        CONSTRAINT FK_Approvals_Resident FOREIGN KEY (resident_id) REFERENCES Users(user_id)
    );
END
GO

-- ============================================================
-- 3. EntryLogs Table
-- Tracks all entry/exit events for visitors and residents
-- ============================================================
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'EntryLogs')
BEGIN
    CREATE TABLE EntryLogs (
        log_id           INT IDENTITY(1,1) PRIMARY KEY,
        person_type      VARCHAR(20)  NOT NULL CHECK (person_type IN ('VISITOR','RESIDENT')),
        person_id        INT          NOT NULL,       -- user_id for residents, approval_id for visitors
        approval_id      INT          NULL,
        entry_timestamp  DATETIME     NOT NULL DEFAULT GETDATE(),
        exit_timestamp   DATETIME     NULL,
        category         VARCHAR(50)  NULL,
        guard_id         INT          NULL,
        status           VARCHAR(20)  DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','EXITED')),
        CONSTRAINT FK_EntryLogs_Approval FOREIGN KEY (approval_id) REFERENCES Approvals(approval_id),
        CONSTRAINT FK_EntryLogs_Guard    FOREIGN KEY (guard_id)    REFERENCES Users(user_id)
    );
END
GO

-- ============================================================
-- 4. AccessRules Table
-- Society-level visitor access rules defined by admin
-- ============================================================
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'AccessRules')
BEGIN
    CREATE TABLE AccessRules (
        rule_id              INT IDENTITY(1,1) PRIMARY KEY,
        rule_name            VARCHAR(100) NOT NULL,
        category             VARCHAR(50)  NOT NULL,    -- Guest, Delivery, Service, etc.
        allowed_start_time   TIME         NOT NULL,
        allowed_end_time     TIME         NOT NULL,
        max_duration_minutes INT          DEFAULT 120,
        max_visitors_per_day INT          DEFAULT 10,
        is_active            BIT          DEFAULT 1,
        created_by           INT          NULL,
        created_at           DATETIME     DEFAULT GETDATE(),
        CONSTRAINT FK_AccessRules_Admin FOREIGN KEY (created_by) REFERENCES Users(user_id)
    );
END
GO

-- ============================================================
-- 5. Violations Table
-- Detected violations (overstay, unauthorized entry, etc.)
-- ============================================================
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'Violations')
BEGIN
    CREATE TABLE Violations (
        violation_id    INT IDENTITY(1,1) PRIMARY KEY,
        log_id          INT          NOT NULL,
        violation_type  VARCHAR(30)  NOT NULL CHECK (violation_type IN ('OVERSTAY','UNAUTHORIZED','BLACKLISTED')),
        description     VARCHAR(500) NULL,
        action_taken    VARCHAR(50)  NULL CHECK (action_taken IN ('WARNING','FINE','BLACKLIST', NULL)),
        action_by       INT          NULL,
        status          VARCHAR(20)  DEFAULT 'PENDING' CHECK (status IN ('PENDING','WARNING','RESOLVED')),
        detected_at     DATETIME     DEFAULT GETDATE(),
        resolved_at     DATETIME     NULL,
        CONSTRAINT FK_Violations_Log   FOREIGN KEY (log_id)     REFERENCES EntryLogs(log_id),
        CONSTRAINT FK_Violations_Admin FOREIGN KEY (action_by)  REFERENCES Users(user_id)
    );
END
GO

-- ============================================================
-- 6. Notifications Table
-- In-app notification messages
-- ============================================================
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'Notifications')
BEGIN
    CREATE TABLE Notifications (
        notification_id INT IDENTITY(1,1) PRIMARY KEY,
        recipient_id    INT          NOT NULL,
        message         VARCHAR(500) NOT NULL,
        type            VARCHAR(30)  DEFAULT 'INFO',
        is_read         BIT          DEFAULT 0,
        created_at      DATETIME     DEFAULT GETDATE(),
        CONSTRAINT FK_Notifications_User FOREIGN KEY (recipient_id) REFERENCES Users(user_id)
    );
END
GO

-- ============================================================
-- 7. ArrivalRequests Table
-- Guard-to-resident approval requests for walk-in visitors
-- ============================================================
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'ArrivalRequests')
BEGIN
    CREATE TABLE ArrivalRequests (
        request_id      INT IDENTITY(1,1) PRIMARY KEY,
        guard_id        INT          NOT NULL,
        resident_id     INT          NOT NULL,
        visitor_name    VARCHAR(100) NOT NULL,
        visitor_purpose VARCHAR(255) NULL,
        status          VARCHAR(20)  DEFAULT 'PENDING' CHECK (status IN ('PENDING','APPROVED','REJECTED')),
        created_at      DATETIME     DEFAULT GETDATE(),
        responded_at    DATETIME     NULL,
        CONSTRAINT FK_ArrivalReq_Guard    FOREIGN KEY (guard_id)    REFERENCES Users(user_id),
        CONSTRAINT FK_ArrivalReq_Resident FOREIGN KEY (resident_id) REFERENCES Users(user_id)
    );
END
GO

-- ============================================================
-- Indexes for performance
-- ============================================================
CREATE NONCLUSTERED INDEX IX_Approvals_ResidentID    ON Approvals(resident_id);
CREATE NONCLUSTERED INDEX IX_Approvals_Status        ON Approvals(status);
CREATE NONCLUSTERED INDEX IX_Approvals_VisitDate     ON Approvals(visit_date);
CREATE NONCLUSTERED INDEX IX_EntryLogs_Status        ON EntryLogs(status);
CREATE NONCLUSTERED INDEX IX_EntryLogs_PersonType    ON EntryLogs(person_type, person_id);
CREATE NONCLUSTERED INDEX IX_Violations_Status       ON Violations(status);
CREATE NONCLUSTERED INDEX IX_Notifications_Recipient ON Notifications(recipient_id, is_read);
CREATE NONCLUSTERED INDEX IX_ArrivalReq_Resident     ON ArrivalRequests(resident_id, status);
GO

-- ============================================================
-- Seed Data: Default users for testing
-- Password for all: "password123" (plain text for demo)
-- ============================================================
-- Admin account
IF NOT EXISTS (SELECT 1 FROM Users WHERE username = 'admin')
BEGIN
    INSERT INTO Users (username, password_hash, role, full_name, contact)
    VALUES ('admin', 'password123', 'ADMIN', 'System Administrator', '0300-1234567');
END

-- Guard accounts
IF NOT EXISTS (SELECT 1 FROM Users WHERE username = 'guard1')
BEGIN
    INSERT INTO Users (username, password_hash, role, full_name, contact)
    VALUES ('guard1', 'password123', 'GUARD', 'Ahmed Khan', '0301-2345678');
END

IF NOT EXISTS (SELECT 1 FROM Users WHERE username = 'guard2')
BEGIN
    INSERT INTO Users (username, password_hash, role, full_name, contact)
    VALUES ('guard2', 'password123', 'GUARD', 'Ali Hassan', '0302-3456789');
END

-- Resident accounts
IF NOT EXISTS (SELECT 1 FROM Users WHERE username = 'resident1')
BEGIN
    INSERT INTO Users (username, password_hash, role, full_name, unit_number, contact, qr_code)
    VALUES ('resident1', 'password123', 'RESIDENT', 'Rana Hanan', 'A-101', '0311-1111111', 'RES-QR-1001');
END

IF NOT EXISTS (SELECT 1 FROM Users WHERE username = 'resident2')
BEGIN
    INSERT INTO Users (username, password_hash, role, full_name, unit_number, contact, qr_code)
    VALUES ('resident2', 'password123', 'RESIDENT', 'Sara Ahmed', 'B-205', '0312-2222222', 'RES-QR-1002');
END

IF NOT EXISTS (SELECT 1 FROM Users WHERE username = 'resident3')
BEGIN
    INSERT INTO Users (username, password_hash, role, full_name, unit_number, contact, qr_code)
    VALUES ('resident3', 'password123', 'RESIDENT', 'Usman Ali', 'C-310', '0313-3333333', 'RES-QR-1003');
END

-- Default access rules
IF NOT EXISTS (SELECT 1 FROM AccessRules WHERE rule_name = 'Guest Default')
BEGIN
    INSERT INTO AccessRules (rule_name, category, allowed_start_time, allowed_end_time, max_duration_minutes, max_visitors_per_day, created_by)
    VALUES ('Guest Default', 'Guest', '08:00', '22:00', 180, 5, 1);
END

IF NOT EXISTS (SELECT 1 FROM AccessRules WHERE rule_name = 'Delivery Default')
BEGIN
    INSERT INTO AccessRules (rule_name, category, allowed_start_time, allowed_end_time, max_duration_minutes, max_visitors_per_day, created_by)
    VALUES ('Delivery Default', 'Delivery', '09:00', '18:00', 30, 20, 1);
END

IF NOT EXISTS (SELECT 1 FROM AccessRules WHERE rule_name = 'Service Default')
BEGIN
    INSERT INTO AccessRules (rule_name, category, allowed_start_time, allowed_end_time, max_duration_minutes, max_visitors_per_day, created_by)
    VALUES ('Service Default', 'Service', '08:00', '20:00', 120, 10, 1);
END
GO

PRINT 'Smart Society Entry Management System database schema created successfully.';
PRINT 'Default users created: admin, guard1, guard2, resident1, resident2, resident3';
PRINT 'Default password for all users: password123';
GO
