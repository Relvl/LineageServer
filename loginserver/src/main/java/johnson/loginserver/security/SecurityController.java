package johnson.loginserver.security;

import johnson.loginserver.LoginServer;
import johnson.loginserver.config.BannedIpAddresses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Johnson / 04.06.2017
 */
public class SecurityController {
    private static final Logger LOGGER = LoggerFactory.getLogger(SecurityController.class);

    private final Map<InetAddress, Long> bannedIps = new ConcurrentHashMap<>();
    private final Map<InetAddress, FailedLoginAttempt> bruteProtection = new ConcurrentHashMap<>();

    private SecurityController() {
        for (BannedIpAddresses.BannedAddressEntry entry : BannedIpAddresses.load().getAddresses()) {
            try {
                long duration = ChronoUnit.MILLIS.between(LocalDateTime.now(), entry.getExpired());
                if (duration <= 0) { continue; }
                InetAddress address = InetAddress.getByName(entry.getIp());
                addBannedIpAddress(address, duration);
            } catch (UnknownHostException e) {
                LOGGER.error("Failed to load banned IP address {}", entry.getIp(), e);
            }
        }
        LOGGER.info("{} loaded with {} banned IPs.", getClass().getSimpleName(), bannedIps.size());
    }

    public void addBannedIpAddress(InetAddress address, long duration) {
        if (!bannedIps.containsKey(address)) {
            bannedIps.put(address, System.currentTimeMillis() + duration);
        }
        else {
            LOGGER.warn("IP address {} allready banned.", address.toString());
        }
    }

    public boolean isBannedAddress(InetAddress address) {
        Long expiration = bannedIps.get(address);
        if (expiration != null) {
            if (expiration > 0 && System.currentTimeMillis() > expiration) {
                bannedIps.remove(address);
                return false;
            }
            return true;
        }
        return false;
    }

    /** TODO Это не полная проверка. Нужно прикрутить еще защиту от брута с нескольких IP, и обход с корректным логином на другую учетку. */
    public void handleIncorrectLognis(InetAddress address, String password) {
        FailedLoginAttempt failedAttempt = bruteProtection.get(address);
        if (failedAttempt == null) {
            bruteProtection.put(address, failedAttempt = new FailedLoginAttempt(address, password));
        }
        else {
            failedAttempt.increaseCounter(password);
        }
        if (failedAttempt.getCount() >= LoginServer.config.clientListener.loginsTryBeforeBan) {
            LOGGER.info("Banning '{}' for {} seconds due to {} invalid user/pass attempts", address.getHostAddress(), LoginServer.config.clientListener.loginsBlockAfterBan, failedAttempt.getCount());
            addBannedIpAddress(address, LoginServer.config.clientListener.loginsBlockAfterBan * 1000);
        }
    }

    public void handleCorrectLogin(InetAddress address) {
        bruteProtection.remove(address);
    }

    public static SecurityController getInstance() {
        return SingletonHolder.instance;
    }

    private static final class SingletonHolder {
        private static final SecurityController instance = new SecurityController();
    }
}
