package uk.co.keirahopkins.hiam.velocity.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.proxy.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.keirahopkins.hiam.velocity.database.AccountRepository;
import uk.co.keirahopkins.hiam.velocity.database.CredentialRepository;
import uk.co.keirahopkins.hiam.velocity.model.Account;
import uk.co.keirahopkins.hiam.velocity.model.AuthState;
import uk.co.keirahopkins.hiam.velocity.util.ClientDetector;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PostLoginEventListener {
    private static final Logger logger = LoggerFactory.getLogger(PostLoginEventListener.class);
    
    private final AccountRepository accountRepository;
    private final CredentialRepository credentialRepository;
    private final Map<UUID, AuthState> playerStates;

    public PostLoginEventListener(AccountRepository accountRepository, CredentialRepository credentialRepository) {
        this.accountRepository = accountRepository;
        this.credentialRepository = credentialRepository;
        this.playerStates = new ConcurrentHashMap<>();
    }

    @Subscribe
    public void onPostLogin(PostLoginEvent event) {
        Player player = event.getPlayer();
        String username = player.getUsername();
        UUID playerId = player.getUniqueId();
        
        logger.info("Player {} ({}) joined the server", username, playerId);
        
        Optional<Account> accountOpt = accountRepository.findByUsername(username.toLowerCase());
        
        if (accountOpt.isEmpty()) {
            playerStates.put(playerId, AuthState.NEW);
            logger.info("New player detected: {}", username);
        } else {
            Account account = accountOpt.get();
            boolean isEagler = ClientDetector.isEaglerClient(player);
            boolean hasPassword = credentialRepository.hasPassword(account.getId());
            Optional<UUID> premiumUuid = credentialRepository.getPremiumUuid(account.getId());

            if (isEagler) {
                if (hasPassword) {
                    playerStates.put(playerId, AuthState.OFFLINE_REGISTERED);
                    logger.info("Password auth required (Eagler): {}", username);
                } else {
                    playerStates.put(playerId, AuthState.NEW);
                    logger.info("Account has no credentials set (Eagler): {}", username);
                }
                return;
            }

            if (premiumUuid.isPresent()) {
                if (!premiumUuid.get().equals(playerId)) {
                    logger.warn("Premium UUID mismatch for {}: stored={}, session={}", username, premiumUuid.get(), playerId);
                }
                playerStates.put(playerId, AuthState.PREMIUM_ACTIVE);
                logger.info("Premium player authenticated: {}", username);
                return;
            }

            if (hasPassword) {
                playerStates.put(playerId, AuthState.OFFLINE_REGISTERED);
                logger.info("Password auth required: {}", username);
                return;
            }

            playerStates.put(playerId, AuthState.NEW);
            logger.info("Account has no credentials set: {}", username);
        }
    }

    public AuthState getPlayerState(UUID playerId) {
        return playerStates.getOrDefault(playerId, AuthState.NEW);
    }

    public void setPlayerState(UUID playerId, AuthState state) {
        playerStates.put(playerId, state);
    }

    public void removePlayerState(UUID playerId) {
        playerStates.remove(playerId);
    }

    public void markAuthenticated(UUID playerId) {
        setPlayerState(playerId, AuthState.AUTHENTICATED);
    }

    public boolean isAuthenticated(UUID playerId) {
        AuthState state = getPlayerState(playerId);
        return state == AuthState.AUTHENTICATED || state == AuthState.PREMIUM_ACTIVE;
    }
}
