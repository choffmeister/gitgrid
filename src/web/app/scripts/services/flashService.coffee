angular.module("app").factory("flashService", ["$rootScope", ($rootScope) ->
  $rootScope.flashMessages = []

  success: (title, message) -> $rootScope.flashMessages.push({ type: "success", title: title, message: message })
  info: (title, message) -> $rootScope.flashMessages.push({ type: "info", title: title, message: message })
  warning: (title, message) -> $rootScope.flashMessages.push({ type: "warning", title: title, message: message })
  error: (title, message) -> $rootScope.flashMessages.push({ type: "error", title: title, message: message })

  clear: () -> $rootScope.flashMessages.length = 0
])
