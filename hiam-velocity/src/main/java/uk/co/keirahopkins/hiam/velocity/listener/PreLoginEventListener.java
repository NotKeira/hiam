package uk.co.keirahopkins.hiam.velocity.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.keirahopkins.hiam.velocity.manager.RateLimiter;

public class PreLoginEventListener {
    private static final Logger logger = LoggerFactory.getLogger(PreLoginEventListener.class);
    
    private final RateLimiter rateLimiter;

    public PreLoginEventListener(RateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    @Subscribe
    public void onPreLogin(PreLoginEvent event) {
        String username = event.getUsername();
        
        if (rateLimiter.isLocked(username)) {
            logger.warn("Blocked login attempt for locked account: {}", username);
            event.setResult(PreLoginEvent.PreLoginComponentResult.denied(
                Component.text("Too many failed login attempts. Please try again later.")
                    .color(NamedTextColor.RED)
            ));
        }
    }
}
