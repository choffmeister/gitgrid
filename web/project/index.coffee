fs = require("fs")
path = require("path")
gulp = require("gulp")

# find all tasks
tasks = fs.readdirSync("./project/tasks").filter((file) -> /\.(js|coffee)$/i.test(file))

# include all tasks
tasks.forEach((task) -> require("./tasks/" + task))

gulp.task "build", ["coffee", "less", "jade", "vendor"]
gulp.task "default", ["build", "connect", "watch"]
