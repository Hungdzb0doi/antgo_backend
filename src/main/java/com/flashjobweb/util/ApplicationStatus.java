package com.flashjobweb.util;

public enum ApplicationStatus {
    PENDING,    // Vừa gửi lời mời, chờ ứng viên/nhà tuyển dụng xem xét & đồng ý
    BOOKED,     // Ứng viên đã đồng ý nhận việc
    IN_PROGRESS,// Đang làm việc (checkin thành công)
    CANCELLED,  // Hủy  (bởi ứng viên hoặc nhà tuyển dụng)
    NO_SHOW,    // Ứng viên không đến như hẹn
    COMPLETED
}
