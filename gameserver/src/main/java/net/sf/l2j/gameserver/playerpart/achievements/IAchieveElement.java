package net.sf.l2j.gameserver.playerpart.achievements;

/**
 * @author Johnson / 23.07.2017
 */
public interface IAchieveElement {
    /** ���������. */
    String title();

    /**  */
    String description();

    /** ���������� "���", ������� ����� ��������� �������� �� ����������. */
    default int getCount() { return 1; }

    /**  */
    String getId();

    /**  */
    default String getFullId() {
        EAchieveGroup group = EAchieveGroup.getByClass(this);
        return group.name() + "." + getId();
    }
}
