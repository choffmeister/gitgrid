var argv = require('yargs').argv,
    bower = require('main-bower-files'),
    coffee = require('gulp-coffee'),
    concat = require('gulp-concat'),
    connect = require('connect'),
    filter = require('gulp-filter'),
    gif = require('gulp-if'),
    gulp = require('gulp'),
    gutil = require('gulp-util'),
    jade = require('gulp-jade'),
    less = require('gulp-less'),
    livereload = require('gulp-livereload'),
    proxy = require('proxy-middleware'),
    rename = require('gulp-rename'),
    rewrite = require('connect-modrewrite'),
    uglify = require('gulp-uglify'),
    url = require('url');

var config = {
  debug: !argv.dist,
  src: 'src/',
  dest: 'target/',
  port: 9000
};

gulp.task('jade', function () {
  return gulp.src(config.src + 'jade/**/*.jade')
    .pipe(jade({ pretty: config.debug }))
    .on('error', function (err) {
      gutil.log(err.message);
      gutil.beep();
      this.end();
    })
    .pipe(gulp.dest(config.dest))
    .pipe(livereload({ auto: false }));
});

gulp.task('less', function () {
  return gulp.src(config.src + 'less/main.less')
    .pipe(less({ compress: !config.debug }))
    .on('error', function (err) {
      gutil.log(err.message);
      gutil.beep();
      this.end();
    })
    .pipe(rename('style.css'))
    .pipe(gulp.dest(config.dest))
    .pipe(livereload({ auto: false }));
});

gulp.task('coffee', function () {
  return gulp.src(config.src + 'coffee/**/*.coffee')
    .pipe(coffee({ bare: false }))
    .on('error', function (err) {
      gutil.log(err);
      gutil.beep();
      this.end();
    })
    .pipe(concat('app.js'))
    .pipe(gif(!config.debug, uglify()))
    .pipe(gulp.dest(config.dest))
    .pipe(livereload({ auto: false }));
});

gulp.task('vendor', function () {
  var filters = {
    js: filter('**/*.js')
  };

  return gulp.src(bower(), { base: 'bower_components' })
    .pipe(filters.js)
    .pipe(gif(!config.debug, uglify({ preserveComments: 'some' })))
    .pipe(filters.js.restore())
    .pipe(gulp.dest(config.dest + 'vendor'));
});

gulp.task('watch', ['build'], function () {
  livereload.listen({ auto: true });
  gulp.watch(config.src + 'coffee/**/*.coffee', ['coffee']);
  gulp.watch(config.src + 'less/**/*.less', ['less']);
  gulp.watch(config.src + 'jade/**/*.jade', ['jade']);
});

gulp.task('connect', function (next) {
  connect()
    .use('/api', proxy(url.parse('http://localhost:8080/api')))
    .use(rewrite(['!(\.(html|css|js|png|jpg|gif|ttf|woff|svg|eot))$ /index.html [L]']))
    .use(connect.static(config.dest))
    .listen(config.port, next)
});

gulp.task('build', ['coffee', 'less', 'jade', 'vendor']);
gulp.task('default', ['build', 'connect', 'watch']);
