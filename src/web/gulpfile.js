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
    manifest = require('gulp-manifest'),
    path = require('path'),
    proxy = require('proxy-middleware'),
    rename = require('gulp-rename'),
    replace = require('gulp-replace'),
    rewrite = require('connect-modrewrite'),
    uglify = require('gulp-uglify'),
    url = require('url');

var config = {
  debug: !argv.dist,
  src: function (p) {
    return path.join('app', p || '');
  },
  dest: function (p) {
    return path.join(argv.target || 'target', p || '');
  },
  port: argv.port || 9000
};

gulp.task('jade', function () {
  return gulp.src(config.src('**/*.jade'))
    .pipe(jade({ pretty: config.debug }))
    .on('error', function (err) {
      gutil.log(err.message);
      gutil.beep();
      this.end();
    })
    .pipe(gulp.dest(config.dest()))
    .pipe(livereload({ auto: false }));
});

gulp.task('less', function () {
  return gulp.src(config.src('styles/main.less'))
    .pipe(less({ compress: !config.debug }))
    .on('error', function (err) {
      gutil.log(err.message);
      gutil.beep();
      this.end();
    })
    .pipe(rename('styles/main.css'))
    .pipe(gulp.dest(config.dest()))
    .pipe(livereload({ auto: false }));
});

gulp.task('coffee', function () {
  return gulp.src(config.src('scripts/**/*.coffee'))
    .pipe(coffee({ bare: false }))
    .on('error', function (err) {
      gutil.log(err);
      gutil.beep();
      this.end();
    })
    .pipe(concat('scripts/app.js'))
    .pipe(gif(!config.debug, uglify()))
    .pipe(gulp.dest(config.dest()))
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
    .pipe(gulp.dest(config.dest('vendor')));
});

gulp.task('watch', ['compile'], function () {
  livereload.listen({ auto: true });
  gulp.watch(config.src('scripts/**/*.coffee'), ['coffee']);
  gulp.watch(config.src('styles/**/*.less'), ['less']);
  gulp.watch(config.src('**/*.jade'), ['jade']);
});

gulp.task('connect', function (next) {
  connect()
    .use('/api', proxy(url.parse('http://localhost:8080/api')))
    .use(rewrite(['!(\.(html|css|js|png|jpg|gif|ttf|woff|svg|eot|manifest))$ /index.html [L]']))
    .use(connect.static(config.dest()))
    .listen(config.port, next)
});

gulp.task('manifest-include', ['compile'], function () {
  return gulp.src(config.dest('index.html'))
    .pipe(replace('<html', '<html manifest="/cache.manifest"'))
    .pipe(gulp.dest(config.dest()));
});

gulp.task('manifest-generate', ['manifest-include'], function () {
  return gulp.src(config.dest('**/*'))
    .pipe(manifest({
      filename: 'cache.manifest',
      exclude: 'cache.manifest',
      hash: true,
      timestamp: false,
      preferOnline: false
    }))
    .pipe(gulp.dest(config.dest()));
});

gulp.task('compile', ['coffee', 'less', 'jade', 'vendor']);
gulp.task('build', ['compile', 'manifest-include', 'manifest-generate']);
gulp.task('default', ['compile', 'connect', 'watch']);
