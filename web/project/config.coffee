argv = require("yargs").argv

config =
  debug: not argv.dist
  src: "src/"
  dest: "target/"

module.exports = config
