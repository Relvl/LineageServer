package net.sf.l2j;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import net.sf.l2j.commons.serialize.converter.BooleanFalseOnlyConverter;
import net.sf.l2j.gameserver.model.item.EItemBodyPart;
import net.sf.l2j.gameserver.model.item.type.*;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Johnson / 19.07.2017
 */
@JsonRootName("list")
public class ItemXmlFile {

    @JacksonXmlProperty(localName = "item")
    @JacksonXmlElementWrapper(useWrapping = false)
    public List<ItemXmlElement> list = new ArrayList<>();

    @JsonInclude(Include.NON_EMPTY)
    public static class ItemXmlElement {
        @JacksonXmlProperty(localName = "id", isAttribute = true)
        public Integer id;
        @JacksonXmlProperty(localName = "type", isAttribute = true)
        public String type;
        @JacksonXmlProperty(localName = "name", isAttribute = true)
        private String name;

        @JacksonXmlProperty(localName = "set")
        @JacksonXmlElementWrapper(useWrapping = false)
        @JsonInclude(Include.NON_EMPTY)
        public List<SetElement> sets = new ArrayList<>();

        @JacksonXmlProperty(localName = "cond")
        public CondElement condElement;

        @JacksonXmlProperty(localName = "for")
        public ForContainer forContainer;

        // ================================================= new properties

        @JacksonXmlProperty(localName = "sellable")
        @JsonSerialize(converter = BooleanFalseOnlyConverter.class)
        public Boolean sellable = true;

        @JacksonXmlProperty(localName = "depositable")
        @JsonSerialize(converter = BooleanFalseOnlyConverter.class)
        public Boolean depositable = true;

        @JacksonXmlProperty(localName = "destroyable")
        @JsonSerialize(converter = BooleanFalseOnlyConverter.class)
        public Boolean destroyable = true;

        @JacksonXmlProperty(localName = "dropable")
        @JsonSerialize(converter = BooleanFalseOnlyConverter.class)
        public Boolean dropable = true;

        @JacksonXmlProperty(localName = "stackable")
        @JsonInclude(Include.NON_DEFAULT)
        public Boolean stackable;

        @JacksonXmlProperty(localName = "tradable")
        @JsonSerialize(converter = BooleanFalseOnlyConverter.class)
        public Boolean tradable = true;

        @JacksonXmlProperty(localName = "olympiadRestricted")
        @JsonInclude(Include.NON_DEFAULT)
        public Boolean olympiadRestricted;

        @JacksonXmlProperty(localName = "armorType")
        public ArmorType armorType; // armor only

        @JacksonXmlProperty(localName = "handlerName")
        public String handlerName;

        @JacksonXmlProperty(localName = "attackRange")
        public Integer attackRange; // weapon only

        @JacksonXmlProperty(localName = "weaponType")
        public EWeaponType weaponType; // weapon only

        @JacksonXmlProperty(localName = "itemSkill")
        @JacksonXmlElementWrapper(useWrapping = false)
        public List<ItemSkillInfo> itemSkill;

        @JacksonXmlProperty(localName = "onCastSkill")
        public ItemSkillInfo onCastSkill;

        @JacksonXmlProperty(localName = "onCritSkill")
        public ItemSkillInfo onCritSkill;

        @JacksonXmlProperty(localName = "enchant4skill")
        public ItemSkillInfo enchant4skill;

        @JacksonXmlProperty(localName = "soulshots")
        public Integer soulshots;

        @JacksonXmlProperty(localName = "reducedSoulshot")
        public Integer reducedSoulshot; // weapon only, for "Miser" SA

        @JacksonXmlProperty(localName = "reducedSoulshotChance")
        public Integer reducedSoulshotChance; // weapon only, for "Miser" SA

        @JacksonXmlProperty(localName = "spiritshots")
        public Integer spiritshots;

        @JacksonXmlProperty(localName = "crystalType")
        public CrystalType crystalType;

        @JacksonXmlProperty(localName = "crystalCount")
        public Integer crystalCount;

        @JacksonXmlProperty(localName = "material")
        public MaterialType material;

        @JacksonXmlProperty(localName = "magical")
        @JsonInclude(Include.NON_DEFAULT)
        public Boolean magical;

        @JacksonXmlProperty(localName = "reuseDelay")
        public Integer reuseDelay;

        @JacksonXmlProperty(localName = "reuseDelaySharedGroup")
        public Integer reuseDelaySharedGroup;

        @JacksonXmlProperty(localName = "reuseDelayEquip")
        public Integer reuseDelayEquip;

        @JacksonXmlProperty(localName = "price")
        public Integer price;

        @JacksonXmlProperty(localName = "mpConsume")
        public Integer mpConsume;

        @JacksonXmlProperty(localName = "mpConsumeReduce")
        public Integer mpConsumeReduce;

        @JacksonXmlProperty(localName = "mpConsumeReduceChance")
        public Integer mpConsumeReduceChance;

        @JacksonXmlProperty(localName = "bodyPart")
        public EItemBodyPart bodyPart;

        @JacksonXmlProperty(localName = "randomDamage")
        public Integer randomDamage;

        @JacksonXmlProperty(localName = "duration")
        public Integer duration;

        @JacksonXmlProperty(localName = "weight")
        public Integer weight;

        @JacksonXmlProperty(localName = "etcItemType")
        public EtcItemType etcItemType;

        @JacksonXmlProperty(localName = "defaultActionType")
        public ActionType defaultActionType;
    }

    @JsonInclude(Include.NON_EMPTY)
    public static class ItemSkillInfo {
        @JacksonXmlProperty(localName = "id", isAttribute = true)
        public Integer id;
        @JacksonXmlProperty(localName = "level", isAttribute = true)
        public Integer level;
        @JacksonXmlProperty(localName = "chance", isAttribute = true)
        public Integer chance;
    }

    public static class SetElement {
        @JacksonXmlProperty(localName = "name", isAttribute = true)
        public String name;
        @JacksonXmlProperty(localName = "val", isAttribute = true)
        public String val;
    }

    @JsonInclude(Include.NON_EMPTY)
    public static class CondElement {
        @JacksonXmlProperty(localName = "msgId", isAttribute = true)
        private Integer msgId;

        @JacksonXmlProperty(localName = "and")
        private CondElement andCollection;

        @JacksonXmlProperty(localName = "player")
        @JacksonXmlElementWrapper(useWrapping = false)
        private List<CondPlayerElement> players;
    }

    @JsonInclude(Include.NON_EMPTY)
    private static class CondPlayerElement {
        @JacksonXmlProperty(localName = "isHero", isAttribute = true)
        private Boolean isHero;
        @JacksonXmlProperty(localName = "castle", isAttribute = true)
        private Integer castle;
        @JacksonXmlProperty(localName = "pledgeClass", isAttribute = true)
        private Integer pledgeClass;
        @JacksonXmlProperty(localName = "sex", isAttribute = true)
        private Integer sex;
        @JacksonXmlProperty(localName = "clanHall", isAttribute = true)
        private Integer clanHall;
        @JacksonXmlProperty(localName = "pkCount", isAttribute = true)
        private Integer pkCount;
        @JacksonXmlProperty(localName = "level", isAttribute = true)
        private Integer level;
    }

    @JsonInclude(Include.NON_EMPTY)
    public static class ForContainer {
        @JacksonXmlProperty(localName = "set")
        @JacksonXmlElementWrapper(useWrapping = false)
        private final List<ForElement> setElements = new ArrayList<>();

        @JacksonXmlProperty(localName = "add")
        @JacksonXmlElementWrapper(useWrapping = false)
        private final List<ForElement> addElements = new ArrayList<>();

        @JacksonXmlProperty(localName = "sub")
        @JacksonXmlElementWrapper(useWrapping = false)
        private final List<ForElement> subElements = new ArrayList<>();

        @JacksonXmlProperty(localName = "enchant")
        @JacksonXmlElementWrapper(useWrapping = false)
        private final List<ForElement> enchantElements = new ArrayList<>();
    }

    @JsonInclude(Include.NON_EMPTY)
    public static class ForElement {
        @JacksonXmlProperty(localName = "order", isAttribute = true)
        private String order;
        @JacksonXmlProperty(localName = "stat", isAttribute = true)
        private String stat;
        @JacksonXmlProperty(localName = "val", isAttribute = true)
        private String val;
    }
}
