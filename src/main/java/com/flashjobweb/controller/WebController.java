package com.flashjobweb.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WebController {
    @GetMapping("/interface/authentication")
    public String getAuthenticationPage() {
        return "forward:/authentication.html";
    }
    @GetMapping("/interface/forgotpassword")
    public String getForgotPasswordPage() {
        return "forward:/forgot_password.html";
    }
    @GetMapping("/interface/profile")
    public String getProfile() {
        return "forward:/profile.html";
    }
    @GetMapping("/interface/identify")
    public String getIdentify() {
        return "forward:/identify.html";
    }
    @GetMapping("/interface/createjob")
    public String getCreateJob() {
        return "forward:/createjob.html";
    }
    @GetMapping("/interface/managejob")
    public String getManageJob() {
        return "forward:/managejob.html";
    }
    @GetMapping("/interface/findworker")
    public String getFindWorker() {
        return "forward:/findworker.html";
    }
    @GetMapping("/interface/manageapplication")
    public String getApplicationForCurrentUser() {
        return "forward:/manageapplication.html";
    }
    @GetMapping("/interface/findjob")
    public String getFindJob() {
        return "forward:/findjob.html";
    }
    @GetMapping("/interface/checkapplication")
    public String getCheckApplication() {
        return "forward:/checkapplication.html";
    }
    @GetMapping("/interface/scanner")
    public String getscanner() {
        return "forward:/scanner.html";
    }
    @GetMapping("/interface/generateqr")
    public String getgenerateQr() {
        return "forward:/generate_qr.html";
    }
    @GetMapping("/interface/detailnotification")
    public String getDetailNotification() {
        return "forward:/detailnotification.html";
    }
}
