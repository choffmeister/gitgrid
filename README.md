# GitGrid

## Development

~~~ bash
# run this once to install NPM and bower packages
$ sbt web/webAppInit

# run this to start development server up
$ sbt web/webAppStart server/run
~~~

## Testing

~~~ bash
# run this to run backend tests
$ sbt test

# run this to run backend tests with coverage report
$ sbt jacoco:cover
~~~

## Distributing

~~~ bash
# run this to clean and then package the whole application
$ sbt clean dist
~~~
