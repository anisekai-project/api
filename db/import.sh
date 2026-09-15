#!/usr/bin/env sh
rm dump.sql
echo "Dumping the remote database..."
mariadb-dump --host=192.168.1.3 --port=10112 --user=anisekai -p anisekai > dump.sql
echo "Importing the database locally..."
mariadb --host=127.0.0.1 --port=3306 --user=anisekai -p anisekai < dump.sql
