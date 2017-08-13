package net.sf.l2j.gameserver.skills.effects;

import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.skill.chance.ChanceCondition;
import net.sf.l2j.gameserver.skills.AbnormalEffect;
import net.sf.l2j.gameserver.skills.func.Env;
import net.sf.l2j.gameserver.skills.conditions.ACondition;
import net.sf.l2j.gameserver.skills.func.FuncTemplate;
import net.sf.l2j.gameserver.skills.func.lambda.ILambda;
import net.sf.l2j.gameserver.templates.skills.L2SkillType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

public final class EffectTemplate {
    private static final Logger LOGGER = LoggerFactory.getLogger(EffectTemplate.class);

    private final Class<?> _func;
    private final Constructor<?> _constructor;

    public final ACondition attachCond;
    public final ACondition applayCond;
    public final ILambda lambda;
    public final int counter;
    public final int period; // in seconds
    public final AbnormalEffect abnormalEffect;
    public List<FuncTemplate> funcTemplates;
    public final String stackType;
    public final float stackOrder;
    public final boolean icon;
    public final double effectPower; // to handle chance
    public final L2SkillType effectType; // to handle resistances etc...

    public final int triggeredId;
    public final int triggeredLevel;
    public final ChanceCondition chanceCondition;

    public EffectTemplate(ACondition pAttachCond, ACondition pApplayCond, String func, ILambda pLambda, int pCounter, int pPeriod, AbnormalEffect pAbnormalEffect, String pStackType, float pStackOrder, boolean showicon, double ePower, L2SkillType eType, int trigId, int trigLvl, ChanceCondition chanceCond) {
        attachCond = pAttachCond;
        applayCond = pApplayCond;
        lambda = pLambda;
        counter = pCounter;
        period = pPeriod;
        abnormalEffect = pAbnormalEffect;
        stackType = pStackType;
        stackOrder = pStackOrder;
        icon = showicon;
        effectPower = ePower;
        effectType = eType;

        triggeredId = trigId;
        triggeredLevel = trigLvl;
        chanceCondition = chanceCond;

        try {
            _func = Class.forName("net.sf.l2j.gameserver.skills.effects.Effect" + func);
        }
        catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        try {
            _constructor = _func.getConstructor(Env.class, EffectTemplate.class);
        }
        catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public L2Effect getEffect(Env env) {
        if (attachCond != null && !attachCond.test(env)) { return null; }
        try {
            return (L2Effect) _constructor.newInstance(env, this);
        }
        catch (IllegalAccessException e) {
            LOGGER.error("", e);
            return null;
        }
        catch (InstantiationException e) {
            LOGGER.error("", e);
            return null;
        }
        catch (InvocationTargetException e) {
            LOGGER.error("Error creating new instance of Class {} Exception was: {}", _func, e.getTargetException().getMessage(), e.getTargetException());
            return null;
        }
    }

    public void attach(FuncTemplate f) {
        if (funcTemplates == null) { funcTemplates = new ArrayList<>(); }

        funcTemplates.add(f);
    }
}