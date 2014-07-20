angular.module("app").factory("authService", ["$rootScope", "restService", ($rootScope, restService) ->
  login: (userName, password) ->
    restService.login(userName, password).success((res) -> $rootScope.user = res.user)
  logout: () ->
    restService.logout().success((res) -> $rootScope.user = null)
])
