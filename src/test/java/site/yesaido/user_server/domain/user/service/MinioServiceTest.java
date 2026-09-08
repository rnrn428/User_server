package site.yesaido.user_server.domain.user.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import site.yesaido.common.storage.MinioObjectStorage;
import site.yesaido.common.storage.MinioObjectStorageException;
import site.yesaido.user_server.domain.inquiry.exception.FileDeleteException;
import site.yesaido.user_server.domain.inquiry.exception.FileStorageException;
import site.yesaido.user_server.domain.inquiry.exception.FileUploadException;
import site.yesaido.user_server.domain.inquiry.exception.InvalidFileException;

import java.time.Duration;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MinioServiceTest {
    @Mock
    private MinioObjectStorage minioObjectStorage;

    @Mock
    private StringRedisTemplate redis;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private MinioService minioService;

    @BeforeEach
    void setUp(){
        ReflectionTestUtils.setField(minioService, "minioInternalBaseUrl", "http://storage.internal:9000");
        ReflectionTestUtils.setField(minioService, "minioPublicBaseUrl", "https://yes-nhn.site/storage-proxy");
        lenient().when(redis.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("프로필 이미지 업로드 성공")
    void profile_image_upload_success() {
        MockMultipartFile file = new MockMultipartFile("file", "mushroom.jpg", "image/jpeg", "image data".getBytes());

        String objectKey = minioService.uploadProfileImage(1L, file);

        assertThat(objectKey).startsWith("profiles/1/").endsWith(".jpg");
        verify(minioObjectStorage).put(objectKey, file);
    }

    @Test
    @DisplayName("서버 시작 시 버킷 확인/생성을 MinioObjectStorage에 위임한다")
    void ensureBucketExists_delegatesToMinioObjectStorage() {
        minioService.ensureBucketExists();

        verify(minioObjectStorage).ensureBucketExists();
    }

    @Test
    @DisplayName("허용하지 않은 이미지 타입이면 InvalidFileException 예외가 발생한다")
    void uploadProfileImage_throwsExceptionForUnsupportedContentType(){
        MockMultipartFile file = new MockMultipartFile("file", "document.pdf", "application/pdf", "file data".getBytes());

        assertThatThrownBy(() -> minioService.uploadProfileImage(1L, file))
                .isInstanceOf(InvalidFileException.class)
                .hasMessage("JPG, PNG, WEBP 이미지만 업로드할 수 있습니다.");

        verifyNoInteractions(minioObjectStorage);
    }

    @Test
    @DisplayName("빈 파일이면 InvalidFileException 예외가 발생한다")
    void uploadProfileImage_throwsExceptionForEmptyFile(){
        MockMultipartFile file = new MockMultipartFile("file", "empty.jpg", "image/jpeg", new byte[0]);

        assertThatThrownBy(() -> minioService.uploadProfileImage(1L, file))
                .isInstanceOf(InvalidFileException.class)
                .hasMessage("업로드할 파일이 존재하지 않습니다.");

        verifyNoInteractions(minioObjectStorage);
    }

    @Test
    @DisplayName("파일을 삭제한다")
    void deleteFile_success(){
        minioService.deleteFile("profiles/1/test.jpg");

        verify(minioObjectStorage).remove("profiles/1/test.jpg");
    }

    @Test
    @DisplayName("빈 objectKey는 삭제하지 않는다")
    void deleteFile_doesNothingForBlankObjectKey() {
        minioService.deleteFile(" ");
        verifyNoInteractions(minioObjectStorage);
    }

    @Test
    @DisplayName("실패 : 파일이 null이면 InvalidFileException 예외가 발생한다")
    void uploadProfileImage_throwsExceptionForNullFile() {
        assertThatThrownBy(() -> minioService.uploadProfileImage(1L, null))
                .isInstanceOf(InvalidFileException.class)
                .hasMessage("업로드할 파일이 존재하지 않습니다.");

        verifyNoInteractions(minioObjectStorage);
    }

    @Test
    @DisplayName("성공 : 파일명이 공백이면 기본 .jpg 확장자로 업로드된다")
    void uploadProfileImage_blankFilename_defaultsToJpg() {
        MockMultipartFile file = new MockMultipartFile("file", "   ", "image/jpeg", "data".getBytes());

        String objectKey = minioService.uploadProfileImage(1L, file);

        assertThat(objectKey).startsWith("profiles/1/").endsWith(".jpg");
    }

    @Test
    @DisplayName("실패 : ContentType이 null이면 InvalidFileException 예외가 발생한다")
    void uploadProfileImage_throwsExceptionForNullContentType() {
        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", null, "data".getBytes());

        assertThatThrownBy(() -> minioService.uploadProfileImage(1L, file))
                .isInstanceOf(InvalidFileException.class)
                .hasMessage("JPG, PNG, WEBP 이미지만 업로드할 수 있습니다.");
    }

    @Test
    @DisplayName("실패 : 5MB를 초과하는 대용량 파일이면 InvalidFileException 예외가 발생한다")
    void uploadProfileImage_throwsExceptionForExceedingFileSize() {
        byte[] largeBytes = new byte[5 * 1024 * 1024 + 1];
        MockMultipartFile file = new MockMultipartFile("file", "large.jpg", "image/jpeg", largeBytes);

        assertThatThrownBy(() -> minioService.uploadProfileImage(1L, file))
                .isInstanceOf(InvalidFileException.class)
                .hasMessage("사진은 5MB 이하만 업로드할 수 있습니다.");
    }

    @Test
    @DisplayName("성공 : 확장자가 없는 파일명이어도 기본 .jpg 확장자가 부여된다")
    void uploadProfileImage_withoutExtension_defaultsToJpg() {
        MockMultipartFile file = new MockMultipartFile("file", "mushroom_no_ext", "image/jpeg", "data".getBytes());

        String objectKey = minioService.uploadProfileImage(1L, file);

        assertThat(objectKey).endsWith(".jpg");
    }

    @Test
    @DisplayName("성공 : webp 확장자 파일 업로드 시 .webp 확장자가 유지된다")
    void uploadProfileImage_webp_success() {
        MockMultipartFile file = new MockMultipartFile("file", "test.webp", "image/webp", "data".getBytes());

        String objectKey = minioService.uploadProfileImage(1L, file);

        assertThat(objectKey).endsWith(".webp");
    }

    @Test
    @DisplayName("성공 : 문의 사진 업로드 시 inquiries/{answerId}/ 경로로 생성된다")
    void uploadInquiryPhoto_success() {
        MockMultipartFile file = new MockMultipartFile("file", "inquiry.png", "image/png", "data".getBytes());

        String objectKey = minioService.uploadInquiryPhoto(100L, file);

        assertThat(objectKey).startsWith("inquiries/100/").endsWith(".png");
        verify(minioObjectStorage).put(objectKey, file);
    }

    @Test
    @DisplayName("파일 업로드 도중 MinIO 예외 발생 시 FileUploadException이 발생한다")
    void uploadProfileImage_minioError_throwsFileUploadException() {
        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "data".getBytes());
        doThrow(new MinioObjectStorageException("MinIO 서버 다운", new RuntimeException()))
                .when(minioObjectStorage).put(anyString(), any());

        assertThatThrownBy(() -> minioService.uploadProfileImage(1L, file))
                .isInstanceOf(FileUploadException.class)
                .hasMessage("사진 업로드에 실패했습니다.");
    }

    @Test
    @DisplayName("null인 objectKey로 파일 삭제 시 아무 작업도 하지 않는다")
    void deleteFile_nullKey_doesNothing() {
        minioService.deleteFile(null);
        verifyNoInteractions(minioObjectStorage);
    }

    @Test
    @DisplayName("파일 삭제 실패 시 FileDeleteException 예외가 발생한다")
    void deleteFile_minioError_throwsFileDeleteException() {
        doThrow(new MinioObjectStorageException("삭제 에러", new RuntimeException()))
                .when(minioObjectStorage).remove(anyString());

        assertThatThrownBy(() -> minioService.deleteFile("profiles/1/test.jpg"))
                .isInstanceOf(FileDeleteException.class)
                .hasMessage("사진 삭제에 실패했습니다.");
    }

    @Test
    @DisplayName("deleteQuietly는 MinioObjectStorage.removeQuietly로 위임한다")
    void deleteQuietly_delegatesToMinioObjectStorage() {
        assertThatCode(() -> {
            minioService.deleteQuietly("profiles/1/test.jpg");
            minioService.deleteQuietly(null);
            minioService.deleteQuietly("  ");
        }).doesNotThrowAnyException();

        verify(minioObjectStorage).removeQuietly("profiles/1/test.jpg");
        verify(minioObjectStorage).removeQuietly(null);
        verify(minioObjectStorage).removeQuietly("  ");
    }

    @Test
    @DisplayName("서버 시작 시 MinIO 통신 장애 발생 시 FileStorageException이 발생한다")
    void ensureBucketExists_minioError_throwsFileStorageException() {
        doThrow(new MinioObjectStorageException("MinIO 연결 실패", new RuntimeException()))
                .when(minioObjectStorage).ensureBucketExists();

        assertThatThrownBy(() -> minioService.ensureBucketExists())
                .isInstanceOf(FileStorageException.class)
                .hasMessage("이미지 저장소를 사용할 수 없습니다.");
    }

    @Test
    @DisplayName("presigned URL 발급 성공 시 내부 주소가 공개 프록시 주소로 치환된다")
    void presignedGetUrl_rewritesToPublicUrl() {
        when(minioObjectStorage.presignedGetUrl(eq("inquiries/1/uuid.jpg"), any(Duration.class)))
                .thenReturn("http://storage.internal:9000/bucket/inquiries/1/uuid.jpg?X-Amz-Signature=abc");

        String url = minioService.presignedGetUrl("inquiries/1/uuid.jpg");

        assertThat(url).isEqualTo("https://yes-nhn.site/storage-proxy/bucket/inquiries/1/uuid.jpg?X-Amz-Signature=abc");
    }

    @Test
    @DisplayName("presigned URL 발급 실패 시 예외를 던지지 않고 null을 반환한다")
    void presignedGetUrl_failure_returnsNull() {
        when(minioObjectStorage.presignedGetUrl(anyString(), any(Duration.class)))
                .thenThrow(new MinioObjectStorageException("발급 실패", new RuntimeException()));

        String url = minioService.presignedGetUrl("inquiries/1/uuid.jpg");

        assertThat(url).isNull();
    }

    @Test
    @DisplayName("objectKey가 없으면 presigned URL을 발급하지 않는다")
    void presignedGetUrl_blankObjectKey_returnsNull() {
        assertThat(minioService.presignedGetUrl(null)).isNull();
        assertThat(minioService.presignedGetUrl(" ")).isNull();
        verifyNoInteractions(minioObjectStorage);
    }

    @Test
    @DisplayName("캐시 히트 시 MinIO를 호출하지 않고 캐시된 URL을 반환한다")
    void presignedGetUrl_returnsCachedUrl_whenCacheHit() {
        String objectKey = "profiles/1/uuid.jpg";
        String cachedUrl = "https://yes-nhn.site/storage-proxy/bucket/profiles/1/uuid.jpg?X-Amz-Signature=cached";
        when(valueOperations.get("minio:presigned-url:" + objectKey)).thenReturn(cachedUrl);

        String url = minioService.presignedGetUrl(objectKey);

        assertThat(url).isEqualTo(cachedUrl);
        verifyNoInteractions(minioObjectStorage);
    }

    @Test
    @DisplayName("캐시 미스 시 새로 발급받은 Public URL을 25분 TTL로 Redis에 저장한다")
    void presignedGetUrl_cachesNewlyIssuedUrl_whenCacheMiss() {
        String objectKey = "profiles/1/uuid.jpg";
        String expectedPublicUrl = "https://yes-nhn.site/storage-proxy/bucket/profiles/1/uuid.jpg?X-Amz-Signature=new";
        when(valueOperations.get("minio:presigned-url:" + objectKey)).thenReturn(null);
        when(minioObjectStorage.presignedGetUrl(objectKey, Duration.ofMinutes(30)))
                .thenReturn("http://storage.internal:9000/bucket/profiles/1/uuid.jpg?X-Amz-Signature=new");

        String url = minioService.presignedGetUrl(objectKey);

        assertThat(url).isEqualTo(expectedPublicUrl);
        verify(valueOperations).set("minio:presigned-url:" + objectKey, expectedPublicUrl, Duration.ofMinutes(25));
    }

    @Test
    @DisplayName("Redis 조회 실패 시에도 예외 없이 MinIO에서 직접 발급하여 정상 반환한다")
    void presignedGetUrl_fallsBackToMinio_whenRedisFails() {
        String objectKey = "profiles/1/uuid.jpg";
        when(valueOperations.get(anyString())).thenThrow(new RuntimeException("Redis connection refused"));
        when(minioObjectStorage.presignedGetUrl(eq(objectKey), any(Duration.class)))
                .thenReturn("http://storage.internal:9000/bucket/profiles/1/uuid.jpg?X-Amz-Signature=direct");

        String url = minioService.presignedGetUrl(objectKey);

        assertThat(url).isEqualTo("https://yes-nhn.site/storage-proxy/bucket/profiles/1/uuid.jpg?X-Amz-Signature=direct");
    }

    @Test
    @DisplayName("deleteQuietly 호출 시 MinIO 객체 삭제와 함께 Redis 캐시도 삭제한다")
    void deleteQuietly_evictsCache() {
        String objectKey = "profiles/1/uuid.jpg";

        minioService.deleteQuietly(objectKey);

        verify(minioObjectStorage).removeQuietly(objectKey);
        verify(redis).delete("minio:presigned-url:" + objectKey);
    }

    @Test
    @DisplayName("deleteFile 호출 시 MinIO 객체 삭제와 함께 Redis 캐시도 삭제한다")
    void deleteFile_evictsCache() {
        String objectKey = "profiles/1/uuid.jpg";

        minioService.deleteFile(objectKey);

        verify(minioObjectStorage).remove(objectKey);
        verify(redis).delete("minio:presigned-url:" + objectKey);
    }
}