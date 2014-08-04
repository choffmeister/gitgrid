angular.module("app").controller("showUserController", ["$scope", "$data", ($scope, $data) ->
  $scope.user = $data.user.data
  $scope.projects = $data.projects.data
])

angular.module("app").factory("showUserController$Data", ["restService", (restService) -> ($routeParams) ->
  user: restService.retrieveUser($routeParams.userName)
  projects: restService.listProjectsForOwner($routeParams.userName)
])
