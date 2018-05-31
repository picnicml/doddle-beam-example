## doddle-beam-example
An example of how to serve a trained [doddle-model](https://github.com/picnicml/doddle-model) in a pipeline implemented with [Apache Beam](https://beam.apache.org) and [Scio](https://github.com/spotify/scio). It includes batching of individual examples for faster vectorized predictions. Code is available [here](src/main/scala/ServingPipeline.scala).

Run the pipeline with:
```scala
sbt run --runner=DirectRunner
```
An example output:
```
processing a batch of 72 examples
processing a batch of 72 examples
processing a batch of 71 examples
processing a batch of 71 examples
predicted probability: 0.9991 --- label: 1.0
predicted probability: 0.9901 --- label: 1.0
predicted probability: 0.0000 --- label: 0.0
predicted probability: 0.9998 --- label: 1.0
predicted probability: 0.9235 --- label: 1.0
predicted probability: 0.9990 --- label: 1.0
predicted probability: 0.9951 --- label: 1.0
predicted probability: 0.0070 --- label: 0.0
predicted probability: 0.0012 --- label: 0.0
predicted probability: 0.9997 --- label: 1.0
...
```

### Setup
To run the examples locally you will need to publish a local snapshot version of the repository:
```
git clone https://github.com/picnicml/doddle-model.git
cd doddle-model
sbt publishLocal
```
Ensure the published version matches the version contained within the `project/Dependencies.scala` file.

### Resources
The breast cancer dataset is from [UCI Machine Learning Repository. Irvine, CA: University of California, School of Information and Computer Science, Dua, D. and Karra Taniskidou, E.](http://archive.ics.uci.edu/ml)
