import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

public class NewQueryExpansion {
  QryResult qryResult;

  String query;

  TermVector tv;

  int fbDocs = 0; // Acceptable values are integers > 0. This value determines the number of
                  // documents to use for query expansion.

  int fbTerms = 0; // Acceptable values are integers > 0. This value determines the number of terms
                   // that are added to the query.

  int fbMu = 0;// Acceptable values are integers >= 0. This value determines the amount of smoothing
               // use to calculate p(r|d).

  double fbOrigWeight = 0;// Acceptable values are between 0.0 and 1.0. This value determines the
                          // weight on the original query. The weight on the expanded query is
                          // (1-fbOrigWeight).

  String smoothing;

  LinkedList<MyTerm> mergedList;

  HashMap<String, Double> dictionary = new HashMap<String, Double>();

  HashMap<String, Double> defaultScore = new HashMap<String, Double>();

  public NewQueryExpansion(String query, QryResult qryResult) {
    this.qryResult = qryResult;
    this.query = query;
    fbDocs = Integer.parseInt(QryEval.params.get("fbDocs"));
    fbTerms = Integer.parseInt(QryEval.params.get("fbTerms"));
    fbMu = Integer.parseInt(QryEval.params.get("fbMu"));
    fbOrigWeight = Double.parseDouble(QryEval.params.get("fbOrigWeight"));
    smoothing = QryEval.params.get("Indri:smoothing");
  }

  public String expandedQuery() throws IOException {
    for (int i = 0; i < fbDocs; i++) {
      // before every time,
      // give each term in the dictionary their default score.
      Set<String> terms = dictionary.keySet();
      Iterator<String> keyIte = terms.iterator();

      while (keyIte.hasNext()) {
        String key = keyIte.next();
        double score = dictionary.get(key);
        score += this.defaultScore.get(key) * this.qryResult.docScores.getDocidScore(i);
        dictionary.put(key, score);
      }
      System.gc();

      int docId = this.qryResult.docScores.getDocid(i);
      TermVector tv = new TermVector(docId, "body");
      for (int j = 1; j < tv.stemsLength(); j++) {// directly jump the stop words.
        // calculate the smoothing part.
        double mle = 0;
        if (smoothing.equals("ctf")) {
          mle = tv.totalStemFreq(j) * 1.0 / QryEval.READER.getSumTotalTermFreq("body");
        } else {
          mle = tv.stemDf(j) * 1.0 / QryEval.READER.getDocCount("body");
        }
        // calculate the scores.
        long docLength = QryEval.dlc.getDocLength("body", docId);
        double score = (tv.stemFreq(j) + fbMu * mle) * 1.0 / (fbMu + docLength);
        String term = tv.stemString(j);

        if (dictionary.containsKey(term)) {
          double newscore = dictionary.get(term) + (score - defaultScore.get(term))
                  * this.defaultScore.get(term) * this.qryResult.docScores.getDocidScore(i);
          dictionary.put(term, newscore);

        } else {
          dictionary.put(term, score);
          // calculate default score. The default score for the same term is
          // same across the whole corpus.
          double avg_len = QryEval.READER.getSumTotalTermFreq("body")
                  / (double) QryEval.READER.getDocCount("body");
          double defaultScore = fbMu * mle / (fbMu + avg_len);
          this.defaultScore.put(term, defaultScore);
        }
      }

    }
    // after we put all the terms. now we need to find the top scores.
    //
    Set<String> terms = dictionary.keySet();
    Iterator<String> keyIte = terms.iterator();
    LinkedList<MyTerm> topTerms = new LinkedList<MyTerm>();
    while (keyIte.hasNext()) {
      String term = keyIte.next();
      double score = dictionary.get(term);

      MyTerm myTerm = new MyTerm();
      myTerm.setTerm(term);
      myTerm.setScore(score);
      this.insertTermByScore(myTerm, topTerms, fbTerms);
    }
    // now we can conduct the new query string
    //
    String latter = "#WEIGHT(";
    for (int i = 0; i < topTerms.size(); i++) {
      latter += topTerms.get(i).getScore() + " " + topTerms.get(i).getTerm() + " ";
    }
    latter += ")";
    String whole = "#WEIGHT( " + fbOrigWeight + " " + query + " " + (1 - fbOrigWeight) + " "
            + latter + ")";
    return whole;
  }

  public void insertTermByScore(MyTerm myTerm, LinkedList<MyTerm> list, int sizeLimit) {
    if (list.size() < 1) {
      list.add(myTerm);
      if (list.size() > sizeLimit) {
        list.removeLast();
      }
      return;
    }

    for (int i = 0; i < list.size(); i++) {
      MyTerm t = list.get(i);
      if (t.getScore() < myTerm.getScore()) {
        list.add(i, myTerm);
        if (list.size() > sizeLimit) {
          list.removeLast();
        }
        return;
      }
    }
    // end of loop, the largest index.
    list.addLast(myTerm);
    // limit the size to sizeLimit
    if (list.size() > sizeLimit) {
      list.removeLast();
    }
  }

}
