#!/bin/bash
#
# Sync to http://atag.one webserver.
#
rsync -a images javascripts index.html stylesheets vps9:/home/www/www.atag.one/http/
