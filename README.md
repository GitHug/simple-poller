# SimplePoller

This is a simple application using [Vert.x](https://vertx.io/). It allows users to add services to poll. These services will then be polled every 60 seconds, reporting either __OK__ or __FAIL__. The majority of the work has been done on the frontend. It does have a __VERY__ basic frontend that can be used to enter services and check their statuses. 

## API
The API documentation can be found at [SwaggerHub](https://app.swaggerhub.com/apis/takanoha/simple-poller/1.0.0).

## Start the application
```bash
./gradlew clean run
```

The application is running at `localhost:8080`.
