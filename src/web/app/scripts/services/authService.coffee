angular.module("app").factory("authService", ["$http", "$rootScope", ($http, $rootScope) ->
  isAuthenticated: () ->
    $rootScope.user?
  getUser: () ->
    $rootScope.user

  login: (userName, password) ->
    $http.post("/api/auth/login", { user: userName, pass: password })
      .success((res) -> $rootScope.user = res.user)
  logout: () ->
    $http.post("/api/auth/logout", {})
      .success((res) -> $rootScope.user = null)
])
