package net.sf.l2j.gameserver.model.actor.knownlist;

import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.actor.L2Character;

@SuppressWarnings("ObjectEquality")
public class CharKnownList extends ObjectKnownList {
    public CharKnownList(L2Character activeChar) {
        super(activeChar);
    }

    @Override
    public boolean removeKnownObject(L2Object object) {
        if (!super.removeKnownObject(object)) { return false; }
        L2Character character = (L2Character) this.object;
        if (object == character.getTarget()) { character.setTarget(null); }
        return true;
    }

    @Override
    public final void removeAllKnownObjects() {
        super.removeAllKnownObjects();
        L2Character character = (L2Character) object;
        character.setTarget(null);
        if (character.hasAI()) { character.setAI(null); }
    }
}