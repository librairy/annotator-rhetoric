package org.librairy.rhetoric.parser;

import edu.upf.taln.dri.lib.Factory;
import edu.upf.taln.dri.lib.exception.DRIexception;
import edu.upf.taln.dri.lib.model.Document;
import edu.upf.taln.dri.lib.model.ext.RhetoricalClassENUM;
import edu.upf.taln.dri.lib.model.ext.Section;
import edu.upf.taln.dri.lib.model.ext.Sentence;
import edu.upf.taln.dri.lib.model.ext.SentenceSelectorENUM;
import edu.upf.taln.dri.lib.util.ModuleConfig;
import edu.upf.taln.dri.lib.util.PDFtoTextConvMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */
@Component
public class RhetoricalParser {

    private static final Logger LOG = LoggerFactory.getLogger(RhetoricalParser.class);

    @PostConstruct
    public void setup(){
//        String path = this.getClass().getClassLoader().getResource("DRIconfig.properties").getFile();
//        LOG.info("Config path: " + path);
//
//        if (!Factory.setDRIPropertyFilePath(path)) throw new RuntimeException("Text mining librairy not configured but properties file missing");

        // To use PDFX:
//        Factory.setPDFtoTextConverter(PDFtoTextConvMethod.PDFX);

        // To use GROBID:
        Factory.setPDFtoTextConverter(PDFtoTextConvMethod.GROBID);

        // Instantiate the ModuleConfig class - the constructor sets all modules enabled by default
        ModuleConfig modConfigurationObj = new ModuleConfig();

        // Enable the parsing of bibliographic entries by means of online services (Bibsonomy, CrossRef, FreeCite, etc.)
        modConfigurationObj.setEnableBibEntryParsing(false);

        // Enable BabelNet Word Sense Disambiguation and Entity Linking over the text of the paper
        modConfigurationObj.setEnableBabelNetParsing(false);

        // Enable the parsing of the information from the header of the paper by means of online services (Bibsonomy, CrossRef, FreeCite, etc.)
        modConfigurationObj.setEnableHeaderParsing(false);

        // Enable the extraction of candidate terms from the sentences of the paper
        modConfigurationObj.setEnableTerminologyParsing(false);

        // Enable the dependency parsing of the sentences of a paper
        modConfigurationObj.setEnableGraphParsing(false);

        // Enable coreference resolution
        modConfigurationObj.setEnableCoreferenceResolution(false);

        // Enable the extraction of causal relations
        modConfigurationObj.setEnableCausalityParsing(false);

        // Enable the association of a rhetorical category to the sentences of the paper
        modConfigurationObj.setEnableRhetoricalClassification(true);

        // Improt the configuration parameters set in the ModuleConfig instance
        Factory.setModuleConfig(modConfigurationObj);

        LOG.info("Text Mining Modules' enable status: " + Factory.getModuleConfig().toString());

        try {
            Factory.initFramework();
        } catch (DRIexception drIexception) {
            throw new RuntimeException("Error initializing text mining library", drIexception);
        }
    }

    public Map<String,String> getParts(String id, String content){

        Map<String,String> parts = new HashMap<>();

        try {
            LOG.info("Retrieving the rhetorical parts of the document: '" + id + "' ");

            Document document = Factory.getPlainTextLoader().parseString(content, id);

            // Sections
            List<Section> sections = document.extractSections(false);

            // ->   inner sections
            sections.forEach(section -> parts.put(section.getName().toLowerCase(),_join(section.getSentences())
            ));

            // Rhetorical Classes
            List<Sentence> sentences = document.extractSentences(SentenceSelectorENUM.ALL);

            // -> approach
            List<Sentence> approachSentences = sentences.stream().filter(s -> s.getRhetoricalClass
                    ().equals(RhetoricalClassENUM.DRI_Approach)).collect(Collectors.toList());
            parts.put("approach",_join(approachSentences));

            // -> background
            List<Sentence> backgroundSentences = sentences.stream().filter(s -> s.getRhetoricalClass
                    ().equals(RhetoricalClassENUM.DRI_Background)).collect(Collectors.toList());
            parts.put("background",_join(backgroundSentences));

            // -> outcome
            List<Sentence> outcomeSentences = sentences.stream().filter(s -> s.getRhetoricalClass
                    ().equals(RhetoricalClassENUM.DRI_Outcome)).collect(Collectors.toList());
            parts.put("outcome",_join(outcomeSentences));

            // -> futureWork
            List<Sentence> futureSentences = sentences.stream().filter(s -> s.getRhetoricalClass
                    ().equals(RhetoricalClassENUM.DRI_FutureWork)).collect(Collectors.toList());
            parts.put("futureWork",_join(futureSentences));

            // -> challenge
            List<Sentence> challengeSentences = sentences.stream().filter(s -> s.getRhetoricalClass
                    ().equals(RhetoricalClassENUM.DRI_Challenge)).collect(Collectors.toList());
            parts.put("challenge",_join(challengeSentences));

            // CleanUp
            document.cleanUp();

            LOG.info("Analysis completed!");

        } catch (DRIexception drIexception) {
            LOG.error("Error processing document: " + id, drIexception);
        }

        return parts;
    }

    private String _join(List<Sentence> sentences){
        return sentences.stream().map(s -> s.getText()).collect(Collectors.joining(" "));
    }

}
