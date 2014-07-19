angular.module("app").controller("logoutController", ["$location", "authService", ($location, authService) ->
  authService.logout().then(() -> $location.path("/").replace(true))
])
