if [[ -z "${LOCATION_MODE}" ]]; then
  echo "Starting Services"
  java -jar digit-classification-container.jar &
  java -jar live-drawing-canvas-container.jar &

  echo "Services started"
  wait < <(jobs -p)
  echo "Services stopped"
else
   echo "Starting Location Service"
   java -jar location-container.jar
fi