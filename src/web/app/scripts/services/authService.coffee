angular.module("app").factory("authService", ["$http", "$rootScope", "storageService", "flashService", ($http, $rootScope, storageService, flashService) ->
  isAuthenticated: () ->
    storageService.get("session")?.isAuthenticated or false
  getBearerToken: () ->
    storageService.get("session")?.bearerToken or null
  getUser: () ->
    storageService.get("session")?.user or null

  login: (userName, password) ->
    $http.post("/api/auth/login", { userName: userName, password: password })
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
      bearerToken: bt
      user: u
    )
    $rootScope.user = u
  unsetSession: () ->
    storageService.set("session",
      isAuthenticated: false
      bearerToken: null
      user: null
    )
    $rootScope.user = null
])

angular.module("app").factory("authService.tokenInjector", ["$injector", ($injector) ->
  request: (config) ->
    authService = $injector.get("authService")
    if authService.isAuthenticated()
      config.headers["Authorization"] = "Bearer #{authService.getBearerToken()}"
    config.headers["X-WWW-Authenticate-Filter"] = "Bearer"
    config
])

angular.module("app").factory("authService.tokenRefresher", ["$injector", "$q", ($injector, $q) ->
  responseError: (res) ->
    authService = $injector.get("authService")
    flashService = $injector.get("flashService")
    if res.status == 401
      ah = res.headers("www-authenticate")
      renewCounter = res.config.renewCounter or 0
      if ah.indexOf("Bearer ") == 0 and ah.indexOf("invalid_token") >= 0 and ah.indexOf("token expired") >= 0 and renewCounter < 1
          deferred = $q.defer()
          config = angular.extend(res.config, { renewCounter: renewCounter + 1 })
          $http = $injector.get("$http")
          $http.get("/api/auth/renew").then(deferred.resolve, deferred.reject)

          deferred.promise.then (res2) ->
            if res2.status == 200
              newToken = res2.data.token
              authService.setSession(newToken, authService.getUser())
              $http(res.config)
            else
              authService.unsetSession()
              flashService.error("Your session has ended")
      else if ah.indexOf("Bearer ") == 0 and ah.indexOf("invalid_token") >= 0
        authService.unsetSession()
        flashService.error("Your session has ended")
        $q.reject(res)
      else
        $q.reject(res)
    else
      $q.reject(res)
])
