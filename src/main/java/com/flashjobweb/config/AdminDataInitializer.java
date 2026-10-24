package com.flashjobweb.config;

import com.flashjobweb.entity.EmployerProfileEntity;
import com.flashjobweb.entity.UserEntity;
import com.flashjobweb.entity.WorkerProfileEntity;
import com.flashjobweb.repository.EmployerProfileRepository;
import com.flashjobweb.repository.UserRepository;
import com.flashjobweb.repository.WorkerProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminDataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final WorkerProfileRepository workerProfileRepository;
    private final EmployerProfileRepository employerProfileRepository;
    private final com.flashjobweb.repository.JobCategoryRepository jobCategoryRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        initCategories();
        initAdminAccount();
    }

    private void initCategories() {
        if (jobCategoryRepository.count() == 0) {
            java.util.List<String> defaultCategories = java.util.List.of(
                    "Phục vụ F&B (Nhà hàng, Quán cafe)",
                    "Pha chế (Barista, Bartender)",
                    "Phụ bếp & Nấu ăn",
                    "Thu ngân & Bán hàng",
                    "Bốc xếp & Vận chuyển",
                    "Phụ kho & Đóng gói",
                    "Tạp vụ & Vệ sinh",
                    "Giao hàng (Shipper)",
                    "Bảo vệ & Trông giữ xe",
                    "Tổ chức sự kiện & PG/PB"
            );

            for (String catName : defaultCategories) {
                com.flashjobweb.entity.JobCategoryEntity cat = new com.flashjobweb.entity.JobCategoryEntity();
                cat.setName(catName);
                jobCategoryRepository.save(cat);
            }
            log.info("[AntGo] Auto-seeded {} default job categories.", defaultCategories.size());
        }
    }

    private void initAdminAccount() {
        String adminPhone = "0999999999";
        userRepository.findByPhone(adminPhone).ifPresentOrElse(
                admin -> {
                    if (!"ADMIN".equalsIgnoreCase(admin.getRole())) {
                        admin.setRole("ADMIN");
                        userRepository.save(admin);
                        log.info("Updated role to ADMIN for user phone: {}", adminPhone);
                    }
                },
                () -> {
                    UserEntity admin = new UserEntity();
                    admin.setPhone(adminPhone);
                    admin.setPasswordHash(passwordEncoder.encode("Admin@123456"));
                    admin.setFullName("Quản trị viên Hệ thống");
                    admin.setEmail("nger22072005@gmail.com");
                    admin.setRole("ADMIN");
                    admin.setCurrentMode("EMPLOYER");
                    admin.setStatus("ACTIVE");
                    admin.setIdentityVerified(true);
                    admin.setIdentityCard("000000000000");

                    admin = userRepository.save(admin);

                    WorkerProfileEntity workerProfile = new WorkerProfileEntity();
                    workerProfile.setUserEntity(admin);
                    workerProfileRepository.save(workerProfile);

                    EmployerProfileEntity employerProfile = new EmployerProfileEntity();
                    employerProfile.setUserEntity(admin);
                    employerProfile.setEmployerName("AntGo Administration");
                    employerProfileRepository.save(employerProfile);

                    log.info("=================================================");
                    log.info("[AntGo] Auto-created Default Admin Account:");
                    log.info("Phone: {}", adminPhone);
                    log.info("Password: Admin@123456");
                    log.info("Role: ADMIN");
                    log.info("=================================================");
                }
        );
    }
}
