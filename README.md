# GitGrid

## Development

~~~ bash
# run this once to install NPM and bower packags
$ sbt webAppInit

# run this to start development server up
$ sbt webAppStart run
~~~

## Testing

~~~ bash
# run this to run backend tests
$ sbt test

# run this to run backend tests with coverage report
$ sbt jacoco:cover
~~~

## Packaging

~~~ bash
# run this to clean and then package the whole application
$ sbt clean pack
~~~
