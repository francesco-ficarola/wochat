#!/bin/bash
cd target/
if [ ! -d "WoChat" ]; then
	tar -xzf WoChat.tar.gz
fi
cd WoChat/
./startup.sh
