# Распределенные вычисления статистик по генотипическим данным из 1000 Genomes Project

# Table of Contents
1. [Требования](#Требования)
2. [Сборка](#Сборка)
3. [Схема работы](#Схема-работы)

## Требования
Нужно установить: Apache Spark, Apache Cassandra, scala, sbt, vcflatten(собрать вручную, см. далее).

vcf-файлы можно взять здесь http://www.internationalgenome.org/data. Тестировались, например, эти два:
- ftp://ftp.1000genomes.ebi.ac.uk/vol1/ftp/pilot_data/release/2010_07/low_coverage/snps/CEU.low_coverage.2010_07.xchr.genotypes.vcf.gz
- ftp://ftp.1000genomes.ebi.ac.uk/vol1/ftp/pilot_data/release/2010_07/low_coverage/snps/YRI.low_coverage.2010_07.xchr.genotypes.vcf.gz)

Замечание: так как данные из 1000 Genomes анонимны, то все фенотипические данные для тестов, которые можно оттуда получить -- место сбора (трехбуквенный код в названии файла). Их мы и используем. Потому надо следить, чтобы vcf-файлы назывались по такому же шаблону, как и в примерах выше или же имели только фенотип в названии ("фенотип.vcf").

### Установка Scala
см. http://www.scala-sbt.org/
```
(using Homebrew on Mac)
$ brew install scala
$ brew install sbt
```
Проверить корректность установки можно командами
```
$ scala
Welcome to Scala 2.12.2 (Java HotSpot(TM) 64-Bit Server VM, Java 1.8.0_121).
Type in expressions for evaluation. Or try :help.

scala>
```
```
$ sbt
```
### Запуск Cassandra
Для начала необходимо скачать Dockerfile с дистрибутивом Cassandra, официальные образы можно найти здесь (там же находятся и инструкция для запуска) - https://hub.docker.com/_/cassandra/

После необходимо запустить Docker-container. Пример команды для запуска последней версии дистрибутива Кассандры с присвоением имени "cassandra":
```
$ docker run --name cassandra -d cassandra:latest
```
В результате должен быть запущен соответствующий контейнер. 
Доступ к командной строке кассандры при запущенном докер-образе:
```
$ docker run -it --link cassandra:cassandra --rm cassandra sh -c 'exec cqlsh "$CASSANDRA_PORT_9042_TCP_ADDR"'

Connected to Test Cluster at 172.17.0.2:9042.
[cqlsh 5.0.1 | Cassandra 3.10 | CQL spec 3.4.4 | Native protocol v4]
Use HELP for help.
```

### Запуск Spark
TBD

## Сборка
В корне проекта выполнить следующую команду:
`sbt package`

## Схема работы
.vcf-файл (1)-> .tsv-файлы в количестве числа индивидов (2)-> Cassandra DB (3)-> Apache Spark

Шаг (1) -- фильтрация .vcf-файла и разделение его на много .tsv-файлов(один на каждого индивида), которые можно будет потом удобно загрузить в Cassandra. На данный момент проводится с помощью инструмента vcfimp/vcflatten (https://github.com/innovativemedicine/vcfimp). Этот проект довольно давно уже неактивен, и страница с документацией у них лежит, но экспериментальным путем выяснено, что для его сборки нужно:
- Java 7 (!)
- в plugins.sbt добавить `resolvers += Resolver.url("bintray-sbt-plugins", url("http://dl.bintray.com/sbt/sbt-plugin-releases"))(Resolver.ivyStylePatterns)`
- там же `addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.9.2")`
- после этого выполнить команды, как у них написано в readme: `sbt`, `project vcflatten`, `dist` -- это сгенерит архив с jar-файлом.  

Запускать командой `java -jar vcflatten-assembly-0.7.0.jar /path/to/your/vcf --pattern "%s.%p"`.  
Впоследствии может быть заменен другим инструментом / переписан самостоятельно / оставлен как есть(работает же!).

Шаг (2) -- перекладывание .tsv-файлов в Cassandra, для более быстрого и удобного доступа и хранения. Каждый .tsv может обрабатываться отдельно, поэтому можно это делать на Spark параллельно. Сейчас реализован в виде TSVImport.scala. Его запускать на Spark так:  
`Your_spark_dir/bin/spark-submit --packages datastax:spark-cassandra-connector:2.0.1-s_2.11 --class "TSVImport" --master local /path/to/built/tsvimport/jar/file /path/to/tsv/file`

Шаг (3) представляет из себя вычисление на Spark различных статистик над данными, которые уже лежат в Cassandra, и размещение результатов в ней же. To be developed.
