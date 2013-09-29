/*
 *  Copyright (c) 2013, Carnegie Mellon University.  All Rights Reserved.
 */

import java.io.IOException;

public class QryopScore extends Qryop {

  /**
   * The SCORE operator accepts just one argument.
   * 
   * HW2 added. Added new parameter to accept new model. If not specified, it should reads the
   * Qry.model as its model
   */
  private int model;

  public QryopScore(Qryop q) {
    this.args.add(q);
    this.model = QryEval.model;
  }

  public QryopScore(Qryop q, int model) {
    this.args.add(q);
    this.model = model;
  }

  /**
   * Evaluate the query operator.
   */
  public QryResult evaluate() throws IOException {

    // Evaluate the query argument.
    QryResult result = args.get(0).evaluate();

    // Each pass of the loop computes a score for one document. Note: If the evaluate operation
    // above returned a score list (which is very possible), this loop gets skipped.
    for (int i = 0; i < result.invertedList.df; i++) {

      // DIFFERENT RETRIEVAL MODELS IMPLEMENT THIS DIFFERENTLY.
      // Unranked Boolean. All matching documents get a score of 1.0.
      // Ranked Boolean. All documents get a score of term frequency.
      // BM25: Implemented according to Lecture 7. Parameter should read directly from parameter
      // files.
      // Indri: Implemented according to Lecture 7. Parameter should read directly from parameter
      // files.

      // different methods have already been supported here.
      if (this.model == QryEval.RANKEDBOOLEAN) {
        result.docScores.add(result.invertedList.postings.get(i).docid,
                (float) result.invertedList.postings.get(i).tf);
      } else if (this.model == QryEval.UNRANKEDBOOLEAN) {
        result.docScores.add(result.invertedList.postings.get(i).docid, (float) 1.0);
      } else if (this.model == QryEval.BM25) {
        // BM25 to be implemented here.
        int N = QryEval.READER.getDocCount("body");
        int df = result.invertedList.df;
        int tf = result.invertedList.postings.get(i).tf;
        long doclen = QryEval.dlc.getDocLength("body", result.invertedList.postings.get(i).docid);
        double avg_doclen = QryEval.READER.getSumTotalTermFreq("body")
                / (double) QryEval.READER.getDocCount("bpdy");
        int qtf = 1;// default for each term-by-term method. however, it might be right if two same
                    // term appears in the query.

        float k1 = Float.parseFloat(QryEval.params.get("k_1"));
        float b = Float.parseFloat(QryEval.params.get("b"));
        float k3 = Float.parseFloat(QryEval.params.get("k_3"));

        float score = (float) (Math.log((N - df + 0.5) / (df + 0.5))
                * (tf / tf + k1 * ((1 - b) + b * doclen / avg_doclen)) * (k3 + 1) * qtf / (k3 + qtf));
        result.docScores.add(result.invertedList.postings.get(i).docid, score);
      } else {
        // Indri here
        // Basically,
        // only Query terms & UW & NEAR would call the score method to calculate the scores here.
        // So it is OK to regard them as
        float mu = Float.parseFloat(QryEval.params.get("mu"));
        float lambda = Float.parseFloat(QryEval.params.get("lambda"));
        String smoothing = QryEval.params.get("smoothing");
        int tf = result.invertedList.postings.get(i).tf;

        // calculate the MLE function.
        float mle = 0;
        if (smoothing.equals("df")) {
          if (args.get(0).getClass().equals(QryopTerm.class)) {
            mle = (float) (result.invertedList.df * 1.0 / QryEval.dlc
                    .getDocLength(((QryopTerm) args.get(0)).getField(),
                            result.invertedList.postings.get(i).docid));
          } else {
            mle = (float) (result.invertedList.df * 1.0 / QryEval.dlc.getDocLength("body",
                    result.invertedList.postings.get(i).docid));
          }

        } else {
          if (args.get(0).getClass().equals(QryopTerm.class)) {
            mle = (float) (result.invertedList.ctf * 1.0 / QryEval.READER
                    .getSumTotalTermFreq(((QryopTerm) args.get(0)).getField()));
          } else {
            mle = (float) (result.invertedList.ctf * 1.0 / QryEval.READER
                    .getSumTotalTermFreq("body"));
          }
        }
        long doclen = 0;

        if (args.get(0).getClass().equals(QryopTerm.class))
          doclen = QryEval.dlc.getDocLength(((QryopTerm) args.get(0)).getField(),
                  result.invertedList.postings.get(i).docid);
        else
          doclen = QryEval.dlc.getDocLength("body", result.invertedList.postings.get(i).docid);

        float score = (float) Math.log(lambda * (tf + mu * mle) / (doclen + mu) + (1 - lambda)
                * mle);
        result.docScores.add(result.invertedList.postings.get(i).docid, score);
      }

    }

    // The SCORE operator should not return a populated inverted list.
    // If there is one, replace it with an empty inverted list.
    if (result.invertedList.df > 0)
      result.invertedList = new InvList();

    return result;
  }

}
