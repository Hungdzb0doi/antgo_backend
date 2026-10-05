package com.flashjobweb.service.impl;

import com.flashjobweb.dto.request.RequestInvitationDTO;
import com.flashjobweb.dto.request.RequestJobDTO;
import com.flashjobweb.dto.response.ResponseNotificationDTO;
import com.flashjobweb.dto.response.ResponseJobDTO;
import com.flashjobweb.dto.response.ResponseWorkerDTO;
import com.flashjobweb.entity.*;
import com.flashjobweb.exception.AppException;
import com.flashjobweb.exception.ErrorCode;
import com.flashjobweb.repository.*;
import com.flashjobweb.service.JobService;
import com.flashjobweb.service.NotificationService;
import com.flashjobweb.util.ApplicationStatus;
import com.flashjobweb.util.ApplicationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.modelmapper.ModelMapper;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.domain.geo.GeoReference;
import org.springframework.data.redis.domain.geo.Metrics;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class JobServiceImpl implements JobService {

    private final UserRepository userRepository;
    private final WorkerProfileRepository workerProfileRepository;
    private final EmployerProfileRepository employerProfileRepository;
    private final JobCategoryRepository jobCategoryRepository;
    private final JobRepository jobRepository;
    private final ReviewRepository reviewRepository;
    private final ModelMapper modelMapper;
    private final StringRedisTemplate redisTemplate;
    private final ApplicationRepository applicationRepository;
    private final NotificationService notificationService;

    private static final String GEO_KEY = "worker:locations";
    private static final String ACTIVE_KEY_PREFIX = "worker:active:";

    @Override
    public void createJob(RequestJobDTO requestJobDTO) {
        String currentPhone = SecurityContextHolder.getContext().getAuthentication().getName();
        UserEntity user = userRepository.findByPhone(currentPhone)
                .orElseThrow(() -> new AppException(ErrorCode.PHONE_ALREADY_EXISTS));
        EmployerProfileEntity employer = employerProfileRepository.findById(user.getId())
                .orElseThrow(() -> new AppException(ErrorCode.EMPLOYER_PROFILE_NOT_FOUND));

        if (employer.getEmployerScore() != null && employer.getEmployerScore() < 50) {
            throw new AppException(ErrorCode.REPUTATION_SCORE_TOO_LOW);
        }

        if (requestJobDTO.getStartTime() == null || requestJobDTO.getEndTime() == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        if (!requestJobDTO.getStartTime().isBefore(requestJobDTO.getEndTime())) {
            throw new AppException(ErrorCode.JOB_TIME_INVALID);
        }

        if (requestJobDTO.getStartTime().isBefore(LocalDateTime.now())) {
            throw new AppException(ErrorCode.JOB_TIME_INVALID);
        }

        JobCategoryEntity jobCategory = jobCategoryRepository.findById(requestJobDTO.getCategoryId())
                .orElseThrow(() -> new AppException(ErrorCode.JOB_CATEGORY_NOT_FOUND));

        GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
        Point point = geometryFactory.createPoint(new Coordinate(requestJobDTO.getLongitude(), requestJobDTO.getLatitude()));

        JobEntity job = new JobEntity();
        job.setEmployer(employer);
        job.setCategory(jobCategory);
        job.setLocation(point);
        job.setTitle(requestJobDTO.getTitle());
        job.setStartTime(requestJobDTO.getStartTime());
        job.setEndTime(requestJobDTO.getEndTime());
        job.setRequiredWorkers(requestJobDTO.getRequiredWorkers());
        job.setHourlyRate(requestJobDTO.getHourlyRate());

        jobRepository.save(job);


        long delayInSeconds = Duration.between(LocalDateTime.now(), job.getStartTime()).getSeconds();

        if (delayInSeconds > 0) {
            String redisKey = "job_start:" + job.getId();

            redisTemplate.opsForValue().set(redisKey, "PENDING", delayInSeconds, TimeUnit.SECONDS);
            log.info("Đã hẹn giờ Redis cho Job {}, đổi trạng thái sau {} giây", job.getId(), delayInSeconds);
        }

        long endDelayInSeconds = Duration.between(LocalDateTime.now(), job.getEndTime()).getSeconds();
        if (endDelayInSeconds > 0) {
            String redisKeyEnd = "job_end:" + job.getId();
            redisTemplate.opsForValue().set(redisKeyEnd, "PENDING", endDelayInSeconds, TimeUnit.SECONDS);
            log.info("Đã hẹn giờ Redis kết thúc Job {}, đổi trạng thái COMPLETED sau {} giây", job.getId(), endDelayInSeconds);
        }
    }

    @Override
    public List<ResponseJobDTO> getAllJobs(String status, String keyword) {
        String currentPhone = SecurityContextHolder.getContext().getAuthentication().getName();
        UserEntity user = userRepository.findByPhone(currentPhone)
                .orElseThrow(() -> new AppException(ErrorCode.EMPLOYER_PROFILE_NOT_FOUND));
        String cleanStatus = (status != null && !status.trim().isEmpty()) ? status.trim() : null;
        String cleanKeyword = (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : null;
        List<JobEntity> jobList = jobRepository.searchEmployerJobs(user.getId(), cleanStatus, cleanKeyword);
        return jobList.stream().map(job -> {
            ResponseJobDTO dto = modelMapper.map(job, ResponseJobDTO.class);
            if (job.getLocation() != null) {
                dto.setLatitude(job.getLocation().getY());
                dto.setLongitude(job.getLocation().getX());
            }
            return dto;
        }).toList();
    }

    @Override
    public List<ResponseWorkerDTO> findAllWorkerNearbyJob(UUID jobId, double radiusInKm) {
        JobEntity job = jobRepository.findById(jobId).orElseThrow(() -> new AppException(ErrorCode.JOB_NOT_FOUND));

        String currentPhone = SecurityContextHolder.getContext().getAuthentication().getName();
        UserEntity currentUser = userRepository.findByPhone(currentPhone)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (job.getEmployer() == null || !job.getEmployer().getUserId().equals(currentUser.getId())) {
            throw new AppException(ErrorCode.JOB_NOT_BELONG_TO_EMPLOYER);
        }

        double jobLng = job.getLocation().getX();
        double jobLat = job.getLocation().getY();

        GeoResults<RedisGeoCommands.GeoLocation<String>> results = redisTemplate.opsForGeo()
                .search(
                        GEO_KEY,
                        GeoReference.fromCoordinate(jobLng, jobLat),
                        new Distance(radiusInKm, Metrics.KILOMETERS),
                        RedisGeoCommands.GeoSearchCommandArgs.newGeoSearchArgs()
                                .includeDistance()
                                .includeCoordinates()
                                .sortAscending()
                );

        List<ResponseWorkerDTO> nearbyWorkers = new ArrayList<>();

        if (results != null) {
            results.forEach(result -> {
                String workerIdStr = result.getContent().getName();
                Boolean isOnline = redisTemplate.hasKey(ACTIVE_KEY_PREFIX + workerIdStr);

                if (Boolean.TRUE.equals(isOnline)) {
                    ResponseWorkerDTO dto = new ResponseWorkerDTO();
                    dto.setUserId(UUID.fromString(workerIdStr));
                    dto.setDistance(result.getDistance().getValue());

                    workerProfileRepository.findById(UUID.fromString(workerIdStr)).ifPresent(worker -> {
                        // Chỉ hiển thị thợ có điểm uy tín >= 50 và đang bật trạng thái sẵn sàng
                        if (worker.getWorkerScore() != null && worker.getWorkerScore() < 50) {
                            return;
                        }
                        if (!Boolean.TRUE.equals(worker.getIsAvailable())) {
                            return;
                        }

                        dto.setFullName(worker.getUserEntity().getFullName());
                        dto.setAvatar(worker.getUserEntity().getAvatarUrl());
                        dto.setLatitude(result.getContent().getPoint().getY());
                        dto.setLongitude(result.getContent().getPoint().getX());
                        dto.setEmail(worker.getUserEntity().getEmail());
                        dto.setPhone(worker.getUserEntity().getPhone());
                        dto.setIdentityVerified(worker.getUserEntity().getIdentityVerified());
                        dto.setSkills(worker.getSkills());
                        dto.setWorkerScore(worker.getWorkerScore());
                        dto.setCurrentLocation(worker.getCurrentLocation());

                        boolean hasApplied = applicationRepository.existsByworker_userIdAndJob_id(worker.getUserId(), jobId);
                        dto.setHasApplied(hasApplied);

                        // Lấy đánh giá sao trung bình & số lượt đánh giá
                        UUID workerUserId = worker.getUserId();
                        Double avgRating = reviewRepository.findAverageRatingByRevieweeId(workerUserId);
                        long reviewCount = reviewRepository.countByRevieweeId(workerUserId);
                        dto.setAverageRating(avgRating != null ? Math.round(avgRating * 10.0) / 10.0 : null);
                        dto.setReviewCount(reviewCount);

                        // Tính điểm ưu tiên theo Phương án 2 (Weighted Score):
                        // Đánh giá thực tế (60%) + Điểm uy tín (40%)
                        // Người mới chưa có đánh giá: điểm đánh giá cơ sở tính 4.5 sao (90/100)
                        double effectiveRatingScore = (avgRating != null && reviewCount > 0) ? (avgRating * 20.0) : 90.0;
                        double repScore = (worker.getWorkerScore() != null) ? worker.getWorkerScore().doubleValue() : 100.0;
                        double priorityScore = (effectiveRatingScore * 0.6) + (repScore * 0.4);
                        dto.setPriorityScore(Math.round(priorityScore * 10.0) / 10.0);

                        nearbyWorkers.add(dto);
                    });
                } else {
                    redisTemplate.opsForGeo().remove(GEO_KEY, workerIdStr);
                    log.info("Đã dọn dẹp tọa độ rác : {}", workerIdStr);
                }
            });
        }

        // Sắp xếp ưu tiên:
        // 1. Điểm ưu tiên (priorityScore) cao nhất xếp trước
        // 2. Điểm bằng nhau -> Khoảng cách (distance) gần nhất xếp trước
        nearbyWorkers.sort((w1, w2) -> {
            int scoreCompare = Double.compare(
                    w2.getPriorityScore() != null ? w2.getPriorityScore() : 0.0,
                    w1.getPriorityScore() != null ? w1.getPriorityScore() : 0.0
            );
            if (scoreCompare != 0) {
                return scoreCompare;
            }
            double d1 = w1.getDistance() != null ? w1.getDistance() : Double.MAX_VALUE;
            double d2 = w2.getDistance() != null ? w2.getDistance() : Double.MAX_VALUE;
            return Double.compare(d1, d2);
        });

        return nearbyWorkers;
    }

    @Override
    public void inviteWorker(UUID jobId, UUID workerId) {
        JobEntity job = jobRepository.findById(jobId).orElseThrow(() -> new AppException(ErrorCode.JOB_NOT_FOUND));
        WorkerProfileEntity worker = workerProfileRepository.findById(workerId).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        String currentPhone = SecurityContextHolder.getContext().getAuthentication().getName();
        UserEntity currentUser = userRepository.findByPhone(currentPhone)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (job.getEmployer() == null || !job.getEmployer().getUserId().equals(currentUser.getId())) {
            throw new AppException(ErrorCode.JOB_NOT_BELONG_TO_EMPLOYER);
        }

        if (job.getEmployer() != null && job.getEmployer().getEmployerScore() != null && job.getEmployer().getEmployerScore() < 50) {
            throw new AppException(ErrorCode.REPUTATION_SCORE_TOO_LOW);
        }

        if (worker.getWorkerScore() != null && worker.getWorkerScore() < 50) {
            throw new AppException(ErrorCode.REPUTATION_SCORE_TOO_LOW);
        }

        if (applicationRepository.existsByworker_userIdAndJob_id(workerId, jobId)) {
            throw new AppException(ErrorCode.APPLICATION_ALREADY_EXISTS);
        }

        ApplicationEntity application = new ApplicationEntity();
        application.setJob(job);
        application.setWorker(worker);
        application.setStatus(ApplicationStatus.PENDING);
        application.setType(ApplicationType.INVITATION);
        applicationRepository.save(application);


        String title = "Lời mời làm việc mới từ: " + job.getEmployer().getUserEntity().getFullName();
        String content = "Bạn vừa nhận được lời mời cho công việc: " + job.getTitle();
        notificationService.sendNotification(worker.getUserEntity(), title, content, "INVITATION", job.getId());
    }

    @Override
    public List<ResponseJobDTO> findNearbyJobs(double lat, double lng, double radiusInKm) {
        double radiusInMeters = radiusInKm * 1000;
        List<JobEntity> nearbyJobs = jobRepository.findNearbyJobs(lng, lat, radiusInMeters, "OPEN");

        List<ResponseJobDTO> resultList = nearbyJobs.stream()
                .filter(job -> job.getEmployer() == null || job.getEmployer().getEmployerScore() == null || job.getEmployer().getEmployerScore() >= 50)
                .map(job -> {
                    ResponseJobDTO dto = modelMapper.map(job, ResponseJobDTO.class);
                    if (job.getLocation() != null) {
                        dto.setLatitude(job.getLocation().getY());
                        dto.setLongitude(job.getLocation().getX());
                    }

                    EmployerProfileEntity employer = job.getEmployer();
                    if (employer != null) {
                        String empName = (employer.getEmployerName() != null && !employer.getEmployerName().trim().isEmpty())
                                ? employer.getEmployerName()
                                : (employer.getUserEntity() != null ? employer.getUserEntity().getFullName() : "Nhà tuyển dụng");
                        dto.setEmployerName(empName);
                        dto.setEmployerAvatar(employer.getUserEntity() != null ? employer.getUserEntity().getAvatarUrl() : null);
                        dto.setEmployerPhone(employer.getUserEntity() != null ? employer.getUserEntity().getPhone() : null);
                        dto.setEmployerScore(employer.getEmployerScore() != null ? employer.getEmployerScore() : 100);

                        UUID employerId = employer.getUserId();
                        Double empRating = reviewRepository.findAverageRatingByRevieweeId(employerId);
                        long empReviewCount = reviewRepository.countByRevieweeId(employerId);
                        dto.setEmployerRating(empRating != null ? Math.round(empRating * 10.0) / 10.0 : null);
                        dto.setEmployerReviewCount(empReviewCount);

                        // Tính điểm ưu tiên theo Phương án 2 (Weighted Score):
                        double effectiveRatingScore = (empRating != null && empReviewCount > 0) ? (empRating * 20.0) : 90.0;
                        double repScore = (employer.getEmployerScore() != null) ? employer.getEmployerScore().doubleValue() : 100.0;
                        double priorityScore = (effectiveRatingScore * 0.6) + (repScore * 0.4);
                        dto.setPriorityScore(Math.round(priorityScore * 10.0) / 10.0);
                    } else {
                        dto.setEmployerScore(100);
                        dto.setPriorityScore(90.0);
                    }

                    return dto;
                })
                .collect(Collectors.toList());

        // Sắp xếp ưu tiên:
        // 1. Điểm ưu tiên của nhà tuyển dụng (priorityScore) cao nhất lên đầu
        // 2. Nếu điểm bằng nhau -> Giữ thứ tự khoảng cách
        resultList.sort((j1, j2) -> Double.compare(
                j2.getPriorityScore() != null ? j2.getPriorityScore() : 0.0,
                j1.getPriorityScore() != null ? j1.getPriorityScore() : 0.0
        ));

        return resultList;
    }

    @Override
    public void applyForJob(UUID jobId) {
        String currentPhone = SecurityContextHolder.getContext().getAuthentication().getName();
        UserEntity user = userRepository.findByPhone(currentPhone)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        WorkerProfileEntity worker = workerProfileRepository.findById(user.getId())
                .orElseThrow(() -> new AppException(ErrorCode.WORKER_PROFILE_NOT_FOUND));
        JobEntity job = jobRepository.findById(jobId)
                .orElseThrow(() -> new AppException(ErrorCode.JOB_NOT_FOUND));

        if (job.getEmployer() != null && job.getEmployer().getUserId().equals(user.getId())) {
            throw new AppException(ErrorCode.CANNOT_APPLY_OWN_JOB);
        }

        if (worker.getWorkerScore() != null && worker.getWorkerScore() < 50) {
            throw new AppException(ErrorCode.REPUTATION_SCORE_TOO_LOW);
        }

        if (job.getEmployer() != null && job.getEmployer().getEmployerScore() != null && job.getEmployer().getEmployerScore() < 50) {
            throw new AppException(ErrorCode.REPUTATION_SCORE_TOO_LOW);
        }

        if (applicationRepository.existsByworker_userIdAndJob_id(worker.getUserId(), jobId)) {
            throw new AppException(ErrorCode.APPLICATION_ALREADY_EXISTS);
        }

        long overlappingCount = applicationRepository.existsOverlappingJob(
                worker.getUserId(), job.getStartTime(), job.getEndTime(), ApplicationStatus.BOOKED
        );
        if (overlappingCount > 0) {
            throw new AppException(ErrorCode.HAVING_APPLICATION_BOOKED);
        }

        ApplicationEntity application = new ApplicationEntity();
        application.setJob(job);
        application.setWorker(worker);
        application.setStatus(ApplicationStatus.PENDING);
        application.setType(ApplicationType.APPLICATION);
        applicationRepository.save(application);


        String title = "Ứng viên mới ứng tuyển!";
        String content = "Ứng viên " + worker.getUserEntity().getFullName() + " vừa ứng tuyển vào công việc: " + job.getTitle();
        notificationService.sendNotification(job.getEmployer().getUserEntity(), title, content, "APPLICATION", job.getId());
    }


    @Override
    public void handleJobStart(UUID jobId) {
        JobEntity job = jobRepository.findById(jobId).orElse(null);
        if (job == null || !"OPEN".equals(job.getStatus())) return;

        Long bookedCount = applicationRepository.countByJob_IdAndStatus(job.getId(), ApplicationStatus.BOOKED);

        if (bookedCount == null || bookedCount == 0) {
            job.setStatus("CANCELLED");
            log.info("Đã HỦY Job {}: Không có người ứng tuyển", jobId);

            notificationService.sendNotification(job.getEmployer().getUserEntity(), "Công việc bị hủy tự động",
                    "Rất tiếc, công việc '" + job.getTitle() + "' đã bị hủy vì đến giờ nhưng chưa tìm được thợ.", "SYSTEM", job.getId());

            List<ApplicationEntity> pendingApps = applicationRepository.findByJob_IdAndStatus(job.getId(), ApplicationStatus.PENDING);
            for (ApplicationEntity app : pendingApps) {
                app.setStatus(ApplicationStatus.CANCELLED);
                applicationRepository.save(app);
                notificationService.sendNotification(app.getWorker().getUserEntity(), "Công việc đã bị hủy",
                        "Công việc '" + job.getTitle() + "' đã bị hủy do đã quá giờ bắt đầu.", "SYSTEM", job.getId());
            }
        } else {
            job.setStatus("IN_PROGRESS");
            log.info("Job {} ĐANG THỰC HIỆN với {} ứng viên", jobId, bookedCount);

            notificationService.sendNotification(job.getEmployer().getUserEntity(), "Công việc bắt đầu",
                    "Đã đến giờ! Công việc '" + job.getTitle() + "' đang bắt đầu với " + bookedCount + " thợ.", "SYSTEM", job.getId());

            List<ApplicationEntity> bookedApps = applicationRepository.findByJob_IdAndStatus(job.getId(), ApplicationStatus.BOOKED);
            for (ApplicationEntity app : bookedApps) {
                notificationService.sendNotification(app.getWorker().getUserEntity(), "Đến giờ làm việc!",
                        "Công việc '" + job.getTitle() + "' đã bắt đầu. Vui lòng mở ứng dụng và quét mã Check-in.", "SYSTEM", job.getId());
            }

            // Tự động đóng các đơn PENDING còn lại (ứng viên chưa được duyệt hoặc lời mời chưa nhận)
            List<ApplicationEntity> remainingPendingApps = applicationRepository.findByJob_IdAndStatus(job.getId(), ApplicationStatus.PENDING);
            for (ApplicationEntity app : remainingPendingApps) {
                app.setStatus(ApplicationStatus.CANCELLED);
                applicationRepository.save(app);
                String notifTitle = (app.getType() == ApplicationType.INVITATION) ? "Lời mời làm việc hết hạn" : "Đơn ứng tuyển hết hạn";
                String notifContent = (app.getType() == ApplicationType.INVITATION)
                        ? "Lời mời cho công việc '" + job.getTitle() + "' đã hết hạn do công việc đã chính thức bắt đầu."
                        : "Rất tiếc, công việc '" + job.getTitle() + "' đã bắt đầu và đã chốt danh sách thợ. Đơn ứng tuyển của bạn đã hết hạn.";
                notificationService.sendNotification(app.getWorker().getUserEntity(), notifTitle, notifContent, "SYSTEM", job.getId());
            }
        }
        jobRepository.save(job);
    }

    @Override
    public void handleJobEndTime(UUID jobId) {
        JobEntity job = jobRepository.findById(jobId).orElse(null);
        if (job == null || !"IN_PROGRESS".equals(job.getStatus())) return;

        long activeWorkers = applicationRepository.countByJob_IdAndStatus(job.getId(), ApplicationStatus.IN_PROGRESS);
        long bookedWorkers = applicationRepository.countByJob_IdAndStatus(job.getId(), ApplicationStatus.BOOKED);

        if (activeWorkers == 0 && bookedWorkers == 0) {
            handleJobComplete(jobId);
            return;
        }

        // Nếu có thợ đang làm dở -> KHÔNG tự ý đóng Job, gửi nhắc nhở cho cả 2 bên
        if (activeWorkers > 0) {
            log.info("Job {} đến giờ kết thúc dự kiến nhưng vẫn có {} thợ đang làm -> Gửi nhắc nhở", jobId, activeWorkers);

            notificationService.sendNotification(job.getEmployer().getUserEntity(), "Ca làm việc đến giờ kết thúc",
                    "Công việc '" + job.getTitle() + "' đã đến giờ kết thúc dự kiến. Vui lòng chuẩn bị mã QR Check-out và thanh toán tiền công cho thợ.", "SYSTEM", job.getId());

            List<ApplicationEntity> inProgressApps = applicationRepository.findByJob_IdAndStatus(job.getId(), ApplicationStatus.IN_PROGRESS);
            for (ApplicationEntity app : inProgressApps) {
                notificationService.sendNotification(app.getWorker().getUserEntity(), "Đến giờ kết thúc ca làm",
                        "Công việc '" + job.getTitle() + "' đã đến giờ kết thúc dự kiến. Hãy liên hệ nhà tuyển dụng để quét mã Check-out chốt giờ làm và nhận lương.", "SYSTEM", job.getId());
            }
        }

        // Nếu có thợ được duyệt nhưng cả ca không đến Check-in -> Đánh dấu NO_SHOW
        if (bookedWorkers > 0) {
            log.warn("Job {} có {} thợ nhận việc nhưng không đến Check-in -> Đánh dấu NO_SHOW", jobId, bookedWorkers);
            List<ApplicationEntity> bookedApps = applicationRepository.findByJob_IdAndStatus(job.getId(), ApplicationStatus.BOOKED);
            for (ApplicationEntity app : bookedApps) {
                app.setStatus(ApplicationStatus.NO_SHOW);
                applicationRepository.save(app);
                
                // Trừ 10 điểm uy tín vì lỗi bỏ ca
                WorkerProfileEntity worker = app.getWorker();
                if (worker.getWorkerScore() != null) {
                    worker.setWorkerScore(Math.max(0, worker.getWorkerScore() - 10));
                    workerProfileRepository.save(worker);
                }

                notificationService.sendNotification(app.getWorker().getUserEntity(), "Đơn bị đánh dấu Bỏ ca",
                        "Bạn đã không có mặt Check-in công việc '" + job.getTitle() + "'. Đơn đã bị ghi nhận Bỏ ca (NO_SHOW) và bạn bị trừ 10 điểm uy tín.", "SYSTEM", job.getId());
                notificationService.sendNotification(job.getEmployer().getUserEntity(), "Thợ không đến nhận việc",
                        "Ứng viên " + app.getWorker().getUserEntity().getFullName() + " đã không đến Check-in công việc '" + job.getTitle() + "'.", "SYSTEM", job.getId());
            }

            if (activeWorkers == 0) {
                handleJobComplete(jobId);
            }
        }
    }

    @Override
    public void handleJobComplete(UUID jobId) {
        JobEntity job = jobRepository.findById(jobId).orElse(null);
        if (job == null || !"IN_PROGRESS".equals(job.getStatus())) return;

        job.setStatus("COMPLETED");
        jobRepository.save(job);
        log.info("Job {} ĐÃ HOÀN THÀNH", jobId);

        notificationService.sendNotification(job.getEmployer().getUserEntity(), "Công việc đã hoàn thành",
                "Công việc '" + job.getTitle() + "' đã kết thúc.", "SYSTEM", job.getId());

        // Dọn dẹp bất kỳ đơn PENDING nào còn sót lại
        List<ApplicationEntity> pendingApps = applicationRepository.findByJob_IdAndStatus(job.getId(), ApplicationStatus.PENDING);
        for (ApplicationEntity app : pendingApps) {
            app.setStatus(ApplicationStatus.CANCELLED);
            applicationRepository.save(app);
            String notifTitle = (app.getType() == ApplicationType.INVITATION) ? "Lời mời làm việc hết hạn" : "Đơn ứng tuyển hết hạn";
            String notifContent = "Công việc '" + job.getTitle() + "' đã kết thúc. Đơn/lời mời của bạn đã hết hạn.";
            notificationService.sendNotification(app.getWorker().getUserEntity(), notifTitle, notifContent, "SYSTEM", job.getId());
        }
    }

    @Scheduled(cron = "0 */5 * * * *")
    public void updateJobStatus() {
        LocalDateTime now = LocalDateTime.now();

        // 1. Quét các Job còn OPEN nhưng đã quá startTime
        List<JobEntity> overdueOpenJobs = jobRepository.findOverdueOpenJobs(now);
        for (JobEntity job : overdueOpenJobs) {
            try {
                handleJobStart(job.getId());
            } catch (Exception e) {
                log.error("Lỗi khi xử lý bắt đầu Job quá hạn {}: ", job.getId(), e);
            }
        }

        // 2. Quét các Job đang IN_PROGRESS: Chỉ đóng nếu không còn ai làm hoặc quá 12h an toàn
        List<JobEntity> overdueInProgressJobs = jobRepository.findOverdueInProgressJobs(now);
        for (JobEntity job : overdueInProgressJobs) {
            try {
                long active = applicationRepository.countByJob_IdAndStatus(job.getId(), ApplicationStatus.IN_PROGRESS);
                long booked = applicationRepository.countByJob_IdAndStatus(job.getId(), ApplicationStatus.BOOKED);

                if (active == 0 && booked == 0) {
                    handleJobComplete(job.getId());
                } else if (job.getEndTime() != null && job.getEndTime().plusHours(12).isBefore(now)) {
                    log.info("Chốt chặn an toàn 12h: Tự động đóng Job {} quá hạn lâu ngày", job.getId());
                    
                    // Xử lý các đơn IN_PROGRESS bị kẹt
                    List<ApplicationEntity> remainingActive = applicationRepository.findByJob_IdAndStatus(job.getId(), ApplicationStatus.IN_PROGRESS);
                    for (ApplicationEntity app : remainingActive) {
                        if (app.getCheckInAt() != null) {
                            app.setCheckOutAt(job.getEndTime());
                            long minutes = Duration.between(app.getCheckInAt(), job.getEndTime()).toMinutes();
                            double hours = Math.max(0, Math.round(((double) minutes / 60.0) * 100.0) / 100.0);
                            app.setEarnedAmount(job.getHourlyRate().multiply(BigDecimal.valueOf(hours)).setScale(0, RoundingMode.HALF_UP));
                        }
                        app.setStatus(ApplicationStatus.COMPLETED);
                        applicationRepository.save(app);
                    }

                    // Xử lý các đơn BOOKED bị kẹt (không đến làm)
                    List<ApplicationEntity> remainingBooked = applicationRepository.findByJob_IdAndStatus(job.getId(), ApplicationStatus.BOOKED);
                    for (ApplicationEntity app : remainingBooked) {
                        app.setStatus(ApplicationStatus.NO_SHOW);
                        applicationRepository.save(app);

                        // Trừ uy tín thợ
                        WorkerProfileEntity worker = app.getWorker();
                        if (worker.getWorkerScore() != null) {
                            worker.setWorkerScore(Math.max(0, worker.getWorkerScore() - 10));
                            workerProfileRepository.save(worker);
                        }
                    }

                    handleJobComplete(job.getId());
                }
            } catch (Exception e) {
                log.error("Lỗi khi xử lý Job IN_PROGRESS quá hạn {}: ", job.getId(), e);
            }
        }
    }

    @Override
    public void cancelJob(UUID jobId) {
        String currentPhone = SecurityContextHolder.getContext().getAuthentication().getName();
        UserEntity user = userRepository.findByPhone(currentPhone)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        JobEntity job = jobRepository.findById(jobId)
                .orElseThrow(() -> new AppException(ErrorCode.JOB_NOT_FOUND));

        if (job.getEmployer() == null || !job.getEmployer().getUserId().equals(user.getId())) {
            throw new AppException(ErrorCode.JOB_NOT_BELONG_TO_EMPLOYER);
        }

        if ("CANCELLED".equals(job.getStatus())) {
            throw new AppException(ErrorCode.JOB_ALREADY_CANCELLED);
        }

        if ("COMPLETED".equals(job.getStatus())) {
            throw new AppException(ErrorCode.JOB_ALREADY_COMPLETED);
        }

        job.setStatus("CANCELLED");
        jobRepository.save(job);

        // Xóa hẹn giờ Redis nếu có
        try {
            redisTemplate.delete("job_start:" + job.getId());
            redisTemplate.delete("job_end:" + job.getId());
        } catch (Exception e) {
            log.warn("Lỗi khi xóa key redis của job {}: {}", jobId, e.getMessage());
        }

        // Cập nhật tất cả đơn ứng tuyển đang PENDING hoặc BOOKED thành CANCELLED
        List<ApplicationEntity> apps = applicationRepository.findApplicationsByJobId(jobId);
        if (apps != null) {
            for (ApplicationEntity app : apps) {
                if (app.getStatus() == ApplicationStatus.PENDING || app.getStatus() == ApplicationStatus.BOOKED) {
                    app.setStatus(ApplicationStatus.CANCELLED);
                    applicationRepository.save(app);

                    try {
                        if (app.getWorker() != null && app.getWorker().getUserEntity() != null) {
                            notificationService.sendNotification(
                                    app.getWorker().getUserEntity(),
                                    "Công việc đã bị hủy",
                                    "Chủ nhà đã hủy công việc \"" + job.getTitle() + "\".",
                                    "JOB_CANCELLED"
                            );
                        }
                    } catch (Exception e) {
                        log.error("Lỗi gửi thông báo khi hủy job: {}", e.getMessage());
                    }
                }
            }
        }
    }
}