package net.sf.l2j.gameserver.model;

import net.sf.l2j.gameserver.datatables.AccessLevels;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class L2AccessLevel {
    private static final Logger LOGGER = LoggerFactory.getLogger(L2AccessLevel.class);

    private int _accessLevel;
    private String _name;

    L2AccessLevel[] _childsAccessLevel;
    private String _childs;

    private int _nameColor;
    private int _titleColor;

    private boolean _isGm;

    private boolean _allowPeaceAttack;
    private boolean _allowFixedRes;
    private boolean _allowTransaction;
    private boolean _allowAltG;
    private boolean _giveDamage;
    private boolean _takeAggro;
    private boolean _gainExp;

    public L2AccessLevel(int accessLevel, String name, int nameColor, int titleColor, String childs, boolean isGm, boolean allowPeaceAttack, boolean allowFixedRes, boolean allowTransaction, boolean allowAltG, boolean giveDamage, boolean takeAggro, boolean gainExp) {
        _accessLevel = accessLevel;
        _name = name;
        _nameColor = nameColor;
        _titleColor = titleColor;
        _childs = childs;
        _isGm = isGm;
        _allowPeaceAttack = allowPeaceAttack;
        _allowFixedRes = allowFixedRes;
        _allowTransaction = allowTransaction;
        _allowAltG = allowAltG;
        _giveDamage = giveDamage;
        _takeAggro = takeAggro;
        _gainExp = gainExp;
    }

    public int getLevel() {
        return _accessLevel;
    }

    public String getName() {
        return _name;
    }

    public int getNameColor() {
        return _nameColor;
    }

    public int getTitleColor() {
        return _titleColor;
    }

    public boolean isGm() {
        return _isGm;
    }

    public boolean allowPeaceAttack() {
        return _allowPeaceAttack;
    }

    public boolean allowFixedRes() {
        return _allowFixedRes;
    }

    public boolean allowTransaction() {
        return _allowTransaction;
    }

    public boolean allowAltG() {
        return _allowAltG;
    }

    public boolean canGiveDamage() {
        return _giveDamage;
    }

    public boolean canTakeAggro() {
        return _takeAggro;
    }

    public boolean canGainExp() {
        return _gainExp;
    }

    public boolean hasChildAccess(L2AccessLevel accessLevel) {
        if (_childsAccessLevel == null) {
            if (_childs == null) { return false; }

            setChildAccess(_childs);
            for (L2AccessLevel childAccess : _childsAccessLevel) {
                if (childAccess != null && (childAccess.getLevel() == accessLevel.getLevel() || childAccess.hasChildAccess(accessLevel))) { return true; }
            }
        }
        else {
            for (L2AccessLevel childAccess : _childsAccessLevel) {
                if (childAccess != null && (childAccess.getLevel() == accessLevel.getLevel() || childAccess.hasChildAccess(accessLevel))) { return true; }
            }
        }
        return false;
    }

    private void setChildAccess(String childs) {
        String[] childsSplit = childs.split(";");

        _childsAccessLevel = new L2AccessLevel[childsSplit.length];

        for (int i = 0; i < childsSplit.length; ++i) {
            L2AccessLevel accessLevelInst = AccessLevels.getInstance().getAccessLevel(Integer.parseInt(childsSplit[i]));

            if (accessLevelInst == null) {
                LOGGER.warn("AccessLevel: Undefined child access level {}", childsSplit[i]);
                continue;
            }

            if (accessLevelInst.hasChildAccess(this)) {
                LOGGER.warn("AccessLevel: Child access tree overlapping for {} and {}", _name, accessLevelInst.getName());
                continue;
            }

            _childsAccessLevel[i] = accessLevelInst;
        }
    }
}