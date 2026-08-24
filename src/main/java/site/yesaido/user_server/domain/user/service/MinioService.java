package site.yesaido.user_server.domain.user.service;

import io.minio.*;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import site.yesaido.user_server.domain.inquiry.exception.FileDeleteException;
import site.yesaido.user_server.domain.inquiry.exception.FileStorageException;
import site.yesaido.user_server.domain.inquiry.exception.FileUploadException;
import site.yesaido.user_server.domain.inquiry.exception.InvalidFileException;

import java.io.InputStream;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MinioService {

    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024;

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    private final MinioClient minioClient;

    @Value("${minio.bucket}")
    private String bucketName;

    /**
     * 프로필 이미지를 MinIO에 업로드하고 object key를 반환한다.
     */
    public String uploadProfileImage(Long userId, MultipartFile file) {

        String objectName = "profiles/" + userId + "/" + UUID.randomUUID() + getExtension(file.getOriginalFilename());

        return uploadImage(file, objectName);
    }

    /**
     * 문의 이미지를 MinIO에 업로드하고 object key를 반환한다.
     */
    public String uploadInquiryPhoto(Long inquiryAnswerId, MultipartFile file) {

        String objectName = "inquiries/" + inquiryAnswerId + "/" + UUID.randomUUID() + getExtension(file.getOriginalFilename());

        return uploadImage(file, objectName);
    }



    private String uploadImage(MultipartFile file, String objectName){
        validateFile(file);
        validateFileSize(file.getSize());
        try (InputStream inputStream = file.getInputStream()) {

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(inputStream, file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );
            log.info("이미지 업로드 성공 : {}", objectName);
            return objectName;
        } catch (Exception e) {
            log.error("이미지 업로드 실패 : {}", objectName, e);
            throw new FileUploadException("사진 업로드에 실패했습니다.");
        }
    }


    public void deleteFile(String objectName){
        if(objectName == null || objectName.isBlank()){
            return;
        }

        try{
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build()
            );
        }catch (Exception e){
            log.error("MiniO 파일 삭제 실패 : {}", objectName, e);
            throw new FileDeleteException("프로필 사진 삭제에 실패했습니다.");
        }
    }

    public void deleteQuietly(String objectName){
        if(objectName == null || objectName.isBlank()){
            return;
        }
        try{
            deleteFile(objectName);
        }catch (Exception e){
            log.warn("MiniO 파일 삭제 실패 : objectName={}", objectName, e);
        }
    }


    private String getExtension(String originalFilename){
        if(originalFilename == null || !originalFilename.contains(".")){
            return ".jpg";
        }

        String extension = originalFilename.substring(
                originalFilename.lastIndexOf(".")
        ).toLowerCase();

        return switch (extension){
            case ".jpg", ".jpeg", ".png", ".webp" -> extension;
            default -> ".jpg";
        };
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty() || !StringUtils.hasText(file.getOriginalFilename())) {
            throw new InvalidFileException("업로드할 파일이 존재하지 않습니다.");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new InvalidFileException("JPG, PNG, WEBP 이미지만 업로드할 수 있습니다.");
        }
    }

    private void validateFileSize(long fileSize) {
        if (fileSize == 0) {
            throw new InvalidFileException("업로드할 파일이 존재하지 않습니다.");
        }

        if (fileSize > MAX_FILE_SIZE) {
            throw new InvalidFileException("프로필 사진은 5MB 이하만 업로드할 수 있습니다.");
        }
    }

    @PostConstruct
    public void ensureBucketExists(){
        try{
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder()
                            .bucket(bucketName)
                            .build()
            );

            if (!exists){
                minioClient.makeBucket(
                        MakeBucketArgs.builder()
                                .bucket(bucketName)
                                .build()
                );
            }
        }catch (Exception e){
            log.error("MiniO 버킷 확인 또는 생성 실패: {}", bucketName, e);
            throw new FileStorageException("이미지 저장소를 사용할 수 없습니다.");
        }
    }


}
