import java.io.IOException;

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

  public QryResult evaluate() throws IOException {
    return evaluate(false);
  }

  public QryResult evaluate(boolean isAnd) throws IOException {
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
        if (iDoc < rDoc) {
          rResult.docScores.scores.add(rIndex, iResult.docScores.scores.get(iIndex));
          iIndex++;
         //rIndex++;

        } else if (iDoc == rDoc) {
          if (QryEval.model == QryEval.RANKEDBOOLEAN) {
            rResult.docScores.setDocidScore(
                    rIndex,
                    Math.max(rResult.docScores.getDocidScore(rIndex),
                            iResult.docScores.getDocidScore(iIndex)));
          } else if (QryEval.model == QryEval.BM25 && isAnd) {
            // if some term doesn't appear in the list,
            // then its score should not be changed, since its term frequency is zero
            // and the score it gets in the list where it doesn't show up is also zero.
            // this implements BM25's sum function.
            // To distinguish from the BM25's OR (although not implemented here...)
            // Add a boolean to distinguish that.
            rResult.docScores.setDocidScore(rIndex, rResult.docScores.getDocidScore(rIndex)
                    + iResult.docScores.getDocidScore(iIndex));
          }

          iIndex++;
          rIndex++;
        } else {
          rIndex++;
        }
      }

      if (iIndex < iResult.docScores.scores.size()) {
        for (int j = iIndex; j < iResult.docScores.scores.size(); j++) {
          rResult.docScores.scores.add(iResult.docScores.scores.get(j));
        }
      }

    }
    return rResult;
  }

}
