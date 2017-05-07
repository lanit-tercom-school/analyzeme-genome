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

    val snp_pos = args(0).toInt
    val chrom = args(1).toString

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
    val phenotypes = spark.sql("SELECT DISTINCT pheno FROM input_view").collect

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

    sc.stop()
  }
}