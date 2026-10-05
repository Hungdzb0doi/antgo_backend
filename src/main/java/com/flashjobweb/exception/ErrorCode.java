package com.flashjobweb.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    // ===== COMMON =====
    INTERNAL_SERVER_ERROR(9000, "Lỗi hệ thống", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_REQUEST(9001, "Dữ liệu đầu vào không hợp lệ", HttpStatus.BAD_REQUEST),
    UNAUTHORIZED(9002, "Chưa xác thực", HttpStatus.UNAUTHORIZED),
    FORBIDDEN(9003, "Không có quyền thực hiện", HttpStatus.FORBIDDEN),
    RESOURCE_NOT_FOUND(9004, "Không tìm thấy tài nguyên", HttpStatus.NOT_FOUND),

    // ===== AUTH =====
    INVALID_CREDENTIALS(1000, "Số điện thoại hoặc mật khẩu không đúng", HttpStatus.UNAUTHORIZED),
    ACCOUNT_LOCKED(1001, "Tài khoản đã bị khóa", HttpStatus.FORBIDDEN),
    TOKEN_EXPIRED(1002, "Token đã hết hạn", HttpStatus.UNAUTHORIZED),
    TOKEN_INVALID(1003, "Token không hợp lệ", HttpStatus.UNAUTHORIZED),
    REFRESH_TOKEN_INVALID(1004, "Refresh token không hợp lệ", HttpStatus.UNAUTHORIZED),

    // ===== USER =====
    USER_NOT_FOUND(2000, "Không tìm thấy người dùng", HttpStatus.NOT_FOUND),
    PHONE_ALREADY_EXISTS(2001, "Số điện thoại đã được sử dụng", HttpStatus.CONFLICT),
    EMAIL_ALREADY_EXISTS(2002, "Email đã được sử dụng", HttpStatus.CONFLICT),
    IDENTITY_ALREADY_VERIFIED(2003, "Danh tính đã được xác minh", HttpStatus.BAD_REQUEST),
    INVALID_CURRENT_MODE(2004, "Chế độ hiện tại không hợp lệ", HttpStatus.BAD_REQUEST),
    WORKER_PROFILE_NOT_FOUND(2005, "Không tìm thấy hồ sơ lao động", HttpStatus.NOT_FOUND),
    EMPLOYER_PROFILE_NOT_FOUND(2006, "Không tìm thấy hồ sơ nhà tuyển dụng", HttpStatus.NOT_FOUND),
    WORKER_PROFILE_ALREADY_EXISTS(2007, "Hồ sơ lao động đã tồn tại", HttpStatus.CONFLICT),
    EMPLOYER_PROFILE_ALREADY_EXISTS(2008, "Hồ sơ nhà tuyển dụng đã tồn tại", HttpStatus.CONFLICT),
    NOT_GENERATE_TOKEN(2009, "Không thể tạo token", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_OTP(2010, "Mã OTP không hợp lệ", HttpStatus.BAD_REQUEST),
    NOT_SUBMIT_EKYC(2011, "Không gửi được căn cước ", HttpStatus.BAD_REQUEST),
    REPUTATION_SCORE_TOO_LOW(2012, "Điểm uy tín của bạn quá thấp (dưới 50 điểm), tạm thời bị hạn chế thực hiện chức năng này", HttpStatus.FORBIDDEN),

    // ===== JOB =====
    JOB_NOT_FOUND(3000, "Không tìm thấy công việc", HttpStatus.NOT_FOUND),
    JOB_CATEGORY_NOT_FOUND(3001, "Không tìm thấy danh mục công việc", HttpStatus.NOT_FOUND),
    JOB_CATEGORY_ALREADY_EXISTS(3002, "Danh mục công việc đã tồn tại", HttpStatus.CONFLICT),
    JOB_NOT_OPEN(3003, "Công việc không còn mở để ứng tuyển", HttpStatus.BAD_REQUEST),
    JOB_ALREADY_CANCELLED(3004, "Công việc đã bị hủy", HttpStatus.BAD_REQUEST),
    JOB_ALREADY_COMPLETED(3005, "Công việc đã hoàn thành", HttpStatus.BAD_REQUEST),
    JOB_FULL(3006, "Công việc đã đủ số lượng lao động", HttpStatus.BAD_REQUEST),
    JOB_TIME_INVALID(3007, "Thời gian bắt đầu phải trước thời gian kết thúc", HttpStatus.BAD_REQUEST),
    JOB_NOT_BELONG_TO_EMPLOYER(3008, "Công việc không thuộc về nhà tuyển dụng này", HttpStatus.FORBIDDEN),

    // ===== APPLICATION =====
    APPLICATION_NOT_FOUND(4000, "Không tìm thấy đơn ứng tuyển", HttpStatus.NOT_FOUND),
    APPLICATION_ALREADY_EXISTS(4001, "Bạn đã ứng tuyển công việc này rồi", HttpStatus.CONFLICT),
    APPLICATION_NOT_BELONG_TO_WORKER(4002, "Đơn ứng tuyển không thuộc về lao động này", HttpStatus.FORBIDDEN),
    APPLICATION_ALREADY_CANCELLED(4003, "Đơn ứng tuyển đã bị hủy", HttpStatus.BAD_REQUEST),
    APPLICATION_ALREADY_COMPLETED(4004, "Đơn ứng tuyển đã hoàn thành", HttpStatus.BAD_REQUEST),
    APPLICATION_NOT_IN_PROGRESS(4005, "Đơn ứng tuyển chưa được bắt đầu", HttpStatus.BAD_REQUEST),
    CHECKIN_ALREADY_DONE(4006, "Đã check-in trước đó", HttpStatus.BAD_REQUEST),
    CHECKOUT_BEFORE_CHECKIN(4007, "Chưa check-in, không thể check-out", HttpStatus.BAD_REQUEST),
    CHECKOUT_ALREADY_DONE(4008, "Đã check-out trước đó", HttpStatus.BAD_REQUEST),
    CANNOT_APPLY_OWN_JOB(4009, "Không thể ứng tuyển công việc của chính mình", HttpStatus.BAD_REQUEST),
    APPLICATION_IN_DIFFERENT_STATUS(4010,"Đơn ứng tuyển Sai trạng thái hoặc loại", HttpStatus.BAD_REQUEST),
    HAVING_APPLICATION_BOOKED(4011, "Bạn đang có đơn ứng tuyển trước đó chưa hoàn thành", HttpStatus.BAD_REQUEST),
    QR_CODE_EXPIRED(4012, "Mã QR đã hết hạn", HttpStatus.BAD_REQUEST),
    // ===== REVIEW =====
    REVIEW_NOT_FOUND(5000, "Không tìm thấy đánh giá", HttpStatus.NOT_FOUND),
    REVIEW_ALREADY_EXISTS(5001, "Bạn đã đánh giá cho đơn này rồi", HttpStatus.CONFLICT),
    REVIEW_NOT_ALLOWED(5002, "Chỉ được đánh giá sau khi công việc hoàn thành", HttpStatus.BAD_REQUEST),
    CANNOT_REVIEW_YOURSELF(5003, "Không thể tự đánh giá bản thân", HttpStatus.BAD_REQUEST),
    REVIEW_NOT_PARTICIPANT(5004, "Bạn không tham gia vào đơn ứng tuyển này", HttpStatus.FORBIDDEN),

    // ===== MESSAGE =====
    MESSAGE_NOT_FOUND(6000, "Không tìm thấy tin nhắn", HttpStatus.NOT_FOUND),
    MESSAGE_NOT_ALLOWED(6001, "Chỉ được nhắn tin trong đơn ứng tuyển đang hoạt động", HttpStatus.BAD_REQUEST),
    MESSAGE_NOT_PARTICIPANT(6002, "Bạn không tham gia vào cuộc trò chuyện này", HttpStatus.FORBIDDEN),

    // ===== REPORT =====
    REPORT_NOT_FOUND(7000, "Không tìm thấy báo cáo", HttpStatus.NOT_FOUND),
    REPORT_ALREADY_EXISTS(7001, "Bạn đã báo cáo người dùng này trong đơn ứng tuyển này rồi", HttpStatus.CONFLICT),
    CANNOT_REPORT_YOURSELF(7002, "Không thể tự báo cáo bản thân", HttpStatus.BAD_REQUEST),
    REPORT_ALREADY_RESOLVED(7003, "Báo cáo đã được xử lý", HttpStatus.BAD_REQUEST),

    // ===== NOTIFICATION =====
    NOTIFICATION_NOT_FOUND(8000, "Không tìm thấy thông báo", HttpStatus.NOT_FOUND),
    NOTIFICATION_NOT_BELONG_TO_USER(8001, "Thông báo không thuộc về người dùng này", HttpStatus.FORBIDDEN),

    // ===== DEVICE TOKEN =====
    DEVICE_TOKEN_NOT_FOUND(8100, "Không tìm thấy token thiết bị", HttpStatus.NOT_FOUND),
    DEVICE_TOKEN_ALREADY_EXISTS(8101, "Token thiết bị đã được đăng ký", HttpStatus.CONFLICT);

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;

    ErrorCode(int code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }
}
