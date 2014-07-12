gulp = require("gulp")
config = require("../config")
connect = require("connect")
rewrite = require("connect-modrewrite")
proxy = require("proxy-middleware")
url = require("url")

gulp.task "connect", (next) ->
  connect()
    .use("/api", proxy(url.parse("http://localhost:8080/api")))
    .use(rewrite(["!(\.(html|css|js|png|jpg|gif|ttf|woff|svg|eot))$ /index.html [L]"]))
    .use(connect.static(config.dest))
    .listen(config.port, next)
