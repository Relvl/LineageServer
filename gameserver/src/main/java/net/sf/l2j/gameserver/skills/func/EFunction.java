package net.sf.l2j.gameserver.skills.func;

import net.sf.l2j.gameserver.skills.func.custom.*;

/**
 * @author Johnson / 13.08.2017
 */
public enum EFunction {
    // ---------------------------------------- Математические функции. Все действия происходят в результате калькуляции лямбды.
    /** Устанавливает значение. */
    FUNC_SET("Set", true, FuncSet::new, null),
    /** Добавляет базовое значение, умноженное на аргумент. */
    FUNC_BASE_MUL("BaseMul", true, FuncBaseMul::new, null),
    /** Добавляет значение. */
    FUNC_ADD("Add", true, FuncAdd::new, null),
    /** Вычитает значение. */
    FUNC_SUB("Sub", true, FuncSub::new, null),
    /** Умножает на значение. */
    FUNC_MUL("Mul", true, FuncMul::new, null),
    /** Делит на значение. */
    FUNC_DIV("Div", true, FuncDiv::new, null),

    // ---------------------------------------- Особые функции.
    /** Вычисляет в зависимости от заточки шмотки. */
    FUNC_ENCHANT("Enchant", true, FuncEnchant::new, null),

    // ---------------------------------------- Функции, регулирующие базовые статы.
    HENNA_CON(false, null, new FuncHennaCON()),
    HENNA_DEX(false, null, new FuncHennaDEX()),
    HENNA_INT(false, null, new FuncHennaINT()),
    HENNA_MEN(false, null, new FuncHennaMEN()),
    HENNA_STR(false, null, new FuncHennaSTR()),
    HENNA_WIT(false, null, new FuncHennaWIT()),

    P_ATK_MOD(false, null, new FuncPAtkMod()),
    P_ATK_SPEED(false, null, new FuncPAtkSpeed()),
    ATK_CRITICAL(false, null, new FuncAtkCritical()),
    ATK_ACCURACY(false, null, new FuncAtkAccuracy()),

    P_DEF_MOD(false, null, new FuncPDefMod()),
    ATK_EVASION(false, null, new FuncAtkEvasion()),

    M_ATK_MOD(false, null, new FuncMAtkMod()),
    M_ATK_SPEED(false, null, new FuncMAtkSpeed()),
    M_ATK_CRITICAL(false, null, new FuncMAtkCritical()),

    M_DEF_MOD(false, null, new FuncMDefMod()),

    MAX_CP_MUL(false, null, new FuncMaxCpMul()),
    MAX_HP_MUL(false, null, new FuncMaxHpMul()),
    MAX_MP_MUL(false, null, new FuncMaxMpMul()),

    MOVE_SPEED(false, null, new FuncMoveSpeed());

    private final String funcName;
    private final boolean commonFunc;
    private final IFuncFactory<Func> funcFactory;
    private final Func func;

    EFunction(String funcName, boolean commonFunc, IFuncFactory<Func> funcFactory, Func func) {
        this.funcName = funcName;
        this.commonFunc = commonFunc;
        this.funcFactory = funcFactory;
        this.func = func;
    }

    EFunction(boolean commonFunc, IFuncFactory<Func> funcFactory, Func func) {
        this.funcName = null;
        this.commonFunc = commonFunc;
        this.funcFactory = funcFactory;
        this.func = func;
    }

    public IFuncFactory<Func> getFuncFactory() { return funcFactory; }

    public Func getFunc() { return func; }

    public static EFunction getByName(String name) {
        for (EFunction function : values()) {
            if (function.commonFunc && function.funcName.equalsIgnoreCase(name)) {
                return function;
            }
        }
        return null;
    }
}
