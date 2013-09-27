
import java.io.IOException;
import java.util.Vector;

/**
 * The NEAR/n oprator should have one additional field to indicate the number n.
 */

/**
 * @author jerry
 * 
 */
public class QryopNear extends Qryop {
  private int distance;

  public QryopNear(Qryop... q) {
    new QryopNear(0, q);
  }

  public QryopNear(int distance, Qryop... q) {
    this.setDistance(distance);
    for (int i = 0; i < q.length; i++)
      this.args.add(q[i]);
  }

  @Override
  public QryResult evaluate() throws IOException {
    /*
     * Starts with the first operator to have the result.
     */
    Qryop impliedQryOp = args.get(0);
    QryResult rResult = impliedQryOp.evaluate();
    //rResult.invertedList.print();

    for (int i = 1; i < args.size(); i++) {
      // InvList newList = new InvList();
      /* Starts the next operator */
      impliedQryOp = args.get(i);
      QryResult iResult = impliedQryOp.evaluate();
      // Compare two inverted List to generate one.
      int rIndex = 0;
      int iIndex = 0;
      InvList newList = new InvList();
      while (rIndex < rResult.invertedList.postings.size()
              && iIndex < iResult.invertedList.postings.size()) {
        
        int rDoc = rResult.invertedList.postings.get(rIndex).docid;
        int iDoc = iResult.invertedList.postings.get(iIndex).docid;
        if (rDoc < iDoc) {
          rIndex++;
        } else if (rDoc > iDoc) {
          int tf = iResult.invertedList.postings.get(iIndex).tf;
          iIndex++;
        } else {
          Vector<Integer> rList = rResult.invertedList.postings.get(rIndex).positions;
          Vector<Integer> iList = iResult.invertedList.postings.get(iIndex).positions;
          InvList.DocPosting matches = newList.new DocPosting(rDoc);

          int rL = 0, iL = 0;
          while (rL < rList.size() && iL < iList.size()) {

            int difference = iList.get(iL) - rList.get(rL);
            if (difference < 0) {
              iL++;
            } else if (difference <= distance) {
              matches.positions.add(iList.get(iL));
              iL++;
            } else {
              rL++;
            }
          }
          if(!matches.positions.isEmpty()){
            matches.tf = matches.positions.size();
            newList.postings.add(matches);
            newList.df++;
            newList.ctf+=matches.tf;
            }
          
          iIndex++;
          rIndex++;
        }
        
      }
      rResult.invertedList = newList;
      }

    //rResult.invertedList.print();
    return rResult;
  }

  public int getDistance() {
    return distance;
  }

  public void setDistance(int distance) {
    this.distance = distance;
  }

}
