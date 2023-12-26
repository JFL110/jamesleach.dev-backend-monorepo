FROM amazoncorretto:11

ADD digit-classification-container.jar .
ADD live-drawing-canvas-container.jar .
ADD location-container.jar .
ADD start.sh .
ADD version.json .
ADD globe-shapes ./globe-shapes

CMD ["bash", "start.sh"]