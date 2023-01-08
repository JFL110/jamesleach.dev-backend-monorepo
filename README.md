# JamesLeach.dev Backend Monorepo
Monorepo for demo projects at [jamesleach.dev](https://jamesleach.dev).

The frontend is written in React and the repository is [here](https://github.com/JFL110/jamesleach.dev).

[![Test](https://github.com/JFL110/jamesleach.dev-backend-monorepo/actions/workflows/test.yml/badge.svg)](https://github.com/JFL110/jamesleach.dev-backend-monorepo/actions/workflows/test.yml)\
[![Push to ECR & Deploy to Staging](https://github.com/JFL110/jamesleach.dev-backend-monorepo/actions/workflows/ecr-push-staging-deploy.yml/badge.svg)](https://github.com/JFL110/jamesleach.dev-backend-monorepo/actions/workflows/ecr-push-staging-deploy.yml)\
[![Promote to Production & Deploy](https://github.com/JFL110/jamesleach.dev-backend-monorepo/actions/workflows/promote-to-production.yml/badge.svg)](https://github.com/JFL110/jamesleach.dev-backend-monorepo/actions/workflows/promote-to-production.yml)

## Project structure

This repository contains three demo projects, all written in Kotlin and built with Gradle. 
All the demos use Spring and reuse common code through Gradle subprojects.

The Gradle configuration for each subproject is kept simple by using [custom Gradle plugins](https://github.com/JFL110/jamesleach.dev-backend-monorepo/tree/main/buildSrc/src/main/groovy)
, rather than parent buildscripts, to standardise how projects representing applications or shared libraries are built.
This approach means that splitting this monorepo into multiple repositories would require very minimal refactoring,
as the plugins and shared dependencies could simply be published as artifacts and consumed in the same way they're consumed currently.

### Locations
 
This demo takes location data from a number of sources including Google Takeout, photo EXIF data and manually input.
Locations are 'digested' by stripping timestamps and simply recording if a 'square' of the globe has been visited, 
where squares are equally sized areas in the latitude/longitude space
(this does mean that squares have different actual sizes depending on their distance from the equator, but it keeps the calculations simple).
The result of this digest is uploaded to S3 and read by the
frontend statically, so displaying this data essentially costs nothing. 

Vector data describing the borders of countries is used to produce an estimate of how many 'land squares' exist (around 54 million)
and have been visited (significantly less than 54 million).

Try it [here](https://www.jamesleach.dev/travel-map).\
Service status page [here](https://o4zjbqbsp5.execute-api.eu-west-2.amazonaws.com/main/production/8092/status).

### ML Digit Recognition

This demo exposes a [simple neural network](https://github.com/JFL110/jamesleach.dev-backend-monorepo/blob/main/neural/digit-classification/src/main/kotlin/dev/jamesleach/neural/mnist/MNistFeedForward.kt) 
trained on the MNIST hand-drawn digit dataset as an API.
The network is trained by a CLI application and uploaded to S3. An ECS service loads the network and
provides an endpoint that accepts an image in raster format and returns the computed probabilities
that the image represents one of the digits 0 - 9. 

The aim of the demo is to show how machine learning could be incorporated into an application using
standard AWS components. Networks can be trained, versioned and consumed as artifacts.
New networks could be verified and rolled out using A/B deployments, without the need to update code or
redeploy the services that consume the artifacts.

Try it [here](https://www.jamesleach.dev/ml-digit).\
Service status page [here](https://o4zjbqbsp5.execute-api.eu-west-2.amazonaws.com/main/production/8091/status).

### Websocket Drawing Canvas

This demo uses websockets and a simple in-memory datastore to allow multiple users to view and edit a
drawing canvas in real time.

Try it [here](http://d1kzdlgex69htr.cloudfront.net/random).\
Service status page [here](https://o4zjbqbsp5.execute-api.eu-west-2.amazonaws.com/main/production/8093/status).

## Infrastructure

The frontend and backends are hosted in AWS. To reduce costs, the backend services are 
bundled into a single Docker image which runs three Java applications 
(not something to be done for a production-grade application).
This container runs in ECS and sits behind an application load balancer, which is accessed via an API Gateway.

Data is stored in DynamoDB and S3.
Cloudwatch Events are used to trigger routine tasks, such as producing a digest of location data.

## Deployment

GitHub Actions is used for CI and CD.

The services are packaged into a Docker image which is pushed to ECR.
This image is promoted to a testing environment, and after manual verification,
promoted to the production environment.

## Environments

### Staging

The staging environment is served [here](https://d2k1hseid387ot.cloudfront.net).
To reduce costs, the staging backend is normally turned off.

### Production

The production environment is served at [jamesleach.dev](https://jamesleach.dev).
