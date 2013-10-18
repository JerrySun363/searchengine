import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class QryopWeight extends Qryop {

  public List<Double> weight;
 
  private float totalScore = (float) 0.0;
  
  public QryopWeight() {
    this.weight = new LinkedList<Double>();
  }

  public QryopWeight(Qryop... q) {
    for (int i = 0; i < args.size(); i++) {
      this.args.add(q[i]);
    }
    this.weight = new LinkedList<Double>();
  }

  @Override
  public QryResult evaluate() throws IOException {
    /*
     * use the log to do this work This work similar with OR operator only different that score
     * should be calculated with weight.
     */
    if(args.size()==0){
      return new QryResult(); 
    }
    
    double totalWeight = 0;
    for (int i = 0; i < weight.size(); i++) {
      totalWeight += weight.get(i);
    }
    
    double accuWeight = 0;
    /*
     * Starts with the first operator to have the result.
     */
     
    Qryop impliedQryOp = new QryopScore(args.get(0));
    QryResult rResult = impliedQryOp.evaluate();
    accuWeight += weight.get(0);
    // initlize it with weighted list.
    for (int i = 0; i < rResult.docScores.scores.size(); i++) {
      rResult.docScores.setDocidScore(i, (float) (rResult.docScores.getDocidScore(i) * accuWeight));
    }
    totalScore +=rResult.defaultScore * weight.get(0);
    //System.out.println("default is "+rResult.defaultScore+" weight is"+ weight.get(0));
    
    /* Starts the next operator */   
    for (int i = 1; i < args.size(); i++) {
      impliedQryOp = new QryopScore(args.get(i));
      QryResult iResult = impliedQryOp.evaluate();
      /*new empty lists*/ 
      QryResult toReturn = new QryResult();

      /* Combine the two result list into one. */
      // result.invertedList.insert(iResult.invertedList);
      int rIndex = 0; // Index of docs in the list
      int iIndex = 0;
      while (iIndex < iResult.docScores.scores.size() && rIndex < rResult.docScores.scores.size()) {
        int iDoc = iResult.docScores.getDocid(iIndex);
        int rDoc = rResult.docScores.getDocid(rIndex);
        if (rDoc > iDoc) {
          // here for some articles that not showing in previous other query results.
          // apply default scores to all queries before.
          // Different from sum because default score is not zero here.
         
          float score = (float) ((totalScore+ iResult.docScores
                          .getDocidScore(iIndex) * this.weight.get(i)));
                 
          toReturn.docScores.add(iDoc, score);

          iIndex++;
        } else if (iDoc == rDoc) {
    
          float score= (float) (rResult.docScores.getDocidScore(rIndex) + weight.get(i) * iResult.docScores.getDocidScore(iIndex)); 
        
          toReturn.docScores.add(iDoc, score);
          
          iIndex++;
          rIndex++;
        } else {
          // TODO:apply an default score to the current rIndex since it doesn't show the iResult
          // list
          // here for some articles that not showing in iResult but in rResult.
          // apply default scores to this query.
          // Different from sum because default score is not zero here.
          float score = (float) (rResult.docScores.getDocidScore(rIndex) + iResult.defaultScore
                  * weight.get(i));          
          toReturn.docScores.add(rDoc, score);
          rIndex++;
        }

      }
      // for the original list
      if (rIndex < rResult.docScores.scores.size()) {
        for (int j = rIndex; j < rResult.docScores.scores.size(); j++) {
            toReturn.docScores.add(rResult.docScores.getDocid(j),(float) (rResult.docScores.getDocidScore(j) + iResult.defaultScore
                  * weight.get(i)));
        }
      }

      // for the new list
      if (iIndex < iResult.docScores.scores.size()) {
        for (int j = iIndex; j < iResult.docScores.scores.size(); j++) {
         
          toReturn.docScores.add(iResult.docScores.getDocid(j),
                  (float) ((totalScore + iResult.docScores.getDocidScore(j)
                          * weight.get(i))));
        }
      }
     
      totalScore += iResult.defaultScore * weight.get(i);
      accuWeight += weight.get(i);
      //System.out.println("default is "+iResult.defaultScore+" weight is "+ weight.get(i));

      rResult = toReturn;
    }
    
     
    rResult.defaultScore=(float) (totalScore/totalWeight);
    
    //System.out.println("default is "+rResult.defaultScore+" accuweight is "+ accuWeight);
    
    for(int i =0; i < rResult.docScores.scores.size();i++){
       rResult.docScores.setDocidScore(i, (float) (rResult.docScores.getDocidScore(i)/totalWeight));
    }
    
    return rResult;

  }
}
