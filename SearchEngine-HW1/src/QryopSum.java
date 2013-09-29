import java.io.IOException;

public class QryopSum extends Qryop {
  //since the sum operator has quite simiar behaviour like OR.
  //I will use modified OR operator to do the calculating
  //In this way, I saved the energy to do the loop.
  
  private QryopOr sum; ;
  public QryopSum(){
    sum = new QryopOr();
  }
  
  public QryopSum(Qryop... q) {
    sum = new QryopOr();
    for (int i = 0; i < args.size(); i++) {
      this.sum.args.add(q[i]);
    }
  }

  @Override
  public QryResult evaluate() throws IOException {
         QryResult result = sum.evaluate();
    return result;
  }

}
