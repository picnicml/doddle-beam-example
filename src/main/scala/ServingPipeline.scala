import breeze.linalg.DenseMatrix
import com.picnicml.doddlemodel.data.{Dataset, Simplex, Target, loadBreastCancerDataset}
import com.picnicml.doddlemodel.linear.LogisticRegression
import com.picnicml.doddlemodel.syntax.ClassifierSyntax._
import com.spotify.scio.ContextAndArgs
import com.spotify.scio.values.WindowOptions
import org.apache.beam.sdk.transforms.windowing.{AfterFirst, AfterPane, AfterProcessingTime, Repeatedly}
import org.apache.beam.sdk.values.WindowingStrategy.AccumulationMode
import org.joda.time.Duration

object ServingPipeline {

  private val minBatchElements = 10
  private val maxBatchWaitTimeInMillis = 50

  // e.g. replace this with loading a pretrained model from a GCS bucket
  val (x, y) = loadBreastCancerDataset
  val trainedModel: LogisticRegression = LogisticRegression().fit(x, y)

  def main(args: Array[String]): Unit = {
    val (scioContext, _) = ContextAndArgs(args)

    // a composite trigger that fires whenever the pane has at least 'minBatchElements' elements
    // or after 'maxBatchWaitTimeInMillis', whatever happens first
    val groupedWithinTrigger = Repeatedly.forever(AfterFirst.of(
      AfterPane.elementCountAtLeast(minBatchElements),
      AfterProcessingTime.pastFirstElementInPane().plusDelayOf(Duration.millis(maxBatchWaitTimeInMillis))
    ))

    scioContext
      // e.g. replace this with a pubsub source to turn the pipeline into a streaming job
      .textFile(getClass.getResource("breast_cancer.csv").toString)

      .withGlobalWindow(WindowOptions(
        trigger = groupedWithinTrigger,
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
