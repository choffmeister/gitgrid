gulp = require("gulp")
config = require("../config")
gif = require("gulp-if")
uglify = require("gulp-uglify")
bower = require("main-bower-files")
filter = require("gulp-filter")

gulp.task "vendor", ->
  filters =
    js: filter("**/*.js")

  gulp.src(bower(), { base: "bower_components" })
    .pipe(filters.js)
    .pipe(gif(not config.debug, uglify({ preserveComments: "some" })))
    .pipe(filters.js.restore())
    .pipe(gulp.dest(config.dest + "vendor"))
