import java.io.IOException;
import java.util.StringTokenizer;

/**
 * This class will read the query string and analyze the query according to the operator specifies
 * It also use the method borrowed from other QryEval.java to better explain it.
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
      if (!query.startsWith("(")) {
        query = "#OR(" + query + ")";
      } else
        query = "#OR" + query;
    }
    StringTokenizer st = new StringTokenizer(query, " ()", true);

    return parse(st, null, true);

  }

  public Qryop parse(StringTokenizer st, Qryop oprator, boolean isStart) throws IOException {

    Qryop next = null;
    while (st.hasMoreTokens()) {
      String token = st.nextToken();
      if (token.startsWith("#")) {
        if (token.equals("#AND")) {
          next = new QryopAnd();
        } else if (token.equals("#SYN")) {
          next = new QryopSyn();
        } else if (token.equals("#OR")) {
          next = new QryopOr();
          
        }/*else if(token.equals("#SCORE")){
          next = new QryopScore(); 
        }*/
        else if (token.contains("#NEAR")) {
          String[] paras= token.split("/");
          //System.out.println(paras[1]);
          next = new QryopNear(Integer.parseInt(paras[1]));
        }
        if (isStart) {
          oprator = next;
          continue;
        }
      } else if (token.equals("(")) {
        Qryop test = parse(st, next, false);
        if (!oprator.equals(test))
          oprator.args.add(test);

      } else if (token.equals(")")) {
        return oprator;
      } else {
        String[] analyzed = QryEval.tokenizeQuery(token);
        if (analyzed.length >= 1) {
          if (oprator == null) {
            oprator = new QryopOr();//set default to Or oprator.
          }
          if(analyzed[0].contains(".")){
            String[] newTerm =  analyzed[0].split("\\.");
            //if the query specifies the fields to run the query.
            oprator.args.add(new QryopTerm(newTerm[0],newTerm[1]));
          }else
            oprator.args.add(new QryopTerm(analyzed[0]));
         //System.out.println("I've add " + token); debug information.
        }
        continue;
      }

    }
    return oprator;

  }

  /*public static void main(String args[]) {
    QueryParser test = new QueryParser("test1 #AND(test2 test3) #OR(test4 test5) test6");
    try {
      Qryop ops = test.parse();
      int i = 0;
      System.out.println(ops.getClass());
      while (i < ops.args.size()) {
        System.out.println(ops.args.get(i).getClass());
        i++;
      }
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

  }*/

}
