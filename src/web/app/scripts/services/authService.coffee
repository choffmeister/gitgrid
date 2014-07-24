angular.module("app").factory("authService", ["$http", "$rootScope", "storageService", "flashService", ($http, $rootScope, storageService, flashService) ->
  isAuthenticated: () ->
    storageService.get("session")?.isAuthenticated or false
  getBearerToken: () ->
    storageService.get("session")?.bearerToken or null
  getUser: () ->
    storageService.get("session")?.user or null

  login: (userName, password) ->
    $http.post("/api/auth/login", { user: userName, pass: password })
      .success((res) => if res.user? and res.token?
        @setSession(res.token, res.user)
        flashService.success("Welcome, #{res.user.userName}!")
      )
  logout: () ->
    @unsetSession()
    flashService.clear()

  initSession: () ->
    if @isAuthenticated()
      @setSession(@getBearerToken(), @getUser())
  setSession: (bt, u) ->
    storageService.set("session",
      isAuthenticated: true
      user: u
      bearerToken: bt
    )
    $rootScope.user = u
  unsetSession: () ->
    storageService.set("session",
      isAuthenticated: false
      user: null
      bearerToken: null
    )
    $rootScope.user = null
])

angular.module("app").factory("authService.tokenInjector", ['$injector', ($injector) ->
  request: (config) ->
    authService = $injector.get("authService")
    if (authService.isAuthenticated())
      config.headers['Authorization'] = "Bearer #{authService.getBearerToken()}"
    config
])

angular.module("app").config(["$httpProvider", ($httpProvider) ->
  $httpProvider.interceptors.push("authService.tokenInjector")
])

angular.module("app").run(["authService", (authService) ->
  authService.initSession()
])
