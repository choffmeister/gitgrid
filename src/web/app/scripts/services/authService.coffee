angular.module("app").service("authService", ["$http", "$rootScope", "storageService", "flashService", ($http, $rootScope, storageService, flashService) ->
  isAuthenticated: () ->
    storageService.get("session")?.isAuthenticated or false
  getBearerToken: () ->
    storageService.get("session")?.bearerToken or null
  getUser: () ->
    storageService.get("session")?.user or null

  login: (userName, password) ->
    credentials = btoa("#{userName}:#{password}")
    authHeader = { Authorization: "Basic #{credentials}"}
    $http.get("/api/auth/token/create", { headers: authHeader, preventErrorLogging: true })
      .success((res) =>
        token = res.access_token
        user = JSON.parse(atob(token)).data
        @setSession(token, user)
        flashService.success("Welcome, #{user.userName}!")
      )
  logout: () ->
    @unsetSession()
    flashService.success("Goodbye!")

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
    console.log(config)
    if authService.isAuthenticated() and not config.headers["Authorization"]?
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
      if ah? and ah.indexOf("Bearer ") == 0 and ah.indexOf("invalid_token") >= 0 and ah.indexOf("token expired") >= 0 and renewCounter < 1
          deferred = $q.defer()
          config = angular.extend(res.config, { renewCounter: renewCounter + 1 })
          $http = $injector.get("$http")
          $http.get("/api/auth/token/renew").then(deferred.resolve, deferred.reject)

          deferred.promise.then (res2) ->
            if res2.status == 200
              newToken = res2.data.access_token
              authService.setSession(newToken, authService.getUser())
              $http(res.config)
            else
              authService.unsetSession()
              flashService.error("Your session has ended")
      else if ah? and ah.indexOf("Bearer ") == 0 and ah.indexOf("invalid_token") >= 0 and ah.indexOf("token malformed") >= 0
        authService.unsetSession()
        flashService.error("Your session token is malformed")
        res.config.preventErrorLogging = true
        $q.reject(res)
      else if ah? and ah.indexOf("Bearer ") == 0 and ah.indexOf("invalid_token") >= 0
        authService.unsetSession()
        flashService.error("Your session has ended")
        res.config.preventErrorLogging = true
        $q.reject(res)
      else
        flashService.error("You are not allowed to access this route")
        res.config.preventErrorLogging = true
        $q.reject(res)
    else
      $q.reject(res)
])
