import java.io.IOException;

/**
 * 
 */

/**
 * @author Jerry Sun This is the implementation of Or Operator. It simply refers to the And
 *         operator.
 * 
 */
public class QryopOr extends Qryop {

  /*
   * (non-Javadoc)
   * 
   * @see Qryop#evaluate()
   */
  /* let the operator accepts a number of arguments */
  public QryopOr(Qryop... q) {
    for (int i = 0; i < q.length; i++)
      this.args.add(q[i]);
  }

  @Override
  public QryResult evaluate() throws IOException {
    /*
     * Starts with the first operator to have the result.
     */
    Qryop impliedQryOp = new QryopScore(args.get(0));
    QryResult rResult = impliedQryOp.evaluate();
    for (int i = 1; i < args.size(); i++) {
      /* Starts the next operator */
      impliedQryOp = new QryopScore(args.get(i));
      QryResult iResult = impliedQryOp.evaluate();

      /* Combine the two result list into one. */
      // result.invertedList.insert(iResult.invertedList);
      int rIndex = 0; // Index of docs in the list
      int iIndex = 0;
      while (iIndex < iResult.docScores.scores.size() && rIndex < rResult.docScores.scores.size()) {
        int iDoc = iResult.docScores.getDocid(iIndex);
        int rDoc = rResult.docScores.getDocid(rIndex);
        int rDocNext = 0;
        boolean isLast= false;
        if (rIndex != rResult.docScores.scores.size()) {// rIndex is not the last in the list.
          rDocNext = rResult.docScores.getDocid(rIndex + 1);
        } else {
          rDocNext=rDoc;
          isLast=true;
        }
        
        if(iDoc >rDoc && iDoc <rDocNext){
          rResult.docScores.scores.add(rIndex+1, iResult.docScores.scores.get(iIndex));
        }else if(iDoc <= rDoc){
          iIndex++;  
        }else{
          if(isLast && iDoc > rDocNext){
            rResult.docScores.scores.add(iResult.docScores.scores.get(iIndex));  
          }
          rIndex++;
        }
      }
    }
    return rResult;
  }
}
