import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;

public class QueryExpansion {

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

  float defaultScore = 0; // this is the default score for all the terms that were not shown in the
                          // list.

  /*
   * String fbFile= The value is a string that contains the name of a file where your program should
   * write a copy of its expanded queries. The output file format is given below.
   */

  public QueryExpansion(String query, QryResult qryResult) {
    this.qryResult = qryResult;
    this.query = query;
    fbDocs = Integer.parseInt(QryEval.params.get("fbDocs"));
    fbTerms = Integer.parseInt(QryEval.params.get("fbTerms"));
    fbMu = Integer.parseInt(QryEval.params.get("fbMu"));
    fbOrigWeight = Double.parseDouble(QryEval.params.get("fbOrigWeight"));
    smoothing = QryEval.params.get("Indri:smoothing");
  }

  public String expandedQuery() throws IOException {
    // A default score is calculated before any work should be done.

    // read the docs of the results
    for (int i = 0; i < fbDocs; i++) {
      int docId = this.qryResult.docScores.getDocid(i);
      TermVector tv = new TermVector(docId, "body");
      LinkedList<MyTerm> MyTerms = new LinkedList<MyTerm>();

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
        MyTerm newMyTerm = new MyTerm();

        newMyTerm.setScore(score);
        newMyTerm.setTerm(tv.stemString(j));
        // calculate default score. The default score for the same term is
        // same across the whole corpus.

        double avg_len = QryEval.READER.getSumTotalTermFreq("body")
                / (double) QryEval.READER.getDocCount("body");
        double defaultScore = fbMu * mle / (fbMu + avg_len);
        newMyTerm.setDefaultScore(defaultScore);
        this.insertTerm(newMyTerm, MyTerms);
      }
      // now the MyTerms contain all the infos of one doc.
      // I should merge at this stage to avoid future complex operations.
      if (i == 0) {
        mergedList = MyTerms;
      } else {
        mergedList = mergeMyTermList(mergedList, MyTerms, i);
      }
    }
    //after calculate all the results, now we should take steps in finding
    //the top terms.
    LinkedList<MyTerm> topTerms= new LinkedList<MyTerm>();
    Iterator<MyTerm> iterator = mergedList.iterator();
    while(iterator.hasNext()){
        this.insertTermByScore(iterator.next(), topTerms, fbTerms);
    }
    //now we have all the top Terms with their scores.
    //we can re-construct the query now. 
    //
    String latter = "#WEIGHT(";
    for(int i=0; i<topTerms.size();i++){
       latter += topTerms.get(i).getScore()+" "+topTerms.get(i).getTerm()+" ";
    }
    latter +=")";
    String whole = "#WEIGHT( "+fbOrigWeight +" "+query+" "+ (1-fbOrigWeight)+" "+latter+")";
    return whole;
  }

  private LinkedList<MyTerm> mergeMyTermList(LinkedList<MyTerm> mergedList,
          LinkedList<MyTerm> MyTerms, int merged) {
    // Merge the list
    LinkedList<MyTerm> newList = new LinkedList<MyTerm>();
    int mIndex = 0;
    int tIndex = 0;
    while (mIndex < mergedList.size() && tIndex < MyTerms.size()) {

      String mTerm = mergedList.get(mIndex).getTerm();
      String tTerm = MyTerms.get(tIndex).getTerm();

      if (mTerm.compareTo(tTerm) > 0) {// not in the mergedList. Add it.
        MyTerm mt = MyTerms.get(tIndex);
        double score = mt.getScore();
        double defaultScore = mt.getDefaultScore();
        score += defaultScore * merged;
        mt.setScore(score);
        newList.add(mt);

        tIndex++;
      } else if (mTerm.compareTo(tTerm) == 0) {// if this the same MyTerm.
        MyTerm newMyTerm = new MyTerm();
        newMyTerm.setTerm(MyTerms.get(tIndex).getTerm());
        newMyTerm.setScore(mergedList.get(mIndex).getScore() + MyTerms.get(tIndex).getScore());
        newList.add(newMyTerm);
        tIndex++;
        mIndex++;
      } else {// m < t
        MyTerm mt = mergedList.get(mIndex);
        mt.setScore(mt.getScore() + mt.getDefaultScore());
        newList.add(mt);
        mIndex++;
      }
      // if the list has not reached the end.
      if (mIndex < mergedList.size()) {
        for (int j = mIndex; j < mergedList.size(); j++) {
          MyTerm mt = mergedList.get(j);
          mt.setScore(mt.getScore() + mt.getDefaultScore());
          newList.add(mt);
        }
      }
      if (tIndex < MyTerms.size()) {
        for (int j = tIndex; j < MyTerms.size(); j++) {
          MyTerm mt = mergedList.get(j);
          mt.setScore(mt.getScore() + mt.getDefaultScore());
          newList.add(mt);
        }
      }

    }
    return newList;
  }

  public void insertTerm(MyTerm myTerm, LinkedList<MyTerm> list) {
    // insert the MyTerm elements to the list.
    // sort by the index.
    // quick for later comparison and merge.
    if (list.size() < 1) {
      list.add(myTerm);
      return;
    }

    for (int i = 0; i < list.size(); i++) {
      MyTerm t = list.get(i);
      if (t.getTerm().compareTo(myTerm.getTerm()) > 0) {
        list.add(i, myTerm);
        return;
      }
    }
    // end of loop, the largest index.
    list.addLast(myTerm);
  }

  public void insertTermByScore(MyTerm myTerm, LinkedList<MyTerm> list, int sizeLimit) {
    if (list.size() < 1) {
      list.add(myTerm);
      return;
    }

    for (int i = 0; i < list.size(); i++) {
      MyTerm t = list.get(i);
      if (t.getScore() < myTerm.getScore() ) {
        list.add(i, myTerm);
        return;
      }
    }
    // end of loop, the largest index.
    list.addLast(myTerm);
    //limit the size to sizeLimit
    if(list.size() > sizeLimit){
      list.removeLast();
    }
  }
  
}
