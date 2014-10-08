angular.module("app", ["ngRoute", "angular-loading-bar", "infinite-scroll", "hljs"])

angular.module("app").config(["$httpProvider", ($httpProvider) ->
  $httpProvider.interceptors.push("httpErrorLogger")
  $httpProvider.interceptors.push("authService.tokenRefresher")
  $httpProvider.interceptors.push("authService.tokenInjector")
])

angular.module("app").run(["$rootScope", ($rootScope) ->
  window.setTimeout(() ->
    $rootScope.$on("$routeChangeStart", () ->
      $("button.navbar-toggle:visible:not(.collapsed)").click()
    )
  , 0)
])

angular.module("app").run(["authService", (authService) ->
  authService.initSession()
])

angular.module("app").config(["hljsServiceProvider", (hljsServiceProvider) ->
  hljsServiceProvider.setOptions(
    tabReplace: '    '
    # disable auto language detection
    languages: []
  )
])
