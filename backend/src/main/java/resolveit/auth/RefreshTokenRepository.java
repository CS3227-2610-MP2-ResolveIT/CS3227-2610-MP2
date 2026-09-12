package resolveit.auth;

import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Persists refresh-token hashes and performs atomic revocation. */
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Integer> {
    /**
     * Finds persisted refresh-token state by its SHA-256 hash.
     *
     * @param tokenHash refresh-token hash
     * @return matching token state, if present
     */
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /**
     * Deletes refresh-token state that expired before the supplied time.
     *
     * @param now expiry cutoff
     * @return number of deleted rows
     */
    long deleteByExpiresAtBefore(Instant now);

    /**
     * Atomically revokes an unexpired token that has not already been revoked.
     *
     * @param tokenHash refresh-token hash
     * @param now revocation time
     * @return number of rows revoked
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update RefreshToken token
               set token.revokedAt = :now,
                   token.version = token.version + 1
             where token.tokenHash = :tokenHash
               and token.revokedAt is null
               and token.expiresAt > :now
            """)
    int revokeIfUsable(@Param("tokenHash") String tokenHash, @Param("now") Instant now);

    /**
     * Revokes a token only when it belongs to the authenticated account.
     *
     * @param tokenHash refresh-token hash
     * @param email authenticated account email
     * @param now revocation time
     * @return number of rows revoked
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update RefreshToken token
               set token.revokedAt = :now,
                   token.version = token.version + 1
             where token.tokenHash = :tokenHash
               and lower(token.user.email) = lower(:email)
               and token.revokedAt is null
            """)
    int revokeForUser(@Param("tokenHash") String tokenHash,
                      @Param("email") String email,
                      @Param("now") Instant now);
}
