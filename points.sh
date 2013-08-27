hadoop fs -rm -skipTrash points.txt
awk -f points.awk | hadoop fs -put - points.txt