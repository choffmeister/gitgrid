gulp = require("gulp")
gutil = require("gulp-util")
config = require("../config")
livereload = require("gulp-livereload")
rename = require("gulp-rename")
less = require("gulp-less")

gulp.task "less", ->
  gulp.src(config.src + "less/main.less")
    .pipe(less(
      compress: not config.debug
    ))
    .on("error", (err) ->
      gutil.log(err.message)
      gutil.beep()
      this.end()
    )
    .pipe(rename("style.css"))
    .pipe(gulp.dest(config.dest))
    .pipe(livereload({ auto: false }))
