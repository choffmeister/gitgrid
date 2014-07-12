# GitGrid

## Development

~~~ bash
# run this once
$ sbt webAppToolsInit

# run this to start development server up
$ sbt webAppStart run

# run this to run backend tests
$ sbt test

# run this to run backend tests with coverage report
$ sbt jacoco:cover
~~~

## Packaging

~~~ bash
$ sbt clean webAppToolsInit webAppBuild pack
~~~
