/**
 * This class is used to store the term relevant 
 * information stored in the terms when do query 
 * expansion.
 * Only used for homework 3. 
 * */
public class MyTerm {
  
  private double score; //the score for the query, used for sorting(?)
  private String term; //the content of the term
  private double defaultScore;
 
  public double getScore() {
    return score;
  }
  public void setScore(double score) {
    this.score = score;
  }
  public String getTerm() {
    return term;
  }
  public void setTerm(String term) {
    this.term = term;
  }
  public double getDefaultScore() {
    return defaultScore;
  }
  public void setDefaultScore(double defaultScore) {
    this.defaultScore = defaultScore;
  }
  
  

}
