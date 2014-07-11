gulp = require("gulp")
config = require("../config")
livereload = require("gulp-livereload")

gulp.task "watch", ["build"], ->
  livereload.listen({ auto: true })
  gulp.watch config.src + "coffee/**/*.coffee", ["coffee"]
  gulp.watch config.src + "less/**/*.less", ["less"]
  gulp.watch config.src + "jade/**/*.jade", ["jade"]
  return
