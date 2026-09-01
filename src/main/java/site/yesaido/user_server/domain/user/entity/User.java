package site.yesaido.user_server.domain.user.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import site.yesaido.user_server.domain.user.entity.en.Role;
import site.yesaido.user_server.domain.user.entity.en.UserStatus;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class User {
    private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "password")
    private String password;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "role", nullable = false, length = 20)
    private Role role = Role.USER;

    @Column(name = "nickname", nullable = false, unique = true, length = 50)
    private String nickName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private UserStatus status = UserStatus.ACTIVE;

    @Builder.Default
    @Column(name = "email_verified", nullable = false)
    private Boolean emailVerified = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "deleted_at", nullable = true)
    private LocalDateTime deletedAt;

    // 로컬 가입용
    public User(String email, String password, String nickname){
        this.email = email;
        this.password = password;
        this.nickName = nickname;
        this.status = UserStatus.ACTIVE;
        this.emailVerified = false;
    }


    // OAuth 가입용
    public User(String email, String nickname){
        this.email = email;
        this.password = null;
        this.nickName = nickname;
        this.role = Role.USER;
        this.status = UserStatus.ACTIVE;
        this.emailVerified = true;
    }

    public void updateNickname(String nickName){
        this.nickName = nickName;
    }

    public void updateLastLoginAt(){
        this.lastLoginAt = LocalDateTime.now(KOREA_ZONE);
    }

    public void updatePassword(String password){
        this.password = password;
    }

    public void changeToDormant(){
        this.status = UserStatus.DORMANT;
    }

    public void activate(){
        this.status = UserStatus.ACTIVE;
        this.lastLoginAt = LocalDateTime.now(KOREA_ZONE);
    }

    public void withdraw(){
        this.status = UserStatus.DELETED;
        this.deletedAt = LocalDateTime.now(KOREA_ZONE);
    }

    public void releaseDormant(){
        this.status = UserStatus.ACTIVE;
    }

    public void anonymize() {
        String suffix = id + "-" + UUID.randomUUID().toString().substring(0, 12);
        this.email = "deleted-" + suffix + "@deleted.invalid";
        this.nickName = "deleted-" + suffix;
        this.password = null;
        this.emailVerified = false;
    }


}
