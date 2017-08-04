package net.sf.l2j.gameserver.model.actor;

import net.sf.l2j.commons.DefaultConstructor;
import net.sf.l2j.commons.database.AUserDefinedType;
import net.sf.l2j.commons.database.annotation.OrmTypeParam;

@Deprecated
public class PcAppearance extends AUserDefinedType {
    @OrmTypeParam(0)
    private Byte face;
    @OrmTypeParam(1)
    private Byte hairColor;
    @OrmTypeParam(2)
    private Byte hairStyle;
    @OrmTypeParam(3)
    private Boolean isFemale;
    @OrmTypeParam(4)
    private Integer nameColor = 0xFFFFFF;
    @OrmTypeParam(5)
    private Integer titleColor = 0xFFFF77;
    @OrmTypeParam(6)
    private Integer recomHave = 0;
    @OrmTypeParam(7)
    private Integer recomLeft = 0;

    @DefaultConstructor
    public PcAppearance() {}

    public PcAppearance(byte face, byte hairColor, byte hairStyle, boolean isFemale) {
        this.face = face;
        this.hairColor = hairColor;
        this.hairStyle = hairStyle;
        this.isFemale = isFemale;
    }

    public final byte getFace() { return face; }

    public final void setFace(int value) { face = (byte) value; }

    public final byte getHairColor() { return hairColor; }

    public final void setHairColor(int value) { hairColor = (byte) value; }

    public final byte getHairStyle() { return hairStyle; }

    public final void setHairStyle(int value) { hairStyle = (byte) value; }

    public final boolean isFemale() { return isFemale; }

    public final void setFemale(boolean isfemale) { isFemale = isfemale; }

    public int getNameColor() { return nameColor; }

    public void setNameColor(int nameColor) { this.nameColor = nameColor; }

    public void setNameColor(int red, int green, int blue) { nameColor = (red & 0xFF) + ((green & 0xFF) << 8) + ((blue & 0xFF) << 16); }

    public int getTitleColor() { return titleColor; }

    public void setTitleColor(int titleColor) { this.titleColor = titleColor; }

    public void setTitleColor(int red, int green, int blue) { titleColor = (red & 0xFF) + ((green & 0xFF) << 8) + ((blue & 0xFF) << 16); }

    public Integer getRecomHave() { return recomHave; }

    public void setRecomHave(Integer recomHave) {
        this.recomHave = Math.min(255, Math.max(0, recomHave));
    }

    public void incRecomHave() { setRecomHave(recomHave + 1); }

    public void decRecomHave(int r) { setRecomHave(recomHave - r); }

    public Integer getRecomLeft() { return recomLeft; }

    public void setRecomLeft(Integer recomLeft) {
        this.recomLeft = Math.max(0, recomLeft);
    }

    public void decRecomLeft() {
        setRecomLeft(recomLeft - 1);
    }
}