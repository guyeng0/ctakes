#!/usr/bin/env groovy
/**
** 	This assumes that you have installed Groovy and 
** 	that you have the command groovy available in your path. 
** 	On Debian/Ubuntu systems, installing Groovy should be as easy as apt-get install groovy.
** 	You can download groovy from http://groovy.codehaus.org/
** 	The first run may be slow since it needs to download all of the dependencies.
**  Usage: $./parser.groovy [inputDir]
** 	or enable more verbose status $groovy -Dgroovy.grape.report.downloads=true parser.groovy [inputDir]
**/
@Grab(group='org.apache.ctakes',
      module='ctakes-core',
            version='3.1.0')
@Grab(group='org.apache.ctakes',
      module='ctakes-core-res',
            version='3.1.0')			
@Grab(group='org.apache.ctakes',
      module='ctakes-constituency-parser',
            version='3.1.0')
@Grab(group='org.apache.ctakes',
      module='ctakes-constituency-parser-res',
            version='3.1.0')
@Grab(group='org.cleartk',
      module='cleartk-util',
      version='0.9.2')

/*
@Grab(group='org.apache.ctakes',
      module='ctakes-clinical-pipeline',
            version='3.1.0')
*/
          
import java.io.File;
import org.apache.uima.cas.CAS;
import org.apache.uima.jcas.JCas;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReader;
import org.cleartk.util.cr.FilesCollectionReader;
import org.uimafit.factory.AnalysisEngineFactory;
import org.uimafit.factory.AggregateBuilder;
import org.uimafit.pipeline.SimplePipeline;	
import org.uimafit.component.JCasAnnotator_ImplBase;
import org.uimafit.factory.TypeSystemDescriptionFactory;
import org.uimafit.factory.TypePrioritiesFactory;
import static org.uimafit.util.JCasUtil.*;

import org.apache.ctakes.typesystem.type.syntax.BaseToken;
import org.apache.ctakes.typesystem.type.textspan.Segment;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.ctakes.typesystem.type.syntax.TopTreebankNode;
import org.apache.ctakes.core.resource.FileLocator;
import org.apache.ctakes.core.ae.SentenceDetector;
import org.apache.ctakes.core.ae.SimpleSegmentAnnotator;
import org.apache.ctakes.core.ae.TokenizerAnnotatorPTB;
import org.apache.ctakes.constituency.parser.ae.ConstituencyParser;
import org.uimafit.util.JCasUtil;

		if(args.length < 1) {
		System.out.println("Please specify input directory");
		System.exit(1);
		}
		System.out.println("Reading from directory: " + args[0]);

		CollectionReader collectionReader = FilesCollectionReader.getCollectionReaderWithSuffixes(args[0], CAS.NAME_DEFAULT_SOFA, "txt");
		//Download Models
		//TODO: Seperate downloads from URL here is a hack.  
		//Models should really be automatically downloaded from 
		//maven central as part of ctakes-*-res projects/artifacts via @grab.
		//Illustrative purposes until we have all of the *-res artifacts in maven central.
		downloadFile("http://svn.apache.org/repos/asf/ctakes/trunk/ctakes-core-res/src/main/resources/org/apache/ctakes/core/sentdetect/sd-med-model.zip","sd-med-model.zip");
		downloadFile("http://svn.apache.org/repos/asf/ctakes/trunk/ctakes-constituency-parser-res/src/main/resources/org/apache/ctakes/constituency/parser/models/sharpacq-3.1.bin","sharpacq-3.1.bin");

		//Build the pipeline to run
		AggregateBuilder aggregateBuilder = new AggregateBuilder();
		aggregateBuilder.add(AnalysisEngineFactory.createPrimitiveDescription(SimpleSegmentAnnotator.class));
		aggregateBuilder.add(AnalysisEngineFactory.createPrimitiveDescription(
            SentenceDetector.class,
            SentenceDetector.SD_MODEL_FILE_PARAM,
            "sd-med-model.zip"));
		aggregateBuilder.add(AnalysisEngineFactory.createPrimitiveDescription(TokenizerAnnotatorPTB.class));			
		aggregateBuilder.add(AnalysisEngineFactory.createPrimitiveDescription(
			ConstituencyParser.class,
			ConstituencyParser.PARAM_MODELFILE,
            "sharpacq-3.1.bin"));
		aggregateBuilder.add(AnalysisEngineFactory.createPrimitiveDescription(Writer.class));
		SimplePipeline.runPipeline(collectionReader, aggregateBuilder.createAggregate());

// Custom writer class used at the end of the pipeline to write results to screen
class Writer extends org.uimafit.component.JCasAnnotator_ImplBase {
  void process(JCas jcas) {
	//Get each Treebanknode and print out the text and it's parse string
    //select(jcas, TopTreebankNode).each { println "${it.treebankParse} "  }
    for(TopTreebankNode node : JCasUtil.select(jcas, TopTreebankNode.class)){
        println(node.getTreebankParse());
    }
  }
}

def downloadFile(String url, String filename) {
	System.out.println("Downloading: " + url);
	def file = new File(filename);
	if(file.exists()) {
	  System.out.println("File already exists:" + filename);
	  return;
	}
    def f = new FileOutputStream(url.tokenize("/")[-1])
    def out = new BufferedOutputStream(f)
    out << new URL(url).openStream()
    out.close()
}
