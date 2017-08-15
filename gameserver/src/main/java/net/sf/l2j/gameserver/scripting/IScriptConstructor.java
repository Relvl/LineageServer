package net.sf.l2j.gameserver.scripting;

/**
 * �������������� ������ ��� ������������ ������.
 *
 * @author Johnson / 15.08.2017
 */
@FunctionalInterface
public interface IScriptConstructor<Q extends Quest> {
    Q make();
}
