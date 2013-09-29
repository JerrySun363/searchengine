/*
 *  This software illustrates the architecture for the portion of a
 *  search engine that evaluates queries.  It is a template for class
 *  homework assignments, so it emphasizes simplicity over efficiency.
 *  It implements an unranked Boolean retrieval model, however it is
 *  easily extended to other retrieval models.  For more information,
 *  see the ReadMe.txt file.
 *
 *  Copyright (c) 2013, Carnegie Mellon University.  All Rights Reserved.
 */

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.apache.lucene.analysis.Analyzer.TokenStreamComponents;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

public class QryEval {

  static String usage = "Usage:  java " + System.getProperty("sun.java.command") + " paramFile\n\n";

  /**
   * The index file reader is accessible via a global variable. This isn't great programming style,
   * but the alternative is for every query operator to store or pass this value, which creates its
   * own headaches.
   * Homework 2 notes.
   * Also I introduce some other parameters here to show which method is used here.
   * This modifies several parameters.
   *   
   * */
  public static IndexReader READER;
  
  public static DocLengthStore dlc;

  private static Scanner queryReader = null;
   
  public static Map<String, String> params;
  
  
  //public static PrintStream out = null;
  public static final int BM25= 0;
  public static final int INDRI=1;
  public static final int RANKEDBOOLEAN=2;
  public static final int UNRANKEDBOOLEAN=3;

  public static int model;
  
  public static EnglishAnalyzerConfigurable analyzer = new EnglishAnalyzerConfigurable(
          Version.LUCENE_43);
  static {
    analyzer.setLowercase(true);
    analyzer.setStopwordRemoval(true);
    analyzer.setStemmer(EnglishAnalyzerConfigurable.StemmerType.KSTEM);
  }

  /**
   * 
   * @param args
   *          The only argument should be one file name, which indicates the parameter file.
   * @throws Exception
   */
  public static void main(String[] args) throws Exception {

    // must supply parameter file
    if (args.length < 1) {
      System.err.println(usage);
      System.exit(1);
    }

    // read in the parameter file; 
    //one parameter per line in format of key=value
    //set it global for future scoring use.
    params = new HashMap<String, String>();
    Scanner scan = new Scanner(new File(args[0]));
    String line = null;
    do {
      line = scan.nextLine();
      String[] pair = line.split("=");
      params.put(pair[0].trim(), pair[1].trim());
    } while (scan.hasNext());

    // parameters required for this example to run
    if (!params.containsKey("indexPath")) {
      System.err.println("Error: Parameters were missing.");
      System.exit(1);
    }

    // open the index
    READER = DirectoryReader.open(FSDirectory.open(new File(params.get("indexPath"))));
    
    dlc= new DocLengthStore(READER);
    
    //determine the retrieveal model.
    String rmodel = params.get("retrievalAlgorithm");
    if(rmodel.equals("UnrankedBoolean"))
       model= UNRANKEDBOOLEAN;
    else if(rmodel.equals("RankedBoolean"))
      model= RANKEDBOOLEAN;
    else if(rmodel.equals("Indri"))
      model= UNRANKEDBOOLEAN;
    else if(rmodel.equals("BM25"))
      model= BM25;
    else{
       System.out.println("Unsupported model.");
       System.exit(0);
    }
    
    queryReader = new Scanner(new File(params.get("queryFilePath")));
    //out = new PrintStream(params.get("outPath"));

    if (READER == null) {
      System.err.println(usage);
      System.exit(1);
    }

    // The index is open. Start evaluating queries. The examples below show the query tree that
    // should be created for each query.
    //
    // Modify me so that you read queries from a file, parse it, and form the query tree
    // automatically.
    //String[] newTerm =  "espn.sport".split(".");
    
    
    QueryParser qp = new QueryParser();
      long start=System.currentTimeMillis();
      int i = 0;
      do {
      line = queryReader.nextLine(); 
      String[] term = line.split(":"); 
      String id = term[0]; 
      String query = term[1];
      qp.setQuery(query); 
      QryResult result = qp.parse().evaluate();
      formatPrintResults(id, result);
      
      } while (queryReader.hasNext() );
    

    /*String query = "#NEAR/800(asparagus broccoli)";
    qp.setQuery(query);
    Qryop test0 = qp.parse();
    //formatPrintResults("test0", test0.evaluate());
    printResults("test0", new QryopScore(test0).evaluate());
    out.println("------------------------------------");

    query = "#AND(asparagus broccoli)";
    qp.setQuery(query); 
    Qryop test = qp.parse();
    formatPrintResults("test1", test.evaluate());
    printResults("test1", test.evaluate());
    out.println("------------------------------------");
    
    qp.setQuery("#AND(asparagus)");
    Qryop test2 = qp.parse();
    //formatPrintResults("test2", test2.evaluate());
    printResults("test2", test2.evaluate());
    
    out.println("------------------------------------");
    qp.setQuery("#AND(broccoli)");
    Qryop test3 = qp.parse();
    //formatPrintResults("test3", test3.evaluate());
    printResults("test3", test3.evaluate());*/
    //System.out.println(System.currentTimeMillis()-start);
    
    //out.flush();
    //out.close();
    /*
     * Used for debug test.evaluate();
     * 
     * int i=0; System.out.println(test.getClass()); while(i<test.args.size()){
     * System.out.println(test.args.get(i).getClass()); i++; }
     * printResults("#AND (aparagus broccoli cauliflower #SYN(peapods peas))", test.evaluate());
     */

    /*
     * printResults("asparagus", (new QryopScore(new
     * QryopTerm(tokenizeQuery("asparagus")[0]))).evaluate());
     */
    /*
     * formatPrintResults("test", (new QryopScore(new
     * QryopTerm(tokenizeQuery("asparagus")[0]))).evaluate()); /* printResults("broccoli", (new
     * QryopScore( new QryopTerm(tokenizeQuery("broccoli")[0]))).evaluate());
     * 
     * printResults("cauliflower", (new QryopScore( new
     * QryopTerm(tokenizeQuery("cauliflower")[0]))).evaluate());
     * 
     * printResults("pea", (new QryopScore( new QryopTerm(tokenizeQuery("pea")[0]))).evaluate());
     * 
     * printResults("peas", (new QryopScore( new QryopTerm(tokenizeQuery("peas")[0]))).evaluate());
     * 
     * printResults("peapods", (new QryopScore( new
     * QryopTerm(tokenizeQuery("peapods")[0]))).evaluate());
     * 
     * printResults("#AND (broccoli cauliflower)", (new QryopAnd( new
     * QryopTerm(tokenizeQuery("broccoli")[0]), new
     * QryopTerm(tokenizeQuery("cauliflower")[0]))).evaluate());
     * 
     * printResults("#AND (peas peapods)", (new QryopAnd( new QryopTerm(tokenizeQuery("peas")[0]),
     * new QryopTerm(tokenizeQuery("peapods")[0]))).evaluate());
     * 
     * printResults("#SCORE (#SCORE (#AND (peas peapods)))", (new QryopScore( (new QryopScore( (new
     * QryopAnd( new QryopTerm(tokenizeQuery("peas")[0]), new
     * QryopTerm(tokenizeQuery("peapods")[0]))))))).evaluate());
     */

  }

  /**
   * Get the external document id for a document specified by an internal document id. Ordinarily
   * this would be a simple call to the Lucene index reader, but when the index was built, the
   * indexer added "_0" to the end of each external document id. The correct solution would be to
   * fix the index, but it's too late for that now, so it is fixed here before the id is returned.
   * 
   * @param iid
   *          The internal document id of the document.
   * @throws IOException
   */
  static String getExternalDocid(int iid) throws IOException {
    Document d = QryEval.READER.document(iid);
    String eid = d.get("externalId");

    if ((eid != null) && eid.endsWith("_0"))
      eid = eid.substring(0, eid.length() - 2);

    return (eid);
  }

  /**
   * Prints the query results.
   * 
   * THIS IS NOT THE CORRECT OUTPUT FORMAT. YOU MUST CHANGE THIS METHOD SO THAT IT OUTPUTS IN THE
   * FORMAT SPECIFIED IN THE HOMEWORK PAGE, WHICH IS:
   * 
   * QueryID Q0 DocID Rank Score RunID
   * 
   * @param queryName
   *          Original query.
   * @param result
   *          Result object generated by {@link Qryop#evaluate()}.
   * @throws IOException
   */
  static void printResults(String queryName, QryResult result) throws IOException {

    System.out.println(queryName + ":  ");
    //out.println(queryName + ":  ");
    if (result.docScores.scores.size() < 1) {
      System.out.println("\tNo results.");
      //out.println("\tNo results.");
    } else {
      for (int i = 0; i < result.docScores.scores.size(); i++) {
        System.out.println("\t" + i + ":  " + getExternalDocid(result.docScores.getDocid(i)) + ", "
                + result.docScores.getDocidScore(i));
       // out.println("\t" + i + ":  " + getExternalDocid(result.docScores.getDocid(i)) + ", "
                //+ result.docScores.getDocidScore(i));
      }
    }

  }

  /**
   * This print should sort the QryResult 1st according to their scores then by their; secondly by
   * their internal ID.
   * 
   * Formatted Prints the query results.
   * 
   * THIS IS NOT THE CORRECT OUTPUT FORMAT. YOU MUST CHANGE THIS METHOD SO THAT IT OUTPUTS IN THE
   * FORMAT SPECIFIED IN THE HOMEWORK PAGE, WHICH IS:
   * 
   * QueryID Q0 DocID Rank Score RunID
   * 
   * @param id
   *        the id of the query
   * @param result
   *          Result object generated by {@link Qryop#evaluate()}.
   * @throws IOException
   *
   */
  static void formatPrintResults(String id, QryResult result) throws IOException {
     //System.out.println("printing... Id:"+id);
    // QueryID Q0 DocID Rank Score RunID
    if (result.docScores.scores.size() < 1) {
      // System.out.println("\tNo results.");
      // System.out.printf("%s Q0 dummy 1 run-1", id);
      System.out.printf("%s Q0 dummy 1 0 run-1\n", id);

    } else {
      ScoreList inner = sortList(result.docScores);

      for (int i = 0; i < inner.scores.size(); i++) {
        // QueryID Q0 DocID Rank Score RunID
        // System.out.printf("%s Q0 %s %d %.1f run-1\n", id, getExternalDocid(inner.getDocid(i)),
        // (i + 1), inner.getDocidScore(i));
        if(i == 100)
          break;//at most 100 output.
        System.out.printf("%s Q0 %s %d %.1f run-1\n", id, getExternalDocid(inner.getDocid(i)), (i + 1),
                inner.getDocidScore(i));
      }
    }

  }

  private static ScoreList sortList(ScoreList input) {
    ScoreList output = new ScoreList();
    
    out: for (int i = 0; i < input.scores.size(); i++) {
      float score = input.getDocidScore(i);
      int id = input.getDocid(i);

      for (int j = 0; j < output.scores.size(); j++) {
        if (score > output.getDocidScore(j)) {
          output.scores.add(j, input.scores.get(i));
          
          if(output.scores.size()>100)
            output.scores.remove(100);
          
          continue out;
        } else if (score == output.getDocidScore(j)) {
          if (id < output.getDocid(j)) {
            output.scores.add(j, input.scores.get(i));
            if(output.scores.size()>100)
              output.scores.remove(100);
            
            continue out;
          }
        }
      }
      output.scores.add(input.scores.get(i));
      if(output.scores.size()>100)
        output.scores.remove(100);
    }
    return output;
  }

  /**
   * Given a query string, returns the terms one at a time with stopwords removed and the terms
   * stemmed using the Krovetz stemmer.
   * 
   * Use this method to process raw query terms.
   * 
   * @param query
   *          String containing query
   * @return Array of query tokens
   * @throws IOException
   */
  static String[] tokenizeQuery(String query) throws IOException {

    TokenStreamComponents comp = analyzer.createComponents("dummy", new StringReader(query));
    TokenStream tokenStream = comp.getTokenStream();

    CharTermAttribute charTermAttribute = tokenStream.addAttribute(CharTermAttribute.class);
    tokenStream.reset();

    List<String> tokens = new ArrayList<String>();
    while (tokenStream.incrementToken()) {
      String term = charTermAttribute.toString();
      tokens.add(term);
    }
    return tokens.toArray(new String[tokens.size()]);
  }

}
