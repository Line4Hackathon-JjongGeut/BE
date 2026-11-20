package com.jjonggeut.domain.auth.repository;

import com.jjonggeut.domain.auth.entity.RefreshToken;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

	Optional<RefreshToken> findByUserId(Long userId);

	Optional<RefreshToken> findByTokenHash(String tokenHash);
}
