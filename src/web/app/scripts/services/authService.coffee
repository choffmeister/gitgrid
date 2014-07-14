angular.module("app").factory("authService", ["$rootScope", "$http", ($rootScope, $http) ->
  login: (userName, password) ->
    $http.post("/api/auth/login", { user: userName, pass: password })
      .success((res) -> $rootScope.user = res.user)

  logout: () ->
    $http.post("/api/auth/logout")
      .success((res) -> $rootScope.user = null)
])
