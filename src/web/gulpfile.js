var argv = require('yargs').argv,
    bower = require('main-bower-files'),
    coffee = require('gulp-coffee'),
    concat = require('gulp-concat'),
    connect = require('connect'),
    exec = require('exec-sync'),
    gif = require('gulp-if'),
    gulp = require('gulp'),
    gutil = require('gulp-util'),
    ignore = require('gulp-ignore'),
    jade = require('gulp-jade'),
    less = require('gulp-less'),
    livereload = require('gulp-livereload'),
    manifest = require('gulp-manifest'),
    path = require('path'),
    proxy = require('proxy-middleware'),
    rename = require('gulp-rename'),
    replace = require('gulp-replace'),
    rewrite = require('connect-modrewrite'),
    templates = require('gulp-angular-templatecache'),
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
  port: argv.port || 9000,
  version: argv.dist ? exec('git describe') : 'dev'
};

gulp.task('jade-index', function () {
  return gulp.src(config.src('index.jade'))
    .pipe(jade({ pretty: config.debug }))
    .on('error', function (err) {
      gutil.log(err.message);
      gutil.beep();
      this.end();
    })
    .pipe(replace('%%version%%', config.version))
    .pipe(gulp.dest(config.dest()))
    .pipe(livereload({ auto: false }));
});

gulp.task('jade-other', ['jade-index'], function () {
  return gulp.src(config.src('**/*.jade'))
    .pipe(jade({ pretty: config.debug }))
    .on('error', function (err) {
      gutil.log(err.message);
      gutil.beep();
      this.end();
    })
    .pipe(templates('app/templates.js', { module: 'app', root: '/' }))
    .pipe(gif(!config.debug, uglify()))
    .pipe(gulp.dest(config.dest()))
    .pipe(livereload({ auto: false }));
});

gulp.task('jade', ['jade-index', 'jade-other']);

gulp.task('less', function () {
  return gulp.src(config.src('styles/main.less'))
    .pipe(less({ compress: !config.debug }))
    .on('error', function (err) {
      gutil.log(err.message);
      gutil.beep();
      this.end();
    })
    .pipe(rename('app/styles.css'))
    .pipe(gulp.dest(config.dest()))
    .pipe(livereload({ auto: false }));
});

gulp.task('coffee', function () {
  return gulp.src(config.src('**/*.coffee'))
    .pipe(coffee({ bare: false }))
    .on('error', function (err) {
      gutil.log(err);
      gutil.beep();
      this.end();
    })
    .pipe(concat('app/app.js'))
    .pipe(gif(!config.debug, uglify()))
    .pipe(gulp.dest(config.dest()))
    .pipe(livereload({ auto: false }));
});

gulp.task('vendor-scripts', function () {
  return gulp.src([
      config.src('../bower_components/jquery/dist/jquery.js'),
      config.src('../bower_components/lodash/dist/lodash.js'),
      config.src('../bower_components/bootstrap/dist/js/bootstrap.js'),
      config.src('../bower_components/angular/angular.js'),
      config.src('../bower_components/angular-animate/angular-animate.js'),
      config.src('../bower_components/angular-highlightjs/angular-highlightjs.js'),
      config.src('../bower_components/highlightjs/highlight.pack.js'),
      config.src('../bower_components/angular-loading-bar/build/loading-bar.js'),
      config.src('../bower_components/angular-route/angular-route.js'),
      config.src('../bower_components/cryptojslib/rollups/md5.js')
    ])
    .pipe(concat('app/vendor.js'))
    .pipe(gif(!config.debug, uglify({ preserveComments: 'some' })))
    .pipe(gulp.dest(config.dest()));
});

gulp.task('vendor-assets', function () {
  return gulp.src(bower(), { base: 'bower_components' })
    .pipe(gulp.dest(config.dest('app/assets')));
});

gulp.task('vendor', ['vendor-scripts', 'vendor-assets']);

gulp.task('watch', ['compile'], function () {
  livereload.listen({ auto: true });
  gulp.watch(config.src('**/*.coffee'), ['coffee']);
  gulp.watch(config.src('**/*.less'), ['less']);
  gulp.watch(config.src('**/*.jade'), ['jade']);
});

gulp.task('connect', function (next) {
  connect()
    .use('/api', proxy(url.parse('http://localhost:8080/api')))
    .use(rewrite(['!(^/app/) /index.html [L]']))
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
