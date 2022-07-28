FROM ubuntu:20.04

ENV TZ=Europe/Lisbon
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

RUN apt-get update && \
	apt-get upgrade -y 

RUN apt-get install -y stunnel4

COPY stunnel.conf /etc/stunnel/stunnel.conf