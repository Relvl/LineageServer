package net.sf.l2j.gameserver.network;

import net.sf.l2j.Config;
import net.sf.l2j.commons.lang.HexUtil;
import net.sf.l2j.gameserver.network.clientpackets.*;
import org.mmocore.network.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("MagicNumber")
public final class L2GamePacketHandler implements IPacketHandler<L2GameClient>, IClientFactory<L2GameClient>, IMMOExecutor<L2GameClient> {
    private static final Logger LOGGER = LoggerFactory.getLogger(L2GamePacketHandler.class);

    private static final Map<Integer, Class<? extends ReceivablePacket<L2GameClient>>> PACKETS_CONNECTED = new HashMap<>();
    private static final Map<Integer, Class<? extends ReceivablePacket<L2GameClient>>> PACKETS_AUTHED = new HashMap<>();
    private static final Map<Integer, Class<? extends ReceivablePacket<L2GameClient>>> PACKETS_IN_GAME = new HashMap<>();
    private static final Map<Integer, Class<? extends ReceivablePacket<L2GameClient>>> PACKETS_IN_GAME_D0 = new HashMap<>();

    /* CONNECTED state */
    static {
        PACKETS_CONNECTED.put(0x00, ProtocolVersion.class);
        PACKETS_CONNECTED.put(0x08, AuthLogin.class);
    }

    /* AUTHED state */
    static {
        PACKETS_AUTHED.put(0x09, Logout.class);
        PACKETS_AUTHED.put(0x0B, CharacterCreate.class);
        PACKETS_AUTHED.put(0x0C, CharacterDelete.class);
        PACKETS_AUTHED.put(0x0D, CharacterSelected.class);
        PACKETS_AUTHED.put(0x0E, NewCharacter.class);
        PACKETS_AUTHED.put(0x62, CharacterRestore.class);
        PACKETS_AUTHED.put(0x68, RequestPledgeCrest.class);
    }

    /* IN_GAME state */
    static {
        PACKETS_IN_GAME.put(0x01, MoveBackwardToLocation.class);
        PACKETS_IN_GAME.put(0x03, EnterWorld.class);
        PACKETS_IN_GAME.put(0x04, Action.class);
        PACKETS_IN_GAME.put(0x09, Logout.class);
        PACKETS_IN_GAME.put(0x0A, AttackRequest.class);
        PACKETS_IN_GAME.put(0x0F, RequestItemList.class);
        PACKETS_IN_GAME.put(0x11, RequestUnEquipItem.class);
        PACKETS_IN_GAME.put(0x12, RequestDropItem.class);
        PACKETS_IN_GAME.put(0x14, UseItem.class);
        PACKETS_IN_GAME.put(0x15, TradeRequest.class);
        PACKETS_IN_GAME.put(0x16, AddTradeItem.class);
        PACKETS_IN_GAME.put(0x17, TradeDone.class);
        PACKETS_IN_GAME.put(0x1A, DummyPacket.class); // TODO WTF?!
        PACKETS_IN_GAME.put(0x1B, RequestSocialAction.class);
        PACKETS_IN_GAME.put(0x1C, RequestChangeMoveType.class);
        PACKETS_IN_GAME.put(0x1D, RequestChangeWaitType.class);
        PACKETS_IN_GAME.put(0x1E, RequestSellItem.class);
        PACKETS_IN_GAME.put(0x1F, RequestBuyItem.class);
        PACKETS_IN_GAME.put(0x20, RequestLinkHtml.class);
        PACKETS_IN_GAME.put(0x21, RequestBypassToServer.class);
        PACKETS_IN_GAME.put(0x22, RequestBBSwrite.class);
        PACKETS_IN_GAME.put(0x23, DummyPacket.class); // TODO WTF?!
        PACKETS_IN_GAME.put(0x24, RequestJoinPledge.class);
        PACKETS_IN_GAME.put(0x25, RequestAnswerJoinPledge.class);
        PACKETS_IN_GAME.put(0x26, RequestWithdrawPledge.class);
        PACKETS_IN_GAME.put(0x27, RequestOustPledgeMember.class);
        PACKETS_IN_GAME.put(0x29, RequestJoinParty.class);
        PACKETS_IN_GAME.put(0x2A, RequestAnswerJoinParty.class);
        PACKETS_IN_GAME.put(0x2B, RequestWithdrawParty.class);
        PACKETS_IN_GAME.put(0x2C, RequestOustPartyMember.class);
        PACKETS_IN_GAME.put(0x2E, DummyPacket.class); // TODO WTF?!
        PACKETS_IN_GAME.put(0x2F, RequestMagicSkillUse.class);
        PACKETS_IN_GAME.put(0x30, Appearing.class);
        PACKETS_IN_GAME.put(0x31, SendWarehouseDepositList.class);
        PACKETS_IN_GAME.put(0x32, SendWarehouseWithdrawList.class);
        PACKETS_IN_GAME.put(0x33, RequestShortCutReg.class);
        PACKETS_IN_GAME.put(0x34, DummyPacket.class); // TODO WTF?!
        PACKETS_IN_GAME.put(0x35, RequestShortCutDel.class);
        PACKETS_IN_GAME.put(0x36, CannotMoveAnymore.class);
        PACKETS_IN_GAME.put(0x37, RequestTargetCanceld.class);
        PACKETS_IN_GAME.put(0x38, Say2.class);
        PACKETS_IN_GAME.put(0x3C, RequestPledgeMemberList.class);
        PACKETS_IN_GAME.put(0x3E, DummyPacket.class); // TODO WTF?!
        PACKETS_IN_GAME.put(0x3F, RequestSkillList.class);
        PACKETS_IN_GAME.put(0x42, RequestGetOnVehicle.class);
        PACKETS_IN_GAME.put(0x43, RequestGetOffVehicle.class);
        PACKETS_IN_GAME.put(0x44, AnswerTradeRequest.class);
        PACKETS_IN_GAME.put(0x45, RequestActionUse.class);
        PACKETS_IN_GAME.put(0x46, RequestRestart.class);
        PACKETS_IN_GAME.put(0x48, ValidatePosition.class);
        PACKETS_IN_GAME.put(0x4A, StartRotating.class);
        PACKETS_IN_GAME.put(0x4B, FinishRotating.class);
        PACKETS_IN_GAME.put(0x4D, RequestStartPledgeWar.class);
        PACKETS_IN_GAME.put(0x4E, RequestReplyStartPledgeWar.class);
        PACKETS_IN_GAME.put(0x4F, RequestStopPledgeWar.class);
        PACKETS_IN_GAME.put(0x50, RequestReplyStopPledgeWar.class);
        PACKETS_IN_GAME.put(0x51, RequestSurrenderPledgeWar.class);
        PACKETS_IN_GAME.put(0x52, RequestReplySurrenderPledgeWar.class);
        PACKETS_IN_GAME.put(0x53, RequestSetPledgeCrest.class);
        PACKETS_IN_GAME.put(0x55, RequestGiveNickName.class);
        PACKETS_IN_GAME.put(0x57, RequestShowBoard.class);
        PACKETS_IN_GAME.put(0x58, RequestEnchantItem.class);
        PACKETS_IN_GAME.put(0x59, RequestDestroyItem.class);
        PACKETS_IN_GAME.put(0x5B, SendBypassBuildCmd.class);
        PACKETS_IN_GAME.put(0x5C, RequestMoveToLocationInVehicle.class);
        PACKETS_IN_GAME.put(0x5D, CannotMoveAnymoreInVehicle.class);
        PACKETS_IN_GAME.put(0x5E, RequestFriendInvite.class);
        PACKETS_IN_GAME.put(0x5F, RequestAnswerFriendInvite.class);
        PACKETS_IN_GAME.put(0x60, RequestFriendList.class);
        PACKETS_IN_GAME.put(0x61, RequestFriendDel.class);
        PACKETS_IN_GAME.put(0x63, RequestQuestList.class);
        PACKETS_IN_GAME.put(0x64, RequestQuestAbort.class);
        PACKETS_IN_GAME.put(0x66, RequestPledgeInfo.class);
        PACKETS_IN_GAME.put(0x68, RequestPledgeCrest.class);
        PACKETS_IN_GAME.put(0x69, RequestSurrenderPersonally.class);
        PACKETS_IN_GAME.put(0x6B, RequestAcquireSkillInfo.class);
        PACKETS_IN_GAME.put(0x6C, RequestAcquireSkill.class);
        PACKETS_IN_GAME.put(0x6D, RequestRestartPoint.class);
        PACKETS_IN_GAME.put(0x6E, RequestGMCommand.class);
        PACKETS_IN_GAME.put(0x6F, RequestPartyMatchConfig.class);
        PACKETS_IN_GAME.put(0x70, RequestPartyMatchList.class);
        PACKETS_IN_GAME.put(0x71, RequestPartyMatchDetail.class);
        PACKETS_IN_GAME.put(0x72, RequestCrystallizeItem.class);
        PACKETS_IN_GAME.put(0x73, RequestPrivateStoreManageSell.class);
        PACKETS_IN_GAME.put(0x74, SetPrivateStoreListSell.class);
        PACKETS_IN_GAME.put(0x76, RequestPrivateStoreQuitSell.class);
        PACKETS_IN_GAME.put(0x77, SetPrivateStoreMsgSell.class);
        PACKETS_IN_GAME.put(0x79, RequestPrivateStoreBuy.class);
        PACKETS_IN_GAME.put(0x7B, RequestTutorialLinkHtml.class);
        PACKETS_IN_GAME.put(0x7C, RequestTutorialPassCmdToServer.class);
        PACKETS_IN_GAME.put(0x7D, RequestTutorialQuestionMark.class);
        PACKETS_IN_GAME.put(0x7E, RequestTutorialClientEvent.class);
        PACKETS_IN_GAME.put(0x7F, RequestPetition.class);
        PACKETS_IN_GAME.put(0x80, RequestPetitionCancel.class);
        PACKETS_IN_GAME.put(0x81, RequestGmList.class);
        PACKETS_IN_GAME.put(0x82, RequestJoinAlly.class);
        PACKETS_IN_GAME.put(0x83, RequestAnswerJoinAlly.class);
        PACKETS_IN_GAME.put(0x84, AllyLeave.class);
        PACKETS_IN_GAME.put(0x85, AllyDismiss.class);
        PACKETS_IN_GAME.put(0x86, RequestDismissAlly.class);
        PACKETS_IN_GAME.put(0x87, RequestSetAllyCrest.class);
        PACKETS_IN_GAME.put(0x88, RequestAllyCrest.class);
        PACKETS_IN_GAME.put(0x89, RequestChangePetName.class);
        PACKETS_IN_GAME.put(0x8A, RequestPetUseItem.class);
        PACKETS_IN_GAME.put(0x8B, RequestGiveItemToPet.class);
        PACKETS_IN_GAME.put(0x8C, RequestGetItemFromPet.class);
        PACKETS_IN_GAME.put(0x8E, RequestAllyInfo.class);
        PACKETS_IN_GAME.put(0x8F, RequestPetGetItem.class);
        PACKETS_IN_GAME.put(0x90, RequestPrivateStoreManageBuy.class);
        PACKETS_IN_GAME.put(0x91, SetPrivateStoreListBuy.class);
        PACKETS_IN_GAME.put(0x93, RequestPrivateStoreQuitBuy.class);
        PACKETS_IN_GAME.put(0x94, SetPrivateStoreMsgBuy.class);
        PACKETS_IN_GAME.put(0x96, RequestPrivateStoreSell.class);
        PACKETS_IN_GAME.put(0x9E, RequestPackageSendableItemList.class);
        PACKETS_IN_GAME.put(0x9F, RequestPackageSend.class);
        PACKETS_IN_GAME.put(0xA0, RequestBlock.class);
        PACKETS_IN_GAME.put(0xA2, RequestSiegeAttackerList.class);
        PACKETS_IN_GAME.put(0xA3, RequestSiegeDefenderList.class);
        PACKETS_IN_GAME.put(0xA4, RequestJoinSiege.class);
        PACKETS_IN_GAME.put(0xA5, RequestConfirmSiegeWaitingList.class);
        PACKETS_IN_GAME.put(0xA7, MultiSellChoose.class);
        PACKETS_IN_GAME.put(0xAA, RequestUserCommand.class);
        PACKETS_IN_GAME.put(0xAB, SnoopQuit.class); // TODO Выяснить, что это за механика. У нас не используется.
        PACKETS_IN_GAME.put(0xAC, RequestRecipeBookOpen.class);
        PACKETS_IN_GAME.put(0xAD, RequestRecipeBookDestroy.class);
        PACKETS_IN_GAME.put(0xAE, RequestRecipeItemMakeInfo.class);
        PACKETS_IN_GAME.put(0xAF, RequestRecipeItemMakeSelf.class);
        PACKETS_IN_GAME.put(0xB1, RequestRecipeShopMessageSet.class);
        PACKETS_IN_GAME.put(0xB2, RequestRecipeShopListSet.class);
        PACKETS_IN_GAME.put(0xB3, RequestRecipeShopManageQuit.class);
        PACKETS_IN_GAME.put(0xB5, RequestRecipeShopMakeInfo.class);
        PACKETS_IN_GAME.put(0xB6, RequestRecipeShopMakeItem.class);
        PACKETS_IN_GAME.put(0xB7, RequestRecipeShopManagePrev.class);
        PACKETS_IN_GAME.put(0xB8, ObserverReturn.class);
        PACKETS_IN_GAME.put(0xB9, RequestEvaluate.class);
        PACKETS_IN_GAME.put(0xBA, RequestHennaList.class);
        PACKETS_IN_GAME.put(0xBB, RequestHennaItemInfo.class);
        PACKETS_IN_GAME.put(0xBC, RequestHennaEquip.class);
        PACKETS_IN_GAME.put(0xBD, RequestHennaRemoveList.class);
        PACKETS_IN_GAME.put(0xBE, RequestHennaItemRemoveInfo.class);
        PACKETS_IN_GAME.put(0xBF, RequestHennaRemove.class);
        PACKETS_IN_GAME.put(0xC0, RequestPledgePower.class);
        PACKETS_IN_GAME.put(0xC1, RequestMakeMacro.class);
        PACKETS_IN_GAME.put(0xC2, RequestDeleteMacro.class);
        PACKETS_IN_GAME.put(0xC3, RequestBuyProcure.class);
        PACKETS_IN_GAME.put(0xC4, RequestBuySeed.class);
        PACKETS_IN_GAME.put(0xC5, DlgAnswer.class);
        PACKETS_IN_GAME.put(0xC6, RequestPreviewItem.class);
        PACKETS_IN_GAME.put(0xC7, RequestSSQStatus.class);
        PACKETS_IN_GAME.put(0xCA, GameGuardReply.class);
        PACKETS_IN_GAME.put(0xCC, RequestSendFriendMsg.class);
        PACKETS_IN_GAME.put(0xCD, RequestShowMiniMap.class);
        PACKETS_IN_GAME.put(0xCE, DummyPacket.class); // MSN dialogs?
        PACKETS_IN_GAME.put(0xCF, RequestRecordInfo.class);
    }

    /* IN_GAME state and has second opcode. */
    static {
        PACKETS_IN_GAME_D0.put(0x01, RequestOustFromPartyRoom.class);
        PACKETS_IN_GAME_D0.put(0x02, RequestDismissPartyRoom.class);
        PACKETS_IN_GAME_D0.put(0x03, RequestWithdrawPartyRoom.class);
        PACKETS_IN_GAME_D0.put(0x04, RequestChangePartyLeader.class);
        PACKETS_IN_GAME_D0.put(0x05, RequestAutoSoulShot.class);
        PACKETS_IN_GAME_D0.put(0x06, RequestExEnchantSkillInfo.class);
        PACKETS_IN_GAME_D0.put(0x07, RequestExEnchantSkill.class);
        PACKETS_IN_GAME_D0.put(0x08, RequestManorList.class);
        PACKETS_IN_GAME_D0.put(0x09, RequestProcureCropList.class);
        PACKETS_IN_GAME_D0.put(0x0A, RequestSetSeed.class);
        PACKETS_IN_GAME_D0.put(0x0B, RequestSetCrop.class);
        PACKETS_IN_GAME_D0.put(0x0C, RequestWriteHeroWords.class);
        PACKETS_IN_GAME_D0.put(0x0D, RequestExAskJoinMPCC.class);
        PACKETS_IN_GAME_D0.put(0x0E, RequestExAcceptJoinMPCC.class);
        PACKETS_IN_GAME_D0.put(0x0F, RequestExOustFromMPCC.class);
        PACKETS_IN_GAME_D0.put(0x10, RequestExPledgeCrestLarge.class);
        PACKETS_IN_GAME_D0.put(0x11, RequestExSetPledgeCrestLarge.class);
        PACKETS_IN_GAME_D0.put(0x12, RequestOlympiadObserverEnd.class);
        PACKETS_IN_GAME_D0.put(0x13, RequestOlympiadMatchList.class);
        PACKETS_IN_GAME_D0.put(0x14, RequestAskJoinPartyRoom.class);
        PACKETS_IN_GAME_D0.put(0x15, AnswerJoinPartyRoom.class);
        PACKETS_IN_GAME_D0.put(0x16, RequestListPartyMatchingWaitingRoom.class);
        PACKETS_IN_GAME_D0.put(0x17, RequestExitPartyMatchingWaitingRoom.class);
        PACKETS_IN_GAME_D0.put(0x18, RequestGetBossRecord.class);
        PACKETS_IN_GAME_D0.put(0x19, RequestPledgeSetAcademyMaster.class);
        PACKETS_IN_GAME_D0.put(0x1A, RequestPledgePowerGradeList.class);
        PACKETS_IN_GAME_D0.put(0x1B, RequestPledgeMemberPowerInfo.class);
        PACKETS_IN_GAME_D0.put(0x1C, RequestPledgeSetMemberPowerGrade.class);
        PACKETS_IN_GAME_D0.put(0x1D, RequestPledgeMemberInfo.class);
        PACKETS_IN_GAME_D0.put(0x1E, RequestPledgeWarList.class);
        PACKETS_IN_GAME_D0.put(0x1F, RequestExFishRanking.class);
        PACKETS_IN_GAME_D0.put(0x20, RequestPCCafeCouponUse.class);
        PACKETS_IN_GAME_D0.put(0x22, RequestCursedWeaponList.class);
        PACKETS_IN_GAME_D0.put(0x23, RequestCursedWeaponLocation.class);
        PACKETS_IN_GAME_D0.put(0x24, RequestPledgeReorganizeMember.class);
        PACKETS_IN_GAME_D0.put(0x26, RequestExMPCCShowPartyMembersInfo.class);
        PACKETS_IN_GAME_D0.put(0x27, RequestDuelStart.class);
        PACKETS_IN_GAME_D0.put(0x28, RequestDuelAnswerStart.class);
        PACKETS_IN_GAME_D0.put(0x29, RequestConfirmTargetItem.class);
        PACKETS_IN_GAME_D0.put(0x2A, RequestConfirmRefinerItem.class);
        PACKETS_IN_GAME_D0.put(0x2B, RequestConfirmGemStone.class);
        PACKETS_IN_GAME_D0.put(0x2C, RequestRefine.class);
        PACKETS_IN_GAME_D0.put(0x2D, RequestConfirmCancelItem.class);
        PACKETS_IN_GAME_D0.put(0x2E, RequestRefineCancel.class);
        PACKETS_IN_GAME_D0.put(0x2F, RequestExMagicSkillUseGround.class);
        PACKETS_IN_GAME_D0.put(0x30, RequestDuelSurrender.class);
    }

    @Deprecated
    private static void printDebug(int opcode, ByteBuffer buf, GameClientState state, L2GameClient client) {
        client.onUnknownPacket();
        if (!Config.PACKET_HANDLER_DEBUG) { return; }

        int size = buf.remaining();
        LOGGER.warn("Unknown Packet: 0x{} on State: {} Client: {}", Integer.toHexString(opcode), state, client);
        byte[] array = new byte[size];
        buf.get(array);
        LOGGER.warn(HexUtil.printData(array, size));
    }

    @Deprecated
    private static void printDebugDoubleOpcode(int id2, ByteBuffer buf, GameClientState state, L2GameClient client) {
        client.onUnknownPacket();
        if (!Config.PACKET_HANDLER_DEBUG) { return; }

        int size = buf.remaining();
        LOGGER.warn("Unknown Packet: 0xD0{}:{} on State: {} Client: {}", Integer.toHexString(0xD0), Integer.toHexString(id2), state, client);
        byte[] array = new byte[size];
        buf.get(array);
        LOGGER.warn(HexUtil.printData(array, size));
    }

    @Override
    public ReceivablePacket<L2GameClient> handlePacket(ByteBuffer buf, L2GameClient client) {
        if (client.dropPacket()) { return null; }

        int opcode = buf.get() & 0xFF;
        Class<? extends ReceivablePacket<L2GameClient>> packetClass = null;

        switch (client.getState()) {
            case CONNECTED:
                packetClass = PACKETS_CONNECTED.get(opcode);
                break;
            case AUTHED:
                packetClass = PACKETS_AUTHED.get(opcode);
                break;
            case IN_GAME:
                if (opcode == 0xD0) {
                    if (buf.remaining() < 2) {
                        // LOG hasnt second opcode!
                        LOGGER.error("0XD0 packet without second opcode! Client: {}", client);
                        break;
                    }
                    int secondOpcode = buf.getShort() & 0xffff;
                    packetClass = PACKETS_IN_GAME_D0.get(secondOpcode);
                    if (packetClass == null) {
                        printDebugDoubleOpcode(secondOpcode, buf, client.getState(), client);
                        return null;
                    }
                }
                else {
                    packetClass = PACKETS_IN_GAME.get(opcode);
                }
                break;
        }

        if (packetClass == null) {
            // TODO Аудит левых пакетов сюда.
            printDebug(opcode, buf, client.getState(), client);
            return null;
        }

        try {
            return packetClass.getConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            LOGGER.error("", e);
            return null;
        }
    }

    @Override
    public L2GameClient create(MMOConnection<L2GameClient> connection) {
        return new L2GameClient(connection);
    }

    @Override
    public void execute(ReceivablePacket<L2GameClient> packet) {
        packet.getClient().execute(packet);
    }
}