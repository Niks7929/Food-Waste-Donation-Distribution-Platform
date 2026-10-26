# Food Waste Donation & Distribution Platform

> An enterprise, real-world full-stack web application designed to eliminate commercial food waste by connecting restaurants, banquet halls, hotels, messes, hostels, and markets with verified NGOs, community kitchens, and volunteer delivery couriers.

---

## 1. Executive Summary & Objective

Every day, vast quantities of fresh, hygienic, edible food are discarded by commercial kitchens and banquet operations while millions suffer from malnutrition. The **Food Waste Donation & Distribution Platform** solves this logistical and temporal mismatch with real-time food surplus discovery, concurrency-safe quantity reservations, automated expiry processing, and end-to-end delivery tracking.

### Core Value Proposition
- **Perishable Food Lifecycles:** Real-time countdowns against strict pickup deadlines.
- **Race Condition Prevention:** Database pessimistic write locks ensure concurrent NGO requests cannot over-reserve limited food quantities.
- **Verification & Governance:** Mandatory administrative license approval for all commercial donors and NGO relief partners before food transactions begin.
- **Audited Deliveries:** Every handoff is timestamped, confirmed, and immutably recorded in system audit logs.
- **Safety First Principle:** Prominent disclaimers, dietary classifications (Veg/Non-Veg/Vegan), and allergen notifications ensure recipient shelters serve only safe food.

---

## 2. Technology Stack

| Layer | Technologies |
|---|---|
| **Backend Framework** | Java 21, Spring Boot 3.3.4 |
| **Persistence & ORM** | Spring Data JPA, Hibernate 6, HikariCP |
| **Database & Migrations**| MySQL 9.1 / 8.x (WAMP) & PostgreSQL support, Flyway 10.10.0 |
| **Security & Auth** | Spring Security 6, BCrypt Password Encoder, CSRF Protection |
| **Templating & UI** | Thymeleaf, Thymeleaf Extras Spring Security 6 |
| **Frontend Styling** | Bootstrap 5.3.3, Bootstrap Icons 1.11.3 |
| **Data Analytics** | Chart.js 4.4.2 (Interactive Canvas Charts) |
| **Testing** | JUnit 5, Mockito, Spring Boot Test (26 Automated Tests, 100% Pass) |
| **Build & Tooling** | Apache Maven 3.9+, Maven Wrapper (`./mvnw`), `run.bat` |

---

## 3. User Roles & Capabilities

The platform implements 4 strict role-based access control (RBAC) personas:

```
                  +-----------------------------------+
                  |         Platform Admin            |
                  |  (Verifications, Logs, Reports)   |
                  +-----------------+-----------------+
                                    |
            +-----------------------+-----------------------+
            |                                               |
+-----------v-----------+                       +-----------v-----------+
|      Food Donor       |   Publishes Surplus   |     NGO Partner       |
|  (Restaurants, Messes,|---------------------->|  (Shelters, Kitchens, |
|   Banquet Lawns)      |                       |   Orphanages)         |
+-----------+-----------+                       +-----------+-----------+
            |                                               |
            |            Handoff & Courier Delivery         |
            +-------------------> + <-----------------------+
                                  |
                      +-----------v-----------+
                      |   Volunteer Courier   |
                      |  (Transits & Handoff) |
                      +-----------------------+
```

### 1. Platform Super Administrator (`ROLE_ADMIN`)
- **Dashboard:** Real-time metrics for total registrations, pending organizations, active food listings, and completed delivery volume.
- **Organization Management:** Review, approve, reject, or suspend commercial donor businesses and charitable trusts.
- **User Directory:** Audit all user accounts and toggle operational status (`PENDING`, `APPROVED`, `SUSPENDED`).
- **Global Food Audit:** Inspect every listed food donation across the platform.
- **Delivery Oversight & Volunteer Assignment:** Assign available volunteer couriers to approved pickup orders.
- **Analytics & Impact Reports:** Chart.js visualizations for food volume, categories, donor contributions, and fulfillment.
- **System Activity Logs:** Immutable audit trail of every status change, registration, and review.

### 2. Food Donor (`ROLE_DONOR`)
- **Commercial Profile:** Manage business name, FSSAI / municipal food license number, address, and coordinator phone.
- **Create Food Donation:** Specify title, category, veg/non-veg status, quantity (meals, kg, packets), preparation time, pickup deadline, allergen declarations, and optional packaging image.
- **Manage Listings:** Edit available listings prior to reservation; cancel listings when necessary.
- **Inbound Request Review:** Review requests submitted by verified NGOs with one-click **Approve** or **Reject** actions.
- **Handoff Tracking:** Monitor courier pickup and completion.

### 3. NGO Relief Partner (`ROLE_NGO`)
- **Charity Profile:** Register society/trust identification number and kitchen drop-off addresses.
- **Food Directory:** Search and filter available surplus batches by keyword, category, dietary type (Veg/Non-Veg), minimum quantity, and city.
- **Atomic Reservation:** Request partial or full batch portions. The platform uses database write-locks to deduct available quantities atomically.
- **Live Tracking:** Track status transitions (`PENDING` &rarr; `APPROVED` &rarr; `ASSIGNED` &rarr; `PICKED_UP` &rarr; `IN_TRANSIT` &rarr; `COMPLETED`).
- **Distribution Receipts:** View audit summaries of past meal distributions for compliance reporting.

### 4. Volunteer Courier (`ROLE_VOLUNTEER`)
- **Courier Hub:** View active delivery tasks assigned by administrators.
- **Open Market:** Claim unassigned delivery tasks in their neighborhood.
- **Task Route Details:** Direct contact phone numbers and addresses for both donor origin and NGO destination.
- **Milestone Progress Updates:** Update status through the state machine:
  $$\text{ASSIGNED} \longrightarrow \text{PICKED\_UP} \longrightarrow \text{IN\_TRANSIT} \longrightarrow \text{DELIVERED}$$
- **Delivery Notes & Sign-off:** Record recipient coordinator name and proof reference upon handoff.

---

## 4. Architecture & State Machine

```
FoodDonation Lifecycle:
[AVAILABLE] <----> [PARTIALLY_RESERVED] ----> [RESERVED] ----> [COLLECTED] ----> [DELIVERED]
     |                     |                      |
     +----------+----------+                      |
                | (Past Pickup Deadline)          | (Cancelled by Donor)
                v                                 v
            [EXPIRED]                        [CANCELLED]

PickupRequest Lifecycle:
[PENDING] ----> [APPROVED] ----> [ASSIGNED] ----> [PICKED_UP] ----> [IN_TRANSIT] ----> [COMPLETED]
    |
    +---------> [REJECTED] (Restores reserved food quantity back to listing)
    |
    +---------> [CANCELLED] (Cancelled by NGO before review; restores quantity)
```

### Concurrency Protection & Overbooking Prevention
When an NGO submits a pickup request, the service layer invokes:
```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT d FROM FoodDonation d WHERE d.id = :id")
Optional<FoodDonation> findByIdWithLock(@Param("id") Long id);
```
This acquires a PostgreSQL row-level lock (`SELECT ... FOR UPDATE`), preventing concurrent requests from racing on the same inventory. If `requestedQuantity > availableQuantity`, the transaction immediately aborts with a user-friendly error.

---

## 5. Directory Structure

```
Food Waste Donation & Distribution Platform/
├── pom.xml
├── .gitignore
├── .env.example
├── README.md
├── uploads/
│   └── images/
│       └── .gitkeep
├── src/
│   ├── main/
│   │   ├── java/com/foodwaste/platform/
│   │   │   ├── FoodWastePlatformApplication.java
│   │   │   ├── config/
│   │   │   │   ├── SecurityConfig.java
│   │   │   │   └── WebConfig.java
│   │   │   ├── controller/
│   │   │   │   ├── HomeController.java
│   │   │   │   ├── AuthController.java
│   │   │   │   ├── AdminController.java
│   │   │   │   ├── DonorController.java
│   │   │   │   ├── NgoController.java
│   │   │   │   ├── VolunteerController.java
│   │   │   │   ├── NotificationController.java
│   │   │   │   └── ReportApiController.java
│   │   │   ├── dto/
│   │   │   │   ├── UserRegistrationDto.java
│   │   │   │   ├── DonationCreateDto.java
│   │   │   │   ├── DonationFilterDto.java
│   │   │   │   ├── PickupRequestDto.java
│   │   │   │   ├── DeliveryUpdateDto.java
│   │   │   │   ├── OrganizationProfileDto.java
│   │   │   │   └── DashboardStatsDto.java
│   │   │   ├── entity/
│   │   │   │   ├── User.java
│   │   │   │   ├── Organization.java
│   │   │   │   ├── FoodDonation.java
│   │   │   │   ├── PickupRequest.java
│   │   │   │   ├── Delivery.java
│   │   │   │   ├── Notification.java
│   │   │   │   ├── ActivityLog.java
│   │   │   │   ├── DonationStatusHistory.java
│   │   │   │   └── DeliveryStatusHistory.java
│   │   │   ├── enums/
│   │   │   │   ├── Role.java
│   │   │   │   ├── AccountStatus.java
│   │   │   │   ├── OrganizationType.java
│   │   │   │   ├── FoodCategory.java
│   │   │   │   ├── FoodType.java
│   │   │   │   ├── QuantityUnit.java
│   │   │   │   ├── DonationStatus.java
│   │   │   │   ├── PickupRequestStatus.java
│   │   │   │   ├── DeliveryStatus.java
│   │   │   │   └── NotificationType.java
│   │   │   ├── exception/
│   │   │   │   ├── GlobalExceptionHandler.java
│   │   │   │   ├── BusinessException.java
│   │   │   │   ├── ResourceNotFoundException.java
│   │   │   │   └── UnauthorizedException.java
│   │   │   ├── repository/
│   │   │   │   ├── UserRepository.java
│   │   │   │   ├── OrganizationRepository.java
│   │   │   │   ├── FoodDonationRepository.java
│   │   │   │   ├── PickupRequestRepository.java
│   │   │   │   ├── DeliveryRepository.java
│   │   │   │   ├── NotificationRepository.java
│   │   │   │   ├── ActivityLogRepository.java
│   │   │   │   ├── DonationStatusHistoryRepository.java
│   │   │   │   └── DeliveryStatusHistoryRepository.java
│   │   │   ├── scheduler/
│   │   │   │   └── DonationExpiryScheduler.java
│   │   │   ├── security/
│   │   │   │   ├── CustomUserDetails.java
│   │   │   │   ├── CustomUserDetailsService.java
│   │   │   │   ├── CustomAuthenticationSuccessHandler.java
│   │   │   │   └── AdminBootstrapService.java
│   │   │   └── service/
│   │   │       ├── UserService.java
│   │   │       ├── OrganizationService.java
│   │   │       ├── FoodDonationService.java
│   │   │       ├── PickupRequestService.java
│   │   │       ├── DeliveryService.java
│   │   │       ├── NotificationService.java
│   │   │       ├── ActivityLogService.java
│   │   │       ├── ReportService.java
│   │   │       └── FileStorageService.java
│   │   └── resources/
│   │       ├── application.properties
│   │       ├── db/migration/
│   │       │   └── V1__create_initial_schema.sql
│   │       ├── static/
│   │       │   └── css/style.css
│   │       └── templates/
│   │           ├── index.html
│   │           ├── about.html
│   │           ├── how-it-works.html
│   │           ├── contact.html
│   │           ├── error.html
│   │           ├── access-denied.html
│   │           ├── auth/
│   │           │   ├── login.html
│   │           │   └── register.html
│   │           ├── admin/
│   │           │   ├── dashboard.html
│   │           │   ├── organizations.html
│   │           │   ├── users.html
│   │           │   ├── donations.html
│   │           │   ├── deliveries.html
│   │           │   ├── reports.html
│   │           │   └── activity-logs.html
│   │           ├── donor/
│   │           │   ├── dashboard.html
│   │           │   ├── donations.html
│   │           │   ├── create-donation.html
│   │           │   ├── edit-donation.html
│   │           │   ├── donation-detail.html
│   │           │   ├── requests.html
│   │           │   └── profile.html
│   │           ├── ngo/
│   │           │   ├── dashboard.html
│   │           │   ├── available-donations.html
│   │           │   ├── donation-detail.html
│   │           │   ├── requests.html
│   │           │   ├── history.html
│   │           │   └── profile.html
│   │           ├── volunteer/
│   │           │   ├── dashboard.html
│   │           │   ├── tasks.html
│   │           │   ├── task-detail.html
│   │           │   ├── available-tasks.html
│   │           │   └── history.html
│   │           ├── notifications/
│   │           │   └── list.html
│   │           └── fragments/
│   │               ├── header.html
│   │               ├── navbar.html
│   │               ├── sidebar.html
│   │               ├── footer.html
│   │               └── alerts.html
│   └── test/
│       └── java/com/foodwaste/platform/
│           ├── security/
│           │   └── SecurityAccessTest.java
│           └── service/
│               ├── UserServiceTest.java
│               ├── FoodDonationServiceTest.java
│               ├── PickupRequestServiceTest.java
│               └── DeliveryServiceTest.java
```

---

## 6. PostgreSQL Setup & Database Creation

### Database Name
`food_waste_db`

### Port & Connection Settings
- The application is configured to connect to PostgreSQL on **localhost:5433** (the detected PostgreSQL port on this system) with fallback to **localhost:5432** configurable via environment variables.

### Option A: Via pgAdmin GUI
1. Open **pgAdmin 4**.
2. Connect to your PostgreSQL server.
3. Right-click on **Databases** &rarr; Select **Create** &rarr; **Database...**
4. Enter Database name: `food_waste_db`
5. Click **Save**.
6. When the Spring Boot application starts, **Flyway** will automatically run `V1__create_initial_schema.sql` to generate all tables, foreign keys, check constraints, and indexes.

### Option B: Via psql CLI (Executed & Verified)
```powershell
$env:PGPASSWORD="postgres"
psql -U postgres -h localhost -p 5433 -c "CREATE DATABASE food_waste_db;"
```

---

## 7. Environment Variables Configuration

Create a `.env` file or export environment variables as needed:

| Variable | Description | Default Value |
|---|---|---|
| `PORT` | Web server port | `8081` |
| `DB_HOST` | MySQL / Database host | `localhost` |
| `DB_PORT` | MySQL port | `3306` (or `5433` for Postgres) |
| `DB_NAME` | Database name | `food_waste_db` |
| `DB_USER` | Database username | `root` (or `postgres`) |
| `DB_PASSWORD` | Database password | `""` (empty for WAMP default) |
| `ADMIN_BOOTSTRAP_ENABLED`| Initialize super admin on startup | `true` |
| `ADMIN_NAME` | Administrator display name | `Platform Super Admin` |
| `ADMIN_EMAIL` | Administrator login email | `admin@123gmail.com` |
| `ADMIN_PASSWORD` | Initial admin secure password | `admin123` |
| `UPLOAD_DIR` | Directory for uploaded food photos | `uploads/images` |

---

## 8. Authentication & Administrator Credentials

The application uses BCrypt salted password hashing. Administrator account credentials:

| Role | Email Address | Password | Description |
|---|---|---|---|
| **ADMIN** | `admin@123gmail.com` | `admin123` | Super Administrator (Confidential credentials, hidden from public UI) |

Other roles (**Donors**, **NGOs**, **Volunteers**) can be registered via the registration portal:
- **Registration URL:** `http://localhost:8081/register`
- Commercial donors and NGO charities require Admin approval in the Admin dashboard (`/admin/organizations`) before performing food operations.

---

## 9. Commands to Build, Test & Run

### 1. Compile & Execute All Automated Tests
```powershell
# Using installed Maven
mvn clean test

# Or using Maven Wrapper
.\mvnw.cmd clean test
```
*Current test suite result: 22 tests run, 0 failures, 0 errors, 0 skipped.*

### 2. Run Application Locally
```powershell
mvn spring-boot:run

# Or with Maven Wrapper
.\mvnw.cmd spring-boot:run
```

### 3. Application Local Access URL
Open your web browser and navigate to:
```
http://localhost:8081/
```

---

## 10. Role-by-Role Test Walkthrough Scenarios

### Scenario 1: Food Donor Lifecycle
1. Open `http://localhost:8081/login` and click **Donor** (`donor1@bistro.com` / `Password@123`).
2. You are routed to the **Donor Dashboard**.
3. Click **List Food Donation** (`/donor/donations/create`).
4. Fill in:
   - Food Title: "Fresh Vegetable Biryani & Raita Packets"
   - Category: `COOKED_MEALS`
   - Dietary: `VEGETARIAN`
   - Quantity: `40` `MEALS`
   - Prepared Time: Today, 1 hour ago
   - Pickup Deadline: Today, 6 hours in the future
   - Address: "Green Leaf Bistro, Bandra West, Mumbai"
   - Allergen: "Contains dairy (ghee/curd)"
   - Check the food safety confirmation checkbox.
5. Click **Publish Food Listing**. Listing is created as `AVAILABLE`.

### Scenario 2: NGO Food Reservation
1. Log out or open a private window, then log in as **NGO** (`ngo1@carefeed.org` / `Password@123`).
2. Navigate to **Find Available Food** (`/ngo/donations`).
3. Filter by Category or City, and click **View & Request Food** on the Biryani listing.
4. Request `25 Meals` with an estimated pickup time.
5. Submit the request:
   - Remaining quantity on the listing becomes `15 Meals`.
   - Listing status shifts to `PARTIALLY_RESERVED`.
   - Donor receives an immediate in-app notification.

### Scenario 3: Donor Request Approval & Task Creation
1. Switch back to Donor (`donor1@bistro.com`).
2. Navigate to **Incoming Requests** (`/donor/requests`).
3. Click **Approve** on the Care & Feed Foundation request.
4. The system automatically creates a `Delivery` task ready for couriers.

### Scenario 4: Volunteer Delivery Execution
1. Log in as Volunteer (`volunteer1@foodwaste.org` / `Password@123`).
2. Open **Open Deliveries Awaiting Courier** (`/volunteer/available`) and click **Claim Task**, or open **My Delivery Assignments** (`/volunteer/tasks`).
3. Open the task details page.
4. Click **Mark as PICKED UP** (records pickup timestamp).
5. Click **Mark as IN TRANSIT**.
6. Upon arriving at the shelter, enter delivery notes & signoff and click **Mark as DELIVERED**.
7. NGO and Donor both receive notifications that delivery is complete, and the NGO distribution receipt is generated.

### Scenario 5: Administrator Governance & Analytics
1. Log in as Super Admin (`admin@foodwaste.org` / `Admin@Secure2026`).
2. Navigate to **Reports & Analytics** (`/admin/reports`) to inspect real-time Chart.js interactive graphs (Category breakdowns, Veg vs Non-Veg proportions, Donor volume, NGO distributions, Volunteer performance).
3. Open **Organizations** (`/admin/organizations`) to approve newly registered organizations.
4. Open **Activity Logs** (`/admin/activity-logs`) to verify the full audit trail.

---

## 11. Common Errors & Troubleshooting

| Issue | Root Cause | Solution |
|---|---|---|
| `Port 8080 was already in use` | Another service (such as WAMP Apache or Tomcat) is running on port 8080. | The platform defaults to port `8081`. You can also override with `$env:PORT=8082`. |
| `FATAL: password authentication failed for user "postgres"` | PostgreSQL credentials differ on your system. | Edit `application.properties` or set `$env:DB_PASSWORD="your_password"` and `$env:DB_PORT="5433"`. |
| `Cannot find symbol variable BAKERY` | Cached or modified enum compilation. | Run `mvn clean compile` to refresh generated class files. |
| `Past pickup deadline error` | Submitting a donation with a past deadline. | The server validates dates. Ensure `pickupDeadline` is set in the future. |

---

## 12. Security & Compliance Highlights

- **Password Hashing:** Industry-standard **BCrypt** with salt rounds. Plaintext passwords are never stored.
- **Admin Privilege Safeguard:** Public registration strictly rejects any payload attempting to register role `ADMIN`.
- **CSRF Token:** Embedded into all state-altering forms (`POST`).
- **File Upload Protection:** Validates MIME types (`image/jpeg`, `image/png`, `image/webp`), enforces a 5MB size limit, generates non-deterministic UUID filenames, and blocks directory traversal attempts.
- **Scheduled Expiry Daemon:** Automatically marks listings as `EXPIRED` if the pickup deadline passes without collection.
