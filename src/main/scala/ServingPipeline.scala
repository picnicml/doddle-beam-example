import breeze.linalg.DenseMatrix
import com.picnicml.doddlemodel.data.{Dataset, Simplex, Target, loadBreastCancerDataset}
import com.picnicml.doddlemodel.linear.LogisticRegression
import com.spotify.scio.ContextAndArgs
import com.spotify.scio.values.WindowOptions
import org.apache.beam.sdk.transforms.windowing.{AfterProcessingTime, Repeatedly}
import org.apache.beam.sdk.values.WindowingStrategy.AccumulationMode
import org.joda.time.Duration

object ServingPipeline {

  // e.g. replace this with loading a pretrained model from a GCS bucket
  val (x, y) = loadBreastCancerDataset
  val trainedModel: LogisticRegression = LogisticRegression().fit(x, y)

  def main(args: Array[String]): Unit = {
    val (scioContext, _) = ContextAndArgs(args)

    scioContext
      // e.g. replace this with a pubsub source to turn the pipeline into a streaming job
      .textFile(getClass.getResource("breast_cancer.csv").toString)

      // put all examples that arrive to the pipeline within the same 10 millisecond interval into
      // a batch (note that intervals are non-overlapping), i.e. emit data every 10 milliseconds,
      // triggering should be tuned for optimal throughput
      .withGlobalWindow(WindowOptions(
        trigger = Repeatedly.forever(AfterProcessingTime.pastFirstElementInPane().plusDelayOf(Duration.millis(10))),
        accumulationMode = AccumulationMode.DISCARDING_FIRED_PANES,
        allowedLateness = Duration.ZERO))

      // combine individual examples into a batch
      .combine(x => List(x))((combined, x) => x :: combined)(_ ++ _)
      .map(batch2DoddleModelDataset)

      // use vectorized predict and then split the batch back into individual examples
      .map(predictForBatch)
      .flatMap(splitBackToIndividualExamples)

      // print the result, e.g. replace this with a bigquery sink
      .map(printPredictionAndGroundTruth)
    scioContext.close()
  }

  def batch2DoddleModelDataset(rows: List[String]): Dataset = {
    println(s"processing a batch of ${rows.length} examples")
    val examples = DenseMatrix(rows.map(_.split(",").toList.map(_.toDouble)): _*)
    (examples(::, 0 to -2), examples(::, -1))
  }

  def predictForBatch(examples: Dataset): (Simplex, Target) =
    (trainedModel.predictProba(examples._1), examples._2)

  def splitBackToIndividualExamples(yPredY: (Simplex, Target)): List[(Double, Double)] = {
    val predictions = yPredY._1.toDenseVector
    val labels = yPredY._2
    (predictions.toScalaVector zip labels.toScalaVector).toList
  }

  def printPredictionAndGroundTruth(yPredY: (Double, Double)): Unit = {
    val prediction = yPredY._1
    val label = yPredY._2
    println(f"predicted probability: $prediction%1.4f --- label: $label")
  }
}
