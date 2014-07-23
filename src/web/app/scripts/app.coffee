angular.module("app", ["ngAnimate", "ngRoute"])

angular.module("app").run(["authService", (authService) ->
  authService.checkState()
])
