package site.yesaido.user_server.domain.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import site.yesaido.user_server.domain.user.entity.OAuthUser;

import java.util.Optional;

public interface OAuthUserRepository extends JpaRepository<OAuthUser, Long> {
    Optional<OAuthUser> findByProviderAndProviderSubjectId(String provider, String providerSubjectId);
}
