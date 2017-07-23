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
}
