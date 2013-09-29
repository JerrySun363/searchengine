import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class QryopWeight extends Qryop {

  public List<Double> weight;

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
     * use the log to do this work This work similar with And operator only different that score
     * should be calculated with weight.
     */
    double totalWeight = 0;
    for (int i = 0; i < weight.size(); i++) {
      totalWeight += weight.get(i);
    }
    // Weight is used in INDRI operator.
    // WEIGHT should only operate on Score Lists.
    Qryop impliedQryOp = new QryopScore(args.get(0));
    QryResult result = impliedQryOp.evaluate();

    for (int i = 1; i < args.size(); i++) {
      Qryop newOP = new QryopScore(args.get(1));
      QryResult iResult = newOP.evaluate();
      // The following should work similar to #AND operator.
      // And it should only change the weight part. 
      int rDoc = 0; /* Index of a document in result. */
      int iDoc = 0; /* Index of a document in iResult. */

      while (rDoc < result.docScores.scores.size()) {

        // DIFFERENT RETRIEVAL MODELS IMPLEMENT THIS DIFFERENTLY.
        // Unranked Boolean AND. Remove from the incremental result any documents that weren't
        // returned by the i'th query argument.

        // Ignore documents matched only by the i'th query arg.
        while ((iDoc < iResult.docScores.scores.size())
                && (result.docScores.getDocid(rDoc) > iResult.docScores.getDocid(iDoc))) {
          iDoc++;
        }

        // If the rDoc document appears in both lists, keep it, otherwise discard it.
        if ((iDoc < iResult.docScores.scores.size())
                && (result.docScores.getDocid(rDoc) == iResult.docScores.getDocid(iDoc))) {
          if (QryEval.model == QryEval.INDRI) {//this line should be omitted.
            // sum log space
            if (i == 1) {
              // if it is the first one, put the scores in 1/n
              // since the scores use log space, thus we should use wi/W to give weight.
            result.docScores.setDocidScore(rDoc,
                    (float) (result.docScores.getDocidScore(rDoc) * weight.get(0)/ totalWeight));
            }
            float score = (float) (result.docScores.getDocidScore(rDoc) + weight.get(i)/totalWeight
                        * iResult.docScores.getDocidScore(iDoc));
            result.docScores.setDocidScore(rDoc, score);
          }
          rDoc++;
          iDoc++;
        } else {
          result.docScores.scores.remove(rDoc);
        }
      }
    }

    return result;

  }

}
