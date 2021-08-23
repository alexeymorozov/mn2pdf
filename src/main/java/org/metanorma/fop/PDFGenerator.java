package org.metanorma.fop;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import net.sourceforge.jeuclid.fop.plugin.JEuclidFopFactoryConfigurator;
import org.apache.fop.apps.FOPException;
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FopFactory;
import org.apache.fop.apps.MimeConstants;
import org.apache.fop.events.Event;
import org.apache.fop.events.EventFormatter;
import org.apache.fop.events.model.EventSeverity;
import org.apache.fop.render.intermediate.IFContext;
import org.apache.fop.render.intermediate.IFDocumentHandler;
import org.apache.fop.render.intermediate.IFParser;
import org.apache.fop.render.intermediate.IFSerializer;
import org.apache.fop.render.intermediate.IFUtil;
import static org.metanorma.Constants.*;
import static org.metanorma.fop.fontConfig.DEFAULT_FONT_PATH;
import static org.metanorma.fop.Util.getStreamFromResources;
import org.metanorma.utils.LoggerHelper;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 *
 * @author Alexander Dyuzhev
 */
public class PDFGenerator {
    
    protected static final Logger logger = Logger.getLogger(LoggerHelper.LOGGER_NAME);
    
    private String fontsPath = "";

    private String fontsManifest = "";
    
    final private String inputXMLFilePath;
    
    final private String inputXSLFilePath;
    
    final private String outputPDFFilePath;
    
    //private boolean isDebugMode = false;
    
    private boolean isSkipPDFGeneration = false;
    
    private boolean isSplitByLanguage = false;
    
    private boolean isAddMathAsText = false;
    
    private boolean isAddMathAsAttachment = false;
    
    private Properties xsltParams = new Properties();
    
    int pageCount = 0;
    
    boolean PDFUA_error = false;
    
    public void setFontsPath(String fontsPath) {
        this.fontsPath = fontsPath;
    }

    public void setFontsManifest(String fontsManifest) {
        this.fontsManifest = fontsManifest;
    }

    /*public void setDebugMode(boolean isDebugMode) {
        this.isDebugMode = isDebugMode;
    }*/

    public void setSkipPDFGeneration(boolean isSkipPDFGeneration) {
        this.isSkipPDFGeneration = isSkipPDFGeneration;
    }

    public void setSplitByLanguage(boolean isSplitByLanguage) {
        this.isSplitByLanguage = isSplitByLanguage;
    }

    public void setAddMathAsText(boolean isAddMathAsText) {
        this.isAddMathAsText = isAddMathAsText;
    }
    
    public void setAddMathAsAttachment(boolean isAddMathAsAttachment) {
        this.isAddMathAsAttachment = isAddMathAsAttachment;
    }
    
    
    public void setXSLTParams(Properties xsltParams) {
        this.xsltParams = xsltParams;
    }
    
    
    
    
    public PDFGenerator (String inputXMLFilePath, String inputXSLFilePath, String outputPDFFilePath) {
        this.inputXMLFilePath = inputXMLFilePath;
        this.inputXSLFilePath = inputXSLFilePath;
        this.outputPDFFilePath = outputPDFFilePath;
    }
    
    
    public boolean process() {
        try {
            
            logger.info("Preparing...");
            
            File fXML = new File(inputXMLFilePath);
            if (!fXML.exists()) {
                logger.severe(String.format(INPUT_NOT_FOUND, XML_INPUT, fXML));
                return false;
            }
            
            File fXSL = new File(inputXSLFilePath);
            if (!fXSL.exists()) {
                logger.severe(String.format(INPUT_NOT_FOUND, XSL_INPUT, fXSL));
                return false;
            }
            
            File fFontsManifest = null;
            if (!fontsManifest.isEmpty()) {
                fFontsManifest = new File(fontsManifest);
                if (!fFontsManifest.exists()) {
                    //System.out.println(String.format(INPUT_NOT_FOUND, "Font manifest", fFontManifest));
                    logger.severe(String.format(INPUT_NOT_FOUND, "Font manifest", fFontsManifest));
                    //System.exit(ERROR_EXIT_CODE);
                    return false;
                }
            }
            
            File fPDF = new File(outputPDFFilePath);
            
            if (!fontsManifest.isEmpty() && fontsPath.isEmpty()) {
                    // no output
            } else {
                //System.out.println(String.format(INPUT_LOG, FONTS_FOLDER_INPUT, argFontsPath));
                logger.info(String.format(INPUT_LOG, FONTS_FOLDER_INPUT, fontsPath));
            }
            
            if (fontsPath.isEmpty()) {
                fontsPath = DEFAULT_FONT_PATH;
            }
            
            logger.info(String.format(INPUT_LOG, XML_INPUT, fXML));
            logger.info(String.format(INPUT_LOG, XSL_INPUT, fXSL));
            
            if (!xsltParams.isEmpty()) {                    
                logger.info(String.format(INPUT_LOG, XSL_INPUT_PARAMS, xsltParams.toString()));
            }
            
            logger.info(String.format(OUTPUT_LOG, PDF_OUTPUT, fPDF));
            logger.info("");
            
            SourceXMLDocument sourceXMLDocument = new SourceXMLDocument(fXML);
            
            XSLTconverter xsltConverter = new XSLTconverter(fXSL);
            xsltConverter.setParams(xsltParams);
            
            fontConfig fontcfg = new fontConfig();
            fontcfg.setFontPath(fontsPath);

            fontcfg.setFontManifest(fFontsManifest);
            
            //debug
            fontcfg.outputFontManifestLog(Paths.get(fPDF.getAbsolutePath() + ".fontmanifest.log.txt"));
            
            convertmn2pdf(fontcfg, sourceXMLDocument, xsltConverter, fPDF);
            
            
            if (isSplitByLanguage) {
                int initial_page_number = 1;
                int coverpages_count = Util.getCoverPagesCount(fXSL);
                //determine how many documents in source XML
                ArrayList<String> languages = sourceXMLDocument.getLanguagesList(); 
                for (int i = 0; i< languages.size(); i++) {
                    if (i>=1)  {
                        xsltParams.setProperty("initial_page_number", "" + initial_page_number);
                    }
                    xsltParams.setProperty("doc_split_by_language", "" + languages.get(i));

                    xsltConverter.setParams(xsltParams);

                    //add language code to output PDF
                    String argPDFsplit = outputPDFFilePath;                            
                    argPDFsplit = argPDFsplit.substring(0, argPDFsplit.lastIndexOf(".")) + "_" + languages.get(i) + argPDFsplit.substring(argPDFsplit.lastIndexOf("."));
                    File fPDFsplit = new File(argPDFsplit);

                    logger.log(Level.INFO, "Generate PDF for language ''{0}''.", languages.get(i));
                    logger.log(Level.INFO, "Output: PDF ({0})", fPDFsplit);

                    convertmn2pdf(fontcfg, sourceXMLDocument, xsltConverter, fPDFsplit);

                    // initial page number for 'next' document
                    initial_page_number = (getPageCount() - coverpages_count) + 1;
                }                        
            }
            
            // flush temporary folder
            if (!DEBUG) {
                sourceXMLDocument.flushTempPath();
            }
            
            logger.info("Success!");
            
        } catch (Exception e) {
            e.printStackTrace(System.err);
            return false;
        }
        return true;
    }
    
    
    /**
     * Converts an XML file to a PDF file using FOP
     *
     * @param config the FOP config file
     * @param xml the XML source file
     * @param xsl the XSL file
     * @param pdf the target PDF file
     * @throws IOException In case of an I/O problem
     * @throws FOPException, SAXException In case of a FOP problem
     */
    private void convertmn2pdf(fontConfig fontcfg, SourceXMLDocument sourceXMLDocument, XSLTconverter xsltConverter, File pdf) throws IOException, FOPException, SAXException, TransformerException, ParserConfigurationException {
        
        String imagesxml = sourceXMLDocument.getImageFilePath();
                
        String indexxml = sourceXMLDocument.getIndexFilePath();
        
        try {
            
            //Setup XSLT
            Properties additionalXSLTparams = new Properties();
            additionalXSLTparams.setProperty("svg_images", imagesxml);
            
            File fileXmlIF = new File(indexxml);
            if (fileXmlIF.exists()) {
                // for document by language
                // index.xml was created for bilingual document
                additionalXSLTparams.setProperty("external_index", fileXmlIF.getAbsolutePath());
            }
            additionalXSLTparams.setProperty("basepath", sourceXMLDocument.getDocumentFilePath() + File.separator);
            xsltConverter.setParams(additionalXSLTparams);
            
            //System.out.println("[INFO] XSL-FO file preparation...");
            logger.info("[INFO] XSL-FO file preparation...");
            
            // transform XML to XSL-FO (XML .fo file)
            long startTime = System.currentTimeMillis();
            xsltConverter.transform(sourceXMLDocument);
            long endTime = System.currentTimeMillis();
            if (DEBUG) {
                //System.out.println("processing time: " + (endTime - startTime) + " milliseconds");
                logger.log(Level.INFO, "processing time: {0} milliseconds", endTime - startTime);
            }

            String xmlFO = sourceXMLDocument.getXMLFO();
            
            if (DEBUG) {   
                //DEBUG: write intermediate FO to file                
                String xmlFO_UTF8 = xmlFO.replace("<?xml version=\"1.0\" encoding=\"UTF-16\"?>", "<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
                try ( 
                    BufferedWriter writer = Files.newBufferedWriter(Paths.get(pdf.getAbsolutePath() + ".fo.xml"))) {
                        writer.write(xmlFO_UTF8);                    
                }
                //Setup output
                //OutputStream outstream = new java.io.FileOutputStream(pdf.getAbsolutePath() + ".fo.xml");
                //Resulting SAX events (the generated FO) must be piped through to FOP
                //Result res = new StreamResult(outstream);
                //Start XSLT transformation and FO generating
                //transformer.transform(src, res);
            }
            
            fontcfg.setSourceDocumentFontList(sourceXMLDocument.getDocumentFonts());
            
            Source src = new StreamSource(new StringReader(xmlFO));
            
            src = runSecondPass (indexxml, src, fontcfg, additionalXSLTparams, sourceXMLDocument, xsltConverter, pdf);
            
            
            // FO processing by FOP
            
            //src = new StreamSource(new StringReader(xmlFO));
            
            runFOP(fontcfg, src, pdf);
            
            if(PDFUA_error) {
                logger.info("WARNING: Trying to generate PDF in non PDF/UA-1 mode.");
                fontcfg.setPDFUAmode("DISABLED");
                src = new StreamSource(new StringReader(xmlFO));
                runFOP(fontcfg, src, pdf);
                logger.info(WARNING_NONPDFUA);
            }
            
            for(String msg: fontcfg.getMessages()) {
            	logger.info(msg);
            }
        } catch (Exception e) {
            e.printStackTrace(System.err);
            System.exit(ERROR_EXIT_CODE);
        }
        
            
    }
    
    
    private void runFOP (fontConfig fontcfg, Source src, File pdf) throws IOException, FOPException, SAXException, TransformerException {
        OutputStream out = null;
        try {
            
            String mime = MimeConstants.MIME_PDF;
            
            if (isAddMathAsText) {
                logger.info("Adding Math as text...");
                logger.info("Transforming to Intermediate Format...");
                String xmlIF = generateFOPIntermediatFormat(src, fontcfg.getConfig(), pdf, false);
                logger.info("Updating Intermediate Format...");
                xmlIF = applyXSLT("add_hidden_math.xsl ", xmlIF);
                if (DEBUG) {   //DEBUG: write intermediate IF to file
                try ( 
                    BufferedWriter writer = Files.newBufferedWriter(Paths.get(pdf.getAbsolutePath() + ".if.mathtext.xml"))) {
                        writer.write(xmlIF);                    
                }
            }
                src = new StreamSource(new StringReader(xmlIF));
            }
            
            logger.info("Transforming to PDF...");
            
            TransformerFactory factory = TransformerFactory.newInstance();            
            Transformer transformer = factory.newTransformer(); // identity transformer
            
            //System.out.println("Transforming...");
            
            // Step 1: Construct a FopFactory by specifying a reference to the configuration file
            FopFactory fopFactory = FopFactory.newInstance(fontcfg.getConfig());
            
            //debug
            fontcfg.outputFOPFontsLog(Paths.get(pdf.getAbsolutePath() + ".fopfonts.log.txt"));
            fontcfg.outputAvailableAWTFonts(Paths.get(pdf.getAbsolutePath() + ".awtfonts.log.txt"));

            JEuclidFopFactoryConfigurator.configure(fopFactory);
            FOUserAgent foUserAgent = fopFactory.newFOUserAgent();
            // configure foUserAgent
            foUserAgent.setProducer("Ribose Metanorma mn2pdf version " + Util.getAppVersion());
            
            //Adding a simple logging listener that writes to stdout and stderr            
            //foUserAgent.getEventBroadcaster().addEventListener(new SysOutEventListener());
            // Add your own event listener
            //foUserAgent.getEventBroadcaster().addEventListener(new MyEventListener());

            // Setup output stream.  Note: Using BufferedOutputStream
            // for performance reasons (helpful with FileOutputStreams).
            out = new FileOutputStream(pdf);
            out = new BufferedOutputStream(out);
            
            if (isAddMathAsText) { // process IF to PDF
                //Setup target handler
                IFDocumentHandler targetHandler = fopFactory.getRendererFactory().createDocumentHandler(
                        foUserAgent, mime);
                //Setup fonts
                IFUtil.setupFonts(targetHandler);
                targetHandler.setResult(new StreamResult(out));
                
                IFParser parser = new IFParser();
                
                //Send XSLT result to AreaTreeParser
                SAXResult res = new SAXResult(parser.getContentHandler(targetHandler, foUserAgent));
                
                //Start area tree parsing
                if (!isSkipPDFGeneration) {
                    transformer.transform(src, res);
                    //this.pageCount = fop.getResults().getPageCount();
                }
            } 
                
            else {


                // Construct fop with desired output format
                Fop fop = fopFactory.newFop(mime, foUserAgent, out);

                // Setup JAXP using identity transformer
                //factory = TransformerFactory.newInstance();
                //transformer = factory.newTransformer(); // identity transformer


                // Resulting SAX events (the generated FO) must be piped through to FOP
                Result res = new SAXResult(fop.getDefaultHandler());

                transformer.setErrorListener(new DefaultErrorListener());

                // Start XSLT transformation and FOP processing
                // Setup input stream   

                if (!isSkipPDFGeneration) {
                    transformer.transform(src, res);  

                    this.pageCount = fop.getResults().getPageCount();
                }
            }
            
        } catch (Exception e) {
            String excstr=e.toString();
            if (excstr.contains("PDFConformanceException") && excstr.contains("PDF/UA-1") && !PDFUA_error) { // excstr.contains("all fonts, even the base 14 fonts, have to be embedded")
                //System.err.println(e.toString());
                logger.severe(e.toString());
                PDFUA_error = true;
            } else {
                e.printStackTrace(System.err);
                System.exit(ERROR_EXIT_CODE);
            } 
            
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }
    
    private Source runSecondPass (String indexxml, Source sourceFO, fontConfig fontcfg, Properties xslparams, SourceXMLDocument sourceXMLDocument, XSLTconverter xsltConverter, File pdf)  throws Exception, IOException, FOPException, SAXException, TransformerException, ParserConfigurationException {
        Source src = sourceFO;
        
        File fileXmlIF = new File(indexxml);
        
        if (!indexxml.isEmpty() && !fileXmlIF.exists()) { //there is index
             // if file exist - it means that now document by language is processing
            // and don't need to create intermediate file again

            String xmlIF = generateFOPIntermediatFormat(sourceFO, fontcfg.getConfig(), pdf, true);

            
            //Util.createIndexFile(indexxml, xmlIF);
            createIndexFile(indexxml, xmlIF);

            if (fileXmlIF.exists()) {
                // pass index.xml path to xslt (for second pass)
                xslparams.setProperty("external_index", fileXmlIF.getAbsolutePath());

                xsltConverter.setParams(xslparams);
            }
            
            System.out.println("[INFO] XSL-FO file preparation (second pass)...");
            // transform XML to XSL-FO (XML .fo file)
            xsltConverter.transform(sourceXMLDocument);

            String xmlFO = sourceXMLDocument.getXMLFO();

            if (DEBUG) {   
                //DEBUG: write intermediate FO to file                
                try ( 
                    BufferedWriter writer = Files.newBufferedWriter(Paths.get(pdf.getAbsolutePath() + ".fo.2nd.xml"))) {
                        writer.write(xmlFO);                    
                }
            }
            src = new StreamSource(new StringReader(xmlFO));
            
        }
        return src;
    }
    
    
    private String generateFOPIntermediatFormat(Source src, File fontConfig, File pdf, boolean isSecondPass) throws SAXException, IOException, TransformerConfigurationException, TransformerException {
        String xmlIF = "";
        
        // run 1st pass to produce FOP Intermediate Format
        FopFactory fopFactory = FopFactory.newInstance(fontConfig);
        //Create a user agent
        FOUserAgent userAgent = fopFactory.newFOUserAgent();
        //Create an instance of the target document handler so the IFSerializer
        //can use its font setup
        IFDocumentHandler targetHandler = userAgent.getRendererFactory().createDocumentHandler(
                userAgent, MimeConstants.MIME_PDF);
        //Create the IFSerializer to write the intermediate format
        IFSerializer ifSerializer = new IFSerializer(new IFContext(userAgent));
        //Tell the IFSerializer to mimic the target format
        ifSerializer.mimicDocumentHandler(targetHandler);
        //Make sure the prepared document handler is used
        userAgent.setDocumentHandlerOverride(ifSerializer);
        if (isSecondPass) {
            userAgent.getEventBroadcaster().addEventListener(new SecondPassSysOutEventListener());
        }
        JEuclidFopFactoryConfigurator.configure(fopFactory);
        
        // Setup output
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        //out = new java.io.BufferedOutputStream(out);
        //String ifFilename = indexxml + ".if";
        //OutputStream out = new java.io.FileOutputStream(new File(ifFilename));
        try {
            // Construct FOP (the MIME type here is unimportant due to the override
            // on the user agent)
            Fop fop = fopFactory.newFop(null, userAgent, out);

            Result res = new SAXResult(fop.getDefaultHandler());

            // Setup XSLT
            TransformerFactory factory = TransformerFactory.newInstance();
            Transformer transformer = factory.newTransformer(); // identity transformer

            transformer.setErrorListener(new DefaultErrorListener());
            System.out.println("[INFO] Rendering into intermediate format for index preparation...");
            // Start XSLT transformation and FOP processing
            transformer.transform(src, res);

            xmlIF = out.toString("UTF-8");

            if (DEBUG) {   
                //DEBUG: write intermediate IF to file                
                try ( 
                    BufferedWriter writer = Files.newBufferedWriter(Paths.get(pdf.getAbsolutePath() + ".if.xml"))) {
                        writer.write(xmlIF);                    
                }
            }

        } finally {
            out.close();
        }
        
        return xmlIF;
    }
    
    private void createIndexFile(String indexxmlFilePath, String intermediateXML) {
        try {
            String xmlIndex = applyXSLT("index.xsl", intermediateXML);
            
            if (xmlIndex.length() != 0) {
                try ( 
                    BufferedWriter writer = Files.newBufferedWriter(Paths.get(indexxmlFilePath))) {
                        writer.write(xmlIndex.toString());                    
                }
            }
        }    
        catch (Exception ex) {
            //System.err.println("Can't save index.xml into temporary folder");
            logger.severe("Can't save index.xml into temporary folder");
            ex.printStackTrace();
        }    
    }
    
    // Apply XSL tranformation (file xsltfile) for xml string
    private String applyXSLT(String xsltfile, String xmlStr) throws Exception {
        
        Source srcXSL =  new StreamSource(getStreamFromResources(getClass().getClassLoader(), xsltfile));
        TransformerFactory factory = TransformerFactory.newInstance();
        Transformer transformer = factory.newTransformer(srcXSL);
        Source src = new StreamSource(new StringReader(xmlStr));
        StringWriter resultWriter = new StringWriter();
        StreamResult sr = new StreamResult(resultWriter);
        transformer.transform(src, sr);
        String xmlResult = resultWriter.toString();
        
        return xmlResult;
    }
    
    private class DefaultErrorListener implements javax.xml.transform.ErrorListener {

        public void warning(TransformerException exc) {
            logger.severe(exc.toString());
        }

        public void error(TransformerException exc)
                throws TransformerException {
            throw exc;
        }

        public void fatalError(TransformerException exc)
                throws TransformerException {
            String excstr=exc.toString();
            if (excstr.contains("PDFConformanceException") && excstr.contains("PDF/UA-1") && !PDFUA_error) { // excstr.contains("all fonts, even the base 14 fonts, have to be embedded")
                //System.err.println(exc.toString());
                logger.severe(exc.toString());
                PDFUA_error = true;
            } else {
                throw exc;
            }            
        }
    }
    
   /* private static class MyEventListener implements org.apache.fop.events.EventListener {

        public void processEvent(Event event) {
            if ("org.apache.fop.layoutmgr.BlockLevelEventProducer.overconstrainedAdjustEndIndent".
                    equals(event.getEventID())) {
                //skip
            } else
            if("org.apache.fop.render.RendererEventProducer.endPage".
                    equals(event.getEventID())) {
                //skip
            }else 
            if ("org.apache.fop.pdf.PDFConformanceException".
                    equals(event.getEventID())) {
                System.err.println(new RuntimeException(EventFormatter.format(event)).toString());
                PDFUA_error = true;
            } 
            else
            if ("org.apache.fop.ResourceEventProducer.imageNotFound"
                    .equals(event.getEventID())) {

                //Get the FileNotFoundException that's part of the event's parameters
                //FileNotFoundException fnfe = (FileNotFoundException)event.getParam("fnfe");

                System.out.println("---=== imageNotFound Event for " + event.getParam("uri")
                        + "!!! ===---");
                //Stop processing when an image could not be found. Otherwise, FOP would just
                //continue without the image!

                System.out.println("Throwing a RuntimeException...");
                //throw new RuntimeException(EventFormatter.format(event), fnfe);
            } else {
                //ignore all other events
            }
        }

    }*/

    /** A simple event listener that writes the events to stdout and sterr. */
    //private static class SysOutEventListener implements org.apache.fop.events.EventListener {

        /** {@inheritDoc} */
   /*     public void processEvent(Event event) {
            String msg = EventFormatter.format(event);
            EventSeverity severity = event.getSeverity();
            if (severity == EventSeverity.INFO) {
                System.out.println("[INFO ] " + msg);
            } else if (severity == EventSeverity.WARN) {
                System.out.println("[WARN ] " + msg);
            } else if (severity == EventSeverity.ERROR) {
                System.err.println("[ERROR] " + msg);
            } else if (severity == EventSeverity.FATAL) {
                System.err.println("[FATAL] " + msg);
            } else {
                assert false;
            }
        }
    }*/
    
    /** A simple event listener that writes the events to stdout and sterr. */
    private static class SecondPassSysOutEventListener implements org.apache.fop.events.EventListener {

        /** {@inheritDoc} */
        public void processEvent(Event event) {
            String msg = EventFormatter.format(event);
            EventSeverity severity = event.getSeverity();
            if (severity == EventSeverity.INFO) {
                if(msg.startsWith("Rendered page #")) {
                    //System.out.println("[INFO] Intermediate format. " + msg);
                    logger.log(Level.INFO, "[INFO] Intermediate format. {0}", msg);
                }
            } else if (severity == EventSeverity.WARN) {
                //System.out.println("[WARN] " + msg);
            } else if (severity == EventSeverity.ERROR) {
                //System.err.println("[ERROR] " + msg);
                logger.log(Level.SEVERE, "[ERROR] {0}", msg);
            } else if (severity == EventSeverity.FATAL) {
                //System.err.println("[FATAL] " + msg);
                logger.log(Level.SEVERE, "[FATAL] {0}", msg);
            } else {
                assert false;
            }
        }
    }
    
    
    private int getPageCount() {
        return pageCount;
    }
    
}
