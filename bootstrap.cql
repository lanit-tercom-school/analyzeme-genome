CREATE KEYSPACE IF NOT EXISTS genome
	WITH REPLICATION = {'class':'SimpleStrategy', 'replication_factor':1};
USE genome;

CREATE TABLE IF NOT EXISTS input(
	ind text,
	pheno text,
	chrom text,
	snp_pos int,
	snp_value int,
	PRIMARY KEY(ind, pheno, chrom, snp_pos)
) WITH CLUSTERING ORDER BY (pheno ASC, chrom ASC, snp_pos ASC);

CREATE TABLE IF NOT EXISTS output(
	test_type text,
	chrom text,
	snp_pos int,
	result frozen <map<text, float>>,
	PRIMARY KEY(snp_pos, chrom, result, test_type)
) WITH CLUSTERING ORDER BY (chrom ASC, result ASC, test_type ASC);
