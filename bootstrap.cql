CREATE KEYSPACE IF NOT EXISTS genome
	WITH REPLICATION = {'class':'SimpleStrategy', 'replication_factor':1};
USE genome;

CREATE TABLE IF NOT EXISTS input(
	ind text,
	pheno text,
	chrom text,
	snp_pos int,
	snp_value int,
	PRIMARY KEY(snp_pos, chrom, pheno, snp_value, ind)
) WITH CLUSTERING ORDER BY (chrom ASC, pheno ASC, snp_value ASC, ind ASC);

CREATE TABLE IF NOT EXISTS output(
	test_type text,
	chrom text,
	snp_pos int,
	result frozen <map<text, float>>,
	PRIMARY KEY(snp_pos, chrom, result, test_type)
) WITH CLUSTERING ORDER BY (chrom ASC, result ASC, test_type ASC);
