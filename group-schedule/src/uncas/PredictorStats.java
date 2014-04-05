package uncas;

/* For Homework #3 you will compare a variety of branch predictors.
 * You should implement all of the predictors in the SolutionSimulator.
 * For each predictor you implement, you will maintain the statistics
 * for that predictor with an instance of THIS class.
 */
public class PredictorStats {
    
    long total_predictions;
    long total_correct_predictions;
    double correct_prediction_rate;
    String predictorType = "";
    String heuristic = "";
    
    public PredictorStats(String predictorType, String heuristic) {
        total_predictions = 0;
        total_correct_predictions = 0;
        correct_prediction_rate = 0.0;
        this.predictorType = predictorType;
        this.heuristic = heuristic;
    }
    
    public long totalIncorrect() {
        return total_predictions - total_correct_predictions;
    }
    
    public void updateStat(boolean predictedCorrectly) {
        if (predictedCorrectly)
            total_correct_predictions++;
        total_predictions++;
    } 
    
    // could do this in the previous method, but that's a lot of extra divides...
    public void calculateRate(){
        correct_prediction_rate = total_correct_predictions/(double)total_predictions;
    }       
    
    public void print() {
        System.out.format("%s Predictor, predicting %s. Prediction Accuracy: %.2f%% (Mis-prediction rate: %.2f%%)\n", 
                          predictorType, heuristic, correct_prediction_rate*100.0, (1-correct_prediction_rate)*100.0);
    }
}