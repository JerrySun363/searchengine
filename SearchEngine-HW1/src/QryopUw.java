import java.io.IOException;
import java.util.ArrayList;
import java.util.Vector;

public class QryopUw extends Qryop {
  private int distance;

  public int getDistance() {
    return distance;
  }

  public void setDistance(int distance) {
    this.distance = distance;
  }

  public QryopUw(Qryop... q) {
    new QryopNear(0, q);
  }

  public QryopUw(int distance, Qryop... q) {
    this.setDistance(distance);
    for (int i = 0; i < q.length; i++)
      this.args.add(q[i]);
  }

  @Override
  public QryResult evaluate() throws IOException {

    InvList[] results = new InvList[args.size()];
    InvList toreturn = new InvList();
    for (int i = 0; i < args.size(); i++) {
      results[i] = (args.get(i).evaluate()).invertedList;
    }
    int[] pointers = new int[args.size()];//pointers to record each position in postings
    int maxID = -1;     //max doc ID
    int index = 0;      //

    boolean sameDoc = false;

    reachEnd: while (true) {
      while (sameDoc != true) {
        if (results[index].postings.get(pointers[index]).docid > maxID) {
          /*
           * if it is larger than the current MAX DOC ID, it should be the new current max, and
           * start over from the beginning
           */
          maxID = results[index].postings.get(pointers[index]).docid;
          index = 0;
        } else if (results[index].postings.get(pointers[index]).docid < maxID) {
          // otherwise just moves pointer forward to match the articles
          pointers[index]++;
          if (pointers[index] >= results[index].postings.size()) {
            // it has reached an end of docs.
            break reachEnd;
          }

        } else {
          // If this reaches the end of index,
          // It shows that all the lists have reached the same documents.
          // Thus now we can compare the term locations of them
          if (index == args.size() - 1) {
            sameDoc = true;

          } else {
            index = (index + 1) % args.size();
          }
        }
      }

      /*
       * Since now all the inverted list are pointing the same docs at the moment, we can now
       * compare the term frequencies in these docs.
       */
      // initialize all the docs to at their first pos in the queue.
      int pos[] = new int[args.size()];
      ArrayList<Vector<Integer>> myPositions = new ArrayList<Vector<Integer>>();
      // put all the positions in the lists
      for (int i = 0; i < args.size(); i++) {
        myPositions.add(results[i].postings.get(pointers[i]).positions);
      }

      boolean hasMore = true;
      int maxPos = myPositions.get(0).get(0);
      int minPos = myPositions.get(0).get(0);
      int maxIndex = 0;
      int minIndex = 0;

      InvList.DocPosting matches = toreturn.new DocPosting(maxID);

      outer: while (true) {
        for (int x = 0; x < myPositions.size(); x++) {
          if (pos[x] >= myPositions.get(x).size()) {
            // one document has reached its end.
            // we should break this loop
            break outer;
          }

          int thisPos = myPositions.get(x).get(pos[x]);

          if (thisPos > maxPos) {
            maxPos = thisPos;
            maxIndex = x;
          } else if (thisPos < minPos) {
            minPos = thisPos;
            minIndex = x;
          }
        }
        if (maxPos - minPos <= this.distance) {
          // we got a match here.
          matches.positions.add(maxPos);

          for (int x = 0; x < pos.length; x++) {
            pos[x]++;// each should increment by 1
          }
        } else {
          /*
           * if we don't get a match here. pos[minPos]++ to let it move forward
           */
          pos[minPos]++;
        }
      }
      if (!matches.positions.isEmpty()) {
        matches.tf = matches.positions.size();
        toreturn.postings.add(matches);
        toreturn.df++;
        toreturn.ctf += matches.tf;
      }

    }

    QryResult result = new QryResult();
    result.invertedList = toreturn;
    return result;
  }

}
