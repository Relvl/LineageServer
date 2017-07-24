package net.sf.l2j.gameserver.model.actor;

public class PcAppearance {
    private byte face;
    private byte hairColor;
    private byte hairStyle;
    private boolean isFemale;
    private boolean invisible;
    private int nameColor = 0xFFFFFF;
    private int titleColor = 0xFFFF77;

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

    public boolean isInvisible() { return invisible; }

    public void setInvisible() { invisible = true; }

    public void setVisible() { invisible = false; }

    public int getNameColor() { return nameColor; }

    public void setNameColor(int nameColor) { this.nameColor = nameColor; }

    public void setNameColor(int red, int green, int blue) {
        nameColor = (red & 0xFF) + ((green & 0xFF) << 8) + ((blue & 0xFF) << 16);
    }

    public int getTitleColor() { return titleColor; }

    public void setTitleColor(int titleColor) { this.titleColor = titleColor; }

    public void setTitleColor(int red, int green, int blue) {
        titleColor = (red & 0xFF) + ((green & 0xFF) << 8) + ((blue & 0xFF) << 16);
    }
}