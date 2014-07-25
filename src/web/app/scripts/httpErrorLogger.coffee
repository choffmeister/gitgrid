angular.module("app").factory("httpErrorLogger", ["$q", "flashService", ($q, flashService) ->
  responseError: (res) ->
    flashService.error("There was an HTTP error!")
    $q.reject(res)
])
