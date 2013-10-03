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
        String field = "";
        if (args.get(0).getClass().equals(QryopTerm.class)) {
          field = ((QryopTerm) args.get(0)).getField();
        } else if (args.get(0).getClass().equals(QryopNear.class)) {// near class
          field = ((QryopNear) args.get(0)).getField();
        } else {// UW class
          field = ((QryopUw) args.get(0)).getField();
        }
        int N = QryEval.READER.getDocCount(field);
         
        //int N = QryEval.READER.numDocs();
        
        int df = result.invertedList.df;
        int tf = result.invertedList.postings.get(i).tf;
        long doclen = QryEval.dlc.getDocLength(field, result.invertedList.postings.get(i).docid);
        double avg_doclen = QryEval.READER.getSumTotalTermFreq(field)
                / (double) QryEval.READER.getDocCount(field);

        int qtf = 1;// default for each term-by-term method. however, it might not be right if two
                    // same
                    // term appears in the query.

        float k1 = Float.parseFloat(QryEval.params.get("BM25:k_1"));
        float b = Float.parseFloat(QryEval.params.get("BM25:b"));
        float k3 = Float.parseFloat(QryEval.params.get("BM25:k_3"));

        float score = (float) (Math.log((N - df + 0.5) / (df + 0.5))
                * (tf / (tf + k1 * ((1 - b) + b * doclen / avg_doclen))) * (k3 + 1) * qtf / (k3 + qtf));
        // tf = 0, so its default score is zero, thus ignorable here
        result.docScores.add(result.invertedList.postings.get(i).docid, score);
      } else if (this.model == QryEval.INDRI) {
        // Indri here
        // Basically,
        // only Query terms & UW & NEAR would call the score method to calculate the scores here.
        // So it is OK to regard them as only scores here.
        // tf = 0, so its default score is zero, thus ignorable here
        float mu = Float.parseFloat(QryEval.params.get("Indri:mu"));
        float lambda = Float.parseFloat(QryEval.params.get("Indri:lambda"));
        String smoothing = QryEval.params.get("Indri:smoothing");
        int tf = result.invertedList.postings.get(i).tf;

        // calculate the MLE function.
        long doclen = 0;
        double avg_len;
        float mle = 0;
        String field = null;
        if (args.get(0).getClass().equals(QryopTerm.class)) {
          field = ((QryopTerm) args.get(0)).getField();
        } else if (args.get(0).getClass().equals(QryopNear.class)) {// near class
          field = ((QryopNear) args.get(0)).getField();
        } else {// UW class
          field = ((QryopUw) args.get(0)).getField();
        }
        
        if (smoothing.equals("df")) {
          mle = (float) (result.invertedList.df * 1.0 / QryEval.READER.getDocCount(field));
                  //QryEval.READER.getSumDocFreq(field));
       
        } else {
          mle = (float) (result.invertedList.ctf * 1.0 / QryEval.READER.getSumTotalTermFreq(field));
          
        }

        doclen = QryEval.dlc.getDocLength(field, result.invertedList.postings.get(i).docid);
        avg_len = QryEval.READER.getSumTotalTermFreq(field) * 1d
                / QryEval.READER.getDocCount(field);

        float score = (float) Math.log(lambda * (tf + mu * mle) / (doclen + mu) + (1 - lambda)
                * mle);

        result.defaultScore = (float) Math.log(lambda * (mu * mle) / (avg_len + mu) + (1 - lambda)
                * mle);
        /*
         * if(score>=50){ System.out.println("I met a big score"); }
         */
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
