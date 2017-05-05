import org.apache.spark._
import com.datastax.spark.connector._

object ChiSquareCategorical {
  def main(args: Array[String]) {
    val conf = new SparkConf().setAppName("Chi-square Categorical")
    val sc = new SparkContext(conf)



    sc.stop()
  }
}