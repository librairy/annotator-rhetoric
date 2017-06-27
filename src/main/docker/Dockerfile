FROM openjdk:8-jre-alpine
MAINTAINER Librairy <librairy.framework@gmail.com>
VOLUME /tmp
RUN mkdir /librairy
WORKDIR /librairy
ADD http://backingdata.org/dri/library/3.1/DRIresources-3.1.tar.gz /librairy/driresources.tar.gz
RUN set -x \
        && tar -zxvf driresources.tar.gz \
        && rm driresources.tar.gz \
        && chmod -R 777 /librairy/DRIresources-3.1
ADD DRIconfig.properties /librairy/DRIconfig.properties
ADD annotator-rethoric.jar /librairy/app.jar
#RUN sh -c 'touch /app.jar'
ENTRYPOINT exec java $JAVA_OPTS -server -jar /librairy/app.jar
