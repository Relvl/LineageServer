package net.sf.l2j.loginserver.network.serverpackets;

/**
 * Fromat: d d: the failure reason
 */
public final class LoginFail extends L2LoginServerPacket
{
	public static enum LoginFailReason
	{
		REASON_SYSTEM_ERROR(0x01),
		REASON_PASS_WRONG(0x02),
		REASON_USER_OR_PASS_WRONG(0x03),
		REASON_ACCESS_FAILED(0x04),
		REASON_ACCOUNT_IN_USE(0x07),
		REASON_SERVER_OVERLOADED(0x0f),
		REASON_SERVER_MAINTENANCE(0x10),
		REASON_TEMP_PASS_EXPIRED(0x11),
		REASON_DUAL_BOX(0x23);
		
		private final int _code;
		
		LoginFailReason(int code)
		{
			_code = code;
		}
		
		public final int getCode()
		{
			return _code;
		}
	}
	
	private final LoginFailReason _reason;
	
	public LoginFail(LoginFailReason reason)
	{
		_reason = reason;
	}
	
	@Override
	protected void write()
	{
		writeC(0x01);
		writeD(_reason.getCode());
	}
}