import java.io.IOException;


public class QryopIndriAnd extends Qryop {
  
  private QryopWeight qryWeight= new QryopWeight();
  
  public QryopIndriAnd(Qryop... q) {
    
    for (int i = 0; i < q.length; i++){
      this.args.add(q[i]);
     }
  }
  
  @Override
  public QryResult evaluate() throws IOException {
    // TODO Indri And performs like Common Or operator.
    //It shoud be regarded as an special term of #Weight operator with each term has the same query weight.
    for (int i = 0; i < this.args.size(); i++){
      qryWeight.args.add(this.args.get(i));
      qryWeight.weight.add(1d);
     }
    
    return this.qryWeight.evaluate();
  }

}
