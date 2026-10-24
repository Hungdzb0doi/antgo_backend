package com.flashjobweb.service.impl.outside;

import com.google.api.client.http.InputStreamContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.UserCredentials;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;

@Service
public class GoogleDriveService {

    @Value("${google.drive.folder.ekyc.id}")
    private String folderId;

    @Value("${google.drive.folder.report.id:${google.drive.folder.ekyc.id}}")
    private String reportFolderId;

    @Value("${google.drive.folder.chat.id:${google.drive.folder.report.id:${google.drive.folder.ekyc.id}}}")
    private String chatFolderId;

    @Value("${google.drive.client.id}")
    private String clientId;

    @Value("${google.drive.client.secret}")
    private String clientSecret;

    @Value("${google.drive.refresh.token}")
    private String refreshToken;

    public String uploadFile(MultipartFile multipartFile) throws IOException, GeneralSecurityException {
        return uploadFileToFolder(multipartFile, folderId);
    }

    public String uploadReportFile(MultipartFile multipartFile) throws IOException, GeneralSecurityException {
        String targetFolder = (reportFolderId != null && !reportFolderId.trim().isEmpty()) ? reportFolderId : folderId;
        return uploadFileToFolder(multipartFile, targetFolder);
    }

    public String uploadChatFile(MultipartFile multipartFile) throws IOException, GeneralSecurityException {
        String targetFolder = (chatFolderId != null && !chatFolderId.trim().isEmpty()) ? chatFolderId : folderId;
        return uploadFileToFolder(multipartFile, targetFolder, true);
    }

    public String uploadFileToFolder(MultipartFile multipartFile, String targetFolderId) throws IOException, GeneralSecurityException {
        return uploadFileToFolder(multipartFile, targetFolderId, true);
    }

    public Drive getDriveService() throws IOException, GeneralSecurityException {
        UserCredentials credentials = UserCredentials.newBuilder()
                .setClientId(clientId)
                .setClientSecret(clientSecret)
                .setRefreshToken(refreshToken)
                .build();

        return new Drive.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                new HttpCredentialsAdapter(credentials))
                .setApplicationName("FlashJob-App")
                .build();
    }

    public record DriveFileDownload(byte[] content, String mimeType) {}

    public DriveFileDownload downloadFile(String fileId) throws IOException, GeneralSecurityException {
        Drive driveService = getDriveService();
        File fileMeta = driveService.files().get(fileId).setFields("mimeType").execute();
        String mime = (fileMeta != null && fileMeta.getMimeType() != null) ? fileMeta.getMimeType() : "image/jpeg";
        try (java.io.InputStream in = driveService.files().get(fileId).executeMediaAsInputStream()) {
            return new DriveFileDownload(in.readAllBytes(), mime);
        }
    }

    public String uploadFileToFolder(MultipartFile multipartFile, String targetFolderId, boolean setPermission) throws IOException, GeneralSecurityException {
        Drive driveService = getDriveService();

        //  Thông tin file
        File fileMetadata = new File();
        fileMetadata.setName(System.currentTimeMillis() + "_" + multipartFile.getOriginalFilename());
        fileMetadata.setParents(Collections.singletonList(targetFolderId));

        //  Upload file
        InputStreamContent mediaContent = new InputStreamContent(
                multipartFile.getContentType(),
                multipartFile.getInputStream());

        File uploadedFile = driveService.files().create(fileMetadata, mediaContent)
                .setFields("id")
                .execute();

        if (setPermission) {
            try {
                com.google.api.services.drive.model.Permission permission = new com.google.api.services.drive.model.Permission()
                        .setType("anyone")
                        .setRole("reader");
                driveService.permissions().create(uploadedFile.getId(), permission).execute();
            } catch (Exception ignored) {
            }
        }

        return uploadedFile.getId();
    }
}