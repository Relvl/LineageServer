package johnson.loginserver.security;

import johnson.loginserver.LoginServer;
import johnson.loginserver.config.BannedIpAddresses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Johnson / 04.06.2017
 */
public class SecurityController {
    private static final Logger LOGGER = LoggerFactory.getLogger(SecurityController.class);

    private final BannedIpAddresses bannedIpAddresses = BannedIpAddresses.load();
    private final Map<InetAddress, FailedLoginAttempt> bruteProtection = new ConcurrentHashMap<>();

    private SecurityController() {
        LOGGER.info("{} loaded with {} banned IPs.", getClass().getSimpleName(), bannedIpAddresses.getSize());
    }

    public static SecurityController getInstance() { return SingletonHolder.instance; }

    public void addBannedIpAddress(InetAddress address, long duration, String initiator, String reason) {
        bannedIpAddresses.addBannedIp(address, duration, initiator, reason);
    }

    public boolean isBannedAddress(InetAddress address) { return bannedIpAddresses.isIpBanned(address); }

    /** TODO Это не полная проверка. Нужно прикрутить еще защиту от брута с нескольких IP, и обход с корректным логином на другую учетку. */
    public void handleIncorrectLognis(InetAddress address, String password) {
        FailedLoginAttempt failedAttempt = bruteProtection.get(address);
        if (failedAttempt == null) {
            bruteProtection.put(address, failedAttempt = new FailedLoginAttempt(address, password));
        }
        else {
            failedAttempt.increaseCounter(password);
        }
        if (failedAttempt.getCount() >= LoginServer.CONFIG.loginServer.loginsTryBeforeBan) {
            LOGGER.info("Banning '{}' for {} seconds due to {} invalid user/pass attempts", address.getHostAddress(), LoginServer.CONFIG.loginServer.loginsBlockAfterBan, failedAttempt.getCount());
            addBannedIpAddress(
                    address,
                    LoginServer.CONFIG.loginServer.loginsBlockAfterBan * 1000,
                    "LoginServer",
                    "Too many failed login attempts"
            );
        }
    }

    public void handleCorrectLogin(InetAddress address) { bruteProtection.remove(address); }

    private static final class SingletonHolder {
        private static final SecurityController instance = new SecurityController();
    }
}
