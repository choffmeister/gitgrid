argv = require("yargs").argv

config =
  debug: not argv.dist
  src: "src/"
  dest: "target/"
  port: 9000

module.exports = config
