angular.module("app").controller("logoutController", ["$location", "authService", ($location, authService) ->
  authService.logout()
  $location.path("/").replace(true)
])
