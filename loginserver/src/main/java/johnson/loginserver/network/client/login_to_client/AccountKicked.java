package johnson.loginserver.network.client.login_to_client;

import johnson.loginserver.L2LoginClient;
import org.mmocore.network.SendablePacket;

public final class AccountKicked extends SendablePacket<L2LoginClient> {
    public enum AccountKickedReason {
        REASON_DATA_STEALER(0x01),
        REASON_GENERIC_VIOLATION(0x08),
        REASON_7_DAYS_SUSPENDED(0x10),
        REASON_PERMANENTLY_BANNED(0x20);

        private final int _code;

        AccountKickedReason(int code) {
            _code = code;
        }

        public final int getCode() {
            return _code;
        }
    }

    private final AccountKickedReason _reason;

    public AccountKicked(AccountKickedReason reason) {
        _reason = reason;
    }

    @Override
    protected void write() {
        writeC(0x02);
        writeD(_reason.getCode());
    }
}