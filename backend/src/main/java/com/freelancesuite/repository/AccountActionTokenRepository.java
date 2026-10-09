package com.freelancesuite.repository;

import com.freelancesuite.entity.AccountActionToken;
import com.freelancesuite.entity.enums.AccountTokenPurpose;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface AccountActionTokenRepository extends JpaRepository<AccountActionToken, Long> {
    Optional<AccountActionToken> findByTokenHashAndPurpose(String tokenHash, AccountTokenPurpose purpose);
    Optional<AccountActionToken> findFirstByUser_IdAndPurposeAndUsedAtIsNullOrderByCreatedAtDesc(
            Long userId, AccountTokenPurpose purpose);
    void deleteByUserIdAndPurposeAndUsedAtIsNull(Long userId, AccountTokenPurpose purpose);
}
