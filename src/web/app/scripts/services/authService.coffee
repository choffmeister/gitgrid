angular.module("app").factory("authService", ["$http", "$rootScope", "flashService", ($http, $rootScope, flashService) ->
  $http.get("/api/auth/state")
    .success((res) -> $rootScope.user = res.user if res.user?)

  isAuthenticated: () ->
    $rootScope.user?
  getUser: () ->
    $rootScope.user

  login: (userName, password) ->
    $http.post("/api/auth/login", { user: userName, pass: password })
      .success((res) ->
        $rootScope.user = res.user
        flashService.success("Welcome, #{res.user.userName}!")
      )
  logout: () ->
    $http.post("/api/auth/logout", {})
      .success((res) ->
        $rootScope.user = null
        flashService.clear()
      )
])
