import org.apache.spark._
import com.datastax.spark.connector._
import java.util.Calendar
import java.text.SimpleDateFormat

case class Item(chrom:String, snp_pos:Integer, snp_value:Integer, ind:String, pheno:String)

object TSVImport {
  def main(args: Array[String]) {
    val conf = new SparkConf().setAppName("TSVImport")
    val sc = new SparkContext(conf)

    val input = sc.textFile(args(0))
    
    // Filtering out lines with more than one ref values (separated by commas)
    val snpOnly = input.map(_.split("\t")).filter(s => !s(4).contains(","))

    // Logging the others
    val notSnp = input.map(_.split("\t")).filter(s => s(4).contains(","))
    val time = Calendar.getInstance.getTime
    val timeFormat = new SimpleDateFormat("yy_MM_dd_hh_mm_ss_SSS")
    val log = "tsv_import_not_snp_log" + "--" + timeFormat.format(time)
    notSnp.map(s => s.mkString("\t")).saveAsTextFile(log)

    // Getting columns with chromosome, position and snp value respectively
    val filtered = snpOnly.map(s => s(0) + "\t" +  s(1) + "\t" + s(18))

    // Adding individual id and phenotype extracted from input file name
    // The file name is assumed as following: "$IND_$PHENO.tsv"
    val inputInfo = args(0).replaceAll("./", "").replaceAll(".tsv", "")
    val ind = inputInfo.split("_")(0)
    val pheno = inputInfo.split("_")(1)
    val filteredWithIndAndPheno = filtered.map(s => s + "\t" + ind + "\t" + pheno)

    // Encoding snp values in the following pattern:
    // 0/0 or 0|0 -> 0
    // 0/1 or 0|1 -> 1
    // 1/0 or 1|0 -> 2
    // 1/1 or 1|1 -> 3
    val complete = filteredWithIndAndPheno.
    	map(s => s.replaceAll("0[/,|]0", "0")).
    	map(s => s.replaceAll("0[/,|]1", "1")).
    	map(s => s.replaceAll("1[/,|]0", "2")).
    	map(s => s.replaceAll("1[/,|]1", "3"))

    val toSave = complete.map(s => s.split("\t")).collect{case Array (a,b,c,d,e) => Item(a, Integer.parseInt(b), Integer.parseInt(c), d, e)}
    toSave.saveToCassandra("genome", "input", SomeColumns("chrom", "snp_pos", "snp_value", "ind", "pheno"))

    sc.stop()
  }
}
 
