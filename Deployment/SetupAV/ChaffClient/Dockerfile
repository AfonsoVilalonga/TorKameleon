FROM ubuntu:20.04

ENV TZ=Europe/Lisbon
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

RUN apt-get update && \
	apt-get upgrade -y 

RUN apt-get install -y openjdk-11-jdk-headless	

RUN apt-get install -y torbrowser-launcher

COPY torrc /etc/tor/torrc