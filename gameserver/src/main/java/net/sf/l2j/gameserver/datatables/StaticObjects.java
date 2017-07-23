package net.sf.l2j.gameserver.datatables;

import net.sf.l2j.gameserver.idfactory.IdFactory;
import net.sf.l2j.gameserver.model.actor.instance.L2StaticObjectInstance;
import net.sf.l2j.gameserver.xmlfactory.XMLDocumentFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import java.io.File;

public class StaticObjects {
    private static final Logger LOGGER = LoggerFactory.getLogger(StaticObjects.class);

    public static void load() {
        try {
            File file = new File("./data/xml/static_objects.xml");
            Document doc = XMLDocumentFactory.getInstance().loadDocument(file);

            Node firstChild = doc.getFirstChild();
            for (Node child = firstChild.getFirstChild(); child != null; child = child.getNextSibling()) {
                if (child.getNodeName().equalsIgnoreCase("staticobject")) {
                    NamedNodeMap node = child.getAttributes();

                    L2StaticObjectInstance obj = new L2StaticObjectInstance(IdFactory.getInstance().getNextId());
                    obj.setType(Integer.valueOf(node.getNamedItem("type").getNodeValue()));
                    obj.setStaticObjectId(Integer.valueOf(node.getNamedItem("id").getNodeValue()));
                    obj.getPosition().setXYZ(Integer.valueOf(node.getNamedItem("x").getNodeValue()), Integer.valueOf(node.getNamedItem("y").getNodeValue()), Integer.valueOf(node.getNamedItem("z").getNodeValue()));
                    obj.setMap(node.getNamedItem("texture").getNodeValue(), Integer.valueOf(node.getNamedItem("map_x").getNodeValue()), Integer.valueOf(node.getNamedItem("map_y").getNodeValue()));
                    obj.spawnMe();

                }
            }
        }
        catch (Exception e) {
            LOGGER.warn("StaticObject: Error while creating StaticObjects table: ", e);
        }
    }
}