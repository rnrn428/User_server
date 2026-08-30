package site.yesaido.user_server.domain.inquiry.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import site.yesaido.common.storage.StorageType;

@Entity
@Table(name = "inquiry_photo")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InquiryPhoto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inquiry_answer_id", nullable = false)
    private InquiryAnswer inquiryAnswer;

    @Column(name = "object_key", nullable = false, length = 500)
    private String objectKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "storage_type", nullable = false, length = 50)
    private StorageType storageType;

    public static InquiryPhoto create(
            InquiryAnswer inquiryAnswer,
            String objectKey
    ){
        InquiryPhoto inquiryPhoto = new InquiryPhoto();
        inquiryPhoto.inquiryAnswer = inquiryAnswer;
        inquiryPhoto.objectKey = objectKey;
        inquiryPhoto.storageType = StorageType.MINIO;

        if(inquiryAnswer != null){
            inquiryAnswer.addInquiryPhoto(inquiryPhoto);
        }

        return inquiryPhoto;
    }

}
