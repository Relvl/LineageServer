package johnson.loginserver.security;

import java.net.InetAddress;

/**
 * @author Johnson / 03.06.2017
 */
public class FailedLoginAttempt {
    private int _count;
    private long _lastAttempTime;
    private String _lastPassword;

    public FailedLoginAttempt(InetAddress address, String lastPassword) {
        _count = 1;
        _lastAttempTime = System.currentTimeMillis();
        _lastPassword = lastPassword;
    }

    public void increaseCounter(String password) {
        if (!_lastPassword.equals(password)) {
            // check if theres a long time since last wrong try
            if (System.currentTimeMillis() - _lastAttempTime < 300 * 1000) {
                _count++;
            }
            // restart the status
            else {
                _count = 1;
            }

            _lastPassword = password;
            _lastAttempTime = System.currentTimeMillis();
        }
        // trying the same password is not brute force
        else {
            _lastAttempTime = System.currentTimeMillis();
        }
    }

    public int getCount() {
        return _count;
    }

    public void increaseCounter() {
        _count++;
    }
}
