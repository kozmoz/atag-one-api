#!/bin/bash
#
# Sync to http://atag.one webserver.
#
rsync -a images javascripts index.html stylesheets vps6:/home/www/www.atag.one/http/
