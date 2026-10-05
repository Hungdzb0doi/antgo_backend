# Entity Documentation

## 1. User
Bảng trung tâm, đại diện cho tất cả người dùng trong hệ thống (cả lao động lẫn nhà tuyển dụng).

| Field | Type | Mô tả |
|---|---|---|
| id | UUID | Primary key, tự sinh |
| phone | VARCHAR(20) | Số điện thoại, unique, dùng để đăng nhập |
| passwordHash | VARCHAR(255) | Mật khẩu đã hash |
| fullName | VARCHAR(150) | Họ tên đầy đủ |
| avatarUrl | VARCHAR(500) | URL ảnh đại diện |
| currentMode | VARCHAR(10) | Chế độ hiện tại: `WORKER` hoặc `EMPLOYER` |
| status | VARCHAR(10) | Trạng thái tài khoản: `ACTIVE` hoặc `LOCKED` |
| identityCard | VARCHAR(20) | Số CCCD/CMND |
| identityVerified | BOOLEAN | Đã xác minh danh tính chưa |
| email | VARCHAR(255) | Email, unique, có thể null |
| createdAt | TIMESTAMP | Thời điểm tạo tài khoản |

**Quan hệ:**
- `@OneToOne` → `EmployerProfile`: một user có thể có profile nhà tuyển dụng
- `@OneToOne` → `WorkerProfile`: một user có thể có profile lao động
- `@OneToMany` → `Notification`: danh sách thông báo của user
- `@OneToMany` → `DeviceToken`: danh sách thiết bị đăng ký nhận push notification
- `@OneToMany` → `Report` (reportsMade): các báo cáo user đã gửi
- `@OneToMany` → `Report` (reportsReceived): các báo cáo user bị nhận
- `@OneToMany` → `Review` (reviewsGiven): các đánh giá user đã viết
- `@OneToMany` → `Review` (reviewsReceived): các đánh giá user nhận được
- `@OneToMany` → `Message` (messagesSent): các tin nhắn user đã gửi

---

## 2. EmployerProfile
Profile mở rộng dành riêng cho nhà tuyển dụng. Chỉ tồn tại khi user đăng ký vai trò employer.

| Field | Type | Mô tả |
|---|---|---|
| userId | UUID | Primary key, đồng thời là FK tới `users.id` |
| employerName | VARCHAR(150) | Tên công ty hoặc tên cá nhân tuyển dụng |
| taxId | VARCHAR(20) | Mã số thuế (có thể null) |
| employerScore | INT | Điểm uy tín nhà tuyển dụng, mặc định 100 |

**Quan hệ:**
- `@OneToOne` ← `User`: dùng `@MapsId` để share PK với `users`
- `@OneToMany` → `Job`: danh sách công việc đã đăng

---

## 3. WorkerProfile
Profile mở rộng dành riêng cho lao động. Lưu thêm vị trí địa lý để hỗ trợ tìm kiếm gần đó.

| Field | Type | Mô tả |
|---|---|---|
| userId | UUID | Primary key, đồng thời là FK tới `users.id` |
| skills | VARCHAR(255) | Mô tả kỹ năng dạng text tự do |
| workerScore | INT | Điểm uy tín lao động, mặc định 100 |
| currentLocation | GEOMETRY(Point, 4326) | Tọa độ GPS hiện tại (longitude, latitude) |
| locationUpdatedAt | TIMESTAMP | Thời điểm cập nhật vị trí gần nhất |
| isAvailable | BOOLEAN | Đang sẵn sàng nhận việc hay không |

**Quan hệ:**
- `@OneToOne` ← `User`: dùng `@MapsId` để share PK với `users`
- `@OneToMany` → `Application`: danh sách đơn ứng tuyển
- `@OneToMany` → `WorkerSkill`: danh sách kỹ năng theo category

---

## 4. JobCategory
Danh mục công việc (vd: Bốc vác, Phục vụ, Bảo vệ...). Dùng để phân loại job và match với kỹ năng worker.

| Field | Type | Mô tả |
|---|---|---|
| id | UUID | Primary key, tự sinh |
| name | VARCHAR(100) | Tên danh mục, unique |

**Quan hệ:**
- `@OneToMany` → `Job`: các công việc thuộc danh mục này
- `@OneToMany` → `WorkerSkill`: các worker có kỹ năng thuộc danh mục này

---

## 5. Job
Tin tuyển dụng do employer đăng. Có tọa độ địa lý để hỗ trợ tìm kiếm theo bán kính.

| Field | Type | Mô tả |
|---|---|---|
| id | UUID | Primary key, tự sinh |
| employer | FK → EmployerProfile | Nhà tuyển dụng đăng tin |
| category | FK → JobCategory | Danh mục công việc |
| title | VARCHAR(200) | Tiêu đề công việc |
| location | GEOMETRY(Point, 4326) | Tọa độ địa điểm làm việc |
| startTime | TIMESTAMP | Thời gian bắt đầu ca |
| endTime | TIMESTAMP | Thời gian kết thúc ca |
| requiredWorkers | INT | Số lượng lao động cần tuyển |
| hourlyRate | DECIMAL(10,2) | Mức lương theo giờ |
| status | VARCHAR(15) | `OPEN` / `IN_PROGRESS` / `COMPLETED` / `CANCELLED` |

**Quan hệ:**
- `@ManyToOne` → `EmployerProfile`: thuộc về một employer
- `@ManyToOne` → `JobCategory`: thuộc một danh mục
- `@OneToMany` → `Application`: danh sách đơn ứng tuyển vào job này

---

## 6. Application
Đơn ứng tuyển của worker vào một job. Cũng là trung tâm liên kết giữa worker và employer sau khi match.

| Field | Type | Mô tả |
|---|---|---|
| id | UUID | Primary key, tự sinh |
| job | FK → Job | Công việc được ứng tuyển |
| worker | FK → WorkerProfile | Lao động ứng tuyển |
| checkInAt | TIMESTAMP | Thời điểm worker check-in tại địa điểm |
| checkOutAt | TIMESTAMP | Thời điểm worker check-out |
| employerConfirmed | BOOLEAN | Employer đã xác nhận hoàn thành chưa |
| workerConfirmed | BOOLEAN | Worker đã xác nhận hoàn thành chưa |
| status | VARCHAR(15) | `BOOKED` / `CANCELLED` / `NO_SHOW` / `COMPLETED` |

> Ràng buộc UNIQUE(job_id, worker_id): mỗi worker chỉ ứng tuyển 1 lần cho mỗi job.

**Quan hệ:**
- `@ManyToOne` → `Job`: thuộc về một job
- `@ManyToOne` → `WorkerProfile`: thuộc về một worker
- `@OneToMany` → `Message`: các tin nhắn trong application này
- `@OneToMany` → `Review`: đánh giá sau khi hoàn thành
- `@OneToMany` → `Report`: báo cáo liên quan đến application này

---

## 7. Message
Tin nhắn trao đổi giữa worker và employer trong phạm vi một application.

| Field | Type | Mô tả |
|---|---|---|
| id | UUID | Primary key, tự sinh |
| application | FK → Application | Application mà tin nhắn thuộc về |
| sender | FK → User | Người gửi |
| content | TEXT | Nội dung tin nhắn |
| sentAt | TIMESTAMP | Thời điểm gửi |
| isRead | BOOLEAN | Đã đọc chưa |

**Quan hệ:**
- `@ManyToOne` → `Application`: thuộc một application
- `@ManyToOne` → `User`: do một user gửi

---

## 8. Review
Đánh giá sau khi hoàn thành công việc. Cả employer đánh giá worker và ngược lại đều dùng bảng này.

| Field | Type | Mô tả |
|---|---|---|
| id | UUID | Primary key, tự sinh |
| application | FK → Application | Application được đánh giá |
| reviewer | FK → User | Người viết đánh giá |
| reviewee | FK → User | Người được đánh giá |
| rating | INT | Điểm từ 1 đến 5 |
| comment | TEXT | Nhận xét (có thể null) |

> Ràng buộc UNIQUE(application_id, reviewer_id): mỗi người chỉ đánh giá 1 lần trên mỗi application.

**Quan hệ:**
- `@ManyToOne` → `Application`
- `@ManyToOne` → `User` (reviewer)
- `@ManyToOne` → `User` (reviewee)

---

## 9. Notification
Thông báo hệ thống gửi đến user (ứng tuyển thành công, có tin nhắn mới, v.v.).

| Field | Type | Mô tả |
|---|---|---|
| id | UUID | Primary key, tự sinh |
| user | FK → User | Người nhận thông báo |
| title | VARCHAR(150) | Tiêu đề thông báo |
| content | TEXT | Nội dung chi tiết |
| type | VARCHAR(20) | Loại thông báo (vd: `APPLICATION`, `MESSAGE`, `REVIEW`) |
| isRead | BOOLEAN | Đã đọc chưa |
| createdAt | TIMESTAMP | Thời điểm tạo |

**Quan hệ:**
- `@ManyToOne` → `User`: thuộc về một user

---

## 10. Report
Báo cáo vi phạm giữa các user, có thể gắn với một application cụ thể.

| Field | Type | Mô tả |
|---|---|---|
| id | UUID | Primary key, tự sinh |
| reporter | FK → User | Người gửi báo cáo |
| reported | FK → User | Người bị báo cáo |
| application | FK → Application | Application liên quan (có thể null) |
| reason | VARCHAR(255) | Lý do báo cáo |
| status | VARCHAR(15) | `PENDING` hoặc `RESOLVED` |

**Quan hệ:**
- `@ManyToOne` → `User` (reporter)
- `@ManyToOne` → `User` (reported)
- `@ManyToOne` → `Application` (nullable)

---

## 11. WorkerSkill
Bảng trung gian quan hệ nhiều-nhiều giữa `WorkerProfile` và `JobCategory`. Thể hiện worker có kỹ năng thuộc danh mục nào.

| Field | Type | Mô tả |
|---|---|---|
| workerId | UUID | FK → WorkerProfile (phần của composite PK) |
| categoryId | UUID | FK → JobCategory (phần của composite PK) |

> Composite PK: (workerId, categoryId)

**Quan hệ:**
- `@ManyToOne` → `WorkerProfile`
- `@ManyToOne` → `JobCategory`

---

## 12. DeviceToken
Lưu token thiết bị để gửi push notification (Firebase FCM hoặc APNs).

| Field | Type | Mô tả |
|---|---|---|
| id | UUID | Primary key, tự sinh |
| user | FK → User | Chủ sở hữu token |
| token | VARCHAR(500) | Token thiết bị |
| platform | VARCHAR(10) | `WEB` / `ANDROID` / `IOS` |
| createdAt | TIMESTAMP | Thời điểm đăng ký |

> Ràng buộc UNIQUE(user_id, token): tránh lưu trùng token cho cùng một user.

**Quan hệ:**
- `@ManyToOne` → `User`: thuộc về một user

---

## Sơ đồ quan hệ tổng quát

```
User ──────────────── EmployerProfile ──── Job ──── Application
 │                                          │            │
 │                    WorkerProfile ────────┘       Message
 │                         │                        Review
 │                    WorkerSkill                   Report
 │
 ├── Notification
 ├── DeviceToken
 ├── Report (reporter / reported)
 ├── Review (reviewer / reviewee)
 └── Message (sender)
```
