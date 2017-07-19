package net.sf.l2j;

import net.sf.l2j.ItemXmlFile.ItemSkillInfo;
import net.sf.l2j.ItemXmlFile.ItemXmlElement;
import net.sf.l2j.ItemXmlFile.SetElement;
import net.sf.l2j.commons.serialize.Serializer;
import net.sf.l2j.gameserver.model.item.EItemBodyPart;
import net.sf.l2j.gameserver.model.item.type.*;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * @author Johnson / 17.07.2017
 */
public class TempMainClass {
    public static void main(String... args) throws IOException {

        File dir = new File("./data/xml/items");

        ItemXmlFile weapons = new ItemXmlFile();
        ItemXmlFile armors = new ItemXmlFile();
        ItemXmlFile etcItems = new ItemXmlFile();
        Map<EtcItemType, ItemXmlFile> etcMap = new EnumMap<>(EtcItemType.class);

        for (File file : dir.listFiles()) {
            ItemXmlFile xmlFile = Serializer.MAPPER.readValue(file, ItemXmlFile.class);
            for (ItemXmlElement element : xmlFile.list) {

                Iterator<SetElement> setIterator = element.sets.iterator();
                while (setIterator.hasNext()) {
                    SetElement set = setIterator.next();

                    // region case
                    switch (set.name) {
                        case "is_sellable":
                            element.sellable = Boolean.parseBoolean(set.val);
                            break;
                        case "is_depositable":
                            element.depositable = Boolean.parseBoolean(set.val);
                            break;
                        case "is_destroyable":
                            element.destroyable = Boolean.parseBoolean(set.val);
                            break;
                        case "is_dropable":
                            element.dropable = Boolean.parseBoolean(set.val);
                            break;
                        case "is_stackable":
                            element.stackable = Boolean.parseBoolean(set.val);
                            break;
                        case "is_tradable":
                            element.tradable = Boolean.parseBoolean(set.val);
                            break;
                        case "is_oly_restricted":
                            element.olympiadRestricted = Boolean.parseBoolean(set.val);
                            break;
                        case "armor_type":
                            element.armorType = ArmorType.valueOf(set.val.toUpperCase());
                            break;
                        case "handler":
                            element.handlerName = set.val;
                            break;
                        case "item_skill":
                            String[] splitSkills = set.val.split(";");
                            for (String skill : splitSkills) {
                                String[] skillParts = skill.split("-");
                                ItemSkillInfo info = new ItemSkillInfo();
                                info.id = Integer.parseInt(skillParts[0]);
                                info.level = Integer.parseInt(skillParts[1]);
                                if (info.id != 0 && info.level != 0) {
                                    if (element.itemSkill == null) {
                                        element.itemSkill = new ArrayList<>();
                                    }
                                    element.itemSkill.add(info);
                                }
                            }
                            break;
                        case "enchant4_skill":
                            if (element.enchant4skill == null) {
                                element.enchant4skill = new ItemSkillInfo();
                            }
                            String[] skillParts = set.val.split("-");
                            element.enchant4skill.id = Integer.parseInt(skillParts[0]);
                            element.enchant4skill.level = Integer.parseInt(skillParts[1]);
                            break;
                        case "attack_range":
                            element.attackRange = Integer.parseInt(set.val);
                            break;
                        case "weapon_type":
                            element.weaponType = EWeaponType.valueOf(set.val.toUpperCase());
                            break;
                        case "oncast_skill":
                            String[] parts1 = set.val.split("-");
                            if (element.onCastSkill == null) {
                                element.onCastSkill = new ItemSkillInfo();
                            }
                            element.onCastSkill.id = Integer.parseInt(parts1[0]);
                            if (parts1.length > 1) {
                                element.onCastSkill.level = Integer.parseInt(parts1[1]);
                            }
                            break;
                        case "oncast_chance":
                            if (element.onCastSkill == null) {
                                element.onCastSkill = new ItemSkillInfo();
                            }
                            element.onCastSkill.chance = Integer.parseInt(set.val);
                            break;
                        case "oncrit_skill":
                            String[] parts2 = set.val.split("-");
                            if (element.onCritSkill == null) {
                                element.onCritSkill = new ItemSkillInfo();
                            }
                            element.onCritSkill.id = Integer.parseInt(parts2[0]);
                            element.onCritSkill.level = Integer.parseInt(parts2[1]);
                            break;
                        case "oncrit_chance":
                            if (element.onCritSkill == null) {
                                element.onCritSkill = new ItemSkillInfo();
                            }
                            element.onCritSkill.chance = Integer.parseInt(set.val);
                            break;
                        case "soulshots":
                            element.soulshots = Integer.parseInt(set.val);
                            break;
                        case "spiritshots":
                            element.spiritshots = Integer.parseInt(set.val);
                            break;
                        case "crystal_type":
                            element.crystalType = CrystalType.valueOf(set.val.toUpperCase());
                            break;
                        case "crystal_count":
                            element.crystalCount = Integer.parseInt(set.val);
                            break;
                        case "material":
                            element.material = MaterialType.valueOf(set.val.toUpperCase());
                            break;
                        case "is_magical":
                            element.magical = Boolean.parseBoolean(set.val);
                            break;
                        case "reduced_soulshot":
                            String[] rs = set.val.split(",");
                            element.reducedSoulshotChance = Integer.parseInt(rs[0]);
                            element.reducedSoulshot = Integer.parseInt(rs[1]);
                            break;
                        case "reuse_delay":
                            element.reuseDelay = Integer.parseInt(set.val);
                            break;
                        case "price":
                            element.price = Integer.parseInt(set.val);
                            break;
                        case "mp_consume":
                            element.mpConsume = Integer.parseInt(set.val);
                            break;
                        case "mp_consume_reduce":
                            String[] mpcr = set.val.split(",");
                            element.mpConsumeReduceChance = Integer.parseInt(mpcr[0]);
                            element.mpConsumeReduce = Integer.parseInt(mpcr[1]);
                            break;
                        case "bodypart":
                            element.bodyPart = EItemBodyPart.getByCode(set.val);
                            break;
                        case "random_damage":
                            element.randomDamage = Integer.parseInt(set.val);
                            break;
                        case "equip_condition":
                            break;
                        case "use_condition":
                            break;
                        case "duration":
                            element.duration = Integer.parseInt(set.val);
                            break;
                        case "weight":
                            element.weight = Integer.parseInt(set.val);
                            break;
                        case "etcitem_type":
                            element.etcItemType = EtcItemType.valueOf(set.val.toUpperCase());
                            break;
                        case "equip_reuse_delay":
                            element.reuseDelayEquip = Integer.parseInt(set.val);
                            break;
                        case "shared_reuse_group":
                            element.reuseDelaySharedGroup = Integer.parseInt(set.val);
                            break;
                        case "default_action":
                            element.defaultActionType = ActionType.valueOf(set.val);
                            break;

                    }
                    // endregion case

                    setIterator.remove();
                }
                if (!element.sets.isEmpty()) { System.out.println(">>> element.sets: " + element.sets); }

                switch (element.type) {
                    case "Weapon":
                        weapons.list.add(element);
                        break;
                    case "Armor":
                        armors.list.add(element);
                        break;
                    case "EtcItem":

                        if (element.etcItemType != null) {
                            ItemXmlFile f = etcMap.get(element.etcItemType);
                            if (f == null) {
                                f = new ItemXmlFile();
                            }
                            f.list.add(element);
                            etcMap.put(element.etcItemType, f);
                        }
                        else {
                            etcItems.list.add(element);
                        }

                        break;
                    default:
                        System.out.println(">>> just item: " + element.id);
                        break;
                }
            }

        }

        Comparator<ItemXmlElement> comparator = (o1, o2) -> o1.id.compareTo(o2.id);
        Collections.sort(weapons.list, comparator);
        Collections.sort(armors.list, comparator);
        Collections.sort(etcItems.list, comparator);

        Serializer.MAPPER.writeValue(new File("./data/xml/items_new/weapons.xml"), weapons);
        Serializer.MAPPER.writeValue(new File("./data/xml/items_new/armors.xml"), armors);
        Serializer.MAPPER.writeValue(new File("./data/xml/items_new/etc_none.xml"), etcItems);

        for (Map.Entry<EtcItemType, ItemXmlFile> entry : etcMap.entrySet()) {
            if (entry.getValue() != null) {
                Collections.sort(entry.getValue().list, comparator);
                Serializer.MAPPER.writeValue(new File("./data/xml/items_new/etc_" + entry.getKey().name().toLowerCase() + ".xml"), entry.getValue());
            }
        }

    }
}
