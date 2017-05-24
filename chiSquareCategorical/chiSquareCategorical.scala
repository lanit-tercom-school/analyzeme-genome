import org.apache.spark._
import com.datastax.spark.connector._
import org.apache.spark.sql._

import org.apache.spark.mllib.linalg._
import org.apache.spark.mllib.stat.Statistics
import org.apache.spark.mllib.stat.test.ChiSqTestResult
import org.apache.spark.rdd.RDD

object ChiSquareCategorical {
  def main(args: Array[String]) {
    val conf = new SparkConf().setAppName("Chi-square Categorical")
    val sc = new SparkContext(conf)

    // Input arguments are chromosome and snp positions range
    // Example: X 1234-5678
    val chrom = args(0).toString
    val snp_pos_first = args(1).split("-")(0).toInt
    val snp_pos_last = args(1).split("-")(1).toInt

    // Creating temporary view on input table to work with
    val spark = SparkSession.builder().getOrCreate()
    spark.sql(
	    """CREATE TEMPORARY VIEW input_view
	     USING org.apache.spark.sql.cassandra
	     OPTIONS (
	     table "input",
	     keyspace "genome",
	     cluster "Test Cluster",
	     pushdown "true")"""
		)
    // Getting a list of possible genotypes 
    val phenotypes = spark.sql("SELECT DISTINCT pheno FROM input_view").collect

    for (snp_pos <- snp_pos_first to snp_pos_last){

    	// Counting three snp types for each phenotype to fill the table for chisq-test 
	    var toTest = Array[Double]()
		for (item <- phenotypes){
			val pheno = item.getString(0)
			val snp0 = spark.sql(s"SELECT count(*) FROM input_view WHERE snp_pos = $snp_pos AND chrom = '$chrom' AND pheno = '$pheno' AND snp_value = 0").collect.head.getLong(0)
			val snp12 = spark.sql(s"SELECT count(*) FROM input_view WHERE snp_pos = $snp_pos AND chrom = '$chrom' AND pheno = '$pheno' AND (snp_value = 1 OR snp_value = 2)").collect.head.getLong(0)
			val snp3 = spark.sql(s"SELECT count(*) FROM input_view WHERE snp_pos = $snp_pos AND chrom = '$chrom' AND pheno = '$pheno' AND snp_value = 3").collect.head.getLong(0)
			toTest :+= snp0.toDouble
			toTest :+= snp12.toDouble
			toTest :+= snp3.toDouble
		}

		// Runnning the chisq-test and saving results to output table
		// Catching an exception for when some column or row in the table is all zeros (chisq-test can't be run in this case)
		val matrix: Matrix = Matrices.dense(phenotypes.size, 3, toTest)
		try {
			val result = Statistics.chiSqTest(matrix)
			val resultsMap = Map("pValue" -> result.pValue.toFloat, "statistic" -> result.statistic.toFloat, "degreesOfFreedom" -> result.degreesOfFreedom.toFloat)
			val toSave = sc.parallelize(Seq(("chi_square_categorical", chrom, snp_pos, resultsMap)))
			toSave.saveToCassandra("genome", "output", SomeColumns("test_type", "chrom", "snp_pos", "result"))
			println(result)
		} catch {
			case e: IllegalArgumentException => println(e.getMessage)
		}

	}

    sc.stop()
  }
}