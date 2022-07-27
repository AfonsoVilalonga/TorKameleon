FROM ubuntu:20.04

ENV TZ=Europe/Lisbon
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

RUN apt-get update && \
	apt-get upgrade -y 
	
RUN apt-get install -y stunnel4

RUN apt-get install -y torbrowser-launcher

RUN apt-get install -y openjdk-11-jdk-headless	

RUN apt-get install npm -y

RUN apt-get install -y wget && \
	wget -q https://dl.google.com/linux/direct/google-chrome-stable_current_amd64.deb && \
    apt-get install -y ./google-chrome-stable_current_amd64.deb

RUN apt-get install -y maven