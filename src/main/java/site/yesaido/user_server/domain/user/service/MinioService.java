package site.yesaido.user_server.domain.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import site.yesaido.common.storage.ImageFileValidator;
import site.yesaido.common.storage.MinioObjectStorage;
import site.yesaido.common.storage.MinioObjectStorageException;
import site.yesaido.common.storage.ObjectKeyGenerator;
import site.yesaido.user_server.domain.inquiry.exception.FileDeleteException;
import site.yesaido.user_server.domain.inquiry.exception.FileStorageException;
import site.yesaido.user_server.domain.inquiry.exception.FileUploadException;
import site.yesaido.user_server.domain.inquiry.exception.InvalidFileException;

import java.time.Duration;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class MinioService {

    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024;
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final Duration PRESIGNED_URL_TTL = Duration.ofMinutes(30);
    private static final String PROFILE_DOMAIN = "profiles";
    private static final String INQUIRY_DOMAIN = "inquiries";

    private final MinioObjectStorage minioObjectStorage;

    @Value("${minio.url}")
    private String minioInternalBaseUrl;

    @Value("${minio.public-base-url}")
    private String minioPublicBaseUrl;

    /**
     * 프로필 이미지를 MinIO에 업로드하고 object key를 반환한다.
     */
    public String uploadProfileImage(Long userId, MultipartFile file) {
        return uploadImage(PROFILE_DOMAIN, userId, file);
    }

    /**
     * 문의 이미지를 MinIO에 업로드하고 object key를 반환한다.
     */
    public String uploadInquiryPhoto(Long inquiryAnswerId, MultipartFile file) {
        return uploadImage(INQUIRY_DOMAIN, inquiryAnswerId, file);
    }

    private String uploadImage(String domain, Long ownerId, MultipartFile file) {
        validateFile(file);
        ensureBucketExists();

        String objectKey = ObjectKeyGenerator.generate(domain, ownerId, resolveFilename(file));

        try {
            minioObjectStorage.put(objectKey, file);
            log.info("이미지 업로드 성공 : {}", objectKey);
            return objectKey;
        } catch (MinioObjectStorageException e) {
            log.error("이미지 업로드 실패 : {}", objectKey, e);
            throw new FileUploadException("사진 업로드에 실패했습니다.");
        }
    }

    public void deleteFile(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            return;
        }
        try {
            minioObjectStorage.remove(objectKey);
        } catch (MinioObjectStorageException e) {
            log.error("MinIO 파일 삭제 실패 : {}", objectKey, e);
            throw new FileDeleteException("사진 삭제에 실패했습니다.");
        }
    }

    public void deleteQuietly(String objectKey) {
        minioObjectStorage.removeQuietly(objectKey);
    }

    /**
     * 브라우저에서 바로 열 수 있는 공개 presigned URL을 발급한다. 실패해도 예외를 던지지 않고
     * null을 반환한다(사진 목록 조회 자체가 깨지면 안 되므로 — 사진 하나 URL 발급 실패로 전체 문의
     * 조회가 500이 되는 것을 막기 위함).
     */
    public String presignedGetUrl(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            return null;
        }
        try {
            String presignedUrl = minioObjectStorage.presignedGetUrl(objectKey, PRESIGNED_URL_TTL);
            return presignedUrl.startsWith(minioInternalBaseUrl)
                    ? minioPublicBaseUrl + presignedUrl.substring(minioInternalBaseUrl.length())
                    : presignedUrl;
        } catch (MinioObjectStorageException e) {
            log.warn("presigned URL 발급 실패 : objectKey={}, cause={}", objectKey, e.getMessage());
            return null;
        }
    }

    public void ensureBucketExists() {
        try {
            minioObjectStorage.ensureBucketExists();
        } catch (MinioObjectStorageException e) {
            log.error("MinIO 버킷 확인 또는 생성 실패", e);
            throw new FileStorageException("이미지 저장소를 사용할 수 없습니다.");
        }
    }

    private void validateFile(MultipartFile file) {
        if (ImageFileValidator.isEmpty(file)) {
            throw new InvalidFileException("업로드할 파일이 존재하지 않습니다.");
        }
        if (!ImageFileValidator.isAllowedContentType(file, ALLOWED_CONTENT_TYPES)) {
            throw new InvalidFileException("JPG, PNG, WEBP 이미지만 업로드할 수 있습니다.");
        }
        if (ImageFileValidator.exceedsMaxSize(file, MAX_FILE_SIZE)) {
            throw new InvalidFileException("사진은 5MB 이하만 업로드할 수 있습니다.");
        }
    }

    /**
     * 원본 파일명에 확장자가 없으면 기본 확장자(.jpg)를 붙인다.
     * ObjectKeyGenerator는 확장자가 없는 파일명을 그대로 받아들여 확장자 없는 objectKey를 만들기 때문에,
     * 브라우저에서 파일을 열람할 때 문제가 없도록 여기서 보정한다.
     */
    private String resolveFilename(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null) {
            int dot = originalFilename.lastIndexOf('.');
            boolean hasExtension = dot > 0 && dot < originalFilename.length() - 1;
            if (hasExtension) {
                return originalFilename;
            }
        }
        return "image.jpg";
    }
}