import java.io.IOException;
import java.util.StringTokenizer;

/**
 * This class will read the query string and analyze the query according to the operator specifies
 * It also use the method borrowed from other QryEval.java to better explain it.
 * 
 * In homework 2, I must re-do the parser according to the different models in use.
 * 
 * @author jerry
 * 
 */

public class QueryParser {
  private String query;

  public QueryParser(String query) {
    this.query = query;
  }

  public QueryParser() {

  }

  public String getQuery() {
    return query;
  }

  public void setQuery(String query) {
    this.query = query;
  }

  public Qryop parse() throws IOException {
    if (!query.startsWith("#")) {
      String op ="";
      if (QryEval.model == QryEval.RANKEDBOOLEAN || QryEval.model == QryEval.UNRANKEDBOOLEAN)
         op="#OR"; 
      if (QryEval.model == QryEval.BM25)
         op="#SUM";
      if (QryEval.model == QryEval.INDRI)
        op="#AND";
      
      if (!query.startsWith("(")) {
        query = op+"(" + query + ")";
      } else
        query = op + query;
    }
    StringTokenizer st = new StringTokenizer(query, " ()", true);
    
    return parse(st, null, true);

  }

  public Qryop parse(StringTokenizer st, Qryop oprator, boolean isStart) throws IOException {

    Qryop next = null;
    boolean isScore = true;
    while (st.hasMoreTokens()) {
      String token = st.nextToken();
      if (token.startsWith("#")) {
        isScore = true;
        if (token.equals("#AND")) {
          if(QryEval.model==QryEval.INDRI){
            next = new QryopIndriAnd();
          }else
            next = new QryopAnd();
        } else if (token.equals("#SYN")) {
          next = new QryopSyn();
        } else if (token.equals("#OR")) {
          next = new QryopOr();
        } else if (token.equals("#SUM")) {
          next = new QryopSum();
        } else if (token.equals("#WEIGHT")) {
          next = new QryopWeight();
        }/**/
        else if (token.contains("#NEAR")) {
          String[] paras = token.split("/");
          // System.out.println(paras[1]);
          next = new QryopNear(Integer.parseInt(paras[1]));
        }
        if (isStart) {
          oprator = next;
          continue;
        }
      } else if (token.equals("(")) {
        Qryop test = parse(st, next, false);
        if (!oprator.equals(test) && test.args.size()>0)
          oprator.args.add(test);

      } else if (token.equals(")")) {
        return oprator;
      } else {

        if (oprator == null) {// set default according to different Operators
          if (QryEval.model == QryEval.RANKEDBOOLEAN || QryEval.model == QryEval.UNRANKEDBOOLEAN)
            oprator = new QryopOr();
          if (QryEval.model == QryEval.BM25)
            oprator = new QryopSum();
          if (QryEval.model == QryEval.INDRI)
            oprator = new QryopIndriAnd();
        }
        if(token.equals(" "))
          continue;
        //homework added here. Have a judge whether it is a QryopWeight
        //In that case, we should added weight into corresponding positions.
        //This increases the coupling, but decreases the
        
        if (oprator.getClass().equals(QryopWeight.class)&&isScore) {
           double score = Double.parseDouble(token);
           ((QryopWeight) oprator).weight.add(score);
            isScore = false;
            continue;
        }
        
        isScore = true;
        String[] newTerm = new String[2];
        if (token.contains(".")){
            newTerm = token.split("\\."); 
        }else{
            newTerm[0] = token;
            newTerm[1] = "body";
        }
      
        String[] analyzed = QryEval.tokenizeQuery(newTerm[0]);
        if (analyzed.length >= 1) {
            // if the query specifies the fields to run the query.
            //System.out.println(analyzed[0]);
            oprator.args.add(new QryopTerm(analyzed[0], newTerm[1]));
            // System.out.println("I've add " + token); debug information.
        }
        continue;
      }

    }
    return oprator;

  }

  /*
   * public static void main(String args[]) { QueryParser test = new
   * QueryParser("test1 #AND(test2 test3) #OR(test4 test5) test6"); try { Qryop ops = test.parse();
   * int i = 0; System.out.println(ops.getClass()); while (i < ops.args.size()) {
   * System.out.println(ops.args.get(i).getClass()); i++; } } catch (IOException e) { // TODO
   * Auto-generated catch block e.printStackTrace(); }
   * 
   * }
   */

}
