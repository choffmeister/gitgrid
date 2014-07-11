gulp = require("gulp")
gutil = require("gulp-util")
config = require("../config")
livereload = require("gulp-livereload")
jade = require("gulp-jade")

gulp.task "jade", ->
  gulp.src(config.src + "jade/**/*.jade")
    .pipe(jade(
      pretty: config.debug
    ))
    .on("error", (err) ->
      gutil.log(err.message)
      gutil.beep()
      this.end()
    )
    .pipe(gulp.dest(config.dest))
    .pipe(livereload({ auto: false }))
