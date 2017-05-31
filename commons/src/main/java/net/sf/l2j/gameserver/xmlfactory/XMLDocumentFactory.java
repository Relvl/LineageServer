package net.sf.l2j.gameserver.xmlfactory;

import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;

/**
 * @author Forsaiken
 * @deprecated Я верю, что этот класс тут по ошибке...
 */
@Deprecated
public final class XMLDocumentFactory {
    private final DocumentBuilder _builder;

    protected XMLDocumentFactory() throws Exception {
        try {
            final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setValidating(false);
            factory.setIgnoringComments(true);

            _builder = factory.newDocumentBuilder();
        } catch (Exception e) {
            throw new Exception("Failed initializing", e);
        }
    }

    public static final XMLDocumentFactory getInstance() {
        return SingletonHolder._instance;
    }

    public final Document loadDocument(final String filePath) throws Exception {
        return loadDocument(new File(filePath));
    }

    public final Document loadDocument(final File file) throws Exception {
        if (!file.exists() || !file.isFile())
            throw new Exception("File: " + file.getAbsolutePath() + " doesn't exist and/or is not a file.");

        return _builder.parse(file);
    }

    public final Document newDocument() {
        return _builder.newDocument();
    }

    private static class SingletonHolder {
        protected static final XMLDocumentFactory _instance;

        static {
            try {
                _instance = new XMLDocumentFactory();
            } catch (Exception e) {
                throw new ExceptionInInitializerError(e);
            }
        }
    }
}