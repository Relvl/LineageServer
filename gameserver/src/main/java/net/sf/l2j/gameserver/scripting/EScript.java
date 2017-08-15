package net.sf.l2j.gameserver.scripting;

import net.sf.l2j.gameserver.scripting.quests.*;
import net.sf.l2j.gameserver.scripting.quests.SagasScripts.*;
import net.sf.l2j.gameserver.scripting.scripts.ai.group.*;
import net.sf.l2j.gameserver.scripting.scripts.ai.individual.*;
import net.sf.l2j.gameserver.scripting.scripts.custom.*;
import net.sf.l2j.gameserver.scripting.scripts.teleports.*;
import net.sf.l2j.gameserver.scripting.scripts.village_master.Alliance;
import net.sf.l2j.gameserver.scripting.scripts.village_master.Clan;
import net.sf.l2j.gameserver.scripting.scripts.village_master.FirstClassChange;
import net.sf.l2j.gameserver.scripting.scripts.village_master.SecondClassChange;

import java.util.HashMap;
import java.util.Map;

/**
 * Перечисление всех квестов и скриптов, основанных на механике квестов.
 * Да, я злоебучий наркоман. Но я считаю, что это куда более явно, чем XML+рефлексия.
 *
 * @author Johnson / 15.08.2017
 */
@SuppressWarnings("EnumeratedConstantNamingConvention")
public enum EScript {
    QUEST_Q001_LettersOfLove(Q001_LettersOfLove::new),
    QUEST_Q002_WhatWomenWant(Q002_WhatWomenWant::new),
    QUEST_Q003_WillTheSealBeBroken(Q003_WillTheSealBeBroken::new),
    QUEST_Q004_LongliveThePaagrioLord(Q004_LongliveThePaagrioLord::new),
    QUEST_Q005_MinersFavor(Q005_MinersFavor::new),
    QUEST_Q006_StepIntoTheFuture(Q006_StepIntoTheFuture::new),
    QUEST_Q007_ATripBegins(Q007_ATripBegins::new),
    QUEST_Q008_AnAdventureBegins(Q008_AnAdventureBegins::new),
    QUEST_Q009_IntoTheCityOfHumans(Q009_IntoTheCityOfHumans::new),
    QUEST_Q010_IntoTheWorld(Q010_IntoTheWorld::new),
    QUEST_Q011_SecretMeetingWithKetraOrcs(Q011_SecretMeetingWithKetraOrcs::new),
    QUEST_Q012_SecretMeetingWithVarkaSilenos(Q012_SecretMeetingWithVarkaSilenos::new),
    QUEST_Q013_ParcelDelivery(Q013_ParcelDelivery::new),
    QUEST_Q014_WhereaboutsOfTheArchaeologist(Q014_WhereaboutsOfTheArchaeologist::new),
    QUEST_Q015_SweetWhispers(Q015_SweetWhispers::new),
    QUEST_Q016_TheComingDarkness(Q016_TheComingDarkness::new),
    QUEST_Q017_LightAndDarkness(Q017_LightAndDarkness::new),
    QUEST_Q018_MeetingWithTheGoldenRam(Q018_MeetingWithTheGoldenRam::new),
    QUEST_Q019_GoToThePastureland(Q019_GoToThePastureland::new),
    QUEST_Q020_BringUpWithLove(Q020_BringUpWithLove::new),
    QUEST_Q021_HiddenTruth(Q021_HiddenTruth::new),
    QUEST_Q022_TragedyInVonHellmannForest(Q022_TragedyInVonHellmannForest::new),
    QUEST_Q023_LidiasHeart(Q023_LidiasHeart::new),
    //quests.Q024_InhabitantsOfTheForestOfTheDead
    //quests.Q025_HidingBehindTheTruth
    QUEST_Q027_ChestCaughtWithABaitOfWind(Q027_ChestCaughtWithABaitOfWind::new),
    QUEST_Q028_ChestCaughtWithABaitOfIcyAir(Q028_ChestCaughtWithABaitOfIcyAir::new),
    QUEST_Q029_ChestCaughtWithABaitOfEarth(Q029_ChestCaughtWithABaitOfEarth::new),
    QUEST_Q030_ChestCaughtWithABaitOfFire(Q030_ChestCaughtWithABaitOfFire::new),
    QUEST_Q031_SecretBuriedInTheSwamp(Q031_SecretBuriedInTheSwamp::new),
    QUEST_Q032_AnObviousLie(Q032_AnObviousLie::new),
    QUEST_Q033_MakeAPairOfDressShoes(Q033_MakeAPairOfDressShoes::new),
    QUEST_Q034_InSearchOfCloth(Q034_InSearchOfCloth::new),
    QUEST_Q035_FindGlitteringJewelry(Q035_FindGlitteringJewelry::new),
    QUEST_Q036_MakeASewingKit(Q036_MakeASewingKit::new),
    QUEST_Q037_MakeFormalWear(Q037_MakeFormalWear::new),
    QUEST_Q038_DragonFangs(Q038_DragonFangs::new),
    QUEST_Q039_RedEyedInvaders(Q039_RedEyedInvaders::new),
    QUEST_Q042_HelpTheUncle(Q042_HelpTheUncle::new),
    QUEST_Q043_HelpTheSister(Q043_HelpTheSister::new),
    QUEST_Q044_HelpTheSon(Q044_HelpTheSon::new),
    QUEST_Q045_ToTalkingIsland(Q045_ToTalkingIsland::new),
    QUEST_Q046_OnceMoreInTheArmsOfTheMotherTree(Q046_OnceMoreInTheArmsOfTheMotherTree::new),
    QUEST_Q047_IntoTheDarkForest(Q047_IntoTheDarkForest::new),
    QUEST_Q048_ToTheImmortalPlateau(Q048_ToTheImmortalPlateau::new),
    QUEST_Q049_TheRoadHome(Q049_TheRoadHome::new),
    QUEST_Q050_LanoscosSpecialBait(Q050_LanoscosSpecialBait::new),
    QUEST_Q051_OFullesSpecialBait(Q051_OFullesSpecialBait::new),
    QUEST_Q052_WilliesSpecialBait(Q052_WilliesSpecialBait::new),
    QUEST_Q053_LinnaeusSpecialBait(Q053_LinnaeusSpecialBait::new),
    QUEST_Q101_SwordOfSolidarity(Q101_SwordOfSolidarity::new),
    QUEST_Q102_SeaOfSporesFever(Q102_SeaOfSporesFever::new),
    QUEST_Q103_SpiritOfCraftsman(Q103_SpiritOfCraftsman::new),
    QUEST_Q104_SpiritOfMirrors(Q104_SpiritOfMirrors::new),
    QUEST_Q105_SkirmishWithTheOrcs(Q105_SkirmishWithTheOrcs::new),
    QUEST_Q106_ForgottenTruth(Q106_ForgottenTruth::new),
    QUEST_Q107_MercilessPunishment(Q107_MercilessPunishment::new),
    QUEST_Q108_JumbleTumbleDiamondFuss(Q108_JumbleTumbleDiamondFuss::new),
    QUEST_Q109_InSearchOfTheNest(Q109_InSearchOfTheNest::new),
    QUEST_Q110_ToThePrimevalIsle(Q110_ToThePrimevalIsle::new),
    QUEST_Q111_ElrokianHuntersProof(Q111_ElrokianHuntersProof::new),
    QUEST_Q112_WalkOfFate(Q112_WalkOfFate::new),
    QUEST_Q113_StatusOfTheBeaconTower(Q113_StatusOfTheBeaconTower::new),
    //quests.Q114_ResurrectionOfAnOldManager
    //quests.Q115_TheOtherSideOfTruth
    QUEST_Q116_BeyondTheHillsOfWinter(Q116_BeyondTheHillsOfWinter::new),
    QUEST_Q117_TheOceanOfDistantStars(Q117_TheOceanOfDistantStars::new),
    //quests.Q118_ToLeadAndBeLed
    QUEST_Q119_LastImperialPrince(Q119_LastImperialPrince::new),
    //quests.Q120_PavelsResearch
    QUEST_Q121_PavelTheGiant(Q121_PavelTheGiant::new),
    QUEST_Q122_OminousNews(Q122_OminousNews::new),
    QUEST_Q123_TheLeaderAndTheFollower(Q123_TheLeaderAndTheFollower::new),
    QUEST_Q124_MeetingTheElroki(Q124_MeetingTheElroki::new),
    QUEST_Q125_TheNameOfEvil_1(Q125_TheNameOfEvil_1::new),
    QUEST_Q126_TheNameOfEvil_2(Q126_TheNameOfEvil_2::new),
    QUEST_Q127_KamaelAWindowToTheFuture(Q127_KamaelAWindowToTheFuture::new),
    QUEST_Q151_CureForFeverDisease(Q151_CureForFeverDisease::new),
    QUEST_Q152_ShardsOfGolem(Q152_ShardsOfGolem::new),
    QUEST_Q153_DeliverGoods(Q153_DeliverGoods::new),
    QUEST_Q154_SacrificeToTheSea(Q154_SacrificeToTheSea::new),
    QUEST_Q155_FindSirWindawood(Q155_FindSirWindawood::new),
    QUEST_Q156_MillenniumLove(Q156_MillenniumLove::new),
    QUEST_Q157_RecoverSmuggledGoods(Q157_RecoverSmuggledGoods::new),
    QUEST_Q158_SeedOfEvil(Q158_SeedOfEvil::new),
    QUEST_Q159_ProtectTheWaterSource(Q159_ProtectTheWaterSource::new),
    QUEST_Q160_NerupasRequest(Q160_NerupasRequest::new),
    QUEST_Q161_FruitOfTheMotherTree(Q161_FruitOfTheMotherTree::new),
    QUEST_Q162_CurseOfTheUndergroundFortress(Q162_CurseOfTheUndergroundFortress::new),
    QUEST_Q163_LegacyOfThePoet(Q163_LegacyOfThePoet::new),
    QUEST_Q164_BloodFiend(Q164_BloodFiend::new),
    QUEST_Q165_ShilensHunt(Q165_ShilensHunt::new),
    QUEST_Q166_MassOfDarkness(Q166_MassOfDarkness::new),
    QUEST_Q167_DwarvenKinship(Q167_DwarvenKinship::new),
    QUEST_Q168_DeliverSupplies(Q168_DeliverSupplies::new),
    QUEST_Q169_OffspringOfNightmares(Q169_OffspringOfNightmares::new),
    QUEST_Q170_DangerousSeduction(Q170_DangerousSeduction::new),
    QUEST_Q171_ActsOfEvil(Q171_ActsOfEvil::new),
    QUEST_Q211_TrialOfTheChallenger(Q211_TrialOfTheChallenger::new),
    QUEST_Q212_TrialOfDuty(Q212_TrialOfDuty::new),
    QUEST_Q213_TrialOfTheSeeker(Q213_TrialOfTheSeeker::new),
    QUEST_Q214_TrialOfTheScholar(Q214_TrialOfTheScholar::new),
    QUEST_Q215_TrialOfThePilgrim(Q215_TrialOfThePilgrim::new),
    QUEST_Q216_TrialOfTheGuildsman(Q216_TrialOfTheGuildsman::new),
    QUEST_Q217_TestimonyOfTrust(Q217_TestimonyOfTrust::new),
    QUEST_Q218_TestimonyOfLife(Q218_TestimonyOfLife::new),
    QUEST_Q219_TestimonyOfFate(Q219_TestimonyOfFate::new),
    QUEST_Q220_TestimonyOfGlory(Q220_TestimonyOfGlory::new),
    //quests.Q221_TestimonyOfProsperity
    QUEST_Q222_TestOfTheDuelist(Q222_TestOfTheDuelist::new),
    QUEST_Q223_TestOfTheChampion(Q223_TestOfTheChampion::new),
    QUEST_Q224_TestOfSagittarius(Q224_TestOfSagittarius::new),
    QUEST_Q225_TestOfTheSearcher(Q225_TestOfTheSearcher::new),
    QUEST_Q226_TestOfTheHealer(Q226_TestOfTheHealer::new),
    //quests.Q227_TestOfTheReformer
    QUEST_Q228_TestOfMagus(Q228_TestOfMagus::new),
    //quests.Q229_TestOfWitchcraft
    QUEST_Q230_TestOfTheSummoner(Q230_TestOfTheSummoner::new),
    QUEST_Q231_TestOfTheMaestro(Q231_TestOfTheMaestro::new),
    QUEST_Q232_TestOfTheLord(Q232_TestOfTheLord::new),
    QUEST_Q233_TestOfTheWarSpirit(Q233_TestOfTheWarSpirit::new),
    QUEST_Q234_FatesWhisper(Q234_FatesWhisper::new),
    QUEST_Q235_MimirsElixir(Q235_MimirsElixir::new),
    QUEST_Q241_PossessorOfAPreciousSoul(Q241_PossessorOfAPreciousSoul::new),
    QUEST_Q242_PossessorOfAPreciousSoul(Q242_PossessorOfAPreciousSoul::new),
    QUEST_Q246_PossessorOfAPreciousSoul(Q246_PossessorOfAPreciousSoul::new),
    QUEST_Q247_PossessorOfAPreciousSoul(Q247_PossessorOfAPreciousSoul::new),
    QUEST_Q257_TheGuardIsBusy(Q257_TheGuardIsBusy::new),
    QUEST_Q258_BringWolfPelts(Q258_BringWolfPelts::new),
    QUEST_Q259_RanchersPlea(Q259_RanchersPlea::new),
    QUEST_Q260_HuntTheOrcs(Q260_HuntTheOrcs::new),
    QUEST_Q261_CollectorsDream(Q261_CollectorsDream::new),
    QUEST_Q262_TradeWithTheIvoryTower(Q262_TradeWithTheIvoryTower::new),
    QUEST_Q263_OrcSubjugation(Q263_OrcSubjugation::new),
    QUEST_Q264_KeenClaws(Q264_KeenClaws::new),
    QUEST_Q265_ChainsOfSlavery(Q265_ChainsOfSlavery::new),
    QUEST_Q266_PleasOfPixies(Q266_PleasOfPixies::new),
    QUEST_Q267_WrathOfVerdure(Q267_WrathOfVerdure::new),
    QUEST_Q271_ProofOfValor(Q271_ProofOfValor::new),
    QUEST_Q272_WrathOfAncestors(Q272_WrathOfAncestors::new),
    QUEST_Q273_InvadersOfTheHolyLand(Q273_InvadersOfTheHolyLand::new),
    QUEST_Q274_SkirmishWithTheWerewolves(Q274_SkirmishWithTheWerewolves::new),
    QUEST_Q275_DarkWingedSpies(Q275_DarkWingedSpies::new),
    QUEST_Q276_TotemOfTheHestui(Q276_TotemOfTheHestui::new),
    QUEST_Q277_GatekeepersOffering(Q277_GatekeepersOffering::new),
    QUEST_Q291_RevengeOfTheRedbonnet(Q291_RevengeOfTheRedbonnet::new),
    QUEST_Q292_BrigandsSweep(Q292_BrigandsSweep::new),
    QUEST_Q293_TheHiddenVeins(Q293_TheHiddenVeins::new),
    QUEST_Q294_CovertBusiness(Q294_CovertBusiness::new),
    QUEST_Q295_DreamingOfTheSkies(Q295_DreamingOfTheSkies::new),
    QUEST_Q296_TarantulasSpiderSilk(Q296_TarantulasSpiderSilk::new),
    QUEST_Q297_GatekeepersFavor(Q297_GatekeepersFavor::new),
    QUEST_Q298_LizardmensConspiracy(Q298_LizardmensConspiracy::new),
    QUEST_Q299_GatherIngredientsForPie(Q299_GatherIngredientsForPie::new),
    QUEST_Q300_HuntingLetoLizardman(Q300_HuntingLetoLizardman::new),
    QUEST_Q303_CollectArrowheads(Q303_CollectArrowheads::new),
    QUEST_Q306_CrystalsOfFireAndIce(Q306_CrystalsOfFireAndIce::new),
    QUEST_Q313_CollectSpores(Q313_CollectSpores::new),
    QUEST_Q316_DestroyPlagueCarriers(Q316_DestroyPlagueCarriers::new),
    QUEST_Q317_CatchTheWind(Q317_CatchTheWind::new),
    QUEST_Q319_ScentOfDeath(Q319_ScentOfDeath::new),
    QUEST_Q320_BonesTellTheFuture(Q320_BonesTellTheFuture::new),
    QUEST_Q324_SweetestVenom(Q324_SweetestVenom::new),
    QUEST_Q325_GrimCollector(Q325_GrimCollector::new),
    QUEST_Q326_VanquishRemnants(Q326_VanquishRemnants::new),
    QUEST_Q327_RecoverTheFarmland(Q327_RecoverTheFarmland::new),
    QUEST_Q328_SenseForBusiness(Q328_SenseForBusiness::new),
    QUEST_Q329_CuriosityOfADwarf(Q329_CuriosityOfADwarf::new),
    QUEST_Q330_AdeptOfTaste(Q330_AdeptOfTaste::new),
    QUEST_Q331_ArrowOfVengeance(Q331_ArrowOfVengeance::new),
    //quests.Q333_HuntOfTheBlackLion
    //quests.Q334_TheWishingPotion
    //quests.Q335_SongOfTheHunter
    //quests.Q336_CoinsOfMagic
    QUEST_Q337_AudienceWithTheLandDragon(Q337_AudienceWithTheLandDragon::new),
    QUEST_Q338_AlligatorHunter(Q338_AlligatorHunter::new),
    QUEST_Q340_SubjugationOfLizardmen(Q340_SubjugationOfLizardmen::new),
    QUEST_Q341_HuntingForWildBeasts(Q341_HuntingForWildBeasts::new),
    //quests.Q343_UnderTheShadowOfTheIvoryTower
    QUEST_Q344_1000YearsTheEndOfLamentation(Q344_1000YearsTheEndOfLamentation::new),
    QUEST_Q345_MethodToRaiseTheDead(Q345_MethodToRaiseTheDead::new),
    QUEST_Q347_GoGetTheCalculator(Q347_GoGetTheCalculator::new),
    QUEST_Q348_AnArrogantSearch(Q348_AnArrogantSearch::new),
    QUEST_Q350_EnhanceYourWeapon(Q350_EnhanceYourWeapon::new),
    QUEST_Q351_BlackSwan(Q351_BlackSwan::new),
    QUEST_Q352_HelpRoodRaiseANewPet(Q352_HelpRoodRaiseANewPet::new),
    QUEST_Q353_PowerOfDarkness(Q353_PowerOfDarkness::new),
    QUEST_Q354_ConquestOfAlligatorIsland(Q354_ConquestOfAlligatorIsland::new),
    QUEST_Q355_FamilyHonor(Q355_FamilyHonor::new),
    QUEST_Q356_DigUpTheSeaOfSpores(Q356_DigUpTheSeaOfSpores::new),
    QUEST_Q357_WarehouseKeepersAmbition(Q357_WarehouseKeepersAmbition::new),
    QUEST_Q358_IllegitimateChildOfAGoddess(Q358_IllegitimateChildOfAGoddess::new),
    QUEST_Q359_ForSleeplessDeadmen(Q359_ForSleeplessDeadmen::new),
    QUEST_Q360_PlunderTheirSupplies(Q360_PlunderTheirSupplies::new),
    QUEST_Q362_BardsMandolin(Q362_BardsMandolin::new),
    QUEST_Q363_SorrowfulSoundOfFlute(Q363_SorrowfulSoundOfFlute::new),
    QUEST_Q364_JovialAccordion(Q364_JovialAccordion::new),
    QUEST_Q365_DevilsLegacy(Q365_DevilsLegacy::new),
    QUEST_Q366_SilverHairedShaman(Q366_SilverHairedShaman::new),
    QUEST_Q367_ElectrifyingRecharge(Q367_ElectrifyingRecharge::new),
    QUEST_Q368_TrespassingIntoTheSacredArea(Q368_TrespassingIntoTheSacredArea::new),
    QUEST_Q369_CollectorOfJewels(Q369_CollectorOfJewels::new),
    QUEST_Q370_AnElderSowsSeeds(Q370_AnElderSowsSeeds::new),
    QUEST_Q371_ShriekOfGhosts(Q371_ShriekOfGhosts::new),
    QUEST_Q372_LegacyOfInsolence(Q372_LegacyOfInsolence::new),
    QUEST_Q373_SupplierOfReagents(Q373_SupplierOfReagents::new),
    QUEST_Q374_WhisperOfDreams_Part1(Q374_WhisperOfDreams_Part1::new),
    QUEST_Q375_WhisperOfDreams_Part2(Q375_WhisperOfDreams_Part2::new),
    QUEST_Q376_ExplorationOfTheGiantsCave_Part1(Q376_ExplorationOfTheGiantsCave_Part1::new),
    QUEST_Q377_ExplorationOfTheGiantsCave_Part2(Q377_ExplorationOfTheGiantsCave_Part2::new),
    QUEST_Q378_MagnificentFeast(Q378_MagnificentFeast::new),
    QUEST_Q379_FantasyWine(Q379_FantasyWine::new),
    QUEST_Q380_BringOutTheFlavorOfIngredients(Q380_BringOutTheFlavorOfIngredients::new),
    QUEST_Q381_LetsBecomeARoyalMember(Q381_LetsBecomeARoyalMember::new),
    QUEST_Q382_KailsMagicCoin(Q382_KailsMagicCoin::new),
    QUEST_Q383_SearchingForTreasure(Q383_SearchingForTreasure::new),
    QUEST_Q384_WarehouseKeepersPastime(Q384_WarehouseKeepersPastime::new),
    QUEST_Q385_YokeOfThePast(Q385_YokeOfThePast::new),
    //quests.Q386_StolenDignity
    QUEST_Q401_PathToAWarrior(Q401_PathToAWarrior::new),
    QUEST_Q402_PathToAHumanKnight(Q402_PathToAHumanKnight::new),
    QUEST_Q403_PathToARogue(Q403_PathToARogue::new),
    QUEST_Q404_PathToAHumanWizard(Q404_PathToAHumanWizard::new),
    QUEST_Q405_PathToACleric(Q405_PathToACleric::new),
    QUEST_Q406_PathToAnElvenKnight(Q406_PathToAnElvenKnight::new),
    QUEST_Q407_PathToAnElvenScout(Q407_PathToAnElvenScout::new),
    QUEST_Q408_PathToAnElvenWizard(Q408_PathToAnElvenWizard::new),
    QUEST_Q409_PathToAnElvenOracle(Q409_PathToAnElvenOracle::new),
    QUEST_Q410_PathToAPalusKnight(Q410_PathToAPalusKnight::new),
    QUEST_Q411_PathToAnAssassin(Q411_PathToAnAssassin::new),
    QUEST_Q412_PathToADarkWizard(Q412_PathToADarkWizard::new),
    QUEST_Q413_PathToAShillienOracle(Q413_PathToAShillienOracle::new),
    QUEST_Q414_PathToAnOrcRaider(Q414_PathToAnOrcRaider::new),
    QUEST_Q415_PathToAMonk(Q415_PathToAMonk::new),
    QUEST_Q416_PathToAnOrcShaman(Q416_PathToAnOrcShaman::new),
    QUEST_Q417_PathToBecomeAScavenger(Q417_PathToBecomeAScavenger::new),
    QUEST_Q418_PathToAnArtisan(Q418_PathToAnArtisan::new),
    QUEST_Q419_GetAPet(Q419_GetAPet::new),
    QUEST_Q420_LittleWing(Q420_LittleWing::new),
    QUEST_Q421_LittleWingsBigAdventure(Q421_LittleWingsBigAdventure::new),
    QUEST_Q422_RepentYourSins(Q422_RepentYourSins::new),
    QUEST_Q426_QuestForFishingShot(Q426_QuestForFishingShot::new),
    QUEST_Q431_WeddingMarch(Q431_WeddingMarch::new),
    QUEST_Q432_BirthdayPartySong(Q432_BirthdayPartySong::new),
    //quests.Q501_ProofOfClanAlliance
    //quests.Q503_PursuitOfClanAmbition
    //quests.Q504_CompetitionForTheBanditStronghold
    //quests.Q505_BloodOffering
    QUEST_Q508_AClansReputation(Q508_AClansReputation::new),
    QUEST_Q509_TheClansPrestige(Q509_TheClansPrestige::new),
    QUEST_Q510_AClansReputation(Q510_AClansReputation::new),
    QUEST_Q601_WatchingEyes(Q601_WatchingEyes::new),
    QUEST_Q602_ShadowOfLight(Q602_ShadowOfLight::new),
    QUEST_Q603_DaimonTheWhiteEyed_Part1(Q603_DaimonTheWhiteEyed_Part1::new),
    QUEST_Q604_DaimonTheWhiteEyed_Part2(Q604_DaimonTheWhiteEyed_Part2::new),
    QUEST_Q605_AllianceWithKetraOrcs(Q605_AllianceWithKetraOrcs::new),
    QUEST_Q606_WarWithVarkaSilenos(Q606_WarWithVarkaSilenos::new),
    QUEST_Q607_ProveYourCourage(Q607_ProveYourCourage::new),
    QUEST_Q608_SlayTheEnemyCommander(Q608_SlayTheEnemyCommander::new),
    QUEST_Q609_MagicalPowerOfWater_Part1(Q609_MagicalPowerOfWater_Part1::new),
    QUEST_Q610_MagicalPowerOfWater_Part2(Q610_MagicalPowerOfWater_Part2::new),
    QUEST_Q611_AllianceWithVarkaSilenos(Q611_AllianceWithVarkaSilenos::new),
    QUEST_Q612_WarWithKetraOrcs(Q612_WarWithKetraOrcs::new),
    QUEST_Q613_ProveYourCourage(Q613_ProveYourCourage::new),
    QUEST_Q614_SlayTheEnemyCommander(Q614_SlayTheEnemyCommander::new),
    QUEST_Q615_MagicalPowerOfFire_Part1(Q615_MagicalPowerOfFire_Part1::new),
    QUEST_Q616_MagicalPowerOfFire_Part2(Q616_MagicalPowerOfFire_Part2::new),
    QUEST_Q617_GatherTheFlames(Q617_GatherTheFlames::new),
    QUEST_Q618_IntoTheFlame(Q618_IntoTheFlame::new),
    QUEST_Q619_RelicsOfTheOldEmpire(Q619_RelicsOfTheOldEmpire::new),
    QUEST_Q620_FourGoblets(Q620_FourGoblets::new),
    QUEST_Q621_EggDelivery(Q621_EggDelivery::new),
    QUEST_Q622_SpecialtyLiquorDelivery(Q622_SpecialtyLiquorDelivery::new),
    QUEST_Q623_TheFinestFood(Q623_TheFinestFood::new),
    QUEST_Q624_TheFinestIngredients_Part1(Q624_TheFinestIngredients_Part1::new),
    QUEST_Q625_TheFinestIngredients_Part2(Q625_TheFinestIngredients_Part2::new),
    QUEST_Q626_ADarkTwilight(Q626_ADarkTwilight::new),
    QUEST_Q627_HeartInSearchOfPower(Q627_HeartInSearchOfPower::new),
    QUEST_Q628_HuntOfTheGoldenRamMercenaryForce(Q628_HuntOfTheGoldenRamMercenaryForce::new),
    QUEST_Q629_CleanUpTheSwampOfScreams(Q629_CleanUpTheSwampOfScreams::new),
    QUEST_Q631_DeliciousTopChoiceMeat(Q631_DeliciousTopChoiceMeat::new),
    QUEST_Q632_NecromancersRequest(Q632_NecromancersRequest::new),
    QUEST_Q633_InTheForgottenVillage(Q633_InTheForgottenVillage::new),
    QUEST_Q634_InSearchOfFragmentsOfDimension(Q634_InSearchOfFragmentsOfDimension::new),
    QUEST_Q636_TruthBeyondTheGate(Q636_TruthBeyondTheGate::new),
    QUEST_Q637_ThroughTheGateOnceMore(Q637_ThroughTheGateOnceMore::new),
    QUEST_Q638_SeekersOfTheHolyGrail(Q638_SeekersOfTheHolyGrail::new),
    QUEST_Q639_GuardiansOfTheHolyGrail(Q639_GuardiansOfTheHolyGrail::new),
    QUEST_Q640_TheZeroHour(Q640_TheZeroHour::new),
    QUEST_Q641_AttackSailren(Q641_AttackSailren::new),
    QUEST_Q642_APowerfulPrimevalCreature(Q642_APowerfulPrimevalCreature::new),
    QUEST_Q643_RiseAndFallOfTheElrokiTribe(Q643_RiseAndFallOfTheElrokiTribe::new),
    QUEST_Q644_GraveRobberAnnihilation(Q644_GraveRobberAnnihilation::new),
    QUEST_Q645_GhostsOfBatur(Q645_GhostsOfBatur::new),
    QUEST_Q646_SignsOfRevolt(Q646_SignsOfRevolt::new),
    QUEST_Q647_InfluxOfMachines(Q647_InfluxOfMachines::new),
    //quests.Q648_AnIceMerchantsDream
    QUEST_Q649_ALooterAndARailroadMan(Q649_ALooterAndARailroadMan::new),
    QUEST_Q650_ABrokenDream(Q650_ABrokenDream::new),
    QUEST_Q651_RunawayYouth(Q651_RunawayYouth::new),
    QUEST_Q652_AnAgedExAdventurer(Q652_AnAgedExAdventurer::new),
    QUEST_Q653_WildMaiden(Q653_WildMaiden::new),
    QUEST_Q654_JourneyToASettlement(Q654_JourneyToASettlement::new),
    //quests.Q655_AGrandPlanForTamingWildBeasts
    QUEST_Q659_IdRatherBeCollectingFairyBreath(Q659_IdRatherBeCollectingFairyBreath::new),
    QUEST_Q660_AidingTheFloranVillage(Q660_AidingTheFloranVillage::new),
    QUEST_Q661_MakingTheHarvestGroundsSafe(Q661_MakingTheHarvestGroundsSafe::new),
    QUEST_Q662_AGameOfCards(Q662_AGameOfCards::new),
    QUEST_Q663_SeductiveWhispers(Q663_SeductiveWhispers::new),
    QUEST_Q688_DefeatTheElrokianRaiders(Q688_DefeatTheElrokianRaiders::new),

    /** TODO Какая-то лютая магия с этими сагами... Надо во-первых, проверить, а во-вторых, разобраться и описать хоть как-то. */
    QUEST_Q070_SagaOfThePhoenixKnight(Q070_SagaOfThePhoenixKnight::new),
    QUEST_Q071_SagaOfEvasTemplar(Q071_SagaOfEvasTemplar::new),
    QUEST_Q072_SagaOfTheSwordMuse(Q072_SagaOfTheSwordMuse::new),
    QUEST_Q073_SagaOfTheDuelist(Q073_SagaOfTheDuelist::new),
    QUEST_Q074_SagaOfTheDreadnought(Q074_SagaOfTheDreadnought::new),
    QUEST_Q075_SagaOfTheTitan(Q075_SagaOfTheTitan::new),
    QUEST_Q076_SagaOfTheGrandKhavatari(Q076_SagaOfTheGrandKhavatari::new),
    QUEST_Q077_SagaOfTheDominator(Q077_SagaOfTheDominator::new),
    QUEST_Q078_SagaOfTheDoomcryer(Q078_SagaOfTheDoomcryer::new),
    QUEST_Q079_SagaOfTheAdventurer(Q079_SagaOfTheAdventurer::new),
    QUEST_Q080_SagaOfTheWindRider(Q080_SagaOfTheWindRider::new),
    QUEST_Q081_SagaOfTheGhostHunter(Q081_SagaOfTheGhostHunter::new),
    QUEST_Q082_SagaOfTheSagittarius(Q082_SagaOfTheSagittarius::new),
    QUEST_Q083_SagaOfTheMoonlightSentinel(Q083_SagaOfTheMoonlightSentinel::new),
    QUEST_Q084_SagaOfTheGhostSentinel(Q084_SagaOfTheGhostSentinel::new),
    QUEST_Q085_SagaOfTheCardinal(Q085_SagaOfTheCardinal::new),
    QUEST_Q086_SagaOfTheHierophant(Q086_SagaOfTheHierophant::new),
    QUEST_Q087_SagaOfEvasSaint(Q087_SagaOfEvasSaint::new),
    QUEST_Q088_SagaOfTheArchmage(Q088_SagaOfTheArchmage::new),
    QUEST_Q089_SagaOfTheMysticMuse(Q089_SagaOfTheMysticMuse::new),
    QUEST_Q090_SagaOfTheStormScreamer(Q090_SagaOfTheStormScreamer::new),
    QUEST_Q091_SagaOfTheArcanaLord(Q091_SagaOfTheArcanaLord::new),
    QUEST_Q092_SagaOfTheElementalMaster(Q092_SagaOfTheElementalMaster::new),
    QUEST_Q093_SagaOfTheSpectralMaster(Q093_SagaOfTheSpectralMaster::new),
    QUEST_Q094_SagaOfTheSoultaker(Q094_SagaOfTheSoultaker::new),
    QUEST_Q095_SagaOfTheHellKnight(Q095_SagaOfTheHellKnight::new),
    QUEST_Q096_SagaOfTheSpectralDancer(Q096_SagaOfTheSpectralDancer::new),
    QUEST_Q097_SagaOfTheShillienTemplar(Q097_SagaOfTheShillienTemplar::new),
    QUEST_Q098_SagaOfTheShillienSaint(Q098_SagaOfTheShillienSaint::new),
    QUEST_Q099_SagaOfTheFortuneSeeker(Q099_SagaOfTheFortuneSeeker::new),
    QUEST_Q100_SagaOfTheMaestro(Q100_SagaOfTheMaestro::new),
    //quests.Tutorial

    //    <!-- AI Section -->
    AI_Chests(Chests::new),
    AI_FeedableBeasts(FeedableBeasts::new),
    AI_FleeingNPCs(FleeingNPCs::new),
    AI_FrenzyOnAttack(FrenzyOnAttack::new),
    AI_FrozenLabyrinth(FrozenLabyrinth::new),
    AI_GatekeeperZombies(GatekeeperZombies::new),
    AI_HotSpringDisease(HotSpringDisease::new),
    AI_L2AttackableAIScript(L2AttackableAIScript::new),
    AI_Monastery(Monastery::new),
    AI_PlainsOfDion(PlainsOfDion::new),
    AI_PolymorphingAngel(PolymorphingAngel::new),
    AI_PolymorphingOnAttack(PolymorphingOnAttack::new),
    AI_PrimevalIsle(PrimevalIsle::new),
    AI_SearchingMaster(SearchingMaster::new),
    AI_SpeakingNPCs(SpeakingNPCs::new),
    AI_StakatoNest(StakatoNest::new),
    AI_SummonMinions(SummonMinions::new),
    AI_Antharas(Antharas::new),
    AI_Baium(Baium::new),
    //scripts.ai.individual.Benom
    AI_Core(Core::new),
    AI_DrChaos(DrChaos::new),
    //scripts.ai.individual.Frintezza
    AI_Gordon(Gordon::new),
    //scripts.ai.individual.IceFairySirra
    AI_Orfen(Orfen::new),
    AI_QueenAnt(QueenAnt::new),
    AI_Sailren(Sailren::new),
    AI_Valakas(Valakas::new),
    //scripts.ai.individual.Zaken

    //    <!-- Custom -->
    Custom_EchoCrystals(EchoCrystals::new),
    Custom_ShadowWeapon(ShadowWeapon::new),
    Custom_MissQueen(MissQueen::new),
    Custom_KetraOrcSupport(KetraOrcSupport::new),
    Custom_VarkaSilenosSupport(VarkaSilenosSupport::new),
    Custom_RaidbossInfo(RaidbossInfo::new),
    Custom_NpcLocationInfo(NpcLocationInfo::new),
    Custom_HeroCirclet(HeroCirclet::new),
    Custom_HeroWeapon(HeroWeapon::new),

//    <!-- Events -->
    //scripts.events.GiftOfVitality
    //scripts.events.HeavyMedal
    //scripts.events.TheValentineEvent
    //scripts.events.MasterOfEnchanting

    //    <!-- Teleports -->
    Teleporter_ElrokiTeleporters(ElrokiTeleporters::new),
    Teleporter_GatekeeperSpirit(GatekeeperSpirit::new),
    Teleporter_GrandBossTeleporters(GrandBossTeleporters::new),
    Teleporter_HuntingGroundsTeleport(HuntingGroundsTeleport::new),
    Teleporter_NewbieTravelToken(NewbieTravelToken::new),
    Teleporter_NoblesseTeleport(NoblesseTeleport::new),
    Teleporter_OracleTeleport(OracleTeleport::new),
    Teleporter_PaganTeleporters(PaganTeleporters::new),
    Teleporter_RaceTrack(RaceTrack::new),
    Teleporter_TeleportWithCharm(TeleportWithCharm::new),
    Teleporter_ToIVortex(ToIVortex::new),

    //    <!-- Village Master -->
    VillageMaster_Alliance(Alliance::new),
    VillageMaster_Clan(Clan::new),
    VillageMaster_FirstClassChange(FirstClassChange::new),
    VillageMaster_SecondClassChange(SecondClassChange::new);

    EScript(IScriptConstructor<Quest> quest) {
        Quest questInstance = quest.make();
        QuestHolder.QUEST_BY_ENUM.put(this, questInstance);
        QuestHolder.QUEST_BY_NAME.put(questInstance.getName(), questInstance);
        if (questInstance.getQuestId() > 0) {
            QuestHolder.QUEST_BY_ID.put(questInstance.getQuestId(), questInstance);
        }
    }

    public Quest getQuest() { return QuestHolder.QUEST_BY_ENUM.get(this); }

    public static int getQuestsCount() { return QuestHolder.QUEST_BY_ID.size(); }

    public static int getScriptsCount() { return QuestHolder.QUEST_BY_NAME.size() - QuestHolder.QUEST_BY_ID.size(); }

    public static Quest getQuest(String questName) {
        if (questName == null) { return null; }
        return QuestHolder.QUEST_BY_NAME.get(questName);
    }

    public static Quest getQuest(Integer questId) {
        if (questId == null || questId <= 0) { return null; }
        return QuestHolder.QUEST_BY_ID.get(questId);
    }

    private static final class QuestHolder {
        @SuppressWarnings("MapReplaceableByEnumMap") // Потому что на момент заполнения энам еще не проинициализировался.
        private static final Map<EScript, Quest> QUEST_BY_ENUM = new HashMap<>();
        private static final Map<String, Quest> QUEST_BY_NAME = new HashMap<>();
        private static final Map<Integer, Quest> QUEST_BY_ID = new HashMap<>();
    }
}
